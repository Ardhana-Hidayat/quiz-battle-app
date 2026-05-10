package id.ac.pnm.quizbattleapp.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import id.ac.pnm.quizbattleapp.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore("user_session")

@Singleton
class UserSessionDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val UID          = stringPreferencesKey("uid")
        val EMAIL        = stringPreferencesKey("email")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    val sessionFlow: Flow<User?> = context.dataStore.data.map { prefs ->
        if (prefs[Keys.IS_LOGGED_IN] == true)
            User(
                uid         = prefs[Keys.UID].orEmpty(),
                email       = prefs[Keys.EMAIL].orEmpty(),
                displayName = prefs[Keys.DISPLAY_NAME].orEmpty()
            )
        else null
    }

    suspend fun saveSession(user: User) {
        context.dataStore.edit { prefs ->
            prefs[Keys.UID]          = user.uid
            prefs[Keys.EMAIL]        = user.email
            prefs[Keys.DISPLAY_NAME] = user.displayName
            prefs[Keys.IS_LOGGED_IN] = true
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}