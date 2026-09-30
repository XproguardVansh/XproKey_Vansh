package com.xprokeey2.domain.usecase.security

import com.xprokeey2.domain.model.ServerSessionTimeout
import com.xprokeey2.domain.model.SessionTimeoutSettings
import com.xprokeey2.domain.model.TimeoutAction
import com.xprokeey2.domain.model.TimeoutDuration
import com.xprokeey2.domain.repository.SecuritySettingsRepository
import com.xprokeey2.domain.util.DataError
import com.xprokeey2.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The web Security page's load and save, and its app layout's check on opening the app. */
class SecuritySettingsUseCasesTest {

    private class FakeRepository(
        var server: Resource<ServerSessionTimeout>,
        var saved: SessionTimeoutSettings? = null,
        var updateResult: Resource<Unit> = Resource.Success(Unit),
    ) : SecuritySettingsRepository {
        val updates = mutableListOf<SessionTimeoutSettings>()

        override suspend fun fetchSettings() = server
        override suspend fun updateSettings(settings: SessionTimeoutSettings): Resource<Unit> {
            updates += settings
            return updateResult
        }
        override suspend fun getSavedSettings() = saved
        override suspend fun saveSettings(settings: SessionTimeoutSettings) {
            saved = settings
        }
        override fun observeSavedSettings(): Flow<SessionTimeoutSettings?> = flowOf(saved)
    }

    private fun server(duration: TimeoutDuration?, action: TimeoutAction?) =
        Resource.Success(ServerSessionTimeout(duration, action))

    @Test
    fun wireValuesMatchTheWeb() {
        assertEquals(TimeoutDuration.ONE_HOUR, TimeoutDuration.of("60"))
        assertEquals(TimeoutDuration.NEVER, TimeoutDuration.of("never"))
        assertNull(TimeoutDuration.of("10"))
        assertNull(TimeoutDuration.of(null))
        assertEquals(TimeoutAction.LOCK, TimeoutAction.of("lock"))
        assertNull(TimeoutAction.of("sleep"))
        assertEquals(listOf("1", "5", "15", "30", "60", "240", "never"), TimeoutDuration.entries.map { it.value })
    }

    @Test
    fun openingThePageShowsAndSavesTheServerSettings() = runBlocking {
        val repository = FakeRepository(server(TimeoutDuration.FIVE_MINUTES, TimeoutAction.LOCK))

        val result = GetSecuritySettingsUseCase(repository)()

        val expected = SessionTimeoutSettings(TimeoutDuration.FIVE_MINUTES, TimeoutAction.LOCK)
        assertEquals(expected, (result as Resource.Success).data)
        assertEquals(expected, repository.saved)
    }

    @Test
    fun missingServerValuesBecomeNeverAndLogout() = runBlocking {
        val repository = FakeRepository(server(duration = null, action = null))

        val result = GetSecuritySettingsUseCase(repository)()

        assertEquals(SessionTimeoutSettings(TimeoutDuration.NEVER, TimeoutAction.LOGOUT), (result as Resource.Success).data)
    }

    @Test
    fun withoutTheServerThePageShowsThisDevicesCopy() = runBlocking {
        val saved = SessionTimeoutSettings(TimeoutDuration.THIRTY_MINUTES, TimeoutAction.LOCK)
        val withCopy = FakeRepository(Resource.Error(DataError.NoInternet), saved = saved)
        val withoutCopy = FakeRepository(Resource.Error(DataError.Server(500, "boom")))

        assertEquals(saved, (GetSecuritySettingsUseCase(withCopy)() as Resource.Success).data)
        assertEquals(SessionTimeoutSettings(), (GetSecuritySettingsUseCase(withoutCopy)() as Resource.Success).data)
        assertEquals(
            DataError.SessionExpired,
            (GetSecuritySettingsUseCase(FakeRepository(Resource.Error(DataError.SessionExpired)))() as Resource.Error).error,
        )
    }

    @Test
    fun savingKeepsTheCopyOnlyWhenTheServerAccepted() = runBlocking {
        val settings = SessionTimeoutSettings(TimeoutDuration.ONE_MINUTE, TimeoutAction.LOCK)
        val accepted = FakeRepository(server(null, null))
        val rejected = FakeRepository(server(null, null), updateResult = Resource.Error(DataError.Server(400, "Invalid input")))

        assertTrue(SaveSecuritySettingsUseCase(accepted)(settings) is Resource.Success)
        assertTrue(SaveSecuritySettingsUseCase(rejected)(settings) is Resource.Error)

        assertEquals(settings, accepted.saved)
        assertNull(rejected.saved)
    }

    @Test
    fun firstOpeningOnADeviceTurnsTheServerDefaultOfFifteenMinutesIntoNever() = runBlocking {
        val repository = FakeRepository(server(TimeoutDuration.FIFTEEN_MINUTES, TimeoutAction.LOCK))

        SyncSessionTimeoutUseCase(repository)()

        val expected = SessionTimeoutSettings(TimeoutDuration.NEVER, TimeoutAction.LOCK)
        assertEquals(listOf(expected), repository.updates)
        assertEquals(expected, repository.saved)
    }

    @Test
    fun aMissingTimeoutAlsoBecomesNeverWithLogout() = runBlocking {
        val repository = FakeRepository(server(duration = null, action = null))

        SyncSessionTimeoutUseCase(repository)()

        val expected = SessionTimeoutSettings(TimeoutDuration.NEVER, TimeoutAction.LOGOUT)
        assertEquals(listOf(expected), repository.updates)
        assertEquals(expected, repository.saved)
    }

    @Test
    fun aDeviceWithACopyTakesTheServerValueAsItIs() = runBlocking {
        val repository = FakeRepository(
            server(TimeoutDuration.FIFTEEN_MINUTES, TimeoutAction.LOGOUT),
            saved = SessionTimeoutSettings(TimeoutDuration.NEVER, TimeoutAction.LOGOUT),
        )

        SyncSessionTimeoutUseCase(repository)()

        assertTrue(repository.updates.isEmpty())
        assertEquals(SessionTimeoutSettings(TimeoutDuration.FIFTEEN_MINUTES, TimeoutAction.LOGOUT), repository.saved)
    }

    @Test
    fun otherServerValuesAreCopiedWithoutChange() = runBlocking {
        val repository = FakeRepository(server(TimeoutDuration.FOUR_HOURS, TimeoutAction.LOCK))

        SyncSessionTimeoutUseCase(repository)()

        assertTrue(repository.updates.isEmpty())
        assertEquals(SessionTimeoutSettings(TimeoutDuration.FOUR_HOURS, TimeoutAction.LOCK), repository.saved)
    }

    @Test
    fun withoutTheServerOnlyADeviceWithoutACopyGetsTheDefaults() = runBlocking {
        val fresh = FakeRepository(Resource.Error(DataError.NoInternet))
        val kept = SessionTimeoutSettings(TimeoutDuration.ONE_MINUTE, TimeoutAction.LOCK)
        val withCopy = FakeRepository(Resource.Error(DataError.NoInternet), saved = kept)

        SyncSessionTimeoutUseCase(fresh)()
        SyncSessionTimeoutUseCase(withCopy)()

        assertEquals(SessionTimeoutSettings(), fresh.saved)
        assertEquals(kept, withCopy.saved)
    }

    @Test
    fun theTimeoutInForceFallsBackToTheDefaults() = runBlocking {
        assertEquals(
            SessionTimeoutSettings(TimeoutDuration.NEVER, TimeoutAction.LOGOUT),
            ObserveSessionTimeoutUseCase(FakeRepository(server(null, null)))().first(),
        )
    }
}
