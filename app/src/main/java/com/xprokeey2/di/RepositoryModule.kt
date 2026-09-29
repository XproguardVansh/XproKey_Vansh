package com.xprokeey2.di

import com.xprokeey2.data.crypto.VaultCryptoImpl
import com.xprokeey2.data.local.files.ContentUriFileRepository
import com.xprokeey2.data.local.session.AccessTokenStore
import com.xprokeey2.data.local.session.InMemoryVaultSession
import com.xprokeey2.data.local.session.SessionStorage
import com.xprokeey2.data.local.vault.WeakVaultItemStorage
import com.xprokeey2.data.remote.datasource.AuthRemoteDataSource
import com.xprokeey2.data.remote.datasource.AuthRemoteDataSourceImpl
import com.xprokeey2.data.remote.datasource.CardRemoteDataSource
import com.xprokeey2.data.remote.datasource.CardRemoteDataSourceImpl
import com.xprokeey2.data.remote.datasource.LicenseRemoteDataSource
import com.xprokeey2.data.remote.datasource.LicenseRemoteDataSourceImpl
import com.xprokeey2.data.remote.datasource.SupportRemoteDataSource
import com.xprokeey2.data.remote.datasource.SupportRemoteDataSourceImpl
import com.xprokeey2.data.remote.datasource.TransferRemoteDataSource
import com.xprokeey2.data.remote.datasource.TransferRemoteDataSourceImpl
import com.xprokeey2.data.remote.datasource.VaultRemoteDataSource
import com.xprokeey2.data.remote.datasource.VaultRemoteDataSourceImpl
import com.xprokeey2.data.repository.AuthRepositoryImpl
import com.xprokeey2.data.repository.CardRepositoryImpl
import com.xprokeey2.data.repository.LicenseRepositoryImpl
import com.xprokeey2.data.repository.RecoveryKeyRepositoryImpl
import com.xprokeey2.data.repository.SupportRepositoryImpl
import com.xprokeey2.data.repository.UserRepositoryImpl
import com.xprokeey2.data.repository.VaultRepositoryImpl
import com.xprokeey2.data.repository.VaultTransferRepositoryImpl
import com.xprokeey2.domain.repository.AuthRepository
import com.xprokeey2.domain.repository.CardRepository
import com.xprokeey2.domain.repository.LicenseRepository
import com.xprokeey2.domain.repository.RecoveryKeyRepository
import com.xprokeey2.domain.repository.SupportRepository
import com.xprokeey2.domain.repository.UserFileRepository
import com.xprokeey2.domain.repository.UserRepository
import com.xprokeey2.domain.repository.VaultRepository
import com.xprokeey2.domain.repository.VaultTransferRepository
import com.xprokeey2.domain.repository.WeakVaultItemRepository
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
    abstract fun bindLicenseRemoteDataSource(impl: LicenseRemoteDataSourceImpl): LicenseRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindLicenseRepository(impl: LicenseRepositoryImpl): LicenseRepository

    @Binds
    @Singleton
    abstract fun bindRecoveryKeyRepository(impl: RecoveryKeyRepositoryImpl): RecoveryKeyRepository

    @Binds
    @Singleton
    abstract fun bindCardRemoteDataSource(impl: CardRemoteDataSourceImpl): CardRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindCardRepository(impl: CardRepositoryImpl): CardRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl): UserRepository

    @Binds
    @Singleton
    abstract fun bindVaultRemoteDataSource(impl: VaultRemoteDataSourceImpl): VaultRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindVaultRepository(impl: VaultRepositoryImpl): VaultRepository

    @Binds
    @Singleton
    abstract fun bindWeakVaultItemRepository(impl: WeakVaultItemStorage): WeakVaultItemRepository

    @Binds
    @Singleton
    abstract fun bindTransferRemoteDataSource(impl: TransferRemoteDataSourceImpl): TransferRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindVaultTransferRepository(impl: VaultTransferRepositoryImpl): VaultTransferRepository

    @Binds
    @Singleton
    abstract fun bindUserFileRepository(impl: ContentUriFileRepository): UserFileRepository

    @Binds
    @Singleton
    abstract fun bindSupportRemoteDataSource(impl: SupportRemoteDataSourceImpl): SupportRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindSupportRepository(impl: SupportRepositoryImpl): SupportRepository

    @Binds
    @Singleton
    abstract fun bindAccessTokenStore(impl: SessionStorage): AccessTokenStore

    @Binds
    @Singleton
    abstract fun bindVaultCrypto(impl: VaultCryptoImpl): VaultCrypto

    @Binds
    @Singleton
    abstract fun bindVaultSession(impl: InMemoryVaultSession): VaultSession
}
