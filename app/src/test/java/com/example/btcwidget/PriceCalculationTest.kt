package com.example.btcwidget

import org.junit.Assert.assertEquals
import org.junit.Test

class PriceCalculationTest {

    @Test
    fun testMoscowTimeCalculation() {
        val btcPrice = 100000.0 // 100,000 USD per BTC
        val moscowTimeVal = (100_000_000.0 / btcPrice).toInt()
        assertEquals(1000, moscowTimeVal) // 100,000,000 / 100,000 = 1000 sats/USD
    }

    @Test
    fun testMstrBtcRatioCalculation() {
        val btcPrice = 100000.0
        val mstrPrice = 200.0
        val ratio = mstrPrice / btcPrice
        assertEquals(0.002, ratio, 0.0001) // 200 / 100,000 = 0.002 BTC
    }
}
