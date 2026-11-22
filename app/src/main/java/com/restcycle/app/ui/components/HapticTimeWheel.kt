package com.restcycle.app.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.restcycle.app.R
import com.restcycle.app.ui.RestCycleTestTags
import com.restcycle.app.ui.theme.AtmosphereTheme
import com.restcycle.app.ui.theme.TabularTextStyles
import com.restcycle.app.ui.util.formatClockTime
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import java.time.LocalTime
import kotlin.math.abs

private const val WHEEL_REPEAT_CYCLES = 400
private const val WHEEL_MID_CYCLE = WHEEL_REPEAT_CYCLES / 2

@Composable
fun HapticTimeWheel(
    selectedTime: LocalTime,
    is24HourFormat: Boolean,
    onTimeChange: (LocalTime) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 42.dp,
    wheelTestTag: String = RestCycleTestTags.TIME_WHEEL,
    hourTestTag: String = RestCycleTestTags.WHEEL_HOUR,
    minuteTestTag: String = RestCycleTestTags.WHEEL_MINUTE,
    amPmTestTag: String = RestCycleTestTags.WHEEL_AM_PM
) {
    val palette = AtmosphereTheme.palette
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val wheelHeight = itemHeight * 3 // 126.dp default (>= 110.dp)

    val amLabel = stringResource(R.string.wheel_am)
    val pmLabel = stringResource(R.string.wheel_pm)
    val hoursCd = stringResource(R.string.cd_time_wheel_hours)
    val minutesCd = stringResource(R.string.cd_time_wheel_minutes)
    val amPmCd = stringResource(R.string.cd_time_wheel_am_pm)

    val hourCount = if (is24HourFormat) 24 else 12
    val targetHourSlot = if (is24HourFormat) selectedTime.hour else to12HourIndex(selectedTime.hour)
    val targetMinuteSlot = selectedTime.minute
    val targetIsPm = selectedTime.hour >= 12

    val hourListState = remember(is24HourFormat) {
        LazyListState(firstVisibleItemIndex = WHEEL_MID_CYCLE * hourCount + targetHourSlot)
    }
    val minuteListState = rememberLazyListState(
        initialFirstVisibleItemIndex = WHEEL_MID_CYCLE * 60 + targetMinuteSlot
    )
    val amPmListState = remember(is24HourFormat) {
        LazyListState(firstVisibleItemIndex = if (targetIsPm) 1 else 0)
    }

    var lastCommittedHourSlot by remember(is24HourFormat) { mutableIntStateOf(targetHourSlot) }
    var lastCommittedHalfDayBucket by remember(is24HourFormat) {
        mutableIntStateOf(halfDayBucket12h(WHEEL_MID_CYCLE * hourCount + targetHourSlot))
    }
    var lastCommittedMinuteSlot by remember { mutableIntStateOf(targetMinuteSlot) }
    var lastCommittedIsPm by remember(is24HourFormat) { mutableStateOf(targetIsPm) }
    var isProgrammaticSync by remember { mutableStateOf(false) }

    val latestOnTimeChange by rememberUpdatedState(onTimeChange)
    val latestIs24Hour by rememberUpdatedState(is24HourFormat)

    // Silent programmatic synchronization on external time change or 12h/24h format switch
    LaunchedEffect(selectedTime, is24HourFormat) {
        val desiredHourSlot = if (is24HourFormat) selectedTime.hour else to12HourIndex(selectedTime.hour)
        val desiredMinuteSlot = selectedTime.minute
        val desiredIsPm = selectedTime.hour >= 12

        isProgrammaticSync = true
        try {
            if (!hourListState.isScrollInProgress) {
                val currentCenteredHourIndex =
                    centeredIndexOf(hourListState) ?: hourListState.firstVisibleItemIndex
                lastCommittedHourSlot = desiredHourSlot
                val syncedHourIndex = if (Math.floorMod(currentCenteredHourIndex, hourCount) != desiredHourSlot) {
                    val targetIdx = nearestVirtualIndex(currentCenteredHourIndex, hourCount, desiredHourSlot)
                    hourListState.scrollToItem(targetIdx)
                    targetIdx
                } else {
                    currentCenteredHourIndex
                }
                lastCommittedHalfDayBucket = halfDayBucket12h(syncedHourIndex)
            }

            if (!minuteListState.isScrollInProgress) {
                val currentCenteredMinIndex =
                    centeredIndexOf(minuteListState) ?: minuteListState.firstVisibleItemIndex
                lastCommittedMinuteSlot = desiredMinuteSlot
                if (Math.floorMod(currentCenteredMinIndex, 60) != desiredMinuteSlot) {
                    val targetIdx = nearestVirtualIndex(currentCenteredMinIndex, 60, desiredMinuteSlot)
                    minuteListState.scrollToItem(targetIdx)
                }
            }

            if (!is24HourFormat) {
                if (!amPmListState.isScrollInProgress) {
                    val desiredAmPmIdx = if (desiredIsPm) 1 else 0
                    val currentAmPmIdx =
                        (centeredIndexOf(amPmListState) ?: amPmListState.firstVisibleItemIndex)
                            .coerceIn(0, 1)
                    lastCommittedIsPm = desiredIsPm
                    if (currentAmPmIdx != desiredAmPmIdx) {
                        amPmListState.scrollToItem(desiredAmPmIdx)
                    }
                }
            } else {
                lastCommittedIsPm = desiredIsPm
            }
        } finally {
            isProgrammaticSync = false
        }
    }

    // Observe user-driven hour scroll steps (and auto-toggle AM/PM across 11 <-> 12 in 12h mode)
    LaunchedEffect(hourListState, is24HourFormat) {
        snapshotFlow { centeredIndexOf(hourListState) }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { rawIndex ->
                val slot = Math.floorMod(rawIndex, hourCount)
                val bucket = halfDayBucket12h(rawIndex)
                if (!isProgrammaticSync && (slot != lastCommittedHourSlot || (!latestIs24Hour && bucket != lastCommittedHalfDayBucket))) {
                    if (!latestIs24Hour) {
                        val bucketDelta = abs(bucket - lastCommittedHalfDayBucket)
                        if (bucketDelta % 2 == 1) {
                            lastCommittedIsPm = !lastCommittedIsPm
                            val targetAmPmIdx = if (lastCommittedIsPm) 1 else 0
                            coroutineScope.launch {
                                amPmListState.animateScrollToItem(targetAmPmIdx)
                            }
                        }
                    }
                    lastCommittedHalfDayBucket = bucket
                    lastCommittedHourSlot = slot
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    val newHour24 = if (latestIs24Hour) {
                        slot
                    } else {
                        to24Hour(hour12 = slot + 1, isPm = lastCommittedIsPm)
                    }
                    latestOnTimeChange(LocalTime.of(newHour24, lastCommittedMinuteSlot))
                } else if (isProgrammaticSync) {
                    lastCommittedHalfDayBucket = bucket
                }
            }
    }

    // Observe user-driven minute scroll steps
    LaunchedEffect(minuteListState, is24HourFormat) {
        snapshotFlow { centeredIndexOf(minuteListState)?.let { Math.floorMod(it, 60) } }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { slot ->
                if (!isProgrammaticSync && slot != lastCommittedMinuteSlot) {
                    lastCommittedMinuteSlot = slot
                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    val newHour24 = if (latestIs24Hour) {
                        lastCommittedHourSlot
                    } else {
                        to24Hour(hour12 = lastCommittedHourSlot + 1, isPm = lastCommittedIsPm)
                    }
                    latestOnTimeChange(LocalTime.of(newHour24, slot))
                }
            }
    }

    // Observe user-driven AM/PM scroll steps when in 12h mode
    LaunchedEffect(amPmListState, is24HourFormat) {
        if (!is24HourFormat) {
            snapshotFlow { centeredIndexOf(amPmListState)?.coerceIn(0, 1) }
                .filterNotNull()
                .distinctUntilChanged()
                .collect { idx ->
                    val newIsPm = idx == 1
                    if (!isProgrammaticSync && newIsPm != lastCommittedIsPm) {
                        lastCommittedIsPm = newIsPm
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        val newHour24 = to24Hour(hour12 = lastCommittedHourSlot + 1, isPm = newIsPm)
                        latestOnTimeChange(LocalTime.of(newHour24, lastCommittedMinuteSlot))
                    }
                }
        }
    }

    val centeredHourIndex by remember(hourListState, is24HourFormat) {
        derivedStateOf { centeredIndexOf(hourListState) ?: (WHEEL_MID_CYCLE * hourCount + targetHourSlot) }
    }
    val centeredMinuteIndex by remember(minuteListState) {
        derivedStateOf { centeredIndexOf(minuteListState) ?: (WHEEL_MID_CYCLE * 60 + targetMinuteSlot) }
    }
    val centeredAmPmIndex by remember(amPmListState, is24HourFormat) {
        derivedStateOf { (centeredIndexOf(amPmListState) ?: if (targetIsPm) 1 else 0).coerceIn(0, 1) }
    }

    val displayedHourText = if (is24HourFormat) {
        selectedTime.hour.toString().padStart(2, '0')
    } else {
        to12HourDisplay(selectedTime.hour).toString()
    }
    val displayedMinuteText = selectedTime.minute.toString().padStart(2, '0')
    val displayedAmPmText = if (selectedTime.hour >= 12) pmLabel else amLabel
    val formattedWheelTime = formatClockTime(selectedTime, is24HourFormat)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(wheelHeight)
            .testTag(wheelTestTag)
            .semantics {
                stateDescription = formattedWheelTime
            },
        contentAlignment = Alignment.Center
    ) {
        // Center selection band
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight),
            shape = RoundedCornerShape(16.dp),
            color = palette.accent.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, palette.accent.copy(alpha = 0.36f))
        ) {}

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(wheelHeight)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.00f to Color.Transparent,
                            0.14f to Color.Black,
                            0.86f to Color.Black,
                            1.00f to Color.Transparent,
                            startY = 0f,
                            endY = size.height
                        ),
                        blendMode = BlendMode.DstIn
                    )
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelColumn(
                listState = hourListState,
                itemCount = hourCount * WHEEL_REPEAT_CYCLES,
                centeredIndex = centeredHourIndex,
                itemHeight = itemHeight,
                wheelHeight = wheelHeight,
                width = 76.dp,
                testTag = hourTestTag,
                stateDesc = displayedHourText,
                contentDesc = hoursCd,
                labelForIndex = { index ->
                    val slot = Math.floorMod(index, hourCount)
                    if (is24HourFormat) {
                        slot.toString().padStart(2, '0')
                    } else {
                        (slot + 1).toString()
                    }
                },
                onItemClick = { index ->
                    coroutineScope.launch { hourListState.animateScrollToItem(index) }
                }
            )

            Text(
                text = ":",
                style = TabularTextStyles.wheelDigitSelected,
                color = palette.textPrimary,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            WheelColumn(
                listState = minuteListState,
                itemCount = 60 * WHEEL_REPEAT_CYCLES,
                centeredIndex = centeredMinuteIndex,
                itemHeight = itemHeight,
                wheelHeight = wheelHeight,
                width = 76.dp,
                testTag = minuteTestTag,
                stateDesc = displayedMinuteText,
                contentDesc = minutesCd,
                labelForIndex = { index ->
                    Math.floorMod(index, 60).toString().padStart(2, '0')
                },
                onItemClick = { index ->
                    coroutineScope.launch { minuteListState.animateScrollToItem(index) }
                }
            )

            if (!is24HourFormat) {
                WheelColumn(
                    listState = amPmListState,
                    itemCount = 2,
                    centeredIndex = centeredAmPmIndex,
                    itemHeight = itemHeight,
                    wheelHeight = wheelHeight,
                    width = 68.dp,
                    testTag = amPmTestTag,
                    stateDesc = displayedAmPmText,
                    contentDesc = amPmCd,
                    labelForIndex = { index -> if (index == 0) amLabel else pmLabel },
                    onItemClick = { index ->
                        coroutineScope.launch { amPmListState.animateScrollToItem(index) }
                    },
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun WheelColumn(
    listState: LazyListState,
    itemCount: Int,
    centeredIndex: Int,
    itemHeight: Dp,
    wheelHeight: Dp,
    width: Dp,
    testTag: String,
    stateDesc: String,
    contentDesc: String,
    labelForIndex: (Int) -> String,
    onItemClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = AtmosphereTheme.palette
    val density = LocalDensity.current
    val cameraDistancePx = 10f * density.density
    val baseSnapProvider = remember(listState) { SnapLayoutInfoProvider(listState) }
    val wheelSnapProvider = remember(baseSnapProvider, density) {
        object : SnapLayoutInfoProvider by baseSnapProvider {
            override fun calculateApproachOffset(velocity: Float, decayOffset: Float): Float {
                val velocityDpPerSec = abs(velocity) / density.density
                return if (velocityDpPerSec < 1200f) {
                    0f
                } else {
                    baseSnapProvider.calculateApproachOffset(velocity, decayOffset) * 0.4f
                }
            }
        }
    }
    val snapBehavior = rememberSnapFlingBehavior(snapLayoutInfoProvider = wheelSnapProvider)

    LazyColumn(
        state = listState,
        flingBehavior = snapBehavior,
        contentPadding = PaddingValues(vertical = itemHeight),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(width)
            .height(wheelHeight)
            .testTag(testTag)
            .semantics {
                stateDescription = stateDesc
                contentDescription = contentDesc
            }
    ) {
        items(count = itemCount) { index ->
            val isCentered = index == centeredIndex
            Box(
                modifier = Modifier
                    .width(width)
                    .height(itemHeight)
                    .semantics {
                        selected = isCentered
                    }
                    .clickable { onItemClick(index) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelForIndex(index),
                    style = if (isCentered) {
                        TabularTextStyles.wheelDigitSelected
                    } else {
                        TabularTextStyles.wheelDigitUnselected
                    },
                    color = if (isCentered) palette.textPrimary else palette.textSecondary,
                    modifier = Modifier.graphicsLayer {
                        val layoutInfo = listState.layoutInfo
                        val viewportCenter =
                            (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2f
                        val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
                        val normalizedOffset = if (itemInfo != null && itemInfo.size > 0) {
                            ((itemInfo.offset + itemInfo.size / 2f) - viewportCenter) /
                                itemInfo.size.toFloat()
                        } else {
                            (index - centeredIndex).toFloat()
                        }.coerceIn(-1.5f, 1.5f)
                        val absOffset = abs(normalizedOffset)
                        rotationX = -normalizedOffset * 28f
                        val scale = (1f - absOffset * 0.14f).coerceIn(0.78f, 1f)
                        scaleX = scale
                        scaleY = scale
                        alpha = (1f - absOffset * 0.45f).coerceIn(0.35f, 1f)
                        cameraDistance = cameraDistancePx
                    }
                )
            }
        }
    }
}

private fun centeredIndexOf(listState: LazyListState): Int? {
    val layoutInfo = listState.layoutInfo
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return null
    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
    return visibleItems.minByOrNull { item ->
        abs((item.offset + item.size / 2) - viewportCenter)
    }?.index
}

private fun nearestVirtualIndex(currentIndex: Int, modulus: Int, targetSlot: Int): Int {
    val maxItems = modulus * WHEEL_REPEAT_CYCLES
    val fallback = WHEEL_MID_CYCLE * modulus + targetSlot
    if (currentIndex !in 0 until maxItems) return fallback
    val currentSlot = Math.floorMod(currentIndex, modulus)
    var delta = targetSlot - currentSlot
    if (delta > modulus / 2) delta -= modulus
    if (delta < -modulus / 2) delta += modulus
    val candidate = currentIndex + delta
    return if (candidate in 0 until maxItems) candidate else fallback
}

internal fun halfDayBucket12h(rawIndex: Int): Int = Math.floorDiv(rawIndex + 1, 12)

internal fun to12HourDisplay(hour24: Int): Int {
    val mod = hour24 % 12
    return if (mod == 0) 12 else mod
}

internal fun to12HourIndex(hour24: Int): Int = to12HourDisplay(hour24) - 1

internal fun to24Hour(hour12: Int, isPm: Boolean): Int = when {
    !isPm && hour12 == 12 -> 0
    isPm && hour12 == 12 -> 12
    isPm -> hour12 + 12
    else -> hour12
}
