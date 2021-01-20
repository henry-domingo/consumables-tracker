package com.reavitz.consumablestracker.ui.main

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class SimInfo(
    val lineNumber: Int,
    val label: String,
    val number: String,
    val subscriptionID: String
) : Parcelable