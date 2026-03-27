package io.j3mobile.domain

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BridgeSettingsRepositoryTest {
    @Test
    fun saveLoadAndClearRoundTrips() {
        val settings = MapSettings()
        val repository = BridgeSettingsRepository(settings)

        assertNull(repository.load())

        repository.save(
            BridgeSettings(
                baseUrl = "http://127.0.0.1:8181/",
                jwt = "token-123",
            ),
        )

        assertEquals(
            BridgeSettings(baseUrl = "http://127.0.0.1:8181", jwt = "token-123"),
            repository.load(),
        )

        repository.clear()
        assertNull(repository.load())
    }
}
