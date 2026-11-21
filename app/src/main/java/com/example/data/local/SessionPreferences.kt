package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "defitt_session_prefs")

class SessionPreferences(private val context: Context) {
    private object Keys {
        val ACTIVE_USER_ID = stringPreferencesKey("active_user_id")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val STEP_SERVICE_ENABLED = booleanPreferencesKey("step_service_enabled")
    }

    val activeUserIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_USER_ID]?.takeIf { it.isNotBlank() }
    }

    val notificationsEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    val stepServiceEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.STEP_SERVICE_ENABLED] ?: true
    }

    suspend fun setActiveUserId(userId: String?) {
        context.dataStore.edit { prefs ->
            if (userId.isNullOrBlank()) {
                prefs.remove(Keys.ACTIVE_USER_ID)
            } else {
                prefs[Keys.ACTIVE_USER_ID] = userId
            }
        }
    }

    suspend fun getActiveUserId(): String? {
        return activeUserIdFlow.first()
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setStepServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STEP_SERVICE_ENABLED] = enabled
        }
    }
}
