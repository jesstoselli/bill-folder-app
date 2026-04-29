package com.billfolder.android.data.auth

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.tokenDataStore by preferencesDataStore(name = "billfolder_tokens")

/**
 * Persistência de access + refresh token via DataStore Preferences.
 *
 * Não criptografamos no disco — confiamos na sandbox do Android (cada app
 * tem seu /data/data/<package>, ilegível por outros apps em devices não-rooteados).
 * Se um dia formos pra app de banco level, trocar pra EncryptedSharedPreferences
 * ou pro Tink AEAD com chave em StrongBox.
 */
@Singleton
class TokenStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val accessKey   = stringPreferencesKey("access_token")
    private val refreshKey  = stringPreferencesKey("refresh_token")

    /** Stream do access_token; emite null quando não há token salvo. */
    val accessTokenFlow: Flow<String?> =
        context.tokenDataStore.data.map { it[accessKey] }

    val refreshTokenFlow: Flow<String?> =
        context.tokenDataStore.data.map { it[refreshKey] }

    /** Leituras síncronas pra usar dentro do Authenticator (que é blocking). */
    suspend fun getAccessToken(): String?  = accessTokenFlow.first()
    suspend fun getRefreshToken(): String? = refreshTokenFlow.first()

    suspend fun saveTokens(accessToken: String, refreshToken: String) {
        context.tokenDataStore.edit { prefs ->
            prefs[accessKey]  = accessToken
            prefs[refreshKey] = refreshToken
        }
    }

    suspend fun clear() {
        context.tokenDataStore.edit { prefs ->
            prefs.remove(accessKey)
            prefs.remove(refreshKey)
        }
    }
}
