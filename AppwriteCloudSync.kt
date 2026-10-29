package ru.momnotes.app

import android.content.Context
import io.appwrite.models.InputFile
import io.appwrite.Permission
import io.appwrite.Query
import io.appwrite.Role
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.UUID

/** Private, per-account sync. Local copies remain as an offline cache. */
internal object AppwriteCloudSync {
    private const val DATABASE_ID = "6ada25dd003223b3eae2"
    private const val TABLE_ID = "6ada261c0010964abc6c"
    private const val BUCKET_ID = "6ada2f6c0000b1d070a1"

    suspend fun sync(context: Context, localNotes: List<Note>): List<Note> = withContext(Dispatchers.IO) {
        val ownerId = AppwriteService.account.get().id
        val prefs = context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE)
        val migrationOwner = prefs.getString("cloud_migration_owner", null)
        val ownerLocalNotes = when {
            migrationOwner == null || migrationOwner == ownerId -> localNotes
            else -> readAccountCache(context, ownerId)
        }
        val remote = mutableListOf<Note>()
        var offset = 0
        do {
            val page = AppwriteService.tables.listRows(
                databaseId = DATABASE_ID,
                tableId = TABLE_ID,
                queries = listOf(Query.limit(100), Query.offset(offset))
            )
            page.rows.forEach { row ->
                if (row.data["ownerId"] == ownerId) remote += row.data.toNote(row.id)
            }
            offset += page.rows.size
        } while (offset < page.total && page.rows.isNotEmpty())

        val merged = (ownerLocalNotes + remote)
            .groupBy { it.id }
            .map { (_, versions) -> mergeVersions(versions) }
            .sortedBy { it.createdAt }
            .map { note -> restoreLocalFiles(context, note) }
        val remoteIds = remote.mapTo(mutableSetOf()) { it.id }
        val localOnlyCount = merged.count { it.id !in remoteIds }
        require(remote.isNotEmpty() || merged.isEmpty() || localOnlyCount == merged.size) {
            "Не удалось прочитать облачные заметки: локальная копия сохранена, перенос не отмечен завершённым."
        }

        val rowPermissions = listOf(
            Permission.read(Role.user(ownerId)),
            Permission.update(Role.user(ownerId)),
            Permission.delete(Role.user(ownerId))
        )
        val filePermissions = rowPermissions
        val verifiedFilesKey = "appwrite_verified_files_$ownerId"
        val verifiedFileIds = prefs.getStringSet(verifiedFilesKey, emptySet())?.toMutableSet() ?: mutableSetOf()
        val preparedNotes = merged.map { note -> uploadLocalFiles(note, ownerId, filePermissions, verifiedFileIds) }
        prefs.edit().putStringSet(verifiedFilesKey, verifiedFileIds).apply()
        preparedNotes.forEach { note ->
            AppwriteService.tables.upsertRow(
                databaseId = DATABASE_ID,
                tableId = TABLE_ID,
                rowId = note.id,
                data = note.toCloudData(ownerId),
                permissions = rowPermissions
            )
        }
        if (migrationOwner == null) prefs.edit().putString("cloud_migration_owner", ownerId).apply()
        // Keep the local cache and local file paths for offline display.
        preparedNotes
    }

    private fun readAccountCache(context: Context, ownerId: String): List<Note> =
        try {
            val raw = context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE)
                .getString(accountNotesKey(ownerId), "[]") ?: "[]"
            val array = org.json.JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                val rows = item.optJSONArray("tables") ?: org.json.JSONArray()
                val tables = (0 until rows.length()).mapNotNull { i ->
                    val table = rows.optJSONObject(i) ?: return@mapNotNull null
                    val cellRows = table.optJSONArray("cells") ?: org.json.JSONArray()
                    val cells = (0 until cellRows.length()).map { r ->
                        val row = cellRows.optJSONArray(r) ?: org.json.JSONArray()
                        (0 until row.length()).map { col -> row.optString(col) }
                    }
                    NoteTable(id = table.optString("id"), cells = cells)
                }
                fun stringList(key: String): List<String> {
                    val values = item.optJSONArray(key) ?: return emptyList()
                    return (0 until values.length()).map { values.optString(it) }
                }
                Note(
                    id = item.optString("id"), category = normalizeSectionName(item.optString("category")),
                    title = item.optString("title"), body = item.optString("body"),
                    createdAt = item.optLong("createdAt"), updatedAt = item.optLong("updatedAt"),
                    isFavorite = item.optBoolean("isFavorite"), isDeleted = item.optBoolean("isDeleted"),
                    photoPaths = stringList("photoPaths"), attachmentPaths = stringList("attachmentPaths"),
                    photoIds = stringList("photoIds"), attachmentIds = stringList("attachmentIds"),
                    tables = tables, aiAssistant = item.optString("aiAssistant", "Без ИИ"),
                    aiInstruction = item.optString("aiInstruction", "")
                )
            }
        } catch (_: Exception) { emptyList() }

    private suspend fun uploadLocalFiles(
        note: Note,
        ownerId: String,
        permissions: List<String>,
        verifiedFileIds: MutableSet<String>
    ): Note {
        suspend fun resolveFile(path: String, existingId: String?, kind: String, index: Int): String {
            val id = existingId?.takeIf(String::isNotBlank)
            if (path.isBlank()) return id.orEmpty()
            val source = File(path)
            if (!source.isFile) return id.orEmpty()

            if (id == null) return uploadFile(note, path, kind, index, ownerId, permissions)
            if (id in verifiedFileIds) return id

            try {
                AppwriteService.storage.getFile(BUCKET_ID, id)
                verifiedFileIds += id
                return id
            } catch (error: Exception) {
                val appwriteError = error as? io.appwrite.exceptions.AppwriteException
                if (appwriteError?.code !in setOf(401, 403, 404)) throw error
                // Legacy file has no usable owner permission. Re-upload the local original
                // under a fresh ID, then update the note row to point at the secured copy.
                val replacementId = UUID.randomUUID().toString()
                uploadFile(note, path, kind, index, ownerId, permissions, replacementId)
                verifiedFileIds += replacementId
                return replacementId
            }
        }

        val photoIds = note.photoPaths.mapIndexed { index, path ->
            resolveFile(path, note.photoIds.getOrNull(index), "photo", index)
        }
        val attachmentIds = note.attachmentPaths.mapIndexed { index, path ->
            resolveFile(path, note.attachmentIds.getOrNull(index), "attachment", index)
        }
        return note.copy(photoIds = photoIds, attachmentIds = attachmentIds)
    }

    private suspend fun uploadFile(
        note: Note, path: String, kind: String, index: Int, ownerId: String, permissions: List<String>,
        fileIdOverride: String? = null
    ): String {
        val source = File(path)
        require(source.isFile) { "Локальный файл не найден: ${source.name}" }
        val stableKey = "${note.id}|$kind|$index|${source.name}|${source.length()}|${source.lastModified()}"
        val fileId = fileIdOverride ?: UUID.nameUUIDFromBytes(stableKey.toByteArray(StandardCharsets.UTF_8)).toString()
        try {
            AppwriteService.storage.createFile(
                bucketId = BUCKET_ID,
                fileId = fileId,
                file = InputFile.fromPath(source.absolutePath),
                permissions = permissions
            )
        } catch (uploadError: Exception) {
            // Deterministic IDs make retries idempotent. Do not call getFile here:
            // upload permission does not imply file-read permission.
            val appwriteError = uploadError as? io.appwrite.exceptions.AppwriteException
            if (appwriteError?.code != 409) throw uploadError
        }
        return fileId
    }

    private suspend fun restoreLocalFiles(context: Context, note: Note): Note {
        val photos = restoreFiles(context, note.photoIds, note.photoPaths, "cloud_photos")
        val attachments = restoreFiles(context, note.attachmentIds, note.attachmentPaths, "cloud_files")
        return note.copy(photoPaths = photos, attachmentPaths = attachments)
    }

    private suspend fun restoreFiles(context: Context, ids: List<String>, existing: List<String>, folderName: String): List<String> {
        val folder = File(context.filesDir, folderName).apply { mkdirs() }
        val restored = ids.mapIndexed { index, id ->
            val cached = existing.getOrNull(index)?.takeIf(String::isNotBlank)?.let(::File)
            if (cached != null && cached.isFile) return@mapIndexed cached.absolutePath
            try {
                val metadata = AppwriteService.storage.getFile(BUCKET_ID, id)
                val safeName = metadata.name.substringAfterLast('/').take(120).ifBlank { id }
                val destination = File(folder, "${id}_$safeName")
                if (!destination.isFile) {
                    val bytes = AppwriteService.storage.getFileDownload(BUCKET_ID, id)
                    destination.writeBytes(bytes)
                }
                destination.absolutePath
            } catch (error: Exception) {
                val appwriteError = error as? io.appwrite.exceptions.AppwriteException
                if (appwriteError?.code in setOf(401, 403, 404)) {
                    // Keep the slot aligned with its cloud ID. A device with the local original
                    // can re-upload it with owner permissions during the next sync.
                    ""
                } else throw error
            }
        }
        val localOnly = existing.drop(ids.size).mapNotNull { path ->
            File(path).takeIf { it.isFile }?.absolutePath
        }
        return restored + localOnly
    }

    private fun mergeVersions(versions: List<Note>): Note {
        val winner = versions.maxBy { it.updatedAt }

        fun mergeFiles(
            kind: String,
            primaryPaths: List<String>,
            primaryIds: List<String>,
            pathsOf: (Note) -> List<String>,
            idsOf: (Note) -> List<String>
        ): Pair<List<String>, List<String>> {
            val ordered = versions.sortedByDescending { it.updatedAt }
            val allIds = ordered.flatMap(idsOf).filter(String::isNotBlank).toSet()
            val count = maxOf(
                primaryPaths.size,
                primaryIds.size,
                ordered.maxOfOrNull { maxOf(pathsOf(it).size, idsOf(it).size) } ?: 0
            )
            val paths = ArrayList<String>(count)
            val ids = ArrayList<String>(count)
            for (index in 0 until count) {
                val path = primaryPaths.getOrNull(index)?.takeIf(String::isNotBlank)
                    ?: ordered.firstNotNullOfOrNull { pathsOf(it).getOrNull(index)?.takeIf(String::isNotBlank) }
                        .orEmpty()
                val pathMatchedId = if (path.isBlank()) null else ordered.firstNotNullOfOrNull { version ->
                    val matchedIndex = pathsOf(version).indexOf(path)
                    if (matchedIndex < 0) null else idsOf(version).getOrNull(matchedIndex)?.takeIf(String::isNotBlank)
                }
                val expectedId = if (path.isNotBlank()) runCatching {
                    val source = File(path)
                    val key = "${winner.id}|$kind|$index|${source.name}|${source.length()}|${source.lastModified()}"
                    UUID.nameUUIDFromBytes(key.toByteArray(StandardCharsets.UTF_8)).toString()
                }.getOrNull()?.takeIf { it in allIds } else null
                val alignedId = primaryIds.getOrNull(index)?.takeIf(String::isNotBlank)
                    ?: ordered.firstNotNullOfOrNull { version ->
                        val versionPath = pathsOf(version).getOrNull(index).orEmpty()
                        if (versionPath == path && path.isNotBlank()) idsOf(version).getOrNull(index)?.takeIf(String::isNotBlank) else null
                    }
                paths += path
                ids += pathMatchedId ?: expectedId ?: alignedId.orEmpty()
            }
            return paths to ids
        }

        val (photos, photoIds) = mergeFiles(
            "photo", winner.photoPaths, winner.photoIds,
            { it.photoPaths }, { it.photoIds }
        )
        val (attachments, attachmentIds) = mergeFiles(
            "attachment", winner.attachmentPaths, winner.attachmentIds,
            { it.attachmentPaths }, { it.attachmentIds }
        )
        return winner.copy(
            photoPaths = photos,
            photoIds = photoIds,
            attachmentPaths = attachments,
            attachmentIds = attachmentIds
        )
    }

    private fun Note.toCloudData(ownerId: String): Map<String, Any> = mapOf(
        "ownerId" to ownerId,
        "noteId" to id,
        "category" to category,
        "title" to title,
        "body" to body,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "isFavorite" to isFavorite,
        "isDeleted" to isDeleted,
        "aiAssistant" to aiAssistant,
        "aiInstruction" to aiInstruction,
        "tablesJson" to tablesToJson(tables),
        "photoIds" to photoIds,
        "attachmentIds" to attachmentIds
    )

    private fun Map<String, Any>.toNote(rowId: String): Note {
        val tableJson = this["tablesJson"]?.toString().orEmpty()
        val noteTables = try {
            val rows = org.json.JSONArray(tableJson)
            (0 until rows.length()).mapNotNull { i ->
                val table = rows.optJSONObject(i) ?: return@mapNotNull null
                val cellsJson = table.optJSONArray("cells") ?: org.json.JSONArray()
                val cells = (0 until cellsJson.length()).map { rowIndex ->
                    val cellRow = cellsJson.optJSONArray(rowIndex) ?: org.json.JSONArray()
                    (0 until cellRow.length()).map { colIndex -> cellRow.optString(colIndex) }
                }
                NoteTable(id = table.optString("id"), cells = cells)
            }
        } catch (_: Exception) { emptyList() }
        return Note(
            id = rowId,
            category = normalizeSectionName(this["category"]?.toString().orEmpty()),
            title = this["title"]?.toString().orEmpty(),
            body = this["body"]?.toString().orEmpty(),
            createdAt = (this["createdAt"] as? Number)?.toLong() ?: 0L,
            updatedAt = (this["updatedAt"] as? Number)?.toLong() ?: 0L,
            isFavorite = this["isFavorite"] as? Boolean ?: false,
            isDeleted = this["isDeleted"] as? Boolean ?: false,
            tables = noteTables,
            photoIds = (this["photoIds"] as? List<*>)?.mapNotNull { it?.toString() }.orEmpty(),
            attachmentIds = (this["attachmentIds"] as? List<*>)?.mapNotNull { it?.toString() }.orEmpty(),
            aiAssistant = this["aiAssistant"]?.toString() ?: "Без ИИ",
            aiInstruction = this["aiInstruction"]?.toString().orEmpty()
        )
    }

    private fun tablesToJson(tables: List<NoteTable>): String = org.json.JSONArray().apply {
        tables.forEach { table ->
            put(org.json.JSONObject().put("id", table.id).put("cells", org.json.JSONArray().apply {
                table.cells.forEach { row -> put(org.json.JSONArray(row)) }
            }))
        }
    }.toString()
}
