package com.example.uscovidstatistics.recyclerview

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uscovidstatistics.R
import com.example.uscovidstatistics.appconstants.AppConstants
import com.example.uscovidstatistics.utils.AppUtils
import com.example.uscovidstatistics.views.activities.country.CountryActivity
import com.example.uscovidstatistics.views.activities.homepage.MainActivity
import com.example.uscovidstatistics.views.activities.region.RegionActivity
import com.example.uscovidstatistics.views.dialogs.BottomDialog

class NavRecyclerView(private val mContext: Activity, private val bottomDialog: BottomDialog, private val navigationRecycler: RecyclerView) {
    private lateinit var adapterNavView : NavRecyclerViewAdapter
    private lateinit var recyclerView : RecyclerView
    private lateinit var recyclerData : Map<String, Array<String>>

    fun displayChoices(choice: String){
        recyclerView = navigationRecycler
        recyclerView.setHasFixedSize(true)
        recyclerView.layoutManager = LinearLayoutManager(mContext.applicationContext)
        recyclerData = bottomDialog.getContinentList()
        adapterNavView = NavRecyclerViewAdapter(recyclerData.getValue(choice))
        recyclerView.recycledViewPool.setMaxRecycledViews(0,0)
        recyclerView.adapter = adapterNavView
        adapterNavView.notifyDataSetChanged()
        setListener()
    }

    private fun setListener(){
        adapterNavView.setOnClickListener(object : NavRecyclerViewAdapter.OnClickListener {
            override fun onCountryClick(position: Int, countryName: String, v: View) {
                if (AppConstants.RECYCLER_CLICKABLE) {
                    val actualCountry = AppUtils.getInstance().territoriesDirectLink(countryName, mContext)
                    if (actualCountry != "null") {
                        // Country Name is the territory here
                        val intent = Intent(mContext.applicationContext, RegionActivity::class.java)
                        intent.putExtra(AppConstants.DISPLAY_REGION, countryName)
                            .putExtra(AppConstants.DISPLAY_COUNTRY, actualCountry)
                        bottomDialog.dismiss()
                        mContext.startActivity(intent)
                        mContext.overridePendingTransition(R.anim.enter_right, R.anim.exit_left)
                    } else {
                        val intent = Intent(mContext.applicationContext, CountryActivity::class.java)
                        intent.putExtra(AppConstants.DISPLAY_COUNTRY, countryName)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        bottomDialog.dismiss()
                        if (mContext is MainActivity) {
                            mContext.startActivity(intent)
                            mContext.overridePendingTransition(R.anim.enter_right, R.anim.exit_left)
                        } else {
                            mContext.intent.putExtra(AppConstants.DISPLAY_COUNTRY, countryName)
                            mContext.recreate()
                        }
                    }
                }
            }
        })
    }
}