package com.reavitz.consumablestracker.ui.main

import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CallLog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.loader.app.LoaderManager
import androidx.loader.content.CursorLoader
import androidx.loader.content.Loader
import com.reavitz.consumablestracker.R
import com.reavitz.consumablestracker.Utils
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.concurrent.TimeUnit

class PlaceholderFragment : Fragment(), LoaderManager.LoaderCallbacks<Cursor> {

    var simInfo: SimInfo? = null
    private var isInstanceNew = true

    //TODO customizable currency
    private val currency = "PHP" //Currency.getInstance(Locale("ph")).symbol

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        simInfo = arguments?.getParcelable(ARG_SIM_INFO) as? SimInfo
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        lblSim1.text = simInfo?.label
    }

    override fun onResume() {
        super.onResume()
        if (isInstanceNew) {
            //for SMS
            LoaderManager.getInstance(this).initLoader(0, null, this)
            //for Calls
            LoaderManager.getInstance(this).initLoader(1, null, this)
        } else {
            LoaderManager.getInstance(this).restartLoader(0, null, this)
            LoaderManager.getInstance(this).restartLoader(1, null, this)
        }
    }

    override fun onPause() {
        super.onPause()
        isInstanceNew = false
    }

    override fun onCreateLoader(id: Int, args: Bundle?): Loader<Cursor> {
        //TODO customizable cycle
        val firstDayOfTheMonth = Calendar.getInstance()
        firstDayOfTheMonth.set(Calendar.DAY_OF_MONTH, 1)
        firstDayOfTheMonth.time

        when (id) {
            0 -> {//SMS
                val where = "date >= ${firstDayOfTheMonth.time.time} " +
                        "AND sub_id = '${simInfo?.lineNumber}' "
                return CursorLoader(
                    requireActivity(),
                    Uri.parse("content://sms/sent"),
                    arrayOf("_id"),
                    where,
                    null,
                    "date DESC"
                )
            }
            else -> {//Calls
                //TODO move to Utils
                val freeNumber = when {
                    Utils.checkNetwork(simInfo?.number ?: "") == Utils.NETWORK_GLOBE -> "211"
                    Utils.checkNetwork(simInfo?.number ?: "") == Utils.NETWORK_SMART -> "*888"
                    else -> ""
                }

                val where = "${CallLog.Calls.TYPE}='${CallLog.Calls.OUTGOING_TYPE}' " +
                        "AND ${CallLog.Calls.DATE} >= ${firstDayOfTheMonth.time.time} " +
                        "AND ${CallLog.Calls.PHONE_ACCOUNT_ID}='${simInfo?.subscriptionID}' " +
                        "AND ${CallLog.Calls.NUMBER}!='$freeNumber' "
                return CursorLoader(
                    requireActivity(),
                    CallLog.Calls.CONTENT_URI,
                    arrayOf(
                        CallLog.Calls.NUMBER, CallLog.Calls.DURATION
                    ),
                    where,
                    null,
                    CallLog.Calls.DATE + " DESC"
                )
            }
        }
    }

    override fun onLoadFinished(loader: Loader<Cursor>, data: Cursor?) {
        var consumable = Utils.CONSUMBALE
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
                var minutesNetworkOn = 0L
                var minutesNetworkOff = 0L

                if (data?.count ?: 0 > 0) {
                    while (data?.moveToNext() == true) {
                        val number = data.getString(0)
                        val duration = data.getLong(1)

                        if (Utils.checkNetwork(
                                simInfo?.number ?: ""
                            ) == Utils.checkNetwork(number)
                        ) {
                            minutesNetworkOn += duration
                        } else {
                            minutesNetworkOff += duration
                        }
                    }
                }
                val minutesOn = TimeUnit.SECONDS.toMinutes(minutesNetworkOn).toDouble()
                val minutesOff = TimeUnit.SECONDS.toMinutes(minutesNetworkOff).toDouble()

                //TODO configurable rates
                val rateOn = minutesOn * 6.5
                val rateOff = minutesOff * 7.5

                txtCalls.text = getString(R.string.calls_rate, minutesOn, currency, rateOn)
                txtCallOthers.text = getString(R.string.calls_rate, minutesOff, currency, rateOff)

                consumable -= rateOn
                consumable -= rateOff
            }
        }

        txtConsumableRemaining.text = getString(
            R.string.rate, currency, consumable,
            Utils.CONSUMBALE
        )
    }

    override fun onLoaderReset(loader: Loader<Cursor>) {
        TODO("Not yet implemented")
    }

    companion object {
        private const val ARG_SIM_INFO = "extra_sim"

        @JvmStatic
        fun newInstance(simInfo: SimInfo): PlaceholderFragment {
            return PlaceholderFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SIM_INFO, simInfo)
                }
            }
        }
    }
}