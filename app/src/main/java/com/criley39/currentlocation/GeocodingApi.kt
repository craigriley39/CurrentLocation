package com.criley39.currentlocation

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

data class GeocodingResponse(
    val display_name: String,
    val address: Address?
)

data class Address(
    val city: String?,
    val town: String?,
    val village: String?,
    val county: String?,
    val state: String?,
    val country: String?,
    val postcode: String?
)

interface GeocodingApiService {
    @GET("reverse")
    fun reverseGeocode(
        @Query("lat") latitude: String,
        @Query("lon") longitude: String,
        @Query("format") format: String = "json",
        @Query("addressdetails") addressDetails: Int = 1,
        @Header("User-Agent") userAgent: String = "CurrentLocationApp/1.0 (contact@example.com)"
    ): Call<GeocodingResponse>
}