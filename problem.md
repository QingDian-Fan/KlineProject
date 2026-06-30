# 已知问题与当前状态

> 下表是对原 [fujianlian/KLineChart](https://github.com/fujianlian/KLineChart) 历史 issue 在**当前代码**中的核查结论。
> 标注为「已修复 / 已内置解决」的无需再处理；「使用方式 / 待实现」的属于接入方职责或功能增强，非库缺陷。

## 已修复

### 数据很少时从右边开始显示（留大片空白）
https://github.com/fujianlian/KLineChart/issues/9

数据不足一屏时原先会贴着右边显示。已修复 `BaseKLineChartView.getMinTranslateX()`：
当 `!isFullScreen()` 时返回 `mPointWidth / 2`，使数据从左边开始排列。满屏时行为不变。

## 已内置解决

### 在 NestedScrollView 或 ScrollView 中左右滑动冲突
https://github.com/fujianlian/KLineChart/issues/13

`ScrollAndScaleView` 已内置 `handleParentIntercept` / `requestParentDisallowIntercept`：
横向拖动与双指缩放交给 K 线图，竖向滑动释放给外层滚动容器，**无需**再手动设置
`setOnTouchListener` 处理冲突。若仍有特殊嵌套场景冲突，可参考 issue #13 自查。

## 属于接入方使用方式（库本身不复现）

### 向尾部添加数据时尾部 MA 线形成一条竖线
https://github.com/fujianlian/KLineChart/issues/12

### 新增数据 addHeaderData 后指标线不见了
https://github.com/fujianlian/KLineChart/issues/1

根因是**分页时对每页单独调用 `DataHelper.calculate`**，导致跨页边界的均线/指标不连续。
正确做法：对**完整有序数据集**计算指标（参见 demo 的 `DataRequest.getALL`，全量
`DataHelper.calculate` 后再分页取子集），追加数据后对受影响范围重新计算。

## 待实现的功能增强（非缺陷）

### 展示 5 分钟、10 分钟等分时图
https://github.com/fujianlian/KLineChart/issues/5

当前分时线为单一周期。多周期分时需接入方按周期聚合数据后传入。

### 单击显示详情
https://github.com/fujianlian/KLineChart/issues/10
https://github.com/fujianlian/KLineChart/issues/19

当前仅长按显示十字线与详情框（`onLongPress`）。`onSingleTapUp` 目前返回 `false`，
如需单击选中，可在其中调用选中逻辑并回调 `OnSelectedChangedListener`。

## 需按场景验证

### 当数值过小时，macd 图只显示一条横线
https://github.com/fujianlian/KLineChart/issues/6

`calculateValue()` 已有兜底：`abs(mChildMaxValue) < 0.01 && abs(mChildMinValue) < 0.01`
时将上限置为 `1f`，常规数据下基本缓解。若出现极小量级数据仍异常，需结合实际数据复核。

### 时间轴文字挤在一起
https://github.com/fujianlian/KLineChart/issues/14

时间文字按 `gridColumns` 等分列绘制（demo 默认 4 列不重叠）。当列数较多或字体较大时
可能重叠，必要时减少 `setGridColumns` 或在 `drawText` 绘制日期时加入相邻文本避让。
