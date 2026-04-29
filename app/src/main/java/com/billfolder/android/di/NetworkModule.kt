package com.billfolder.android.di

import com.billfolder.android.BuildConfig
import com.billfolder.android.data.api.BillFolderApi
import com.billfolder.android.data.auth.AuthInterceptor
import com.billfolder.android.data.auth.BaseUrlProvider
import com.billfolder.android.data.auth.TokenAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Qualifier pra distinguir o OkHttpClient *cru* (sem Authenticator nem
 * Interceptor) — usado dentro do TokenAuthenticator pra não loopar.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RawOkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        explicitNulls = false
        encodeDefaults = false
    }

    @Provides
    @Singleton
    fun provideBaseUrlProvider(): BaseUrlProvider = object : BaseUrlProvider {
        override val baseUrl: String = BuildConfig.API_BASE_URL
    }

    @Provides
    @Singleton
    fun provideLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

    /**
     * Client cru — sem AuthInterceptor nem TokenAuthenticator. Usado pelo
     * TokenAuthenticator pra chamar /auth/refresh sem entrar em loop.
     */
    @Provides
    @Singleton
    @RawOkHttpClient
    fun provideRawOkHttpClient(
        logging: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Client principal — usado pelo Retrofit. Tem AuthInterceptor (injeta
     * Bearer) e TokenAuthenticator (renova em 401).
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
        logging: HttpLoggingInterceptor,
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .authenticator(tokenAuthenticator)
        .addInterceptor(logging)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        json: Json,
        baseUrlProvider: BaseUrlProvider,
    ): Retrofit {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl(baseUrlProvider.baseUrl)
            .client(client)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
    }

    @Provides
    @Singleton
    fun provideBillFolderApi(retrofit: Retrofit): BillFolderApi =
        retrofit.create(BillFolderApi::class.java)
}
