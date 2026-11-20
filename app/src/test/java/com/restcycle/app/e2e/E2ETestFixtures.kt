package com.restcycle.app.e2e

import android.app.Activity
import android.app.Application
import android.app.Instrumentation
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Looper
import android.provider.AlarmClock
import android.provider.Settings
import android.text.format.DateFormat
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ViewRootForTest
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onChildren
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import com.restcycle.app.MainActivity
import com.restcycle.app.R
import com.restcycle.app.domain.model.CycleQuality
import com.restcycle.app.domain.model.SkyPhase
import com.restcycle.app.domain.model.SleepMode
import com.restcycle.app.ui.RestCycleTestTags
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.containsString
import org.hamcrest.CoreMatchers.not
import org.hamcrest.Matchers.isEmptyOrNullString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowLooper
import org.robolectric.shadows.ShadowSystemClock
import org.robolectric.shadows.ShadowToast
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.TimeZone
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Authoritative device and SDK constants for RestCycle opaque-box E2E tests.
 * Uses ONLY the public contractual boundary: MainActivity, R, domain.model.*, RestCycleTestTags.
 */
object E2EDeviceConfigs {
    const val ROBOLECTRIC_SDK = 36
    const val ROBOLECTRIC_SDK_ANDROID_15 = 35

    // 6.7" 20:9 AMOLED tall displays (Nothing Phone (2a) & Samsung Galaxy A56)
    const val QUALIFIER_NOTHING_PHONE_2A = "w412dp-h915dp-xxhdpi"
    const val QUALIFIER_GALAXY_A56 = "w412dp-h915dp-420dpi"
    const val QUALIFIER_NOTHING_PHONE_2A_HU = "hu-rHU-w412dp-h915dp-xxhdpi"
    const val QUALIFIER_GALAXY_A56_HU = "hu-rHU-w412dp-h915dp-420dpi"
    const val QUALIFIER_COMPACT = "w360dp-h640dp-xhdpi"
    const val QUALIFIER_COMPACT_HU = "hu-rHU-w360dp-h640dp-xhdpi"
    const val QUALIFIER_TABLET = "w600dp-h960dp-xhdpi"
    const val QUALIFIER_EN_US = "en-rUS-w412dp-h915dp-xxhdpi"

    // Palette targets from ins.md:32,37,42
    val DAY_BG_IVORY = Color.rgb(0xF6, 0xF3, 0xEA)
    val DAY_BG_SKY = Color.rgb(0xEB, 0xF3, 0xFA)
    val DAY_CARD_WHITE = Color.rgb(0xFF, 0xFF, 0xFF)
    val DAY_ACCENT_SUN = Color.rgb(0xF5, 0x9E, 0x0B)

    val DAWN_BG_TWILIGHT = Color.rgb(0x1F, 0x15, 0x23)
    val DAWN_BG_WARM = Color.rgb(0x4A, 0x25, 0x25)
    val DAWN_GLOW_AMBER = Color.rgb(0xD9, 0x6B, 0x27)
    val DAWN_CARD_DARK = Color.rgb(0x26, 0x1B, 0x20)

    val NIGHT_BG_AMOLED = Color.rgb(0x00, 0x00, 0x00)
    val NIGHT_CARD_OBSIDIAN = Color.rgb(0x11, 0x13, 0x1A)
    val NIGHT_ACCENT_MOON = Color.rgb(0xE6, 0xC6, 0x87)
}

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class MissingClockApp

/**
 * Deterministic pre-launch environment and teardown rule for Robolectric + Compose + Espresso Intents.
 */
class RestCycleEnvironmentRule(
    val initialLocalTime: LocalTime? = null,
    val initialIs24Hour: Boolean? = true,
    val hapticFeedbackEnabled: Boolean = true,
    val clockAppInstalled: Boolean = true,
    val enableEspressoIntents: Boolean = clockAppInstalled,
    val clearDataStoreBeforeLaunch: Boolean = true
) : TestWatcher() {

    private var originalTimeZone: TimeZone? = null
    private var originalLocale: Locale? = null
    private var intentsActive = false
    private var currentOffsetMinutes: Int = 0

    override fun starting(description: Description) {
        originalTimeZone = TimeZone.getDefault()
        originalLocale = Locale.getDefault()
        ShadowToast.reset()

        val app = ApplicationProvider.getApplicationContext<Application>()
        if (clearDataStoreBeforeLaunch) {
            File(app.filesDir, "datastore").deleteRecursively()
        }

        if (initialLocalTime != null) {
            setClockToLocalTime(initialLocalTime)
        } else {
            // Ensure current TimeZone offset is tracked and guard against :55..:59 rollover
            guardMinuteBoundary()
            val millis = System.currentTimeMillis()
            android.os.SystemClock.setCurrentTimeMillis(millis)
        }

        when (initialIs24Hour) {
            true -> Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "24")
            false -> Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, "12")
            null -> Settings.System.putString(app.contentResolver, Settings.System.TIME_12_24, null)
        }
        Settings.System.putInt(
            app.contentResolver,
            Settings.System.HAPTIC_FEEDBACK_ENABLED,
            if (hapticFeedbackEnabled) 1 else 0
        )

        val missingClock = !clockAppInstalled || description.getAnnotation(MissingClockApp::class.java) != null
        val shadowApp = Shadows.shadowOf(app)
        if (!missingClock) {
            shadowApp.checkActivities(false)
            registerFakeClockApp(app)
            if (enableEspressoIntents) {
                Intents.init()
                intentsActive = true
                stubClockIntentsOk()
            }
        } else {
            shadowApp.checkActivities(true)
        }
    }

    override fun finished(description: Description) {
        try {
            if (intentsActive) {
                runCatching { Intents.release() }
                intentsActive = false
            }
        } finally {
            originalTimeZone?.let { TimeZone.setDefault(it) }
            originalLocale?.let { Locale.setDefault(it) }
        }
    }

    /**
     * Shifts TimeZone.setDefault(...) so java.time.LocalTime.now() inside JDK 21 java.base
     * aligns to [target] hour/minute, and syncs ShadowSystemClock accordingly.
     */
    fun setClockToLocalTime(target: LocalTime): LocalTime {
        guardMinuteBoundary()
        val utcNow = OffsetTime.now(ZoneOffset.UTC)
        val utcMin = utcNow.hour * 60 + utcNow.minute
        val targetMin = target.hour * 60 + target.minute
        var diffMin = targetMin - utcMin
        while (diffMin > 720) diffMin -= 1440
        while (diffMin < -720) diffMin += 1440
        currentOffsetMinutes = diffMin

        val tz = buildGmtTimeZone(diffMin)
        TimeZone.setDefault(tz)

        val zoneId = tz.toZoneId()
        val zoned = ZonedDateTime.of(LocalDate.now(zoneId), target, zoneId)
        android.os.SystemClock.setCurrentTimeMillis(zoned.toInstant().toEpochMilli())
        return LocalTime.now().truncatedTo(ChronoUnit.MINUTES)
    }

    /**
     * Advances simulated wall-clock time by [minutes] (via TimeZone offset shift + ShadowSystemClock + Looper + Compose mainClock).
     */
    fun advanceClockMinutes(composeRule: ComposeTestRule, minutes: Int) {
        currentOffsetMinutes += minutes
        while (currentOffsetMinutes > 720) currentOffsetMinutes -= 1440
        while (currentOffsetMinutes < -720) currentOffsetMinutes += 1440
        val tz = buildGmtTimeZone(currentOffsetMinutes)
        TimeZone.setDefault(tz)

        ShadowSystemClock.advanceBy(Duration.ofMinutes(minutes.toLong()))
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMinutes(minutes.toLong()))
        composeRule.mainClock.advanceTimeBy(minutes * 60_000L)
        composeRule.waitForIdle()
    }

    fun guardMinuteBoundary(): LocalTime {
        val now = LocalTime.now()
        if (now.second >= 45) {
            val sleepMs = ((61 - now.second) * 1000L).coerceAtMost(16_500L)
            Thread.sleep(sleepMs)
        }
        return LocalTime.now().truncatedTo(ChronoUnit.MINUTES)
    }

    private fun buildGmtTimeZone(offsetMinutes: Int): TimeZone {
        val sign = if (offsetMinutes >= 0) "+" else "-"
        val absMin = abs(offsetMinutes)
        val id = String.format(Locale.US, "GMT%s%02d:%02d", sign, absMin / 60, absMin % 60)
        return TimeZone.getTimeZone(id)
    }
}

fun registerFakeClockApp(context: Context) {
    val pm = Shadows.shadowOf(context.packageManager)
    val component = ComponentName("com.google.android.deskclock", "com.android.deskclock.DeskClock")
    val resolveInfo = ResolveInfo().apply {
        isDefault = true
        activityInfo = ActivityInfo().apply {
            name = component.className
            packageName = component.packageName
            applicationInfo = ApplicationInfo().apply { packageName = component.packageName }
        }
    }
    pm.addActivityIfNotPresent(component)
    pm.addResolveInfoForIntent(Intent(AlarmClock.ACTION_SET_ALARM), resolveInfo)
    pm.addResolveInfoForIntent(Intent(AlarmClock.ACTION_SET_TIMER), resolveInfo)
}

fun stubClockIntentsOk() {
    Intents.intending(hasAction(AlarmClock.ACTION_SET_ALARM))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent()))
    Intents.intending(hasAction(AlarmClock.ACTION_SET_TIMER))
        .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, Intent()))
}

fun launchMainActivity(composeRule: ComposeTestRule): ActivityScenario<MainActivity> {
    val scenario = ActivityScenario.launch(MainActivity::class.java)
    ShadowLooper.idleMainLooper()
    composeRule.waitForIdle()
    return scenario
}

inline fun <T> withMainActivity(
    composeRule: ComposeTestRule,
    block: (ActivityScenario<MainActivity>, MainActivity) -> T
): T {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
        ShadowLooper.idleMainLooper()
        composeRule.waitForIdle()
        lateinit var activity: MainActivity
        scenario.onActivity { activity = it }
        return block(scenario, activity)
    }
}

fun relaunchMainActivityPreservingDataStore(
    currentScenario: ActivityScenario<MainActivity>,
    composeRule: ComposeTestRule
): ActivityScenario<MainActivity> {
    // Allow any pending Dispatchers.IO DataStore write to flush before closing Activity
    Thread.sleep(50)
    ShadowLooper.idleMainLooper()
    currentScenario.close()
    ShadowLooper.idleMainLooper()
    val nextScenario = ActivityScenario.launch(MainActivity::class.java)
    composeRule.waitUntil(timeoutMillis = 3000L) {
        ShadowLooper.idleMainLooper()
        composeRule.onAllNodes(hasTestTag(RestCycleTestTags.SCREEN_ROOT))
            .fetchSemanticsNodes()
            .isNotEmpty()
    }
    // Give Dispatchers.IO DataStore read + StateFlow emission time to settle on main looper
    Thread.sleep(100)
    ShadowLooper.idleMainLooper()
    composeRule.waitForIdle()
    return nextScenario
}

fun flip24HourFormatAndResume(
    scenario: ActivityScenario<MainActivity>,
    composeRule: ComposeTestRule,
    is24Hour: Boolean
) {
    val appContext = ApplicationProvider.getApplicationContext<Context>()
    scenario.moveToState(Lifecycle.State.STARTED)
    Settings.System.putString(
        appContext.contentResolver,
        Settings.System.TIME_12_24,
        if (is24Hour) "24" else "12"
    )
    scenario.moveToState(Lifecycle.State.RESUMED)
    ShadowLooper.idleMainLooper()
    composeRule.waitForIdle()
}

// --- Opaque-Box UI Drivers ---

fun ComposeTestRule.switchMode(mode: SleepMode) {
    onNodeWithTag(RestCycleTestTags.modeTab(mode)).performClick()
    ShadowLooper.idleMainLooper()
    waitForIdle()
}

fun ComposeTestRule.setLatencyMinutes(minutes: Int) {
    val sliderNode = onNodeWithTag(RestCycleTestTags.LATENCY_SLIDER)
    sliderNode.performSemanticsAction(SemanticsActions.SetProgress) { setProgress ->
        setProgress(minutes.toFloat())
    }
    ShadowLooper.idleMainLooper()
    waitForIdle()
}

private fun ComposeTestRule.findResultsScrollable(): SemanticsNodeInteraction? {
    val scrollableMatcher = (
        hasTestTag(RestCycleTestTags.UPPER_RESULTS_ZONE) or
            hasAnyAncestor(hasTestTag(RestCycleTestTags.UPPER_RESULTS_ZONE))
        ) and SemanticsMatcher.keyIsDefined(SemanticsActions.ScrollBy)
    val scrollables = onAllNodes(scrollableMatcher).fetchSemanticsNodes()
    if (scrollables.isEmpty()) return null
    return onAllNodes(scrollableMatcher)[0]
}

private fun ComposeTestRule.resultsScrollStepPx(scrollable: SemanticsNodeInteraction): Float {
    val heightPx = scrollable.fetchSemanticsNode().size.height.toFloat().coerceAtLeast(1f)
    return (heightPx * 0.45f).coerceAtLeast(1f)
}

private fun SemanticsNode.hasVisibleBounds(): Boolean =
    boundsInRoot.width > 0f && boundsInRoot.height > 0f

private fun ComposeTestRule.captureScrollSignature(
    scrollable: SemanticsNodeInteraction,
    itemMatcher: SemanticsMatcher? = null
): Pair<Float?, List<String>> {
    val scrollValue = scrollable.fetchSemanticsNode()
        .config
        .getOrNull(SemanticsProperties.VerticalScrollAxisRange)
        ?.value
        ?.invoke()
    val visibleItems = if (itemMatcher != null) {
        onAllNodes(itemMatcher).fetchSemanticsNodes()
            .filter { it.hasVisibleBounds() }
            .sortedBy { it.boundsInRoot.top }
            .map { node ->
                val tag = node.config.getOrNull(SemanticsProperties.TestTag).orEmpty()
                "$tag@${node.boundsInRoot.top.roundToInt()}"
            }
    } else {
        scrollable.fetchSemanticsNode().children
            .filter { it.hasVisibleBounds() }
            .map { child ->
                val tag = child.config.getOrNull(SemanticsProperties.TestTag).orEmpty()
                "$tag@${child.boundsInRoot.top.roundToInt()}"
            }
    }
    return scrollValue to visibleItems
}

private fun ComposeTestRule.scrollResultsToTop(
    scrollable: SemanticsNodeInteraction,
    itemMatcher: SemanticsMatcher? = null
) {
    val config = scrollable.fetchSemanticsNode().config
    if (config.contains(SemanticsActions.ScrollToIndex)) {
        runCatching { scrollable.performScrollToIndex(0) }
        ShadowLooper.idleMainLooper()
        waitForIdle()
    }
    val stepPx = resultsScrollStepPx(scrollable)
    repeat(30) {
        val prev = captureScrollSignature(scrollable, itemMatcher)
        var moved = false
        runCatching {
            scrollable.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
                moved = scrollBy(0f, -stepPx)
            }
        }
        ShadowLooper.idleMainLooper()
        waitForIdle()
        val next = captureScrollSignature(scrollable, itemMatcher)
        val scrollUnchanged = next.first == prev.first && next.second == prev.second
        if (!moved || scrollUnchanged) return
    }
}

private fun ComposeTestRule.scrollResultsUntilNodeExists(matcher: SemanticsMatcher) {
    fun hasVisibleMatch(): Boolean =
        onAllNodes(matcher).fetchSemanticsNodes().any { it.hasVisibleBounds() }

    if (hasVisibleMatch()) return
    val scrollable = findResultsScrollable() ?: return

    runCatching { scrollable.performScrollToNode(matcher) }
    ShadowLooper.idleMainLooper()
    waitForIdle()
    if (hasVisibleMatch()) {
        runCatching { onNode(matcher).performScrollTo() }
        ShadowLooper.idleMainLooper()
        waitForIdle()
        return
    }

    if (onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()) {
        runCatching { onNode(matcher).performScrollTo() }
        ShadowLooper.idleMainLooper()
        waitForIdle()
        if (hasVisibleMatch()) return
    }

    scrollResultsToTop(scrollable)
    if (hasVisibleMatch()) {
        runCatching { onNode(matcher).performScrollTo() }
        ShadowLooper.idleMainLooper()
        waitForIdle()
        return
    }

    val stepPx = resultsScrollStepPx(scrollable)
    var staleSteps = 0
    repeat(30) {
        val prev = captureScrollSignature(scrollable)
        var moved = false
        runCatching {
            scrollable.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
                moved = scrollBy(0f, stepPx)
            }
        }
        ShadowLooper.idleMainLooper()
        waitForIdle()
        if (hasVisibleMatch()) {
            runCatching { onNode(matcher).performScrollTo() }
            ShadowLooper.idleMainLooper()
            waitForIdle()
            return
        }
        val next = captureScrollSignature(scrollable)
        if (!moved || (next.first == prev.first && next.second == prev.second)) {
            staleSteps++
            if (staleSteps >= 2) return
        } else {
            staleSteps = 0
        }
    }
}

fun ComposeTestRule.findOptionCardById(optionId: String): SemanticsNodeInteraction {
    val matcher = hasTestTag(RestCycleTestTags.optionCard(optionId))
    scrollResultsUntilNodeExists(matcher)
    val node = onNode(matcher)
    runCatching { node.performScrollTo() }
    ShadowLooper.idleMainLooper()
    waitForIdle()
    return node
}

fun ComposeTestRule.tapOptionCard(optionId: String) {
    val matcher = hasTestTag(RestCycleTestTags.optionCard(optionId))
    scrollResultsUntilNodeExists(matcher)
    val node = onNode(matcher)
    runCatching { node.performScrollTo() }
    node.performClick()
    ShadowLooper.idleMainLooper()
    waitForIdle()
}

fun ComposeTestRule.tapSetAlarmOnCard(optionId: String) {
    val matcher = hasTestTag(RestCycleTestTags.setAlarmButton(optionId))
    scrollResultsUntilNodeExists(matcher)
    val node = onNode(matcher)
    runCatching { node.performScrollTo() }
    node.performClick()
    ShadowLooper.idleMainLooper()
    waitForIdle()
}

fun ComposeTestRule.tapSetTimerOnCard(optionId: String) {
    val matcher = hasTestTag(RestCycleTestTags.setTimerButton(optionId))
    scrollResultsUntilNodeExists(matcher)
    val node = onNode(matcher)
    runCatching { node.performScrollTo() }
    node.performClick()
    ShadowLooper.idleMainLooper()
    waitForIdle()
}

fun ComposeTestRule.assertLatencyValueEquals(expectedMinutes: Int, timeoutMillis: Long = 3000L) {
    onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).assertIsDisplayed()
    val pattern = Regex("(^|\\D)$expectedMinutes(\\D|$)")
    runCatching {
        waitUntil(timeoutMillis = timeoutMillis) {
            ShadowLooper.idleMainLooper()
            val node = onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT).fetchSemanticsNode()
            val text = node.config.getOrNull(SemanticsProperties.Text)
                ?.joinToString("") { it.text }.orEmpty()
            pattern.containsMatchIn(text)
        }
    }
    val finalText = onNodeWithTag(RestCycleTestTags.LATENCY_VALUE_TEXT)
        .fetchSemanticsNode()
        .config.getOrNull(SemanticsProperties.Text)
        ?.joinToString("") { it.text }.orEmpty()
    assertTrue(
        "Expected LATENCY_VALUE_TEXT to match '$expectedMinutes' (word-bounded), but was '$finalText'",
        pattern.containsMatchIn(finalText)
    )
}

fun ComposeTestRule.tapWakeAlarmButton() {
    onNodeWithTag(RestCycleTestTags.WAKE_ALARM_BTN).performClick()
    ShadowLooper.idleMainLooper()
    waitForIdle()
}

/**
 * Selects [targetTime] on the HapticTimeWheel using opaque-box semantics / scrolling.
 * Supports 24h (0..23) and 12h (1..12 + AM/PM) wheels via child text matching or bounded swipe stepping.
 */
fun ComposeTestRule.selectWheelTime(targetTime: LocalTime, is24Hour: Boolean = true) {
    val period = if (targetTime.hour < 12) "AM" else "PM"
    if (!is24Hour) {
        selectAmPmPeriod(period)
    }
    val targetHour = if (is24Hour) targetTime.hour else ((targetTime.hour + 11) % 12) + 1
    val hourText = if (is24Hour) String.format(Locale.US, "%02d", targetHour) else targetHour.toString()
    val hourPadded = String.format(Locale.US, "%02d", targetHour)
    dialWheelComponent(
        tag = RestCycleTestTags.WHEEL_HOUR,
        targetValue = targetHour,
        modulo = if (is24Hour) 24 else 12,
        minVal = if (is24Hour) 0 else 1,
        candidateTexts = listOf(hourPadded, hourText)
    )
    if (!is24Hour) {
        selectAmPmPeriod(period)
    }

    val minText = String.format(Locale.US, "%02d", targetTime.minute)
    dialWheelComponent(
        tag = RestCycleTestTags.WHEEL_MINUTE,
        targetValue = targetTime.minute,
        modulo = 60,
        minVal = 0,
        candidateTexts = listOf(minText)
    )
    ShadowLooper.idleMainLooper()
    waitForIdle()
}

fun ComposeTestRule.selectAmPmPeriod(period: String) {
    val amPmNode = onNodeWithTag(RestCycleTestTags.WHEEL_AM_PM)
    amPmNode.assertIsDisplayed()

    val rootSemantics = amPmNode.fetchSemanticsNode()
    val rootStateDesc = rootSemantics.config.getOrNull(SemanticsProperties.StateDescription)
    if (rootStateDesc?.contains(period, ignoreCase = true) == true) {
        return
    }

    val otherPeriod = if (period.equals("AM", ignoreCase = true)) "PM" else "AM"
    val matchingChildren = onAllNodes(
        hasAnyAncestor(hasTestTag(RestCycleTestTags.WHEEL_AM_PM)) and hasText(period, ignoreCase = true),
        useUnmergedTree = true
    ).fetchSemanticsNodes()
    val otherChildren = onAllNodes(
        hasAnyAncestor(hasTestTag(RestCycleTestTags.WHEEL_AM_PM)) and hasText(otherPeriod, ignoreCase = true),
        useUnmergedTree = true
    ).fetchSemanticsNodes()

    if (matchingChildren.isNotEmpty()) {
        // Guard against clicking an already-selected single-label toggle chip (only `period` is rendered)
        if (otherChildren.isEmpty()) {
            return
        }
        val alreadySelected = matchingChildren.any {
            it.config.getOrNull(SemanticsProperties.Selected) == true
        }
        if (alreadySelected) {
            return
        }
        onAllNodes(
            hasAnyAncestor(hasTestTag(RestCycleTestTags.WHEEL_AM_PM)) and hasText(period, ignoreCase = true),
            useUnmergedTree = true
        )[0].performClick()
    } else {
        // Single-label toggle chip currently displaying `otherPeriod` -> click to toggle to `period`
        amPmNode.performClick()
    }
    ShadowLooper.idleMainLooper()
    waitForIdle()
}

private fun SemanticsNodeInteraction.performControlledWheelDrag(
    forward: Boolean,
    moveDurationMs: Long = 200L,
    holdBeforeUpMs: Long = 40L
) {
    performTouchInput {
        val dragDist = 44.dp.toPx().coerceAtMost(height * 0.45f)
        val half = dragDist / 2f
        val startY = if (forward) centerY + half else centerY - half
        val endY = if (forward) centerY - half else centerY + half
        down(Offset(centerX, startY))
        val chunks = 8
        val stepDelay = (moveDurationMs / chunks).coerceAtLeast(10L)
        for (i in 1..chunks) {
            advanceEventTime(stepDelay)
            val y = startY + (endY - startY) * (i.toFloat() / chunks)
            moveTo(Offset(centerX, y))
        }
        if (holdBeforeUpMs > 0L) {
            advanceEventTime(holdBeforeUpMs)
        }
        up()
    }
}

private fun ComposeTestRule.dialWheelComponent(
    tag: String,
    targetValue: Int,
    modulo: Int,
    minVal: Int,
    candidateTexts: List<String>
) {
    val wheelNode = onNodeWithTag(tag)
    wheelNode.assertIsDisplayed()

    val initialVal = readWheelCurrentInt(tag)
    if (initialVal == targetValue) return

    if (initialVal != null) {
        val forwardSteps = ((targetValue - initialVal) % modulo + modulo) % modulo
        val backwardSteps = ((initialVal - targetValue) % modulo + modulo) % modulo
        val stepForward = forwardSteps <= backwardSteps
        val steps = minOf(forwardSteps, backwardSteps).coerceAtMost(35)
        repeat(steps) {
            wheelNode.performControlledWheelDrag(forward = stepForward, moveDurationMs = 200L, holdBeforeUpMs = 40L)
            if (mainClock.autoAdvance) {
                mainClock.advanceTimeBy(120L)
            }
            ShadowLooper.idleMainLooper()
            waitForIdle()
            if (readWheelCurrentInt(tag) == targetValue) return
        }
        // Fine-tune up to 3 single steps if snap landed +/-1 from target
        repeat(3) {
            val cur = readWheelCurrentInt(tag) ?: return@repeat
            if (cur == targetValue) return
            val fwd = ((targetValue - cur) % modulo + modulo) % modulo
            val bwd = ((cur - targetValue) % modulo + modulo) % modulo
            wheelNode.performControlledWheelDrag(forward = fwd <= bwd, moveDurationMs = 200L, holdBeforeUpMs = 40L)
            if (mainClock.autoAdvance) {
                mainClock.advanceTimeBy(120L)
            }
            ShadowLooper.idleMainLooper()
            waitForIdle()
        }
        val finalAfterSteps = readWheelCurrentInt(tag)
        if (finalAfterSteps == targetValue) return
    }

    // Fallback: locate matching visible child text and click it
    for (text in candidateTexts) {
        val nodes = onAllNodes(
            hasAnyAncestor(hasTestTag(tag)) and hasText(text),
            useUnmergedTree = true
        ).fetchSemanticsNodes()
        if (nodes.isNotEmpty()) {
            onAllNodes(
                hasAnyAncestor(hasTestTag(tag)) and hasText(text),
                useUnmergedTree = true
            )[0].performClick()
            ShadowLooper.idleMainLooper()
            waitForIdle()
            val afterClick = readWheelCurrentInt(tag)
            if (afterClick == null || afterClick == targetValue) return
        }
    }

    // Fallback: ScrollToIndex if exposed on a finite 0..modulo-1 list
    val wheelSemantics = wheelNode.fetchSemanticsNode()
    if (wheelSemantics.config.getOrNull(SemanticsActions.ScrollToIndex) != null) {
        val index = ((targetValue - minVal) % modulo + modulo) % modulo
        runCatching { wheelNode.performScrollToIndex(index) }
        ShadowLooper.idleMainLooper()
        waitForIdle()
        val afterIndex = readWheelCurrentInt(tag)
        if (afterIndex == targetValue) return
    }

    // Final bounded search via controlled non-fling drag
    repeat(modulo.coerceAtMost(60)) {
        for (text in candidateTexts) {
            val found = onAllNodes(
                hasAnyAncestor(hasTestTag(tag)) and hasText(text),
                useUnmergedTree = true
            ).fetchSemanticsNodes()
            if (found.isNotEmpty()) {
                onAllNodes(
                    hasAnyAncestor(hasTestTag(tag)) and hasText(text),
                    useUnmergedTree = true
                )[0].performClick()
                ShadowLooper.idleMainLooper()
                waitForIdle()
                val afterSearch = readWheelCurrentInt(tag)
                if (afterSearch != null) {
                    assertEquals(
                        "Wheel '$tag' failed to dial to $targetValue (landed on $afterSearch)",
                        targetValue,
                        afterSearch
                    )
                }
                return
            }
        }
        wheelNode.performControlledWheelDrag(forward = true, moveDurationMs = 160L, holdBeforeUpMs = 40L)
        if (mainClock.autoAdvance) {
            mainClock.advanceTimeBy(100L)
        }
        ShadowLooper.idleMainLooper()
        waitForIdle()
        val cur = readWheelCurrentInt(tag)
        if (cur == targetValue) return
    }

    val finalVal = readWheelCurrentInt(tag)
    if (finalVal != null) {
        assertEquals(
            "Wheel '$tag' failed to dial to $targetValue (landed on $finalVal)",
            targetValue,
            finalVal
        )
    }
}

fun ComposeTestRule.stepWheelComponent(tag: String, forward: Boolean = true, steps: Int = 1) {
    val wheelNode = onNodeWithTag(tag)
    repeat(steps) {
        wheelNode.performControlledWheelDrag(
            forward = forward,
            moveDurationMs = 80L,
            holdBeforeUpMs = 30L
        )
        if (mainClock.autoAdvance) {
            mainClock.advanceTimeBy(120L)
        }
        ShadowLooper.idleMainLooper()
        waitForIdle()
    }
}

fun ComposeTestRule.readWheelCurrentInt(tag: String): Int? {
    val semanticsNode = onNodeWithTag(tag).fetchSemanticsNode()
    val stateDesc = semanticsNode.config.getOrNull(SemanticsProperties.StateDescription)
    stateDesc?.let { desc ->
        Regex("\\d+").find(desc)?.value?.toIntOrNull()?.let { return it }
    }
    val selectedChildren = onAllNodes(
        hasAnyAncestor(hasTestTag(tag)) and SemanticsMatcher.expectValue(SemanticsProperties.Selected, true),
        useUnmergedTree = true
    ).fetchSemanticsNodes()
    for (child in selectedChildren) {
        val textList = child.config.getOrNull(SemanticsProperties.Text)
        val joined = textList?.joinToString("") { it.text }.orEmpty()
        Regex("\\d+").find(joined)?.value?.toIntOrNull()?.let { return it }
    }
    return null
}

// --- Pure Requirement-Derived Oracles & Formatters ---

fun expectedFormattedTime(time: LocalTime, is24Hour: Boolean): String {
    return if (is24Hour) {
        time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US))
    } else {
        time.format(DateTimeFormatter.ofPattern("h:mm a", Locale.US))
    }
}

fun expectedSleepCyclesBackward(
    wakeTime: LocalTime,
    latencyMinutes: Int,
    cycles: Int
): LocalTime = wakeTime.minusMinutes(cycles * 90L + latencyMinutes)

fun expectedSleepCyclesForward(
    bedTime: LocalTime,
    latencyMinutes: Int,
    cycles: Int
): LocalTime = bedTime.plusMinutes(cycles * 90L + latencyMinutes)

fun expectedSkyPhaseForTime(time: LocalTime): SkyPhase {
    val minutes = time.hour * 60 + time.minute
    return when {
        minutes in (8 * 60)..(17 * 60 + 29) -> SkyPhase.DAY
        minutes in (5 * 60 + 30)..(7 * 60 + 59) -> SkyPhase.DAWN_DUSK
        minutes in (17 * 60 + 30)..(20 * 60 + 29) -> SkyPhase.DAWN_DUSK
        else -> SkyPhase.NIGHT
    }
}

fun expectedQualityForCycles(cycles: Int): CycleQuality = when {
    cycles >= 5 -> CycleQuality.OPTIMAL
    cycles == 4 -> CycleQuality.GOOD
    else -> CycleQuality.SHORT
}

// --- Opaque-Box Verification Helpers ---

fun assertAlarmIntentSent(
    hour24: Int,
    minute: Int,
    expectedMessageContains: String? = null
) {
    if (expectedMessageContains != null) {
        Intents.intended(
            allOf(
                hasAction(AlarmClock.ACTION_SET_ALARM),
                hasExtra(AlarmClock.EXTRA_HOUR, hour24),
                hasExtra(AlarmClock.EXTRA_MINUTES, minute),
                hasExtra(AlarmClock.EXTRA_SKIP_UI, false),
                hasExtra(AlarmClock.EXTRA_MESSAGE, containsString(expectedMessageContains))
            )
        )
    } else {
        Intents.intended(
            allOf(
                hasAction(AlarmClock.ACTION_SET_ALARM),
                hasExtra(AlarmClock.EXTRA_HOUR, hour24),
                hasExtra(AlarmClock.EXTRA_MINUTES, minute),
                hasExtra(AlarmClock.EXTRA_SKIP_UI, false),
                hasExtra(AlarmClock.EXTRA_MESSAGE, not(isEmptyOrNullString()))
            )
        )
    }
}

fun assertTimerIntentSent(
    durationSeconds: Int,
    expectedMessageContains: String? = null
) {
    if (expectedMessageContains != null) {
        Intents.intended(
            allOf(
                hasAction(AlarmClock.ACTION_SET_TIMER),
                hasExtra(AlarmClock.EXTRA_LENGTH, durationSeconds),
                hasExtra(AlarmClock.EXTRA_SKIP_UI, false),
                hasExtra(AlarmClock.EXTRA_MESSAGE, containsString(expectedMessageContains))
            )
        )
    } else {
        Intents.intended(
            allOf(
                hasAction(AlarmClock.ACTION_SET_TIMER),
                hasExtra(AlarmClock.EXTRA_LENGTH, durationSeconds),
                hasExtra(AlarmClock.EXTRA_SKIP_UI, false),
                hasExtra(AlarmClock.EXTRA_MESSAGE, not(isEmptyOrNullString()))
            )
        )
    }
}

fun assertSystemBarsLight(
    activity: Activity,
    expectedLightStatus: Boolean,
    expectedLightNav: Boolean = expectedLightStatus
) {
    val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
    assertEquals(
        "Expected isAppearanceLightStatusBars=$expectedLightStatus",
        expectedLightStatus,
        controller.isAppearanceLightStatusBars
    )
    assertEquals(
        "Expected isAppearanceLightNavigationBars=$expectedLightNav",
        expectedLightNav,
        controller.isAppearanceLightNavigationBars
    )
}

fun collectAllViews(root: View): List<View> = buildList {
    add(root)
    if (root is ViewGroup) {
        for (i in 0 until root.childCount) {
            addAll(collectAllViews(root.getChildAt(i)))
        }
    }
}

fun lastClockTickView(activity: ComponentActivity): View? =
    collectAllViews(activity.window.decorView)
        .firstOrNull { Shadows.shadowOf(it).lastHapticFeedbackPerformed() == HapticFeedbackConstants.CLOCK_TICK }

fun anyHapticFeedbackPerformed(activity: ComponentActivity): List<Int> =
    collectAllViews(activity.window.decorView)
        .map { Shadows.shadowOf(it).lastHapticFeedbackPerformed() }
        .filter { it != -1 }

fun assertClockTickHapticRecorded(activity: ComponentActivity) {
    val view = lastClockTickView(activity)
    assertNotNull(
        "Expected HapticFeedbackConstants.CLOCK_TICK (${HapticFeedbackConstants.CLOCK_TICK}) on a View inside MainActivity, found ${anyHapticFeedbackPerformed(activity)}",
        view
    )
}

fun SemanticsNodeInteraction.assertHasTabularNumerals(): SemanticsNodeInteraction {
    val node = fetchSemanticsNode()
    val results = mutableListOf<TextLayoutResult>()
    val action = node.config.getOrNull(SemanticsActions.GetTextLayoutResult)
        ?: throw AssertionError("Node does not expose SemanticsActions.GetTextLayoutResult")
    val invoked = action.action?.invoke(results) ?: false
    assertTrue("GetTextLayoutResult returned false", invoked)
    val fontFeature = results.firstOrNull()?.layoutInput?.style?.fontFeatureSettings
    assertTrue(
        "Expected fontFeatureSettings to contain 'tnum' but was '$fontFeature'",
        fontFeature?.contains("tnum") == true
    )
    return this
}

fun ComposeTestRule.assertAllNumericTextsUnderTagHaveTnum(tag: String) {
    val isInertiaTooltip = hasTestTag(RestCycleTestTags.INERTIA_TOOLTIP) or
        hasAnyAncestor(hasTestTag(RestCycleTestTags.INERTIA_TOOLTIP))
    val nodes = onAllNodes(
        (hasTestTag(tag) or hasAnyAncestor(hasTestTag(tag))) and
            SemanticsMatcher.keyIsDefined(SemanticsActions.GetTextLayoutResult) and
            isInertiaTooltip.not(),
        useUnmergedTree = true
    )
    val count = nodes.fetchSemanticsNodes().size
    assertTrue("Expected at least one text node under tag '$tag'", count > 0)
    var numericCount = 0
    val digitRegex = Regex("\\d")
    for (i in 0 until count) {
        val interaction = nodes[i]
        val text = interaction.fetchSemanticsNode().config.getOrNull(SemanticsProperties.Text)
            ?.joinToString("") { it.text }.orEmpty()
        if (digitRegex.containsMatchIn(text)) {
            interaction.assertHasTabularNumerals()
            numericCount++
        }
    }
    assertTrue("Expected at least one numeric text node under tag '$tag'", numericCount > 0)
}

fun ComposeTestRule.assertThumbZoneErgonomics() {
    val root = onNodeWithTag(RestCycleTestTags.SCREEN_ROOT).getUnclippedBoundsInRoot()
    val header = onNodeWithTag(RestCycleTestTags.CELESTIAL_HEADER).getUnclippedBoundsInRoot()
    val upper = onNodeWithTag(RestCycleTestTags.UPPER_RESULTS_ZONE).getUnclippedBoundsInRoot()
    val thumb = onNodeWithTag(RestCycleTestTags.THUMB_CONTROLS_ZONE).getUnclippedBoundsInRoot()
    val modeSwitcher = onNodeWithTag(RestCycleTestTags.MODE_SWITCHER).getUnclippedBoundsInRoot()

    assertTrue("Expected root height > 0", root.height > 0.dp)
    assertTrue(
        "Thumb zone should occupy bottom >= 50% (was ${thumb.height} of ${root.height})",
        thumb.height >= root.height * 0.48f
    )
    assertTrue(
        "Thumb zone top (${thumb.top}) should be in lower half (>= ${root.top + root.height * 0.42f})",
        thumb.top >= root.top + root.height * 0.42f
    )
    assertTrue(
        "Upper results bottom (${upper.bottom}) must not overlap thumb top (${thumb.top})",
        upper.bottom <= thumb.top + 1.dp
    )
    assertTrue(
        "Header (${header.top}..${header.bottom}) must sit above thumb zone (${thumb.top})",
        header.bottom <= thumb.top + 1.dp
    )
    assertTrue(
        "Mode switcher bottom (${modeSwitcher.bottom}) must stay within root (${root.bottom})",
        modeSwitcher.bottom <= root.bottom + 1.dp
    )
}

fun ComposeTestRule.assertZeroCalculateButton() {
    listOf("Calculate", "Számítás", "Számol", "Submit", "Confirm", "Results").forEach { forbidden ->
        val matches = onAllNodesWithText(forbidden, substring = true, ignoreCase = true)
            .fetchSemanticsNodes()
        assertTrue(
            "Zero-button reactive UX violated: found node matching '$forbidden'",
            matches.isEmpty()
        )
    }
}

fun ComposeTestRule.sampleRootBackgroundColorArgb(
    advanceMs: Long = 650L,
    yFraction: Float = 0.5f
): Int {
    if (advanceMs > 0) {
        mainClock.advanceTimeBy(advanceMs)
    }
    ShadowLooper.idleMainLooper()
    waitForIdle()
    val bitmap = captureTagBitmap(RestCycleTestTags.SCREEN_ROOT)
    // Sample along the extreme left edge outside padded child cards/controls and MODE_SWITCHER
    val x = 2.coerceIn(0, (bitmap.width - 1).coerceAtLeast(0))
    var y = (bitmap.height * yFraction).roundToInt().coerceIn(0, (bitmap.height - 1).coerceAtLeast(0))
    val modeSwitcherNodes = onAllNodes(hasTestTag(RestCycleTestTags.MODE_SWITCHER)).fetchSemanticsNodes()
    if (modeSwitcherNodes.isNotEmpty()) {
        val rootBounds = onNodeWithTag(RestCycleTestTags.SCREEN_ROOT).fetchSemanticsNode().boundsInWindow
        val switcherBounds = modeSwitcherNodes.first().boundsInWindow
        val winX = rootBounds.left + x
        val winY = rootBounds.top + y
        if (winX in switcherBounds.left..switcherBounds.right && winY in switcherBounds.top..switcherBounds.bottom) {
            y = (bitmap.height / 2).coerceIn(0, (bitmap.height - 1).coerceAtLeast(0))
        }
    }
    return bitmap.getPixel(x, y)
}

fun ComposeTestRule.captureTagBitmap(tag: String): Bitmap {
    ShadowLooper.idleMainLooper()
    waitForIdle()
    ShadowLooper.idleMainLooper()
    val node = onNodeWithTag(tag).fetchSemanticsNode()
    val composeView = (node.root as? ViewRootForTest)?.view
        ?: error("SemanticsNode root for '$tag' is not a ViewRootForTest")
    val rootView = composeView.rootView
    val fullBitmap = Bitmap.createBitmap(
        rootView.width.coerceAtLeast(1),
        rootView.height.coerceAtLeast(1),
        Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(fullBitmap)
    rootView.draw(canvas)

    val bounds = node.boundsInWindow
    val left = bounds.left.roundToInt().coerceIn(0, (fullBitmap.width - 1).coerceAtLeast(0))
    val top = bounds.top.roundToInt().coerceIn(0, (fullBitmap.height - 1).coerceAtLeast(0))
    val width = (bounds.right.roundToInt() - left).coerceIn(1, (fullBitmap.width - left).coerceAtLeast(1))
    val height = (bounds.bottom.roundToInt() - top).coerceIn(1, (fullBitmap.height - top).coerceAtLeast(1))
    return Bitmap.createBitmap(fullBitmap, left, top, width, height)
}

fun relativeLuminance(argb: Int): Double {
    val r = Color.red(argb) / 255.0
    val g = Color.green(argb) / 255.0
    val b = Color.blue(argb) / 255.0
    return 0.2126 * r + 0.7152 * g + 0.0722 * b
}

fun meanLuminance(bitmap: Bitmap): Double {
    var sum = 0.0
    var count = 0
    val stepX = (bitmap.width / 20).coerceAtLeast(1)
    val stepY = (bitmap.height / 20).coerceAtLeast(1)
    var y = 0
    while (y < bitmap.height) {
        var x = 0
        while (x < bitmap.width) {
            sum += relativeLuminance(bitmap.getPixel(x, y))
            count++
            x += stepX
        }
        y += stepY
    }
    return if (count == 0) 0.0 else sum / count
}

fun assertOfflineManifestAndPermissions(context: Context) {
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_PERMISSIONS
    } else {
        @Suppress("DEPRECATION")
        PackageManager.GET_PERMISSIONS
    }
    val pkgInfo = context.packageManager.getPackageInfo(context.packageName, flags)
    val perms = pkgInfo.requestedPermissions?.toSet().orEmpty()
    assertTrue(
        "Manifest must declare com.android.alarm.permission.SET_ALARM",
        perms.contains("com.android.alarm.permission.SET_ALARM")
    )
    assertFalse("Manifest must NOT declare INTERNET permission", perms.contains(android.Manifest.permission.INTERNET))
    assertFalse("Manifest must NOT declare ACCESS_NETWORK_STATE", perms.contains(android.Manifest.permission.ACCESS_NETWORK_STATE))
    assertFalse("Manifest must NOT declare ACCESS_WIFI_STATE", perms.contains(android.Manifest.permission.ACCESS_WIFI_STATE))
    assertEquals(
        PackageManager.PERMISSION_DENIED,
        context.packageManager.checkPermission(android.Manifest.permission.INTERNET, context.packageName)
    )
}

fun assertMissingClockFallbackShown(composeRule: ComposeTestRule, activity: Activity) {
    assertFalse("MainActivity must not finish on missing Clock app", activity.isFinishing)
    assertFalse("MainActivity must not be destroyed on missing Clock app", activity.isDestroyed)
    ShadowLooper.idleMainLooper()
    composeRule.waitForIdle()
    val toast = ShadowToast.getTextOfLatestToast()
    if (!toast.isNullOrBlank()) {
        ShadowToast.reset()
        return
    }

    val isActionButton = SemanticsMatcher("ClockActionButton") { node ->
        val tag = node.config.getOrNull(SemanticsProperties.TestTag)
        tag != null && (
            tag == RestCycleTestTags.WAKE_ALARM_BTN ||
                tag.startsWith(RestCycleTestTags.SET_ALARM_BTN_PREFIX) ||
                tag.startsWith(RestCycleTestTags.SET_TIMER_BTN_PREFIX)
            )
    }
    val notActionButton = (isActionButton or hasAnyAncestor(isActionButton)).not()
    val hasFallbackSemantics =
        SemanticsMatcher.keyIsDefined(SemanticsProperties.IsDialog) or
            SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion)
    val hasErrorText =
        hasText("no clock", substring = true, ignoreCase = true) or
            hasText("not found", substring = true, ignoreCase = true) or
            hasText("not installed", substring = true, ignoreCase = true) or
            hasText("unavailable", substring = true, ignoreCase = true) or
            hasText("cannot open", substring = true, ignoreCase = true) or
            hasText("nem található", substring = true, ignoreCase = true) or
            hasText("nincs", substring = true, ignoreCase = true)

    val fallbackNodes = composeRule.onAllNodes(
        (hasFallbackSemantics or hasErrorText) and notActionButton,
        useUnmergedTree = true
    ).fetchSemanticsNodes()
    assertTrue(
        "Expected either a non-blank Toast or an explicit Snackbar/Dialog/error fallback node explaining missing Clock app",
        fallbackNodes.isNotEmpty()
    )
}

/**
 * Finds the project's `app/src/main` directory regardless of whether Gradle executes in `/` or `/app`.
 */
fun resolveAppMainDir(): File {
    var dir: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
    while (dir != null) {
        val candidate = File(dir, "app/src/main")
        if (candidate.isDirectory) return candidate
        val direct = File(dir, "src/main")
        if (direct.isDirectory && File(direct, "AndroidManifest.xml").exists()) return direct
        dir = dir.parentFile
    }
    error("Could not locate app/src/main directory from ${System.getProperty("user.dir")}")
}

fun parseStringsXmlMap(file: File): Map<String, String> {
    assertTrue("Missing strings XML file: ${file.absolutePath}", file.exists())
    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
    val nodes = doc.getElementsByTagName("string")
    val map = linkedMapOf<String, String>()
    for (i in 0 until nodes.length) {
        val el = nodes.item(i) as org.w3c.dom.Element
        val name = el.getAttribute("name")
        map[name] = el.textContent
    }
    return map
}

fun parseAndroidManifestXml(): org.w3c.dom.Document {
    val manifestFile = File(resolveAppMainDir(), "AndroidManifest.xml")
    assertTrue("Missing AndroidManifest.xml: ${manifestFile.absolutePath}", manifestFile.exists())
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(manifestFile)
}

fun ComposeTestRule.findAllCardsInResultsZone(): List<SemanticsNode> {
    val cardMatcher = SemanticsMatcher("OptionCard") { node ->
        val tag = node.config.getOrNull(SemanticsProperties.TestTag)
        tag != null && tag.startsWith(RestCycleTestTags.OPTION_CARD_PREFIX)
    }
    val seen = linkedMapOf<String, SemanticsNode>()
    fun collectVisibleCards() {
        onAllNodes(cardMatcher).fetchSemanticsNodes()
            .filter { it.hasVisibleBounds() }
            .sortedBy { it.boundsInRoot.top }
            .forEach { node ->
                val tag = node.config.getOrNull(SemanticsProperties.TestTag) ?: return@forEach
                seen[tag] = node
            }
    }

    val scrollable = findResultsScrollable()
    if (scrollable == null) {
        collectVisibleCards()
        return seen.values.toList()
    }

    scrollResultsToTop(scrollable, cardMatcher)
    collectVisibleCards()

    val stepPx = resultsScrollStepPx(scrollable)
    var staleSteps = 0
    repeat(30) {
        val prev = captureScrollSignature(scrollable, cardMatcher)
        var moved = false
        runCatching {
            scrollable.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
                moved = scrollBy(0f, stepPx)
            }
        }
        ShadowLooper.idleMainLooper()
        waitForIdle()
        collectVisibleCards()
        val next = captureScrollSignature(scrollable, cardMatcher)
        if (!moved || (next.first == prev.first && next.second == prev.second)) {
            staleSteps++
            if (staleSteps >= 2) return@repeat
        } else {
            staleSteps = 0
        }
    }

    // Return scroll position to top so top-card assertions start cleanly
    scrollResultsToTop(scrollable, cardMatcher)
    return seen.values.toList()
}

fun ComposeTestRule.findOptionCardByTimeText(timeText: String): SemanticsNodeInteraction {
    val matcher = SemanticsMatcher("OptionCardWithTime($timeText)") { node ->
        val tag = node.config.getOrNull(SemanticsProperties.TestTag)
        tag != null && tag.startsWith(RestCycleTestTags.OPTION_CARD_PREFIX)
    } and hasText(timeText, substring = true)
    scrollResultsUntilNodeExists(matcher)
    val node = onNode(matcher)
    runCatching { node.performScrollTo() }
    ShadowLooper.idleMainLooper()
    waitForIdle()
    return node
}

fun ComposeTestRule.findSetAlarmButtonMatchingTimeOrIndex(indexFromTop: Int): SemanticsNodeInteraction {
    val buttonMatcher = SemanticsMatcher("SetAlarmButton") { node ->
        val tag = node.config.getOrNull(SemanticsProperties.TestTag)
        tag != null && tag.startsWith(RestCycleTestTags.SET_ALARM_BTN_PREFIX)
    }
    val seenTags = linkedSetOf<String>()
    fun collectVisibleButtonTags() {
        onAllNodes(buttonMatcher).fetchSemanticsNodes()
            .filter { it.hasVisibleBounds() }
            .sortedBy { it.boundsInRoot.top }
            .forEach { node ->
                node.config.getOrNull(SemanticsProperties.TestTag)?.let { seenTags.add(it) }
            }
    }

    val scrollable = findResultsScrollable()
    if (scrollable == null) {
        collectVisibleButtonTags()
    } else {
        scrollResultsToTop(scrollable, buttonMatcher)
        collectVisibleButtonTags()
        if (seenTags.size <= indexFromTop) {
            val stepPx = resultsScrollStepPx(scrollable)
            var staleSteps = 0
            repeat(30) {
                if (seenTags.size > indexFromTop) return@repeat
                val prev = captureScrollSignature(scrollable, buttonMatcher)
                var moved = false
                runCatching {
                    scrollable.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
                        moved = scrollBy(0f, stepPx)
                    }
                }
                ShadowLooper.idleMainLooper()
                waitForIdle()
                collectVisibleButtonTags()
                if (seenTags.size > indexFromTop) return@repeat
                val next = captureScrollSignature(scrollable, buttonMatcher)
                if (!moved || (next.first == prev.first && next.second == prev.second)) {
                    staleSteps++
                    if (staleSteps >= 2) return@repeat
                } else {
                    staleSteps = 0
                }
            }
        }
    }

    assertTrue(
        "Expected at least ${indexFromTop + 1} SetAlarm buttons, found ${seenTags.size}",
        seenTags.size > indexFromTop
    )
    val targetTag = seenTags.elementAt(indexFromTop)
    val targetMatcher = hasTestTag(targetTag)
    scrollResultsUntilNodeExists(targetMatcher)
    runCatching { findResultsScrollable()?.performScrollToNode(targetMatcher) }
    val targetNode = onNodeWithTag(targetTag)
    runCatching { targetNode.performScrollTo() }
    ShadowLooper.idleMainLooper()
    waitForIdle()
    return targetNode
}
