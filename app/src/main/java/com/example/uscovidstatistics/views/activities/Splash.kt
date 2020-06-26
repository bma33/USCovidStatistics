package com.example.uscovidstatistics.views.activities

import android.Manifest
import android.content.Intent
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.uscovidstatistics.views.activities.homepage.MainActivity
import com.example.uscovidstatistics.R
import com.example.uscovidstatistics.appconstants.AppConstants
import com.example.uscovidstatistics.model.apidata.BaseCountryDataset
import com.example.uscovidstatistics.model.apidata.JhuBaseDataset
import com.example.uscovidstatistics.network.NetworkRequests
import com.example.uscovidstatistics.utils.AppUtils
import com.example.uscovidstatistics.utils.PreferenceUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import es.dmoral.toasty.Toasty
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.Response
import java.lang.Exception
import java.lang.reflect.ParameterizedType
import kotlin.collections.HashMap

class Splash : AppCompatActivity() {

    private lateinit var loadGpsIntent: Intent

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var prefUtil: PreferenceUtils

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        loadGpsIntent = Intent(this, MainActivity::class.java)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        prefUtil = PreferenceUtils.getInstance(this)
        prefUtil.baseInit()

        if (AppUtils().checkLaunchPermissions(this)) {
            setGpsCoords()
        }
    }

    private fun setGpsCoords() {
        fusedLocationClient.lastLocation.addOnSuccessListener {
                location: Location? -> if (location != null) {
            loadGpsIntent.putExtra(
                AppConstants.Current_Gps_Location,
                doubleArrayOf(location.longitude, location.latitude)
            )
            AppConstants.Gps_Data[0] = location.longitude
            AppConstants.Gps_Data[1] = location.latitude
        }
            setLocation()
        }
    }

    private fun setLocation() {
        try {
            AppConstants.Location_Data = AppUtils().getLocationData(this)
        } catch (e: Exception) {
            Toasty.error(this, R.string.no_location, Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
        setCountryFlags()
    }

    private fun setCountryFlags() {
        Observable.defer {
            try {
                val networkRequests = NetworkRequests(0, null, null).getLocationData()
                Observable.just(networkRequests)
            } catch (e: Exception) {
                Observable.error<Exception>(e)
            }
        }.observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(
                {onNext -> onNext as Response
                    setWorldData(onNext)
                },
                { // Snackbar
                    Toasty.error(this, "Could not connect to internet", Toast.LENGTH_SHORT).show()},
                {setLocationalData()}
            )
    }

    private fun setLocationalData() {
        Observable.defer {
            try {
                val networkRequests = NetworkRequests(6, null, null).getLocationData()
                Observable.just(networkRequests)
            } catch (e: Exception) {
                Observable.error<Exception>(e)
            }
        }.observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe(
                {onNext -> onNext as Response
                    setLocationalData(onNext)
                },
                { // Snackbar
                    Toasty.error(this, "Could not connect to internet for regional", Toast.LENGTH_SHORT).show()
                    continueToLaunch()},
                {continueToLaunch()}
            )
    }

    private fun setLocationalData(response: Response) {
        val body = response.body!!
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val type: ParameterizedType = Types.newParameterizedType(List::class.java, JhuBaseDataset::class.java)
        val jsonAdapter: JsonAdapter<List<JhuBaseDataset>> = moshi.adapter(type)
        AppConstants.Regional_Data = jsonAdapter.fromJson(body.string())!!
    }

    private fun setWorldData(response: Response) {
        val body = response.body!!
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val type: ParameterizedType = Types.newParameterizedType(List::class.java, BaseCountryDataset::class.java)
        val jsonAdapter: JsonAdapter<List<BaseCountryDataset>> = moshi.adapter(type)
        AppConstants.World_Data = jsonAdapter.fromJson(body.string())!!

        for (data in AppConstants.World_Data) {
            AppConstants.World_Data_Mapped[data.country!!] = data
        }
    }

    private fun continueToLaunch() {
        startActivity(loadGpsIntent)
        finish()
    }

    @Override
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        val permissionMap = HashMap<String, Int>()
        for ((i, perm) in permissions.withIndex())
            permissionMap[perm] = grantResults[i]

        if (requestCode == AppConstants.Request_Gps_Location && permissionMap[Manifest.permission.ACCESS_COARSE_LOCATION] != 0) {
            Toasty.error(this, "GPS Permission not granted", Toast.LENGTH_SHORT).show()
            setCountryFlags()
        } else {
            Toasty.info(this, "GPS Permission granted", Toast.LENGTH_SHORT).show()
            setGpsCoords()
        }
    }
}
