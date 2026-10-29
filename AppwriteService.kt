package ru.momnotes.app

import android.content.Context
import androidx.activity.ComponentActivity
import io.appwrite.Client
import io.appwrite.enums.OAuthProvider
import io.appwrite.ID
import io.appwrite.services.Account
import io.appwrite.services.TablesDB
import io.appwrite.services.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Appwrite email/password authentication only. Notes remain local. */
internal object AppwriteService {
    private const val ENDPOINT = "https://fra.cloud.appwrite.io/v1"
    private const val PROJECT_ID = "6ad9bbb90027e9523366"

    lateinit var account: Account
        private set
    lateinit var tables: TablesDB
        private set
    lateinit var storage: Storage
        private set

    fun initialize(context: Context) {
        val client = Client(context.applicationContext)
            .setEndpoint(ENDPOINT)
            .setProject(PROJECT_ID)
        account = Account(client)
        tables = TablesDB(client)
        storage = Storage(client)
    }

    suspend fun register(email: String, password: String) = withContext(Dispatchers.IO) {
        account.create(userId = ID.unique(), email = email.trim(), password = password)
        account.createEmailPasswordSession(email.trim(), password)
    }

    suspend fun login(email: String, password: String) = withContext(Dispatchers.IO) {
        account.createEmailPasswordSession(email.trim(), password)
    }

    suspend fun loginWithProvider(activity: ComponentActivity, provider: OAuthProvider) {
        account.createOAuth2Session(activity = activity, provider = provider)
    }

    suspend fun currentEmail(): String = withContext(Dispatchers.IO) { account.get().email }

    suspend fun logout() = withContext(Dispatchers.IO) {
        account.deleteSession("current")
    }
}
