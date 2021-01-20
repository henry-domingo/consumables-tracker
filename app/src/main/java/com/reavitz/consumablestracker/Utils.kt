package com.reavitz.consumablestracker


//TODO if 211, then free
//TODO if same network then 6.50, else 7.50
object Utils {
    const val NETWORK_GLOBE = "Globe/TM"
    const val NETWORK_SMART = "Smart/Sun/TNT"
    const val NETWORK_UNKNOWN = "none"
    const val CONSUMBALE = 300.0//TODO configurable

    //TODO configurable networks
    val NUMBERS_GLOBE = listOf(
        "09173", "09178", "09256", "09175", "09253", "09257",
        "09176", "09255", "09258", "0817", "0905", "0917", "0936", "0954", "0966",
        "0978", "0997", "0906", "0926", "0937", "0955", "0967", "0979", "0915", "0927",
        "0945", "0956", "0975", "0995", "0916", "0935", "0953", "0965", "0977", "0996"
    )
    val NUMBERS_SMART = listOf(
        "0922", "0931", "0940", "0973", "0923", "0932",
        "0941", "0974", "0924", "0933", "0942", "0925", "0934", "0943", "0907",
        "0912", "0946", "0909", "0930", "0948", "0910", "0938", "0950", "0908", "0920",
        "0929", "0947", "0961", "0918", "0921", "0939", "0949", "0998", "0919", "0928",
        "0946", "0951", "0999", "0968"
    )

    fun checkNetwork(number: String): String {
        val cleanNumber = when {
            number.startsWith("+63") -> number.replace("+63", "0")
            number.startsWith("9") -> "0$number"
            else -> number
        }

        if (cleanNumber.length < 5) return NETWORK_UNKNOWN

        val subNumber = cleanNumber.subSequence(0, 4)
        return when {
            NUMBERS_GLOBE.contains(subNumber) -> return NETWORK_GLOBE
            NUMBERS_SMART.contains(subNumber) -> return NETWORK_SMART
            else -> NETWORK_UNKNOWN
        }
    }
}