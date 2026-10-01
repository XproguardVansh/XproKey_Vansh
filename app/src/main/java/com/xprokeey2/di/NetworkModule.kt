package com.xprokeey2.di

import com.xprokeey2.BuildConfig
import com.xprokeey2.data.remote.api.AccountApi
import com.xprokeey2.data.remote.api.AuthApi
import com.xprokeey2.data.remote.api.CardApi
import com.xprokeey2.data.remote.api.GoogleAuthApi
import com.xprokeey2.data.remote.api.LicenseApi
import com.xprokeey2.data.remote.api.PaymentsApi
import com.xprokeey2.data.remote.api.SupportApi
import com.xprokeey2.data.remote.api.TokenApi
import com.xprokeey2.data.remote.api.TransferApi
import com.xprokeey2.data.remote.api.VaultApi
import com.xprokeey2.data.remote.auth.AccessGuardInterceptor
import com.xprokeey2.data.remote.auth.TokenAuthenticator
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
 * Plain client for POST /refresh: it must not go through [TokenAuthenticator] itself. Google sign-in
 * uses it too: its calls come before there is a session, so a 401 must never be retried with a saved
 * access token in place of the setup token.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TokenRefreshClient

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    @Provides
    @Singleton
    @TokenRefreshClient
    fun provideTokenRefreshClient(): OkHttpClient = baseClient().build()

    @Provides
    @Singleton
    fun provideOkHttpClient(authenticator: TokenAuthenticator, accessGuard: AccessGuardInterceptor): OkHttpClient =
        baseClient()
            .addInterceptor(accessGuard)
            .authenticator(authenticator)
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = retrofit(client, json)

    @Provides
    @Singleton
    fun provideTokenApi(@TokenRefreshClient client: OkHttpClient, json: Json): TokenApi =
        retrofit(client, json).create(TokenApi::class.java)

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideGoogleAuthApi(@TokenRefreshClient client: OkHttpClient, json: Json): GoogleAuthApi =
        retrofit(client, json).create(GoogleAuthApi::class.java)

    @Provides
    @Singleton
    fun provideLicenseApi(retrofit: Retrofit): LicenseApi = retrofit.create(LicenseApi::class.java)

    @Provides
    @Singleton
    fun provideCardApi(retrofit: Retrofit): CardApi = retrofit.create(CardApi::class.java)

    @Provides
    @Singleton
    fun provideVaultApi(retrofit: Retrofit): VaultApi = retrofit.create(VaultApi::class.java)

    @Provides
    @Singleton
    fun provideTransferApi(retrofit: Retrofit): TransferApi = retrofit.create(TransferApi::class.java)

    @Provides
    @Singleton
    fun provideSupportApi(retrofit: Retrofit): SupportApi = retrofit.create(SupportApi::class.java)

    @Provides
    @Singleton
    fun provideAccountApi(retrofit: Retrofit): AccountApi = retrofit.create(AccountApi::class.java)

    @Provides
    @Singleton
    fun providePaymentsApi(retrofit: Retrofit): PaymentsApi = retrofit.create(PaymentsApi::class.java)

    private fun baseClient(): OkHttpClient.Builder {
        // BASIC only: request/response bodies carry passwords and tokens, never log them.
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
    }

    private fun retrofit(client: OkHttpClient, json: Json): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
}
