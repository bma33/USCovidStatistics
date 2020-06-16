package com.example.uscovidstatistics.views.activities.country

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.uscovidstatistics.R
import com.example.uscovidstatistics.appconstants.AppConstants
import com.example.uscovidstatistics.databinding.ActivityCountryBreakdownBinding
import com.example.uscovidstatistics.manualdependency.DependencyInjectorImpl
import com.example.uscovidstatistics.model.CleanedUpData
import com.example.uscovidstatistics.model.apidata.JhuCountryDataset
import com.example.uscovidstatistics.recyclerview.CleanedDataRecyclerView
import com.example.uscovidstatistics.utils.AppUtils
import com.example.uscovidstatistics.views.activities.homepage.MainActivity
import com.example.uscovidstatistics.views.dialogs.BottomDialog
import com.example.uscovidstatistics.views.navigation.BaseActivity
import kotlinx.android.synthetic.main.app_toolbar.view.*
import kotlinx.android.synthetic.main.loading_screen.view.*
import java.lang.Exception
import kotlin.collections.ArrayList

class CountryActivity : BaseActivity(), ViewBinding, CountryContract.View {
    private lateinit var binding: ActivityCountryBreakdownBinding

    private lateinit var presenter: CountryContract.Presenter

    private val appUtils = AppUtils.getInstance()

    private lateinit var countryDisplay: String

    private val cleanedDataList = ArrayList<CleanedUpData>()

    private lateinit var recyclerViewData: CleanedDataRecyclerView

    private var usaCheck: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountryBreakdownBinding.inflate(layoutInflater)
        setContentView(root)

        countryDisplay = intent.getStringExtra(AppConstants.DISPLAY_COUNTRY)!!
        usaCheck = (countryDisplay == "USA")
        Log.d("CovidTesting", "$countryDisplay and: It is the USA : $usaCheck ")

        AppConstants.COUNTRY_NAME = countryDisplay
        AppConstants.DATA_SPECIFICS = if (usaCheck) 1 else 4

        setPresenter(CountryPresenter(this, DependencyInjectorImpl(), this))
        presenter.onViewCreated(countryDisplay)

        recyclerViewData = CleanedDataRecyclerView(this, this)

        setSupportActionBar(binding.root.bottom_toolbar)
        setHeader()
        setNavOptions()
    }

    private fun setHeader() {
        try {
            val mappedName = if (countryDisplay == "Burma")
                "Myanmar"
            else
                countryDisplay
            val url = AppConstants.WORLD_DATA_MAPPED[mappedName]!!.countryInfo!!.countryFlag

            Glide.with(this)
                .load(url)
                .apply(RequestOptions().placeholder(R.drawable.ic_earth_flag))
                .into(binding.flag)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val headerText = "$countryDisplay Information"
        binding.casesHeader.text = headerText
    }

    private fun setNavOptions() {
        binding.viewBackBtn.setOnClickListener {
            onBackPressed()
        }

        binding.root.bottom_toolbar.setNavigationOnClickListener {
            BottomDialog(this).newInstance().show(supportFragmentManager, "BottomDialog")
        }
    }

    fun getCleanedUpData(): List<CleanedUpData> {
        if (usaCheck) {
            val tempData = ArrayList<CleanedUpData>()
            tempData.addAll(cleanedDataList)
            cleanedDataList.clear()
            cleanedDataList.addAll(appUtils.cleanUsaData(this, tempData))
        } else {
            cleanedDataList.sortBy { it.name }
        }
        cleanedDataList.add(appUtils.cleanCountryTotals())
        return cleanedDataList
    }

    override fun setPresenter(presenter: CountryContract.Presenter) {
        this.presenter = presenter
    }

    override fun getRoot(): View {
        return binding.root
    }

    override fun displayCountryData(countryData: JhuCountryDataset) {
        val regionList = if (countryData.province!!.contains("bonaire, sint eustatius and saba")) {
            val temp = countryData.province!!.toMutableList()
            temp.remove("bonaire, sint eustatius and saba")
            temp.toTypedArray()
        } else {
            countryData.province!!
        }
        AppConstants.DATA_SPECIFICS = 5
        presenter.getRegionalData(AppConstants.DATA_SPECIFICS, regionList)
    }

    override fun displayCountryList() {
        if (usaCheck) {
            for (state in AppConstants.US_DATA)
                cleanedDataList.add(appUtils.createCleanUsaData(state))
        } else {
            for (province in AppConstants.COUNTRY_PROVINCE_LIST)
                cleanedDataList.add(appUtils.cleanCountryData(province))
        }

        recyclerViewData.displayCleanedData()
        binding.root.loading_layout.visibility = View.GONE
    }

    override fun displayUsList() {
        /*
        val territoryList = ArrayList<StateDataset>()
        val territories = StateDataset()
        territories.state = "Territories"
        territoryList.add(territories)
        val nonStateList = ArrayList<StateDataset>()
        val nonStates = StateDataset()
        nonStates.state = "Other"
        nonStateList.add(nonStates)

        val stateList = ArrayList<StateDataset>()
        for (data in AppConstants.US_DATA) {
            when {
                resources.getStringArray(R.array.us_territories).contains(data.state) -> {
                    territoryList.add(data)
                }
                resources.getStringArray(R.array.us_other).contains(data.state) -> {
                    nonStateList.add(data)
                }
                else -> {
                    stateList.add(data)
                }
            }
        }
        val temp = java.util.ArrayList<StateDataset>()
        temp.addAll(stateList)
        temp.addAll(territoryList)
        temp.addAll(nonStateList)

         */

        for (data in AppConstants.US_DATA) {
            when {
                resources.getStringArray(R.array.us_territories).contains(data.state) -> {
                    data.state = "YYYY${data.state}"
                }
                resources.getStringArray(R.array.us_other).contains(data.state) -> {
                    data.state = "ZZZZ${data.state}"
                }
                else -> {
                    data.state = data.state
                }
            }
        }

        displayCountryList()
    }

    override fun onBackPressed() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        overridePendingTransition(R.anim.enter_left, R.anim.exit_right)
    }

    override fun onStop() {
        super.onStop()
        AppConstants.COUNTRY_PROVINCE_LIST.clear()
        appUtils.resetCountryTotals()
    }

    override fun dataError(throwable: Throwable) {
        Log.d("CovidTesting", "Error with country list! ")
        //TODO Add Snackbar
    }
}
