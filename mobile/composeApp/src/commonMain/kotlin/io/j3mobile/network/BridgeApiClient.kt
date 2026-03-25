package io.j3mobile.network

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import io.j3mobile.protocol.*

@Serializable
data class HealthResponse(
    val status: String,
    val upstreamConnected: Boolean,
)

@Serializable
data class SessionInfo(
    val id: String,
    val userId: String,
    val createdAt: String,
)

class BridgeApiClient(
    private val baseUrl: String,
    private val token: String,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(this@BridgeApiClient.json)
        }
    }

    suspend fun health(): HealthResponse {
        return client.get("$baseUrl/health").body()
    }

    suspend fun getSnapshot(): OrchestrationReadModel {
        return client.get("$baseUrl/snapshot") {
            bearerAuth(token)
        }.body()
    }

    suspend fun listSessions(): List<SessionInfo> {
        return client.get("$baseUrl/sessions") {
            bearerAuth(token)
        }.body()
    }

    fun close() {
        client.close()
    }
}
