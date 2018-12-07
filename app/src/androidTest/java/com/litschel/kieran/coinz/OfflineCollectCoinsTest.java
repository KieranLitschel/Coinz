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
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

// This is the same test as CollectCoinsTest but we run it offline instead.

@LargeTest
@RunWith(AndroidJUnit4.class)
public class OfflineCollectCoinsTest {

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
    public void offlineCollectCoinsTest() {

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

        // Click the collect coins button enough times to have collected some of each coin

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

        ViewInteraction floatingActionButton5 = onView(
                allOf(withId(R.id.collectCoinFAB),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout),
                                        childAtPosition(
                                                withId(R.id.flContent),
                                                0)),
                                2),
                        isDisplayed()));
        floatingActionButton5.perform(click());

        try {
            Thread.sleep(4500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction floatingActionButton6 = onView(
                allOf(withId(R.id.collectCoinFAB),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout),
                                        childAtPosition(
                                                withId(R.id.flContent),
                                                0)),
                                2),
                        isDisplayed()));
        floatingActionButton6.perform(click());

        try {
            Thread.sleep(4500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction floatingActionButton7 = onView(
                allOf(withId(R.id.collectCoinFAB),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout),
                                        childAtPosition(
                                                withId(R.id.flContent),
                                                0)),
                                2),
                        isDisplayed()));
        floatingActionButton7.perform(click());

        try {
            Thread.sleep(4500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction floatingActionButton8 = onView(
                allOf(withId(R.id.collectCoinFAB),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout),
                                        childAtPosition(
                                                withId(R.id.flContent),
                                                0)),
                                2),
                        isDisplayed()));
        floatingActionButton8.perform(click());

        try {
            Thread.sleep(4500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction floatingActionButton9 = onView(
                allOf(withId(R.id.collectCoinFAB),
                        childAtPosition(
                                allOf(withId(R.id.coordinatorLayout),
                                        childAtPosition(
                                                withId(R.id.flContent),
                                                0)),
                                2),
                        isDisplayed()));
        floatingActionButton9.perform(click());

        try {
            Thread.sleep(4500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Go to the balance fragment

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

        // Check the balances updated as expected

        ViewInteraction textView = onView(
                allOf(withId(R.id.GOLDText), withText("GOLD:\n0.0\n"),
                        childAtPosition(
                                allOf(withId(R.id.GOLDRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                0)),
                                0),
                        isDisplayed()));
        textView.check(matches(withText("GOLD:\n0.0\n")));

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.PENYText), withText("PENY:\n6.679450014816521\n"),
                        childAtPosition(
                                allOf(withId(R.id.PENYRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                1)),
                                0),
                        isDisplayed()));
        textView2.check(matches(withText("PENY:\n6.679450014816521\n")));

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.DOLRText), withText("DOLR:\n6.780261952440848\n"),
                        childAtPosition(
                                allOf(withId(R.id.DOLRRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                2)),
                                0),
                        isDisplayed()));
        textView3.check(matches(withText("DOLR:\n6.780261952440848\n")));

        ViewInteraction textView4 = onView(
                allOf(withId(R.id.SHILText), withText("SHIL:\n27.139268122070483\n"),
                        childAtPosition(
                                allOf(withId(R.id.SHILRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                3)),
                                0),
                        isDisplayed()));
        textView4.check(matches(withText("SHIL:\n27.139268122070483\n")));

        ViewInteraction textView5 = onView(
                allOf(withId(R.id.QUIDText), withText("QUID:\n15.475834848170207\n"),
                        childAtPosition(
                                allOf(withId(R.id.QUIDRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                4)),
                                0),
                        isDisplayed()));
        textView5.check(matches(withText("QUID:\n15.475834848170207\n")));
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
