package com.xprokeey2.di

import com.xprokeey2.data.crypto.VaultCryptoImpl
import com.xprokeey2.data.local.session.InMemoryVaultSession
import com.xprokeey2.data.remote.datasource.AuthRemoteDataSource
import com.xprokeey2.data.remote.datasource.AuthRemoteDataSourceImpl
import com.xprokeey2.data.repository.AuthRepositoryImpl
import com.xprokeey2.data.repository.RecoveryKeyRepositoryImpl
import com.xprokeey2.domain.repository.AuthRepository
import com.xprokeey2.domain.repository.RecoveryKeyRepository
import com.xprokeey2.domain.security.VaultCrypto
import com.xprokeey2.domain.security.VaultSession
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(impl: AuthRemoteDataSourceImpl): AuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindRecoveryKeyRepository(impl: RecoveryKeyRepositoryImpl): RecoveryKeyRepository

    @Binds
    @Singleton
    abstract fun bindVaultCrypto(impl: VaultCryptoImpl): VaultCrypto

    @Binds
    @Singleton
    abstract fun bindVaultSession(impl: InMemoryVaultSession): VaultSession
}
