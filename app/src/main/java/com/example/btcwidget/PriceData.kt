package com.example.btcwidget

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class PriceData(
    val btcUsd: Double,
    val moscowTime: Int,
    val mstrUsd: Double,
    val mstrBtc: Double,
    val blockHeight: Int,
    val mstrBtcHeld: Double,
    val btcCirculation: Double,
    val timestamp: Long
)

object PriceRepository {
    private const val TAG = "PriceRepository"
    private const val PREFS_NAME = "btc_widget_prefs"
    private const val KEY_BTC_USD = "btc_usd"
    private const val KEY_MOSCOW_TIME = "moscow_time"
    private const val KEY_MSTR_USD = "mstr_usd"
    private const val KEY_MSTR_BTC = "mstr_btc"
    private const val KEY_BLOCK_HEIGHT = "block_height"
    private const val KEY_MSTR_BTC_HELD = "mstr_btc_held"
    private const val KEY_BTC_CIRCULATION = "btc_circulation"
    private const val KEY_TIMESTAMP = "timestamp"

    fun fetchPriceData(): PriceData? {
        return try {
            // 1. Fetch BTC/USD
            val btcUrl = URL("https://api.coinbase.com/v2/prices/BTC-USD/spot")
            val btcConn = btcUrl.openConnection() as HttpURLConnection
            btcConn.requestMethod = "GET"
            btcConn.connectTimeout = 8000
            btcConn.readTimeout = 8000
            val btcStream = btcConn.inputStream
            val btcReader = BufferedReader(InputStreamReader(btcStream))
            val btcResponse = btcReader.use { it.readText() }
            btcConn.disconnect()
            
            val btcObj = JSONObject(btcResponse)
            val btcPrice = btcObj.getJSONObject("data").getString("amount").toDouble()

            // 2. Fetch MSTR/USD
            val mstrUrl = URL("https://query1.finance.yahoo.com/v8/finance/chart/MSTR")
            val mstrConn = mstrUrl.openConnection() as HttpURLConnection
            mstrConn.requestMethod = "GET"
            mstrConn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            mstrConn.connectTimeout = 8000
            mstrConn.readTimeout = 8000
            val mstrStream = mstrConn.inputStream
            val mstrReader = BufferedReader(InputStreamReader(mstrStream))
            val mstrResponse = mstrReader.use { it.readText() }
            mstrConn.disconnect()

            val mstrObj = JSONObject(mstrResponse)
            val result = mstrObj.getJSONObject("chart").getJSONArray("result").getJSONObject(0)
            val mstrPrice = result.getJSONObject("meta").getDouble("regularMarketPrice")

            // 3. Fetch Block Height
            val blockUrl = URL("https://mempool.space/api/blocks/tip/height")
            val blockConn = blockUrl.openConnection() as HttpURLConnection
            blockConn.requestMethod = "GET"
            blockConn.connectTimeout = 8000
            blockConn.readTimeout = 8000
            val blockStream = blockConn.inputStream
            val blockReader = BufferedReader(InputStreamReader(blockStream))
            val blockResponse = blockReader.use { it.readText() }.trim()
            blockConn.disconnect()

            val height = blockResponse.toInt()

            // Calculate metrics
            val moscowTimeVal = if (btcPrice > 0) (100_000_000.0 / btcPrice).toInt() else 0
            val mstrBtcRatio = if (btcPrice > 0) mstrPrice / btcPrice else 0.0

            // 4. Fetch BTC in circulation
            var circulationVal = 19850000.0
            try {
                val circUrl = URL("https://blockchain.info/q/totalbc")
                val circConn = circUrl.openConnection() as HttpURLConnection
                circConn.requestMethod = "GET"
                circConn.connectTimeout = 8000
                circConn.readTimeout = 8000
                val circStream = circConn.inputStream
                val circReader = BufferedReader(InputStreamReader(circStream))
                val circResponse = circReader.use { it.readText() }.trim()
                circConn.disconnect()
                circulationVal = circResponse.toDouble() / 100_000_000.0
            } catch (e: Exception) {
                Log.e(TAG, "Error fetching circulating supply, using fallback", e)
            }

            // MSTR BTC held: as of May 20, 2026, MicroStrategy holds exactly 843,738 BTC.
            val mstrBtcHeldVal = 843738.0

            PriceData(
                btcUsd = btcPrice,
                moscowTime = moscowTimeVal,
                mstrUsd = mstrPrice,
                mstrBtc = mstrBtcRatio,
                blockHeight = height,
                mstrBtcHeld = mstrBtcHeldVal,
                btcCirculation = circulationVal,
                timestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching price data", e)
            null
        }
    }

    fun saveToPrefs(context: Context, data: PriceData) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putFloat(KEY_BTC_USD, data.btcUsd.toFloat())
            putInt(KEY_MOSCOW_TIME, data.moscowTime)
            putFloat(KEY_MSTR_USD, data.mstrUsd.toFloat())
            putFloat(KEY_MSTR_BTC, data.mstrBtc.toFloat())
            putInt(KEY_BLOCK_HEIGHT, data.blockHeight)
            putFloat(KEY_MSTR_BTC_HELD, data.mstrBtcHeld.toFloat())
            putFloat(KEY_BTC_CIRCULATION, data.btcCirculation.toFloat())
            putLong(KEY_TIMESTAMP, data.timestamp)
            apply()
        }
    }

    fun getFromPrefs(context: Context): PriceData? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_TIMESTAMP)) return null
        
        return PriceData(
            btcUsd = prefs.getFloat(KEY_BTC_USD, 0f).toDouble(),
            moscowTime = prefs.getInt(KEY_MOSCOW_TIME, 0),
            mstrUsd = prefs.getFloat(KEY_MSTR_USD, 0f).toDouble(),
            mstrBtc = prefs.getFloat(KEY_MSTR_BTC, 0f).toDouble(),
            blockHeight = prefs.getInt(KEY_BLOCK_HEIGHT, 0),
            mstrBtcHeld = prefs.getFloat(KEY_MSTR_BTC_HELD, 0f).toDouble(),
            btcCirculation = prefs.getFloat(KEY_BTC_CIRCULATION, 0f).toDouble(),
            timestamp = prefs.getLong(KEY_TIMESTAMP, 0L)
        )
    }
}
