package com.example.uscovidstatistics.appconstants

import com.example.uscovidstatistics.model.*
import com.example.uscovidstatistics.model.apidata.*

class AppConstants {
    companion object {
        // Global Strings
        const val DISPLAY_GPS = "display_gps_location"
        const val DISPLAY_COUNTRY = "display_country_location"
        const val DISPLAY_STATE = "display_state_location"

        // Intents
        const val REQUEST_GPS_LOCATION = 101
        const val CHANNEL_ID = "channel_id_01"

        // Basic Data
        var APP_OPEN = false
        var DATA_SPECIFICS = 3
        var REGION_NAME: String? = null
        var COUNTRY_NAME: String? = null
        var TIMER_DELAY = 0L

        // API Urls
        const val API_DATA_URL_GLOBAL = "https://corona.lmao.ninja/v2/countries"
        const val API_DATA_URL_USA = "https://corona.lmao.ninja/v2/states"
        const val API_DATA_ENDPOINT = "?yesterday=falsecountries&sort"
        const val API_DATA_CONTINENT = "https://corona.lmao.ninja/v2/continents/?yesterday=falsecountries&sort"

        // John Hopkins University, used for non-US countries and breakdowns for Provinces/Counties/etc
        const val API_DATA_JHU = "https://corona.lmao.ninja/v2/jhucsse"
        const val API_DATA_JHU_COUNTRY = "https://corona.lmao.ninja/v2/historical"
        const val API_DATA_JHU_ENDPOINT = "?lastdays=all"

        const val CURRENT_GPS_LOCATION = "current_location"

        // Global data
        val GPS_DATA = DoubleArray(2)
        lateinit var LOCATION_DATA: LocationDataset

        lateinit var WORLD_DATA: List<BaseCountryDataset>
        var WORLD_DATA_MAPPED: HashMap<String, BaseCountryDataset> = HashMap()

        lateinit var US_DATA: List<StateDataset>
        lateinit var US_STATE_DATA: StateDataset
        var US_STATE_DATA_MAPPED: HashMap<String, StateDataset> = HashMap()

        lateinit var CONTINENT_DATA: List<ContinentDataset>
        lateinit var COUNTRY_DATA: JhuCountryDataset
        lateinit var COUNTRY_PROVINCE_DATA: JhuProvinceDataset
        var COUNTRY_PROVINCE_LIST: ArrayList<JhuProvinceDataset> = ArrayList()
    }
}