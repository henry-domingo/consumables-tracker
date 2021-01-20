package com.reavitz.consumablestracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.telephony.SubscriptionManager
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.reavitz.consumablestracker.ui.main.SectionsPagerAdapter
import com.reavitz.consumablestracker.ui.main.SimInfo
import com.tbruyelle.rxpermissions3.RxPermissions
import kotlinx.android.synthetic.main.activity_main2.*

class MainActivity2 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    @SuppressLint("MissingPermission")
    override fun onResume() {
        super.onResume()
        val rxPermissions = RxPermissions(this)
        rxPermissions
            .request(
                Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_SMS,
                Manifest.permission.READ_CALL_LOG
            )
            .subscribe { granted ->
                if (granted) {
                    if (viewPager.adapter != null) return@subscribe

                    val simList = mutableListOf<SimInfo>()
                    (getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE)
                            as? SubscriptionManager)?.let {
                        for ((i, s) in it.activeSubscriptionInfoList.withIndex()) {
                            val name = if (s.displayName.isBlank()) s.carrierName else s.displayName
                            simList.add(
                                SimInfo(
                                    lineNumber = i + 1,
                                    label = name.toString(),
                                    number = s.number,
                                    subscriptionID = s.iccId//TODO check on some device
                                )
                            )
                        }
                    }

                    val sectionsPagerAdapter = SectionsPagerAdapter(
                        this,
                        supportFragmentManager, simList
                    )
                    viewPager.adapter = sectionsPagerAdapter
                    tabs.setupWithViewPager(viewPager)
                } else {
                    finish()
                }
            }
    }
}