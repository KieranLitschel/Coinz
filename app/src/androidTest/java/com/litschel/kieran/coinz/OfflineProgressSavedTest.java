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

// This is the same as the ProgressSavedTest, except we turn off the internet for collection coin and
// exchanging it, and we don't gift coin (as this is disabled offline), then turn it back on after
// we've finished before the first logout

@LargeTest
@RunWith(AndroidJUnit4.class)
public class OfflineProgressSavedTest {

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
    public void offlineProgressSavedTest() {

        try {
            Thread.sleep(7500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Put the app into offline mode

        ViewInteraction floatingActionButtonInternetOff = onView(
                allOf(withId(R.id.internetButton),
                        childAtPosition(
                                allOf(withId(R.id.content_frame),
                                        childAtPosition(
                                                withId(R.id.drawer_layout),
                                                0)),
                                0),
                        isDisplayed()));
        floatingActionButtonInternetOff.perform(click());

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Click the collect coins button 4 times

        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.collectCoinFAB),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout),
                                        childAtPosition(
                                                withId(R.id.flContent),
                                                0)),
                                2),
                        isDisplayed()));
        floatingActionButton.perform(click());

        try {
            Thread.sleep(4500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction floatingActionButton2 = onView(
                allOf(withId(R.id.collectCoinFAB),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout),
                                        childAtPosition(
                                                withId(R.id.flContent),
                                                0)),
                                2),
                        isDisplayed()));
        floatingActionButton2.perform(click());

        try {
            Thread.sleep(4500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction floatingActionButton3 = onView(
                allOf(withId(R.id.collectCoinFAB),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout),
                                        childAtPosition(
                                                withId(R.id.flContent),
                                                0)),
                                2),
                        isDisplayed()));
        floatingActionButton3.perform(click());

        try {
            Thread.sleep(4500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction floatingActionButton4 = onView(
                allOf(withId(R.id.collectCoinFAB),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout),
                                        childAtPosition(
                                                withId(R.id.flContent),
                                                0)),
                                2),
                        isDisplayed()));
        floatingActionButton4.perform(click());

        try {
            Thread.sleep(4500);
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

        ViewInteraction floatingActionButtonExchange = onView(
                allOf(withId(R.id.exchangeCryptoBtn),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.flContent),
                                        0),
                                1),
                        isDisplayed()));
        floatingActionButtonExchange.perform(click());

        // Exchange 6.679450014816521 PENY for GOLD

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

        onView(withText("PENY"))
                .inRoot(isPlatformPopup())
                .perform(click());

        ViewInteraction appCompatEditText = onView(
                allOf(withId(R.id.tradeAmountEditText),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                3),
                        isDisplayed()));
        appCompatEditText.perform(replaceText("6.679450014816521"), closeSoftKeyboard());

        // CLICK ACCEPT TRADE BUTTON

        ViewInteraction appCompatButton4 = onView(
                allOf(withId(android.R.id.button1), withText("Accept Trade"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton4.perform(scrollTo(), click());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Put the app into online mode

        ViewInteraction floatingActionButtonInternetOn = onView(
                allOf(withId(R.id.internetButton),
                        childAtPosition(
                                allOf(withId(R.id.content_frame),
                                        childAtPosition(
                                                withId(R.id.drawer_layout),
                                                0)),
                                0),
                        isDisplayed()));
        floatingActionButtonInternetOn.perform(click());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Logout

        ViewInteraction appCompatImageButtonLogout = onView(
                allOf(withContentDescription("Navigate up"),
                        childAtPosition(
                                allOf(withId(R.id.toolbar),
                                        childAtPosition(
                                                withId(R.id.content_frame),
                                                2)),
                                1),
                        isDisplayed()));
        appCompatImageButtonLogout.perform(click());

        ViewInteraction navigationMenuItemViewLogout = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.design_navigation_view),
                                childAtPosition(
                                        withId(R.id.nav_view),
                                        0)),
                        4),
                        isDisplayed()));
        navigationMenuItemViewLogout.perform(click());

        // Login

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction textInputEditTextLogin = onView(
                allOf(withId(R.id.email),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.email_layout),
                                        0),
                                0)));
        textInputEditTextLogin.perform(scrollTo(), click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction textInputEditTextLogin2 = onView(
                allOf(withId(R.id.email),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.email_layout),
                                        0),
                                0)));
        textInputEditTextLogin2.perform(scrollTo(), replaceText("tester1@coinz.litschel.com"), closeSoftKeyboard());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatButtonLogin = onView(
                allOf(withId(R.id.button_next), withText("Next"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                1)));
        appCompatButtonLogin.perform(scrollTo(), click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction textInputEditTextLogin3 = onView(
                allOf(withId(R.id.password),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.password_layout),
                                        0),
                                0)));
        textInputEditTextLogin3.perform(scrollTo(), replaceText("test1234"), closeSoftKeyboard());

        ViewInteraction appCompatButtonLogin2 = onView(
                allOf(withId(R.id.button_done), withText("Sign in"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                4)));
        appCompatButtonLogin2.perform(scrollTo(), click());

        try {
            Thread.sleep(7500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Go to the balance fragment

        ViewInteraction appCompatImageButtonBal2 = onView(
                allOf(withContentDescription("Navigate up"),
                        childAtPosition(
                                allOf(withId(R.id.toolbar),
                                        childAtPosition(
                                                withId(R.id.content_frame),
                                                2)),
                                1),
                        isDisplayed()));
        appCompatImageButtonBal2.perform(click());

        ViewInteraction navigationMenuItemViewBal2 = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.design_navigation_view),
                                childAtPosition(
                                        withId(R.id.nav_view),
                                        0)),
                        2),
                        isDisplayed()));
        navigationMenuItemViewBal2.perform(click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check the balances updated as expected

        // Note that the value we expect isn't exactly the value we'd get if we calculated the result
        // in a calculator, this is because the true value is larger than a double, so the value is
        // rounded.

        ViewInteraction textView = onView(
                allOf(withId(R.id.GOLDText), withText("GOLD:\n209.86752225905985\n"),
                        childAtPosition(
                                allOf(withId(R.id.GOLDRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                0)),
                                0),
                        isDisplayed()));
        textView.check(matches(withText("GOLD:\n209.86752225905985\n")));

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.PENYText), withText("PENY:\n0.0\n"),
                        childAtPosition(
                                allOf(withId(R.id.PENYRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                1)),
                                0),
                        isDisplayed()));
        textView2.check(matches(withText("PENY:\n0.0\n")));

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.DOLRText), withText("DOLR:\n0.0\n"),
                        childAtPosition(
                                allOf(withId(R.id.DOLRRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                2)),
                                0),
                        isDisplayed()));
        textView3.check(matches(withText("DOLR:\n0.0\n")));

        ViewInteraction textView4 = onView(
                allOf(withId(R.id.SHILText), withText("SHIL:\n12.57541532608107\n"),
                        childAtPosition(
                                allOf(withId(R.id.SHILRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                3)),
                                0),
                        isDisplayed()));
        textView4.check(matches(withText("SHIL:\n12.57541532608107\n")));

        ViewInteraction textView5 = onView(
                allOf(withId(R.id.QUIDText), withText("QUID:\n0.0\n"),
                        childAtPosition(
                                allOf(withId(R.id.QUIDRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                4)),
                                0),
                        isDisplayed()));
        textView5.check(matches(withText("QUID:\n0.0\n")));
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
