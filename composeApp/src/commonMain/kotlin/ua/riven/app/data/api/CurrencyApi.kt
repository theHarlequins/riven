package ua.riven.app.data.api

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class MonobankCurrency(
    val currencyCodeA: Int,
    val currencyCodeB: Int,
    val date: Long,
    val rateBuy: Double? = null,
    val rateSell: Double? = null,
    val rateCross: Double? = null
)

class CurrencyApi(private val client: HttpClient) {

    suspend fun fetchRates(): List<MonobankCurrency> {
        return client.get("https://api.monobank.ua/bank/currency").body()
    }

    companion object {
        fun createHttpClient(): HttpClient = HttpClient {
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                })
            }
        }
    }
}
