package com.example.fmlm

import android.util.Log
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.*
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    // Log
    private val TAG = "Expresso"

    @Rule @JvmField var activityActivityTestRule = ActivityTestRule(MainActivity::class.java)

    /**
     * PERFORMANCE TEST 01: Time to load map after calling Routing Screen
     */
    @Test
    fun perf01DisplayMap() {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        // Start recording once Routing has been pressed
        val startTime = System.currentTimeMillis()
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_routingComponentFragment))
        // Check that map is loaded
        onView(withId(R.id.map)).check(matches(isDisplayed()))
        // Stop timing when map is displayed
        val endTime = System.currentTimeMillis() - startTime
        Log.d(TAG, "PERF01 took: " + endTime.toString() + "ms")
    }

    /**
     * PERFORMANCE TEST 03: Time to get routes in the map after pin is set
     */
    @Test
    fun perf03PinMap() {
        // Record the iterated values
        var resultList: List<Long> = ArrayList()
        // Repeat test for 10 times
        for (i in 1..10) {
            // Navigate to Routing screen first
            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
            onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_routingComponentFragment))
            // Check that map is loaded
            onView(withId(R.id.map)).check(matches(isDisplayed()))
            // Start recording for before map press
            val startTime = System.currentTimeMillis()
            // Click the map
            onView(withId(R.id.map)).perform(click())
            // Stop timing when map is displayed
            val endTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "PERF03 - Iteration " + i + " took: " + endTime.toString() + "ms")
            // Perform back
            onView(isRoot()).perform(ViewActions.pressBack());
            resultList += endTime
        }
        Log.d(TAG, "PREF03 - Average time for 10 iterations: " + resultList.average().toString() + "ms")
    }

    /**
     * PERFORMANCE TEST 04: Time to get routes in the map after location is searched for in textbox
     */
    @Test
    fun perf04SearchMap() {
        // Record the iterated values
        var resultList: List<Long> = ArrayList()
        // Repeat test for 10 times
        for (i in 1..10) {
            // Navigate to Routing screen first
            onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
            onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_routingComponentFragment))
            // Check that map is loaded
            onView(withId(R.id.map)).check(matches(isDisplayed()))
            // Type Tampines Mall into EditBox
            onView(withId(R.id.TextInputEditText)).perform(typeText("Tampines Mall"))
            // Start recording for before search
            val startTime = System.currentTimeMillis()
            // Perform Search
            onView(withId(R.id.button_confirm)).perform(click())
            // Stop timing when map is displayed
            val endTime = System.currentTimeMillis() - startTime
            Log.d(TAG, "PERF04 - Iteration " + i + " took: " + endTime.toString() + "ms")
            // Close keyboard
            onView(isRoot()).perform(ViewActions.pressBack());
            // Perform back
            onView(isRoot()).perform(ViewActions.pressBack());
            resultList += endTime

        }
        Log.d(TAG, "PREF04 - Average time for 10 iterations: " + resultList.average().toString() + "ms")
    }
}