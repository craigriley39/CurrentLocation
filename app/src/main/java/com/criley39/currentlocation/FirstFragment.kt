package com.criley39.currentlocation

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.criley39.currentlocation.databinding.FragmentFirstBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.DecimalFormat

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var weatherApiService: WeatherApiService
    private lateinit var geocodingApiService: GeocodingApiService
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001
    private val decimalFormat = DecimalFormat("#.####")

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        setupWeatherApi()
        setupGeocodingApi()

        binding.buttonFirst.setOnClickListener {
            getCurrentLocation()
        }

        // Automatically get location on launch
        getCurrentLocation()
    }

    private fun setupWeatherApi() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.weather.gov/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        weatherApiService = retrofit.create(WeatherApiService::class.java)
    }

    private fun setupGeocodingApi() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        geocodingApiService = retrofit.create(GeocodingApiService::class.java)
    }

    private fun getCurrentLocation() {
        if (checkLocationPermission()) {
            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            
            binding.textviewStatus.text = "Getting location..."
            
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        val latitude = it.latitude
                        val longitude = it.longitude
                        
                        // Format coordinates to 4 decimal places for weather API
                        val formattedLat = decimalFormat.format(latitude)
                        val formattedLon = decimalFormat.format(longitude)
                        
                        binding.textviewLatitude.text = "Latitude: $formattedLat"
                        binding.textviewLongitude.text = "Longitude: $formattedLon"
                        binding.textviewStatus.text = "Getting location info..."
                        
                        // Get location name (city/state)
                        getLocationName(formattedLat, formattedLon)
                        
                        // Get weather information
                        getWeatherInfo(formattedLat, formattedLon)
                    } ?: run {
                        binding.textviewStatus.text = "Unable to get location. Please try again."
                    }
                }
                .addOnFailureListener {
                    binding.textviewStatus.text = "Failed to get location: ${it.message}"
                }
        } else {
            requestLocationPermission()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation()
            } else {
                binding.textviewStatus.text = "Location permission denied"
            }
        }
    }

    private fun getLocationName(latitude: String, longitude: String) {
        geocodingApiService.reverseGeocode(latitude, longitude)
            .enqueue(object : Callback<GeocodingResponse> {
                override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                    if (response.isSuccessful) {
                        val geocodingResponse = response.body()
                        geocodingResponse?.let {
                            val address = it.address
                            val city = address?.city ?: address?.town ?: address?.village
                            val state = address?.state
                            
                            val locationText = when {
                                city != null && state != null -> "$city, $state"
                                city != null -> city
                                state != null -> state
                                else -> "Location unknown"
                            }
                            
                            binding.textviewLocation.text = "Location: $locationText"
                        } ?: run {
                            binding.textviewLocation.text = "Location: Unknown"
                        }
                    } else {
                        binding.textviewLocation.text = "Location: Unknown"
                    }
                }

                override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                    binding.textviewLocation.text = "Location: Error getting location"
                }
            })
    }

    private fun getWeatherInfo(latitude: String, longitude: String) {
        weatherApiService.getWeatherPoint(latitude, longitude)
            .enqueue(object : Callback<WeatherPoint> {
                override fun onResponse(call: Call<WeatherPoint>, response: Response<WeatherPoint>) {
                    if (response.isSuccessful) {
                        val weatherPoint = response.body()
                        weatherPoint?.let {
                            getForecast(it.properties.forecast)
                        } ?: run {
                            binding.textviewWeather.text = "Weather: No data available"
                            binding.textviewPrecipitation.text = "Precipitation: --"
                            binding.textviewWind.text = "Wind: --"
                            binding.textviewStatus.text = "Weather point data not available"
                        }
                    } else {
                        binding.textviewWeather.text = "Weather: API error"
                        binding.textviewPrecipitation.text = "Precipitation: --"
                        binding.textviewWind.text = "Wind: --"
                        binding.textviewStatus.text = "Weather API error: ${response.code()}"
                    }
                }

                override fun onFailure(call: Call<WeatherPoint>, t: Throwable) {
                    binding.textviewWeather.text = "Weather: Network error"
                    binding.textviewPrecipitation.text = "Precipitation: --"
                    binding.textviewWind.text = "Wind: --"
                    binding.textviewStatus.text = "Weather network error: ${t.message}"
                }
            })
    }

    private fun getForecast(forecastUrl: String) {
        weatherApiService.getForecast(forecastUrl)
            .enqueue(object : Callback<ForecastResponse> {
                override fun onResponse(call: Call<ForecastResponse>, response: Response<ForecastResponse>) {
                    if (response.isSuccessful) {
                        val forecast = response.body()
                        forecast?.let {
                            if (it.properties.periods.isNotEmpty()) {
                                val currentPeriod = it.properties.periods[0]
                                
                                // Display temperature and conditions
                                val weatherText = "${currentPeriod.temperature}°${currentPeriod.temperatureUnit} - ${currentPeriod.shortForecast}"
                                binding.textviewWeather.text = "Weather: $weatherText"
                                
                                // Display precipitation chance
                                val precipChance = currentPeriod.probabilityOfPrecipitation?.value
                                val precipText = if (precipChance != null) {
                                    "Precipitation: ${precipChance}%"
                                } else {
                                    "Precipitation: 0%"
                                }
                                binding.textviewPrecipitation.text = precipText
                                
                                // Display wind information
                                val windText = "Wind: ${currentPeriod.windSpeed} ${currentPeriod.windDirection}"
                                binding.textviewWind.text = windText
                                
                                binding.textviewStatus.text = "All information updated successfully"
                            } else {
                                binding.textviewWeather.text = "Weather: No forecast data"
                                binding.textviewPrecipitation.text = "Precipitation: --"
                                binding.textviewWind.text = "Wind: --"
                                binding.textviewStatus.text = "No forecast periods available"
                            }
                        } ?: run {
                            binding.textviewWeather.text = "Weather: No forecast available"
                            binding.textviewPrecipitation.text = "Precipitation: --"
                            binding.textviewWind.text = "Wind: --"
                            binding.textviewStatus.text = "Forecast data not available"
                        }
                    } else {
                        binding.textviewWeather.text = "Weather: Forecast API error"
                        binding.textviewPrecipitation.text = "Precipitation: --"
                        binding.textviewWind.text = "Wind: --"
                        binding.textviewStatus.text = "Forecast API error: ${response.code()}"
                    }
                }

                override fun onFailure(call: Call<ForecastResponse>, t: Throwable) {
                    binding.textviewWeather.text = "Weather: Forecast network error"
                    binding.textviewPrecipitation.text = "Precipitation: --"
                    binding.textviewWind.text = "Wind: --"
                    binding.textviewStatus.text = "Forecast network error: ${t.message}"
                }
            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}