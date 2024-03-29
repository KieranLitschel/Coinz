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

// This is the same as the basic exchange test but we run it offline instead

@LargeTest
@RunWith(AndroidJUnit4.class)
public class OfflineBasicExchangeTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<MainActivity>(MainActivity.class) {
        @Override
        protected void beforeActivityLaunched() {
            Context context = InstrumentationRegistry.getTargetContext();
            TestSetupMethods.resetSettings(context);
            TestSetupMethods.setTester1LoggedIn(context);
            TestSetupMethods.resetTestDB();
            TestSetupMethods.setupUser("ROtiCeFTuIZ3xNOhEweThG3htXj1", "",
                    new String[][]{new String[]{"QUID", "50.0"}});
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
    public void offlineBasicExchangeTest() {

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

        // Exchange 25 QUID for GOLD

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

        ViewInteraction appCompatEditText = onView(
                allOf(withId(R.id.tradeAmountEditText),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                3),
                        isDisplayed()));
        appCompatEditText.perform(replaceText("25"), closeSoftKeyboard());

        // CHECK EXCHANGE RATE IS CORRECT

        ViewInteraction textView = onView(
                allOf(withId(R.id.exchangeRateText), withText("Exchange rate:\n54.02282290035586"),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                2),
                        isDisplayed()));
        textView.check(matches(withText("Exchange rate:\n54.02282290035586")));

        // CHECK EDIT TEXT IS CORRECT

        ViewInteraction editText = onView(
                allOf(withId(R.id.tradeAmountEditText), withText("25"),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                3),
                        isDisplayed()));
        editText.check(matches(withText("25")));

        // CHECK OFFERED CRYPTO IS CORRECT

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.offeredGoldText), withText("Offered gold for crypto:\n1350.5705725088965"),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                4),
                        isDisplayed()));
        textView2.check(matches(withText("Offered gold for crypto:\n1350.5705725088965")));

        // CHECK REMAINING CRYPTO BANK WILL ACCEPT TODAY IS CORRECT

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.coinsRemainingToday), withText("Remaining crypto bank will accept today:\n0.0"),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                5),
                        isDisplayed()));
        textView3.check(matches(withText("Remaining crypto bank will accept today:\n0.0")));

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

        // Check the balances updated as expected

        ViewInteraction textViewBal = onView(
                allOf(withId(R.id.GOLDText), withText("GOLD:\n1350.5705725088965\n"),
                        childAtPosition(
                                allOf(withId(R.id.GOLDRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                0)),
                                0),
                        isDisplayed()));
        textViewBal.check(matches(withText("GOLD:\n1350.5705725088965\n")));

        ViewInteraction textViewBal2 = onView(
                allOf(withId(R.id.PENYText), withText("PENY:\n0.0\n"),
                        childAtPosition(
                                allOf(withId(R.id.PENYRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                1)),
                                0),
                        isDisplayed()));
        textViewBal2.check(matches(withText("PENY:\n0.0\n")));

        ViewInteraction textViewBal3 = onView(
                allOf(withId(R.id.DOLRText), withText("DOLR:\n0.0\n"),
                        childAtPosition(
                                allOf(withId(R.id.DOLRRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                2)),
                                0),
                        isDisplayed()));
        textViewBal3.check(matches(withText("DOLR:\n0.0\n")));

        ViewInteraction textViewBal4 = onView(
                allOf(withId(R.id.SHILText), withText("SHIL:\n0.0\n"),
                        childAtPosition(
                                allOf(withId(R.id.SHILRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                3)),
                                0),
                        isDisplayed()));
        textViewBal4.check(matches(withText("SHIL:\n0.0\n")));

        ViewInteraction textViewBal5 = onView(
                allOf(withId(R.id.QUIDText), withText("QUID:\n25.0\n"),
                        childAtPosition(
                                allOf(withId(R.id.QUIDRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                4)),
                                0),
                        isDisplayed()));
        textViewBal5.check(matches(withText("QUID:\n25.0\n")));
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
