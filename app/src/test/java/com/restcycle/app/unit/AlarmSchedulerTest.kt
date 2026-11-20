package com.restcycle.app.unit

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.provider.AlarmClock
import android.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.restcycle.app.MainActivity
import com.restcycle.app.platform.intent.AlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
@Config(sdk = [36])
class AlarmSchedulerTest {

    private lateinit var application: Application
    private lateinit var scheduler: AlarmScheduler

    @Before
    fun setUp() {
        application = ApplicationProvider.getApplicationContext()
        scheduler = AlarmScheduler()
        val shadowApp = Shadows.shadowOf(application)
        shadowApp.clearNextStartedActivities()
        shadowApp.checkActivities(false)
    }

    @Test
    fun buildSetAlarmIntent_populatesActionAndExtras() {
        val intent = scheduler.buildSetAlarmIntent(LocalTime.of(22, 45), "Bedtime")

        assertEquals(AlarmClock.ACTION_SET_ALARM, intent.action)
        assertEquals(22, intent.getIntExtra(AlarmClock.EXTRA_HOUR, -1))
        assertEquals(45, intent.getIntExtra(AlarmClock.EXTRA_MINUTES, -1))
        assertEquals("Bedtime", intent.getStringExtra(AlarmClock.EXTRA_MESSAGE))
        assertTrue(intent.hasExtra(AlarmClock.EXTRA_SKIP_UI))
        assertFalse(intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, true))
    }

    @Test
    fun buildSetAlarmIntent_midnightAndEndOfDay() {
        val midnightIntent = scheduler.buildSetAlarmIntent(LocalTime.MIDNIGHT, "Midnight")
        assertEquals(0, midnightIntent.getIntExtra(AlarmClock.EXTRA_HOUR, -1))
        assertEquals(0, midnightIntent.getIntExtra(AlarmClock.EXTRA_MINUTES, -1))

        val endOfDayIntent = scheduler.buildSetAlarmIntent(LocalTime.of(23, 59), "Late")
        assertEquals(23, endOfDayIntent.getIntExtra(AlarmClock.EXTRA_HOUR, -1))
        assertEquals(59, endOfDayIntent.getIntExtra(AlarmClock.EXTRA_MINUTES, -1))
    }

    @Test
    fun buildSetTimerIntent_populatesActionLengthAndSkipUiFalse() {
        val intent = scheduler.buildSetTimerIntent(1800, "Classic Power Nap")

        assertEquals(AlarmClock.ACTION_SET_TIMER, intent.action)
        assertEquals(1800, intent.getIntExtra(AlarmClock.EXTRA_LENGTH, -1))
        assertEquals("Classic Power Nap", intent.getStringExtra(AlarmClock.EXTRA_MESSAGE))
        assertTrue(intent.hasExtra(AlarmClock.EXTRA_SKIP_UI))
        assertFalse(intent.getBooleanExtra(AlarmClock.EXTRA_SKIP_UI, true))
    }

    @Test
    fun buildSetTimerIntent_negativeLength_clampsToZero() {
        val intent = scheduler.buildSetTimerIntent(-30, "Nap")
        assertEquals(0, intent.getIntExtra(AlarmClock.EXTRA_LENGTH, -1))
    }

    @Test
    fun launchSetAlarm_applicationContext_addsNewTaskFlag() {
        val launched = scheduler.launchSetAlarm(application, LocalTime.of(6, 50), "Wake")
        assertTrue(launched)

        val started = Shadows.shadowOf(application).nextStartedActivity
        assertNotNull(started)
        assertEquals(AlarmClock.ACTION_SET_ALARM, started!!.action)
        assertEquals(6, started.getIntExtra(AlarmClock.EXTRA_HOUR, -1))
        assertEquals(50, started.getIntExtra(AlarmClock.EXTRA_MINUTES, -1))
        assertEquals("Wake", started.getStringExtra(AlarmClock.EXTRA_MESSAGE))
        assertTrue((started.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    }

    @Test
    fun launchSetTimer_applicationContext_addsNewTaskFlag() {
        val launched = scheduler.launchSetTimer(application, 1200, "Power Nap")
        assertTrue(launched)

        val started = Shadows.shadowOf(application).nextStartedActivity
        assertNotNull(started)
        assertEquals(AlarmClock.ACTION_SET_TIMER, started!!.action)
        assertEquals(1200, started.getIntExtra(AlarmClock.EXTRA_LENGTH, -1))
        assertEquals("Power Nap", started.getStringExtra(AlarmClock.EXTRA_MESSAGE))
        assertTrue((started.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    }

    @Test
    fun launchSetAlarm_wrappedActivityContext_omitsNewTaskFlag() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        val wrapped = ContextThemeWrapper(activity, 0)

        val launched = scheduler.launchSetAlarm(wrapped, LocalTime.of(7, 0), "Wake")
        assertTrue(launched)

        val started = Shadows.shadowOf(application).nextStartedActivity
        assertNotNull(started)
        assertEquals(0, started!!.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun launchSetTimer_wrappedActivityContext_omitsNewTaskFlag() {
        val activity = Robolectric.buildActivity(MainActivity::class.java).create().get()
        val wrapped = ContextThemeWrapper(activity, 0)

        val launched = scheduler.launchSetTimer(wrapped, 1200, "Power Nap")
        assertTrue(launched)

        val started = Shadows.shadowOf(application).nextStartedActivity
        assertNotNull(started)
        assertEquals(AlarmClock.ACTION_SET_TIMER, started!!.action)
        assertEquals(0, started.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    @Test
    fun launchSetAlarm_noHandler_returnsFalse() {
        Shadows.shadowOf(application).checkActivities(true)

        val launched = scheduler.launchSetAlarm(application, LocalTime.of(6, 50), "Wake")
        assertFalse(launched)
        assertNull(Shadows.shadowOf(application).nextStartedActivity)
    }

    @Test
    fun launchSetTimer_noHandler_returnsFalse() {
        Shadows.shadowOf(application).checkActivities(true)

        val launched = scheduler.launchSetTimer(application, 1200, "Nap")
        assertFalse(launched)
        assertNull(Shadows.shadowOf(application).nextStartedActivity)
    }

    @Test
    fun launchBoth_withRegisteredClockHandlerAndCheckActivitiesTrue_returnsTrue() {
        val clockComponent = ComponentName("com.example.clock", "com.example.clock.ClockActivity")
        val shadowPm = Shadows.shadowOf(application.packageManager)
        shadowPm.addActivityIfNotPresent(clockComponent)
        shadowPm.addIntentFilterForActivity(
            clockComponent,
            IntentFilter(AlarmClock.ACTION_SET_ALARM).apply { addCategory(Intent.CATEGORY_DEFAULT) }
        )
        shadowPm.addIntentFilterForActivity(
            clockComponent,
            IntentFilter(AlarmClock.ACTION_SET_TIMER).apply { addCategory(Intent.CATEGORY_DEFAULT) }
        )
        Shadows.shadowOf(application).checkActivities(true)

        assertTrue(scheduler.launchSetAlarm(application, LocalTime.of(22, 30), "Bedtime"))
        val alarmStarted = Shadows.shadowOf(application).nextStartedActivity
        assertEquals(AlarmClock.ACTION_SET_ALARM, alarmStarted?.action)

        assertTrue(scheduler.launchSetTimer(application, 1800, "Classic Power Nap"))
        val timerStarted = Shadows.shadowOf(application).nextStartedActivity
        assertEquals(AlarmClock.ACTION_SET_TIMER, timerStarted?.action)
    }
}
