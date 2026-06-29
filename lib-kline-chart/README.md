# lib-kline-chart

`lib-kline-chart` 是一个 Android K 线图组件库，核心控件为 `KLineChartView`。它支持蜡烛图、分时线、成交量图、长按选中、横向滑动、双指缩放、嵌套 `ScrollView/NestedScrollView` 场景下的横滑冲突处理，以及常见技术指标展示。

当前库基于 AndroidX：

- `minSdkVersion 24`
- `compileSdkVersion 32`
- 依赖 `androidx.appcompat:appcompat:1.0.2`

## 功能概览

- 主图指标：`MA`、`BOLL`、隐藏主图指标
- 副图指标：`MACD`、`KDJ`、`RSI`、`WR`
- 成交量图：成交量柱状图、成交量 MA5/MA10
- 图表交互：横向拖动、惯性滑动、双指缩放、长按十字线和详情框
- 加载更多：滑动到最左侧时触发 `KChartRefreshListener`
- 样式配置：支持 XML 属性和 Java/Kotlin API 设置颜色、线宽、字号、蜡烛宽度等

## 模块接入

在 app 模块中依赖库模块：

```groovy
dependencies {
    implementation project(':lib-kline-chart')
}
```

`settings.gradle` 中需要包含模块：

```groovy
include ':demo', ':lib-kline-chart'
```

## 布局使用

```xml
<com.common.kline.KLineChartView
    android:id="@+id/kLineChartView"
    android:layout_width="match_parent"
    android:layout_height="450dp"
    android:background="@color/colorPrimary" />
```

如果外层嵌套 `ScrollView` 或 `NestedScrollView`，库内部会根据手势方向处理父布局拦截：横向滑动和双指缩放时优先交给 K 线图，竖向滑动时释放给外层滚动容器。

## 基础初始化

```kotlin
private val adapter by lazy { KLineChartAdapter() }

binding.kLineChartView.adapter = adapter
binding.kLineChartView.dateTimeFormatter = DateFormatter()
binding.kLineChartView.setGridRows(4)
binding.kLineChartView.setGridColumns(4)
```

加载数据后需要通知适配器刷新：

```kotlin
val data = DataRequest.getData(context, offset, 500)
adapter.addFooterData(data)
adapter.notifyDataSetChanged()
binding.kLineChartView.startAnimation()
```

## 数据模型

默认数据实体为 `KLineEntity`，主要字段如下：

```java
public String Date;
public float Open;
public float High;
public float Low;
public float Close;
public float Volume;
```

指标字段包括：

- 价格均线：`MA5Price`、`MA10Price`、`MA20Price`、`MA30Price`、`MA60Price`
- MACD：`dif`、`dea`、`macd`
- KDJ：`k`、`d`、`j`
- RSI：`rsi`
- WR：`r`
- BOLL：`up`、`mb`、`dn`
- 成交量均线：`MA5Volume`、`MA10Volume`

如果原始数据只包含 OHLCV，可以调用 `DataHelper.calculate(dataList)` 计算指标：

```kotlin
DataHelper.calculate(data)
adapter.addFooterData(data)
adapter.notifyDataSetChanged()
```

## 分页加载

`KLineChartView` 滑到最左侧会触发加载更多回调：

```kotlin
private const val PAGE_SIZE = 500
private var loadedCount = 0

binding.kLineChartView.setRefreshListener { chart ->
    Thread {
        val moreData = DataRequest.getData(this, loadedCount, PAGE_SIZE)
        runOnUiThread {
            if (moreData.isNotEmpty()) {
                loadedCount += moreData.size
                adapter.addHeaderData(moreData)
                adapter.notifyDataSetChanged()
            }

            if (moreData.size < PAGE_SIZE) {
                chart.refreshEnd()
            } else {
                chart.refreshComplete()
            }
        }
    }.start()
}
```

加载状态相关 API：

- `justShowLoading()`：仅显示加载状态，常用于首次加载
- `showLoading()`：显示加载状态并触发刷新监听
- `refreshComplete()`：本次加载完成，后续仍可继续触发加载更多
- `refreshEnd()`：加载完成且没有更多数据
- `resetLoadMoreEnd()`：重置没有更多数据的状态

## 指标切换

主图指标：

```kotlin
binding.kLineChartView.changeMainDrawType(Status.MA)
binding.kLineChartView.changeMainDrawType(Status.BOLL)
binding.kLineChartView.changeMainDrawType(Status.NONE)
```

副图指标按内置顺序切换：

```kotlin
binding.kLineChartView.setChildDraw(0) // MACD
binding.kLineChartView.setChildDraw(1) // KDJ
binding.kLineChartView.setChildDraw(2) // RSI
binding.kLineChartView.setChildDraw(3) // WR
binding.kLineChartView.hideChildDraw()
```

K 线图和分时图切换：

```kotlin
binding.kLineChartView.setMainDrawLine(false) // K 线图
binding.kLineChartView.setMainDrawLine(true)  // 分时线
```

## 常用 API

```kotlin
binding.kLineChartView.setScrollEnable(true)
binding.kLineChartView.setScaleEnable(true)
binding.kLineChartView.setAnimationDuration(500)
binding.kLineChartView.setOverScrollRange(100f)
binding.kLineChartView.setPointWidth(6f)
binding.kLineChartView.setTextSize(12f)
binding.kLineChartView.setLineWidth(1f)
binding.kLineChartView.setGridRows(4)
binding.kLineChartView.setGridColumns(4)
```

长按选中回调：

```kotlin
binding.kLineChartView.setOnSelectedChangedListener { _, point, index ->
    // point 为当前选中的数据对象，index 为数据下标
}
```

## XML 样式属性

`KLineChartView` 支持在 XML 中配置以下属性：

| 属性 | 说明 |
| --- | --- |
| `kc_text_size` | 图表文字大小 |
| `kc_text_color` | 图表文字颜色 |
| `kc_line_width` | 指标线宽度 |
| `kc_background_color` | 图表背景色 |
| `kc_selected_line_color` | 长按选中线颜色 |
| `kc_selected_line_width` | 长按选中线宽度 |
| `kc_grid_line_width` | 网格线宽度 |
| `kc_grid_line_color` | 网格线颜色 |
| `kc_point_width` | 数据点间距 |
| `kc_macd_width` | MACD 柱宽度 |
| `kc_dif_color` | DIF 颜色 |
| `kc_dea_color` | DEA 颜色 |
| `kc_macd_color` | MACD 颜色 |
| `kc_k_color` | KDJ K 线颜色 |
| `kc_d_color` | KDJ D 线颜色 |
| `kc_j_color` | KDJ J 线颜色 |
| `kc_rsi1_color` | RSI1 颜色 |
| `kc_rsi2_color` | RSI2 颜色 |
| `kc_ris3_color` | RSI3 颜色 |
| `kc_up_color` | BOLL UP 颜色 |
| `kc_mb_color` | BOLL MB 颜色 |
| `kc_dn_color` | BOLL DN 颜色 |
| `kc_ma5_color` | MA5 颜色 |
| `kc_ma10_color` | MA10 颜色 |
| `kc_ma20_color` | MA20 颜色 |
| `kc_candle_width` | 蜡烛实体宽度 |
| `kc_candle_line_width` | 蜡烛影线宽度 |
| `kc_selector_background_color` | 长按详情框背景色 |
| `kc_selector_text_size` | 长按详情框文字大小 |
| `kc_candle_solid` | 蜡烛是否实心 |

示例：

```xml
<com.common.kline.KLineChartView
    android:id="@+id/kLineChartView"
    android:layout_width="match_parent"
    android:layout_height="450dp"
    app:kc_background_color="@color/chart_background"
    app:kc_grid_line_color="@color/chart_grid_line"
    app:kc_text_color="@color/chart_text"
    app:kc_point_width="6dp"
    app:kc_candle_width="5dp"
    app:kc_candle_solid="true" />
```

## 颜色资源

库内置了一组默认颜色，项目可以在宿主 app 中定义同名资源覆盖：

- `chart_red`
- `chart_green`
- `chart_line`
- `chart_line_background`
- `chart_ma5`
- `chart_ma10`
- `chart_ma30`
- `chart_white`
- `chart_bac`
- `chart_point_bac`
- `chart_grid_line`
- `chart_text`
- `chart_selector`

## 适配器说明

默认适配器为 `KLineChartAdapter`：

```kotlin
val adapter = KLineChartAdapter()
binding.kLineChartView.adapter = adapter
```

常用方法：

- `addFooterData(data)`：向尾部追加较新的数据
- `addHeaderData(data)`：向头部追加较旧的数据
- `changeItem(position, data)`：替换指定位置的数据
- `clearData()`：清空数据
- `notifyDataSetChanged()`：通知图表刷新

如需自定义数据源，可以继承 `BaseKLineChartAdapter` 或实现 `IAdapter`。

## 注意事项

- 数据应按时间从旧到新排列，分页加载更早数据时使用 `addHeaderData`。
- 指标字段需要提前计算。使用默认 `KLineEntity` 时可以直接调用 `DataHelper.calculate(dataList)`。
- `refreshEnd()` 表示没有更多数据，调用后不会继续自动触发加载更多；需要重新允许加载时调用 `resetLoadMoreEnd()`。
- 当前库已迁移到 AndroidX，不再依赖旧 Support 包。
- Demo 工程已经使用 ViewBinding，实际接入时可参考 `demo` 模块。
