# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

Android K-line (candlestick) charting library and demo app. Originally forked from
[tifezh/KChartView](https://github.com/tifezh/KChartView), migrated to AndroidX and split into
two modules. The library renders candlesticks, time-share lines, volume, a long-press crosshair,
horizontal scroll, two-finger zoom, and technical indicators (MA, BOLL, MACD, KDJ, RSI, WR).

## Build & Test

The Gradle daemon is pinned to **JDK 11** via `org.gradle.java.home` in `gradle.properties`. The
Android SDK path is in `local.properties` (untracked). Use the wrapper (`./gradlew`); AGP 7.1.0 /
Gradle 7.2 / Kotlin 1.5.31.

```bash
./gradlew assembleDebug                       # build everything
./gradlew :demo:assembleDebug                 # build just the demo APK
./gradlew :lib-kline-chart:assembleRelease    # build the library

./gradlew test                                # all JVM unit tests
./gradlew :demo:testDebugUnitTest             # one module's unit tests
./gradlew :lib-kline-chart:connectedAndroidTest   # instrumented tests (needs a device/emulator)

# verify only that test source compiles (no device required)
./gradlew :demo:compileDebugAndroidTestKotlin :lib-kline-chart:compileDebugAndroidTestKotlin
```

Note: the test sources are the empty AndroidStudio stubs (`ExampleUnitTest`, `ExampleInstrumentedTest`);
there is no real test suite. Build verification is the practical check. The wrapper `distributionUrl`
points at a Tencent mirror.

## Modules

- `lib-kline-chart` — the chart library, package `com.common.kline`. Kotlin + Android views.
- `demo` — sample app, package `com.common.demo` (Kotlin, ViewBinding). Loads bundled `assets/ibm.json`
  via `DataRequest.getData(context, offset, size)` and paginates it 500 rows at a time. Entry point:
  `demo/src/main/java/com/common/demo/MainActivity.kt`.

## Architecture

### View hierarchy (three layers, each adds one concern)

1. **`ScrollAndScaleView`** (abstract, extends `RelativeLayout`) — raw gesture layer. Owns the
   `GestureDetector`/`ScaleGestureDetector`/`OverScroller`, `mScrollX`, `mScaleX`, fling/over-scroll,
   long-press state, and the parent-intercept logic that resolves scroll conflicts when nested in a
   `ScrollView`/`NestedScrollView` (horizontal drag + pinch stay with the chart; vertical scroll is
   released to the parent).
2. **`BaseKLineChartView`** (abstract, ~1300 lines) — the rendering engine. This is where almost all
   logic lives. It maps data ↔ pixels and orchestrates the draw modules.
3. **`KLineChartView`** — the public, instantiable control. Wires up the concrete draw modules,
   parses XML attributes (`R.styleable.KLineChartView`, prefix `kc_`), and adds the loading
   `ProgressBar` + pull-to-load-more (`KChartRefreshListener`, triggered on scroll to the far left).

### Rendering pipeline

`onDraw` (in `BaseKLineChartView`) runs every frame: `calculateValue()` recomputes the visible index
window (`mStartIndex`..`mStopIndex`) and the per-region max/min, then it calls
`drawGird → drawK → drawText → drawMaxAndMin → drawValue`. `drawK` loops the visible range and, for
each point, delegates to each draw module's `drawTranslated(...)`.

Coordinate model: data X is virtual pixels (`index * mPointWidth`); `mTranslateX` + `mScaleX`
convert to screen via `getX`/`translateXtoX`/`xToTranslateX`/`indexOfTranslateX`. Y values map
through `getMainY`/`getVolY`/`getChildY` using per-region scale factors. The screen is split into
`mMainRect` / `mVolRect` / `mChildRect` in `initRect()` (sub-chart visible: 60/20/20; hidden: 75/25).

### Draw modules (`draw/`, the strategy plug-ins)

Each implements `IChartDraw<T>` and is responsible for one indicator: it draws a segment between two
adjacent points and reports `getMaxValue`/`getMinValue` so the engine can scale that region.

- `MainDraw` — candles or the time-share line; overlays the active main indicator (MA / BOLL), toggled
  by `Status` (`MA`, `BOLL`, `NONE`) via `changeMainDrawType`. `setMainDrawLine(true)` switches to the
  time-share line.
- `VolumeDraw` — volume bars + volume MA. Registered with `setVolDraw`.
- `MACDDraw`, `KDJDraw`, `RSIDraw`, `WRDraw` — the sub-chart indicators. Registered (in this order)
  with `addChildDraw` in `KLineChartView.initView()`; selected by index with `setChildDraw(0..3)` and
  hidden with `hideChildDraw()`.

To add an indicator: implement `IChartDraw`, register it with `addChildDraw`/`setVolDraw`/`setMainDraw`,
and add any new fields to `KLineEntity` + a calculation in `DataHelper`.

### Data flow

- **Adapter** — `IAdapter` ← `BaseKLineChartAdapter` ← `KLineChartAdapter`. Uses a `DataSetObserver`;
  the view registers as an observer and calls `notifyChanged()` on data change. Append newer data with
  `addFooterData`, older data (pagination) with `addHeaderData`. Data must be ordered oldest→newest.
- **Entity** — `KLineEntity` implements the per-indicator interfaces in `entity/` (`ICandle`, `IMACD`,
  `IKDJ`, `IRSI`, `IWR`, `IVolume`, `IKLine`); each draw module consumes only its interface.
- **Indicators** — `DataHelper.calculate(list)` fills all indicator fields (MA → MACD → BOLL → RSI →
  KDJ → WR → volume MA; BOLL depends on MA being computed first). Call it before feeding raw OHLCV data.
- **Formatting** — `formatter/` holds `IValueFormatter`/`IDateTimeFormatter` implementations
  (`ValueFormatter`, `BigValueFormatter`, `DateFormatter`, `TimeFormatter`).

## Conventions

- Library and demo code are both **Kotlin**. Comments and READMEs are in Chinese — match the
  existing language when editing those files.
- XML attributes use the `kc_` prefix; default colors/dimens live in the library's `res/` and can be
  overridden by same-named resources in the host app (see `lib-kline-chart/README.md`).
- Known issues / upstream-issue references are tracked in `problem.md`.