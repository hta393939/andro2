package com.example.myapplication

import kotlin.experimental.and
import kotlin.experimental.or

class FeatureReport(val bytes: ByteArray = ByteArray(1){
    0x01.toByte()
}) {
    var wheelResolutionMultiplier: Boolean
        get() = bytes[0] and 0b1 != 0.toByte()
        set(value) {
            bytes[0] = if (value)
                // ビットを立てる
                bytes[0] or 0b1
            else
                bytes[0] and 0b1110
        }

    var acPanResolutionMultiplier: Boolean
        get() = bytes[0] and 0b100 != 0.toByte()
        set(value) {
            bytes[0] = if (value)
                // ビットを立てる
                bytes[0] or 0b100
            else
                bytes[0] and 0b1011
        }

    companion object {
        const val ID = 0.toByte()
    }
}
