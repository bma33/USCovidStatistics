package com.example.uscovidstatistics.views.activities.homepage

import android.content.Context
import android.content.Intent
import android.os.Looper
import android.util.Log
import com.example.uscovidstatistics.R
import com.example.uscovidstatistics.appconstants.AppConstants
import com.example.uscovidstatistics.manualdependency.DependencyInjector
import com.example.uscovidstatistics.model.DataModelRepository
import com.example.uscovidstatistics.model.apidata.*
import com.example.uscovidstatistics.network.NetworkRequests
import com.example.uscovidstatistics.utils.AppUtils
import com.example.uscovidstatistics.views.activities.country.CountryActivity
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.schedulers.Schedulers
import okhttp3.Response
import java.lang.Exception
import java.lang.reflect.ParameterizedType
import java.util.*
import javax.inject.Inject

class MainPresenter @Inject constructor(view: MainContract.View, dependencyInjector: DependencyInjector) : MainContract.Presenter {

    private val dataModelRepository: DataModelRepository = dependencyInjector.covidDataRepository()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private var view: MainContract.View? = view

    override fun onDestroy() {
        this.view = null
    }

    override fun onViewCreated() {
        loadData(AppConstants.DATA_SPECIFICS)
    }

    /*override fun onDataUpdated() {
        Log.d("CovidTesting", "Data was updated . . .")
    }*/

    override fun openLocationOnLaunch(mContext: Context) {
        if (AppConstants.USER_PREFS.getBoolean(mContext.getString(R.string.preference_gps), false)) {
            if (AppConstants.LOCATION_DATA.country != null) {
                val country = AppConstants.LOCATION_DATA.country
                val region = AppConstants.LOCATION_DATA.region
                val intent = Intent(mContext, CountryActivity::class.java)
                    .putExtra(AppConstants.DISPLAY_REGION, region)

                if (country == "United States") {
                    intent.putExtra(AppConstants.DISPLAY_COUNTRY, "USA")
                        .putExtra(AppConstants.LOAD_STATE, true)
                } else
                    intent.putExtra(AppConstants.DISPLAY_COUNTRY, country)

                mContext.startActivity(intent)
            }
        }
    }

    override fun onServiceStarted() {
        val timer = Timer()
        timer.schedule(object: TimerTask() {
            override fun run() {
                Thread(Runnable {
                    Looper.prepare()
                    loadData(3)
                }).start()
            }
        },0, (AppConstants.Update_Frequency)*60*1000)
    }

    private fun loadData(getSpecifics: Int) {
        Observable.defer {
            try {
                val networkRequests = NetworkRequests(getSpecifics, null, null).getLocationData()
                Observable.just(networkRequests)
            } catch (e: Exception) {
                Observable.error<Exception>(e)
            }
        }.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe (
                { onNext -> onNext as Response
                    setData(onNext)},
                { onError ->  view?.dataError(onError)
                    Log.d("CovidTesting", "Error in the subscription : $onError")},
                { view?.displayContinentData(AppUtils().continentTotals(dataModelRepository.getContinentData())) }
            )
    }

    private fun setData(response: Response) {
        val body = response.body!!
        val type: ParameterizedType = Types.newParameterizedType(List::class.java, ContinentDataset::class.java)

        try {
            val jsonAdapter: JsonAdapter<List<ContinentDataset>> = moshi.adapter(type)
            AppConstants.CONTINENT_DATA = jsonAdapter.fromJson(body.string())!!
        } catch (e: Exception) {
            view?.dataError(e)
            e.printStackTrace()
        }
        body.close()
    }
}