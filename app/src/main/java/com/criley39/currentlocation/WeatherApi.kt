package com.criley39.currentlocation

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

data class WeatherPoint(
    val properties: WeatherProperties
)

data class WeatherProperties(
    val forecast: String,
    val forecastHourly: String,
    val cwa: String,
    val radarStation: String
)

data class ForecastResponse(
    val properties: ForecastProperties
)

data class ForecastProperties(
    val periods: List<ForecastPeriod>
)

data class ForecastPeriod(
    val number: Int,
    val name: String,
    val startTime: String,
    val endTime: String,
    val isDaytime: Boolean,
    val temperature: Int,
    val temperatureUnit: String,
    val windSpeed: String,
    val windDirection: String,
    val shortForecast: String,
    val detailedForecast: String,
    val probabilityOfPrecipitation: PrecipitationChance?
)

data class PrecipitationChance(
    val unitCode: String,
    val value: Int?
)

interface WeatherApiService {
    @GET("points/{lat},{lon}")
    fun getWeatherPoint(
        @Path("lat") latitude: String,
        @Path("lon") longitude: String,
        @Header("User-Agent") userAgent: String = "CurrentLocationApp/1.0 (contact@example.com)"
    ): Call<WeatherPoint>

    @GET
    fun getForecast(
        @retrofit2.http.Url url: String,
        @Header("User-Agent") userAgent: String = "CurrentLocationApp/1.0 (contact@example.com)"
    ): Call<ForecastResponse>
}