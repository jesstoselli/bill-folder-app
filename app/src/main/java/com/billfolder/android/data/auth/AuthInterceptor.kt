package com.billfolder.android.data.auth

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injeta o header `Authorization: Bearer <accessToken>` em toda request.
 * Pula os endpoints públicos de auth pra não enviar token quando ainda
 * não temos um (signup/login) — e pra não invalidar refresh com header
 * antigo expirado.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath

        // Endpoints públicos: passa direto sem header.
        if (path.endsWith("/auth/signup")
            || path.endsWith("/auth/login")
            || path.endsWith("/auth/refresh")) {
            return chain.proceed(original)
        }

        val token = runBlocking { tokenStorage.getAccessToken() }
        val request = if (token.isNullOrBlank()) {
            original
        } else {
            original.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        }
        return chain.proceed(request)
    }
}
