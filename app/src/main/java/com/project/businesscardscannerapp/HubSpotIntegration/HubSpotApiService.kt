// HubSpotApiService.kt - FIXED version
package com.project.businesscardscannerapp.HubSpotIntegration

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*

// HubSpot API Models
data class HubSpotContactResponse(
    @Json(name = "id") val id: String,
    @Json(name = "properties") val properties: Map<String, String>
)

data class HubSpotSearchResponse(
    @Json(name = "results") val results: List<HubSpotContactResponse>,
    @Json(name = "paging") val paging: PagingInfo? = null
)

data class PagingInfo(
    @Json(name = "next") val next: NextPage? = null
)

data class NextPage(
    @Json(name = "after") val after: String
)

data class CreateContactRequest(
    @Json(name = "properties") val properties: Map<String, String>
)

data class SearchRequest(
    @Json(name = "filterGroups") val filterGroups: List<FilterGroup>,
    @Json(name = "properties") val properties: List<String> = listOf("firstname", "lastname", "email", "phone", "company"),
    @Json(name = "limit") val limit: Int = 10
)

data class FilterGroup(
    @Json(name = "filters") val filters: List<Filter>
)

data class Filter(
    @Json(name = "propertyName") val propertyName: String,
    @Json(name = "operator") val operator: String,
    @Json(name = "value") val value: String
)

// Error Response
data class HubSpotErrorResponse(
    @Json(name = "message") val message: String,
    @Json(name = "category") val category: String
)

// Direct HubSpot API Service - FIXED with proper headers
interface HubSpotApiService {
    @Headers("Content-Type: application/json")
    @POST("crm/v3/objects/contacts")
    suspend fun createContact(
        @Header("Authorization") auth: String,
        @Body request: CreateContactRequest
    ): Response<HubSpotContactResponse>

    @Headers("Content-Type: application/json")
    @POST("crm/v3/objects/contacts/search")
    suspend fun searchContacts(
        @Header("Authorization") auth: String,
        @Body searchRequest: SearchRequest
    ): Response<HubSpotSearchResponse>

    @GET("crm/v3/objects/contacts/{contactId}")
    suspend fun getContact(
        @Header("Authorization") auth: String,
        @Path("contactId") contactId: String
    ): Response<HubSpotContactResponse>
}

// Retrofit Client for Direct HubSpot API - SIMPLIFIED
object HubSpotApiClient {
    private const val BASE_URL = "https://api.hubapi.com/"

    private val moshi: Moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private fun createOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        return OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    fun createService(): HubSpotApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .client(createOkHttpClient())
            .build()

        return retrofit.create(HubSpotApiService::class.java)
    }
}