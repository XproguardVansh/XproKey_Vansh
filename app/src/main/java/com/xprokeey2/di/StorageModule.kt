package com.xprokeey2.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RecoveryKeyDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class VaultSecurityDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionTimeoutDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PaymentStateDataStore

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {

    @Provides
    @Singleton
    @SessionDataStore
    fun provideSessionDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("session")
        }

    @Provides
    @Singleton
    @RecoveryKeyDataStore
    fun provideRecoveryKeyDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("pending_recovery_keys")
        }

    @Provides
    @Singleton
    @VaultSecurityDataStore
    fun provideVaultSecurityDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("vault_security")
        }

    @Provides
    @Singleton
    @SessionTimeoutDataStore
    fun provideSessionTimeoutDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("session_timeout")
        }

    @Provides
    @Singleton
    @PaymentStateDataStore
    fun providePaymentStateDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create {
            context.preferencesDataStoreFile("payment_state")
        }
}
