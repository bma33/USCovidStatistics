package com.example.uscovidstatistics.views.activities.country

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.View
import androidx.core.content.ContextCompat
import androidx.viewbinding.ViewBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.uscovidstatistics.R
import com.example.uscovidstatistics.appconstants.AppConstants
import com.example.uscovidstatistics.databinding.ActivityCountryBreakdownBinding
import com.example.uscovidstatistics.manualdependency.DependencyInjectorImpl
import com.example.uscovidstatistics.model.CleanedUpData
import com.example.uscovidstatistics.model.apidata.BaseCountryDataset
import com.example.uscovidstatistics.model.apidata.JhuCountryDataset
import com.example.uscovidstatistics.recyclerview.CleanedDataRecyclerView
import com.example.uscovidstatistics.utils.AppUtils
import com.example.uscovidstatistics.utils.PreferenceUtils
import com.example.uscovidstatistics.utils.SnackbarUtil
import com.example.uscovidstatistics.views.activities.homepage.MainActivity
import com.example.uscovidstatistics.views.dialogs.BottomDialog
import com.example.uscovidstatistics.views.activities.BaseActivity
import com.example.uscovidstatistics.views.activities.state.StateActivity
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.app_toolbar.view.*
import java.lang.Exception
import java.lang.RuntimeException
import java.net.UnknownHostException
import kotlin.collections.ArrayList

class CountryActivity : BaseActivity(), ViewBinding, CountryContract.View {
    private lateinit var binding: ActivityCountryBreakdownBinding

    private lateinit var presenter: CountryContract.Presenter

    private val appUtils = AppUtils.getInstance()

    private lateinit var countryDisplay: String

    private lateinit var appPref: PreferenceUtils

    private val cleanedDataList = ArrayList<CleanedUpData>()

    private lateinit var recyclerViewData: CleanedDataRecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCountryBreakdownBinding.inflate(layoutInflater)
        setContentView(root)

        // Testing
        AppConstants.Country_Province_List.clear()
        appUtils.resetCountryTotals()

        appPref = PreferenceUtils.getInstance(this)

        countryDisplay = intent.getStringExtra(AppConstants.Display_Country)!!

        AppConstants.Usa_Check = (countryDisplay == "USA")
        AppConstants.Country_Name = countryDisplay
        AppConstants.Data_Specifics = if (AppConstants.Usa_Check) 1 else 4

        setPresenter(CountryPresenter(this, DependencyInjectorImpl(), this))
        presenter.onViewCreated(countryDisplay)

        recyclerViewData = CleanedDataRecyclerView(this, this)

        setSupportActionBar(binding.root.bottom_toolbar)
        binding.root.bottom_toolbar.inflateMenu(R.menu.bottom_appbar_country_menu)
        setHeader()
        setNavOptions()
    }

    private fun setHeader() {
        try {
            val mappedName = if (countryDisplay == "Burma")
                "Myanmar"
            else
                countryDisplay
            val url = AppConstants.World_Data_Mapped[mappedName]!!.countryInfo!!.countryFlag

            Glide.with(this)
                .load(url)
                .apply(RequestOptions().placeholder(R.drawable.ic_earth_flag))
                .into(binding.flag)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val headerText = "${if (countryDisplay == "USA") "United States" else if (countryDisplay == "UK") "United Kingdom" else countryDisplay} Information"
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
        if (AppConstants.Usa_Check) {
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

    //////////////// Base Methods ////////////////

    override fun onStart() {
        super.onStart()
        AppConstants.Timer_Delay = AppUtils().setTimerDelay()
        if (!AppConstants.Country_Service_On) {
            Handler().postDelayed(
                {presenter.onServiceStarted(this)}, AppConstants.Timer_Delay
            )
            AppConstants.Country_Service_On = true
        }
    }

    override fun onResume() {
        super.onResume()
        this.activityResumed()

        if (!appUtils.checkNetwork(this)) {
            dataError(UnknownHostException())
        }
    }

    override fun onBackPressed() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        super.onBackPressed()
    }

    override fun onStop() {
        super.onStop()
        this.activityPaused()
        AppConstants.Country_Province_List.clear()
        appUtils.resetCountryTotals()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.bottom_appbar_country_menu, menu)
        if (appPref.checkPref(countryDisplay)) menu.getItem(0).icon = ContextCompat.getDrawable(this, R.drawable.ic_favorite_24px)

        return true
    }

    //////////////// MVP methods ////////////////

    override fun onResumeData() {
        runOnUiThread {
            SnackbarUtil(this).info(root.bottom_toolbar, getString(R.string.snackbar_wifi_enabled))
            presenter.onViewCreated(countryDisplay)
        }
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

        binding.dataProgress.visibility = View.VISIBLE
        binding.informationView.visibility = View.VISIBLE

        displayGeneralData(AppConstants.World_Data_Mapped[countryData.country!!]!!)

        AppConstants.Data_Specifics = 5
        presenter.getRegionalData(AppConstants.Data_Specifics, regionList)
    }

    private fun displayGeneralData(baseCountryDataset: BaseCountryDataset) {
        val statisticsHeader = "${resources.getString(R.string.base_covid_stats)} as of ${appUtils.getFormattedDate()}"
        binding.countryLayoutHeader.text = statisticsHeader

        val popFormatted = "${baseCountryDataset.population?.let { appUtils.formatNumbers(it) }} as of ${appUtils.getFormattedDate()}"
        binding.countryLayoutPopulation.text = popFormatted

        val casesFormatted = "${baseCountryDataset.cases?.let { appUtils.formatNumbers(it) }} (${appUtils.formatPopulation(baseCountryDataset, 0)})"
        binding.countryLayoutCases.text = casesFormatted

        val recoveredFormatted = "${baseCountryDataset.recovered?.let {appUtils.formatNumbers(it)}} (${appUtils.formatPopulation(baseCountryDataset, 1)})"
        binding.countryLayoutRecovered.text = recoveredFormatted

        val deathsFormatted = "${baseCountryDataset.deaths?.let { appUtils.formatNumbers(it) }} (${appUtils.formatPopulation(baseCountryDataset, 2)})"
        binding.countryLayoutDeaths.text = deathsFormatted

        // Per One Million
        val casesPOM = "${baseCountryDataset.perMillionCases} cases ${resources.getString(R.string.details_PerOneMil)}"
        binding.casesPom.text = casesPOM

        val testsPOM = "${baseCountryDataset.perMillionTests} tests ${resources.getString(R.string.details_PerOneMil)}"
        binding.testsPom.text = testsPOM

        val activePOM = "${baseCountryDataset.perMillionActive} active cases ${resources.getString(R.string.details_PerOneMil)}"
        binding.activePom.text = activePOM

        val recoveredPOM = "${baseCountryDataset.perMillionRecovered} recovered cases ${resources.getString(R.string.details_PerOneMil)}"
        binding.recoveredPom.text = recoveredPOM

        val criticalPOM = "${baseCountryDataset.perMillionCritical} critical cases ${resources.getString(R.string.details_PerOneMil)}"
        binding.criticalPom.text = criticalPOM

        val deathPOM = "${baseCountryDataset.perMillionDeaths} deaths ${resources.getString(R.string.details_PerOneMil)}"
        binding.deathsPom.text = deathPOM
    }

    override fun displayCountryList() {
        cleanedDataList.clear()
        if (AppConstants.Usa_Check) {
            for (state in AppConstants.Us_Data)
                cleanedDataList.add(appUtils.createCleanUsaData(state))
        } else {
            if (countryDisplay == "UK")
                countryDisplay = "United Kingdom"
            for (data in AppConstants.Regional_Data) {
                if (data.country!! == countryDisplay) {
                    if (data.province != null) {
                        cleanedDataList.add(appUtils.cleanRegionalData(data))
                    } else {
                        nullProvinceData()
                        break
                    }
                }
            }
        }

        // If in the US and clicked the My Location Data setting, navigate to the State activity to display that Data
        if (intent.getBooleanExtra(AppConstants.Load_State, false)) {
            val intent = Intent(this, StateActivity::class.java)
            intent.putExtra(AppConstants.Display_Country, "USA")
                .putExtra(AppConstants.Display_Region, this.intent.getStringExtra(AppConstants.Display_Region))

            this.intent.putExtra(AppConstants.Load_State, false)
            startActivity(intent)
            overridePendingTransition(R.anim.enter_right, R.anim.exit_left)
        }

        recyclerViewData.displayCleanedData()
        binding.dataProgress.visibility = View.GONE

        if (AppConstants.Updating_Country && this.isActivityVisible()) {
            val dataUpdated = "$countryDisplay ${getString(R.string.data_updated)}"
            SnackbarUtil(this).info(root.bottom_toolbar, dataUpdated)
        } else {
            AppConstants.Updating_Country = true
        }
    }

    private fun nullProvinceData() {
        for (province in AppConstants.Country_Province_List)
            cleanedDataList.add(appUtils.cleanCountryData(province))
    }

    override fun displayUsList() {
        binding.dataProgress.visibility = View.VISIBLE
        binding.informationView.visibility = View.VISIBLE

        for (data in AppConstants.Us_Data) {
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

        displayGeneralData(AppConstants.World_Data_Mapped["USA"]!!)
        displayCountryList()
    }

    override fun dataError(throwable: Throwable) {
        // SnackBar to turn on Wifi/Data if both are off, otherwise SnackBar to attempt another API call due to timeout, incomplete data, etc.
        if (throwable is UnknownHostException) {
            // Enables wifi if there's no connection
            Snackbar.make(root, R.string.snackbar_error_wifi, Snackbar.LENGTH_INDEFINITE)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.colorRed))
                .setAnchorView(root.bottom_toolbar)
                .setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                .setAction(R.string.snackbar_clk_enable){
                    presenter.networkStatus(this)
                }.setActionTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                .show()
        } else {
            // Restarts the API due to timeout/other errors out of the user's control
            Snackbar.make(root, R.string.snackbar_error_timeout, Snackbar.LENGTH_INDEFINITE)
                .setBackgroundTint(ContextCompat.getColor(this, R.color.colorRed))
                .setTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                .setAnchorView(root.bottom_toolbar)
                .setAction(R.string.snackbar_clk_retry){
                    presenter.onViewCreated(countryDisplay)
                }.setActionTextColor(ContextCompat.getColor(this, R.color.colorWhite))
                .show()
        }
    }
}
