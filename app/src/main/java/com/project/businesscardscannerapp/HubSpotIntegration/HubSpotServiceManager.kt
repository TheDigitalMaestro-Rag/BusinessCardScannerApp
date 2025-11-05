// HubSpotServiceManager.kt - FIXED version
package com.project.businesscardscannerapp.HubSpotIntegration

import android.util.Log
import com.project.businesscardscannerapp.RoomDB.Entity.BusinessCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

class HubSpotServiceManager {
    private val tag = "HubSpotServiceManager"

    suspend fun searchContactByEmail(email: String, accessToken: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val service = HubSpotApiClient.createService()
                val authHeader = "Bearer $accessToken"

                val searchRequest = SearchRequest(
                    filterGroups = listOf(
                        FilterGroup(
                            filters = listOf(
                                Filter(
                                    propertyName = "email",
                                    operator = "EQ",
                                    value = email
                                )
                            )
                        )
                    ),
                    limit = 1
                )

                val response: Response<HubSpotSearchResponse> =
                    service.searchContacts(authHeader, searchRequest)

                if (response.isSuccessful) {
                    val searchResponse = response.body()
                    if (searchResponse?.results?.isNotEmpty() == true) {
                        Log.d(tag, "Found existing contact: ${searchResponse.results[0].id}")
                        searchResponse.results[0].id
                    } else {
                        Log.d(tag, "No existing contact found for email: $email")
                        null
                    }
                } else {
                    Log.e(tag, "Search failed: ${response.code()} - ${response.errorBody()?.string()}")
                    null
                }
            } catch (e: Exception) {
                Log.e(tag, "Error searching contact: ${e.message}")
                null
            }
        }
    }

    suspend fun createContact(businessCard: BusinessCard, accessToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val service = HubSpotApiClient.createService()
                val authHeader = "Bearer $accessToken"

                val properties = mutableMapOf<String, String>()

                // Name - split into first and last name
                businessCard.name?.let { fullName ->
                    val nameParts = fullName.split(" ").filter { it.isNotBlank() }
                    if (nameParts.isNotEmpty()) {
                        properties["firstname"] = nameParts[0]
                        if (nameParts.size > 1) {
                            properties["lastname"] = nameParts.subList(1, nameParts.size).joinToString(" ")
                        }
                    }
                }

                // Email
                if (!businessCard.email.isNullOrBlank()) {
                    properties["email"] = businessCard.email
                }

                // Phone
                if (!businessCard.phones.isNullOrEmpty() && !businessCard.phones[0].isNullOrBlank()) {
                    properties["phone"] = businessCard.phones[0]
                }

                // Company
                if (!businessCard.company.isNullOrBlank()) {
                    properties["company"] = businessCard.company
                }

                // Position
                if (!businessCard.position.isNullOrBlank()) {
                    properties["jobtitle"] = businessCard.position
                }

                // Website
                if (!businessCard.website.isNullOrBlank()) {
                    properties["website"] = businessCard.website
                }

                // Address
                if (!businessCard.address.isNullOrBlank()) {
                    properties["address"] = businessCard.address
                }

                // Notes
                if (!businessCard.notes.isNullOrBlank()) {
                    properties["notes"] = businessCard.notes
                }

                // REMOVED: leadsource property since it doesn't exist in your HubSpot
                // properties["leadsource"] = "Business Card Scanner App"

                val request = CreateContactRequest(properties = properties)

                val response: Response<HubSpotContactResponse> =
                    service.createContact(authHeader, request)

                if (response.isSuccessful) {
                    val contactResponse = response.body()
                    Log.d(tag, "Successfully created contact: ${contactResponse?.id}")
                    true
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(tag, "Failed to create contact: ${response.code()} - $errorBody")
                    false
                }
            } catch (e: Exception) {
                Log.e(tag, "Error creating contact: ${e.message}")
                false
            }
        }
    }

    suspend fun testConnection(accessToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val service = HubSpotApiClient.createService()
                val authHeader = "Bearer $accessToken"

                // Simple search to test connection
                val searchRequest = SearchRequest(
                    filterGroups = emptyList(),
                    limit = 1
                )

                val response: Response<HubSpotSearchResponse> =
                    service.searchContacts(authHeader, searchRequest)

                response.isSuccessful
            } catch (e: Exception) {
                Log.e(tag, "Connection test failed: ${e.message}")
                false
            }
        }
    }
}