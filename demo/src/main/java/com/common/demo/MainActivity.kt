package com.common.demo

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.common.kline.KLineChartAdapter
import com.common.kline.KLineChartView
import com.common.kline.draw.Status
import com.common.kline.formatter.DateFormatter
import com.common.demo.databinding.ActivityMainBinding
import java.util.*

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

    companion object {
        private const val PAGE_SIZE = 500
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
