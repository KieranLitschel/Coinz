package com.litschel.kieran.coinz;


import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

// Test whether the map downloaded correctly by looking at the exchange rates and making sure they
// match those for the fixed date for testers (1st December 2018)

@LargeTest
@RunWith(AndroidJUnit4.class)
public class MapDownloadTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<MainActivity>(MainActivity.class) {
        @Override
        protected void beforeActivityLaunched() {
            Context context = InstrumentationRegistry.getTargetContext();
            TestSetupMethods.resetSettings(context);
            TestSetupMethods.setTester1LoggedIn(context);
            TestSetupMethods.resetTestDB();
            TestSetupMethods.setupDefaultTester1InDB();
            super.beforeActivityLaunched();
        }

        @Override
        protected void afterActivityFinished() {
            Context context = InstrumentationRegistry.getTargetContext();
            TestSetupMethods.resetSettings(context);
            super.afterActivityFinished();
        }
    };

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION");

    @Test
    public void mapDownloadTest() {

        try {
            Thread.sleep(7500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Open the balance fragment

        ViewInteraction appCompatImageButton = onView(
                allOf(withContentDescription("Navigate up"),
                        childAtPosition(
                                allOf(withId(R.id.toolbar),
                                        childAtPosition(
                                                withId(R.id.content_frame),
                                                2)),
                                1),
                        isDisplayed()));
        appCompatImageButton.perform(click());

        ViewInteraction navigationMenuItemView = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.design_navigation_view),
                                childAtPosition(
                                        withId(R.id.nav_view),
                                        0)),
                        2),
                        isDisplayed()));
        navigationMenuItemView.perform(click());

        // Open the exchange dialog fragment

        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.exchangeCryptoBtn),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.flContent),
                                        0),
                                1),
                        isDisplayed()));
        floatingActionButton.perform(click());

        // Check the displayed exchange rate for PENY is correct

        ViewInteraction textView = onView(
                allOf(withId(R.id.exchangeRateText), withText("Exchange rate:\n31.419880647886657"),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                2),
                        isDisplayed()));
        textView.check(matches(withText("Exchange rate:\n31.419880647886657")));

        // Change to an exchange for DOLR and make sure the displayed exchange rate is correct

        ViewInteraction appCompatSpinner = onView(
                allOf(withId(R.id.cryptoSpinner),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                1),
                        isDisplayed()));
        appCompatSpinner.perform(click());

        // I wrote the below interaction myself as the espresso test was not recording selecting
        // the item on the spinner, I learnt to do it this way from the answer to this question
        // https://stackoverflow.com/questions/38920141/runtimeexception-in-android-espresso-when-selecting-spinner-in-dialog
        onView(withText("DOLR"))
                .inRoot(isPlatformPopup())
                .perform(click());


        ViewInteraction textView2 = onView(
                allOf(withId(R.id.exchangeRateText), withText("Exchange rate:\n19.617170372971632"),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                2),
                        isDisplayed()));
        textView2.check(matches(withText("Exchange rate:\n19.617170372971632")));

        // Change to an exchange for SHIL and make sure the displayed exchange rate is correct

        ViewInteraction appCompatSpinner2 = onView(
                allOf(withId(R.id.cryptoSpinner),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                1),
                        isDisplayed()));
        appCompatSpinner2.perform(click());

        onView(withText("SHIL"))
                .inRoot(isPlatformPopup())
                .perform(click());

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.exchangeRateText), withText("Exchange rate:\n49.18133521977366"),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                2),
                        isDisplayed()));
        textView3.check(matches(withText("Exchange rate:\n49.18133521977366")));

        // Change to an exchange for QUID and make sure the displayed exchange rate is correct

        ViewInteraction appCompatSpinner3 = onView(
                allOf(withId(R.id.cryptoSpinner),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                1),
                        isDisplayed()));
        appCompatSpinner3.perform(click());

        onView(withText("QUID"))
                .inRoot(isPlatformPopup())
                .perform(click());

        ViewInteraction textView4 = onView(
                allOf(withId(R.id.exchangeRateText), withText("Exchange rate:\n54.02282290035586"),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                2),
                        isDisplayed()));
        textView4.check(matches(withText("Exchange rate:\n54.02282290035586")));
    }

    private static Matcher<View> childAtPosition(
            final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }
}
