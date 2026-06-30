package com.common.demo

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.common.kline.DataHelper
import com.common.kline.KLineChartAdapter
import com.common.kline.KLineChartView
import com.common.kline.KLineEntity
import com.common.kline.draw.Status
import com.common.kline.formatter.DateFormatter
import com.common.demo.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var loadedCount = 0

    private val adapter by lazy { KLineChartAdapter() }

    private val subTexts: ArrayList<TextView> by lazy {
        arrayListOf(binding.macdText, binding.kdjText, binding.rsiText, binding.wrText)
    }
    // 主图指标下标
    private var mainIndex = 0
    // 副图指标下标
    private var subIndex = -1

    // ---- 实时行情演示 ----
    private val realtimeHandler = Handler(Looper.getMainLooper())
    private var isRealtime = false
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val realtimeRunnable = object : Runnable {
        override fun run() {
            pushTick()
            realtimeHandler.postDelayed(this, TICK_INTERVAL_MILLIS)
        }
    }

    companion object {
        private const val PAGE_SIZE = 500

        // 演示用：每根 K 线代表 3 秒（真实场景应为 1 分钟等），方便快速看到「新增一根」
        private const val DEMO_PERIOD_MILLIS = 3_000L
        // 每 150ms 推送一个 tick（模拟一分钟更新很多次的高频行情）
        private const val TICK_INTERVAL_MILLIS = 150L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.kLineChartView.adapter = adapter
        binding.kLineChartView.dateTimeFormatter = DateFormatter()
        binding.kLineChartView.setGridRows(4)
        binding.kLineChartView.setGridColumns(4)
        initData()
        initListener()
    }

    private fun initData() {
        binding.kLineChartView.justShowLoading()
        Thread {
            val data = DataRequest.getData(this@MainActivity, loadedCount, PAGE_SIZE)
            runOnUiThread {
                if (data.isNotEmpty()) {
                    loadedCount += data.size
                    adapter.addFooterData(data)
                    adapter.notifyDataSetChanged()
                    binding.kLineChartView.startAnimation()
                }
                finishLoading(binding.kLineChartView, data.size)
            }
        }.start()
    }

    private fun initListener() {
        binding.maText.setOnClickListener {
            if (mainIndex != 0) {
                binding.kLineChartView.hideSelectData()
                mainIndex = 0
                binding.maText.setTextColor(Color.parseColor("#eeb350"))
                binding.bollText.setTextColor(Color.WHITE)
                binding.kLineChartView.changeMainDrawType(Status.MA)
            }
        }
        binding.bollText.setOnClickListener {
            if (mainIndex != 1) {
                binding.kLineChartView.hideSelectData()
                mainIndex = 1
                binding.bollText.setTextColor(Color.parseColor("#eeb350"))
                binding.maText.setTextColor(Color.WHITE)
                binding.kLineChartView.changeMainDrawType(Status.BOLL)
            }
        }
        binding.mainHide.setOnClickListener {
            if (mainIndex != -1) {
                binding.kLineChartView.hideSelectData()
                mainIndex = -1
                binding.bollText.setTextColor(Color.WHITE)
                binding.maText.setTextColor(Color.WHITE)
                binding.kLineChartView.changeMainDrawType(Status.NONE)
            }
        }
        for ((index, text) in subTexts.withIndex()) {
            text.setOnClickListener {
                if (subIndex != index) {
                    binding.kLineChartView.hideSelectData()
                    if (subIndex != -1) {
                        subTexts[subIndex].setTextColor(Color.WHITE)
                    }
                    subIndex = index
                    text.setTextColor(Color.parseColor("#eeb350"))
                    binding.kLineChartView.setChildDraw(subIndex)
                }
            }
        }
        binding.subHide.setOnClickListener {
            if (subIndex != -1) {
                binding.kLineChartView.hideSelectData()
                subTexts[subIndex].setTextColor(Color.WHITE)
                subIndex = -1
                binding.kLineChartView.hideChildDraw()
            }
        }
        binding.fenText.setOnClickListener {
            binding.kLineChartView.hideSelectData()
            binding.fenText.setTextColor(Color.parseColor("#eeb350"))
            binding.kText.setTextColor(Color.WHITE)
            binding.kLineChartView.setMainDrawLine(true)
        }
        binding.kText.setOnClickListener {
            binding.kLineChartView.hideSelectData()
            binding.kText.setTextColor(Color.parseColor("#eeb350"))
            binding.fenText.setTextColor(Color.WHITE)
            binding.kLineChartView.setMainDrawLine(false)
        }
        binding.kLineChartView.setRefreshListener { chart ->
            loadMoreData(chart)
        }
        binding.realtimeText.setOnClickListener {
            toggleRealtime()
        }
    }

    /** 切换实时行情推送的开/关 */
    private fun toggleRealtime() {
        if (isRealtime) {
            stopRealtime()
        } else {
            // 需要先有数据才能在其后追加实时行情
            if (adapter.getCount() == 0) return
            startRealtime()
        }
    }

    private fun startRealtime() {
        isRealtime = true
        binding.realtimeText.text = "实时⏸"
        binding.realtimeText.setTextColor(Color.parseColor("#eeb350"))
        realtimeHandler.post(realtimeRunnable)
    }

    private fun stopRealtime() {
        isRealtime = false
        binding.realtimeText.text = "实时▶"
        binding.realtimeText.setTextColor(Color.WHITE)
        realtimeHandler.removeCallbacks(realtimeRunnable)
    }

    /**
     * 模拟收到一个实时 tick：在当前周期内更新最后一根，跨周期则新增一根。
     * 真实场景中 tick 来自 socket 回调（子线程），需切回主线程再更新图表。
     */
    private fun pushTick() {
        val last = adapter.getLastData() ?: return
        val basePrice = if (last.Close > 0f) last.Close else 100f
        // 在基准价上下做小幅随机游走
        val price = (basePrice + (Random.nextFloat() - 0.5f) * basePrice * 0.01f).coerceAtLeast(0.01f)
        val volume = 1000f + Random.nextInt(0, 5000)
        val barTime = System.currentTimeMillis() / DEMO_PERIOD_MILLIS * DEMO_PERIOD_MILLIS

        if (last.barTime == barTime) {
            // 同一根：原地更新 OHLCV
            last.Close = price
            last.High = maxOf(last.High, price)
            last.Low = minOf(last.Low, price)
            last.Volume += volume
            DataHelper.calculate(adapter.getDatas())
            adapter.updateLast(last)
        } else {
            // 新的一根
            val bar = KLineEntity().apply {
                this.barTime = barTime
                Date = timeFormat.format(Date(barTime))
                Open = price
                High = price
                Low = price
                Close = price
                Volume = volume
            }
            adapter.addFooterData(listOf(bar))
            DataHelper.calculate(adapter.getDatas())
            adapter.notifyDataSetChanged()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        realtimeHandler.removeCallbacks(realtimeRunnable)
    }

    private fun loadMoreData(chart: KLineChartView) {
        Thread {
            val moreData = DataRequest.getData(this@MainActivity, loadedCount, PAGE_SIZE)
            runOnUiThread {
                if (moreData.isNotEmpty()) {
                    loadedCount += moreData.size
                    adapter.addHeaderData(moreData)
                    adapter.notifyDataSetChanged()
                }
                finishLoading(chart, moreData.size)
            }
        }.start()
    }

    private fun finishLoading(chart: KLineChartView, loadSize: Int) {
        if (loadSize < PAGE_SIZE) {
            chart.refreshEnd()
        } else {
            chart.refreshComplete()
        }
    }
}
