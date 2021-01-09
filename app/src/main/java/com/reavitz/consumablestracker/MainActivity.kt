package com.reavitz.consumablestracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.provider.CallLog.Calls.OUTGOING_TYPE
import android.telephony.TelephonyManager
import androidx.appcompat.app.AppCompatActivity
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.tbruyelle.rxpermissions3.RxPermissions
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), LoaderManager.LoaderCallbacks<Cursor> {

    private var simNumber = ""
    private val currency = "PHP" //Currency.getInstance(Locale("ph")).symbol
    private var isInstanceNew = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

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
                    if (isInstanceNew)
                        loadCursors()
                    else {
                        LoaderManager.getInstance(this).restartLoader(0, null, this)
                        LoaderManager.getInstance(this).restartLoader(1, null, this)
                    }
                } else {
                    finish()
                }
            }
    }

    override fun onPause() {
        super.onPause()
        isInstanceNew = false
    }

    @SuppressLint("MissingPermission")
    private fun loadCursors() {
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        simNumber = tm.line1Number ?: ""
        LoaderManager.getInstance(this).initLoader(0, null, this)
        LoaderManager.getInstance(this).initLoader(1, null, this)
        lblSim1.text = tm.networkOperatorName
        //TODO multi-SIM support
    }

    private fun checkNetwork(number: String): String {
        val cleanNumber = when {
            number.startsWith("+63") -> number.replace("+63", "0")
            number.startsWith("9") -> "0$number"
            else -> number
        }
        return when {
            globeNumber.contains(cleanNumber) -> return NETWORK_GLOBE
            smartNumber.contains(cleanNumber) -> return NETWORK_SMART
            else -> NETWORK_UNKNOWN
        }
    }

    //TODO if 211, then free
    //TODO if same network then 6.50, else 7.50

    companion object {
        const val NETWORK_GLOBE = "Globe/TM"
        const val NETWORK_SMART = "Smart/Sun/TNT"
        const val NETWORK_UNKNOWN = "none"
        const val CONSUMBALE = 300.0//TODO configurable

        //TODO configurable networks
        val globeNumber = listOf(
            "09173", "09178", "09256", "09175", "09253", "09257",
            "09176", "09255", "09258", "0817", "0905", "0917", "0936", "0954", "0966",
            "0978", "0997", "0906", "0926", "0937", "0955", "0967", "0979", "0915", "0927",
            "0945", "0956", "0975", "0995", "0916", "0935", "0953", "0965", "0977", "0996"
        )
        val smartNumber = listOf(
            "0922", "0931", "0940", "0973", "0923", "0932",
            "0941", "0974", "0924", "0933", "0942", "0925", "0934", "0943", "0907",
            "0912", "0946", "0909", "0930", "0948", "0910", "0938", "0950", "0908", "0920",
            "0929", "0947", "0961", "0918", "0921", "0939", "0949", "0998", "0919", "0928",
            "0946", "0951", "0999"
        )
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        val firstDayOfTheMonth = Calendar.getInstance()
        firstDayOfTheMonth.set(Calendar.DAY_OF_MONTH, 1)
        firstDayOfTheMonth.time

        when (id) {
            0 -> {//SMS
                val where = "date >= ${firstDayOfTheMonth.time.time}"
                return (this@MainActivity as? Context)?.let { context ->
                    CursorLoader(
                        context,
                        Uri.parse("content://sms/sent"),
                        arrayOf("_id"),
                        where,
                        null,
                        "date DESC"
                    )
                } ?: throw Exception("Activity cannot be null")
            }
            else -> {//Calls
                val where = "${CallLog.Calls.TYPE}='$OUTGOING_TYPE' " +
                        "AND ${CallLog.Calls.DATE} >= ${firstDayOfTheMonth.time.time} "
                return (this@MainActivity as? Context)?.let { context ->
                    CursorLoader(
                        context,
                        CallLog.Calls.CONTENT_URI,
                        arrayOf(CallLog.Calls.NUMBER, CallLog.Calls.DURATION),
                        where,
                        null,
                        CallLog.Calls.DATE + " DESC"
                    )
                } ?: throw Exception("Activity cannot be null")
            }
        }
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        var consumable = CONSUMBALE
        when (loader.id) {
            0 -> {//SMS
                var countSMS = 0
                if (data?.count ?: 0 > 0) {
                    while (data?.moveToNext() == true) {
                        countSMS++
                    }
                }
                //TODO configurable SMS rate
                //TODO check for mulitpart SMS
                txtSMS.text = getString(
                    R.string.sms_rate, countSMS,
                    currency, countSMS.toDouble()
                )
                consumable -= countSMS.toDouble()
            }
            1 -> {//Calls
                val freeNumber = when {
                    checkNetwork(simNumber) == NETWORK_GLOBE -> "211"
                    checkNetwork(simNumber) == NETWORK_SMART -> "*888"
                    else -> ""
                }

                var minutesNetworkOn = 0L
                var minutesNetworkOff = 0L

                if (data?.count ?: 0 > 0) {
                    while (data?.moveToNext() == true) {
                        val number = data.getString(0)
                        val duration = data.getLong(1)

                        if (number == freeNumber) continue

                        if (checkNetwork(simNumber) == checkNetwork(number)) {
                            minutesNetworkOn += duration
                        } else {
                            minutesNetworkOff += duration
                        }
                    }
                }
                val minutesOn = TimeUnit.SECONDS.toMinutes(minutesNetworkOn).toDouble()
                val minutesOff = TimeUnit.SECONDS.toMinutes(minutesNetworkOn).toDouble()

                //TODO configurable rates
                val rateOn = minutesOn * 6.5
                val rateOff = minutesOff * 7.5

                txtCalls.text = getString(R.string.calls_rate, minutesOn, currency, rateOn)
                txtCallOthers.text = getString(R.string.calls_rate, minutesOff, currency, rateOff)

                consumable -= rateOn
                consumable -= rateOff
            }
        }

        txtConsumableRemaining.text = getString(R.string.rate, currency, consumable, CONSUMBALE)
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        TODO("Not yet implemented")
    }
}