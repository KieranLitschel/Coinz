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

// This test tries to break the gift activity by trying to gift more coin than we have, and trying
// to gift coin to a non-existent user

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BreakSendGiftTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<MainActivity>(MainActivity.class) {
        @Override
        protected void beforeActivityLaunched() {
            Context context = InstrumentationRegistry.getTargetContext();
            TestSetupMethods.resetSettings(context);
            TestSetupMethods.setTester1LoggedIn(context);
            TestSetupMethods.resetTestDB();
            TestSetupMethods.setupUser("ROtiCeFTuIZ3xNOhEweThG3htXj1", "bob", new String[][]{
                    new String[]{"QUID", "25.1"},
                    new String[]{"DOLR", "12.5"}});
            TestSetupMethods.setupUser("8SpoGV9JFlXKlIiuAXkQ22PB0MF3", "jim", new String[][]{});
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
    public void breakSendGiftTest() {

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

        // Open the exchange gift dialog fragment

        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.giftCryptoBtn),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.flContent),
                                        0),
                                2),
                        isDisplayed()));
        floatingActionButton.perform(click());

        // Input want to exchange 25 DOLR

        ViewInteraction appCompatSpinner3 = onView(
                allOf(withId(R.id.cryptoSpinner),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                3),
                        isDisplayed()));
        appCompatSpinner3.perform(click());

        onView(withText("DOLR"))
                .inRoot(isPlatformPopup())
                .perform(click());

        ViewInteraction appCompatEditText = onView(
                allOf(withId(R.id.giftAmountEditText),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                4),
                        isDisplayed()));
        appCompatEditText.perform(replaceText("25"), closeSoftKeyboard());

        // Check trade amount corrected to 12.5 DOLR (amount in balance)

        ViewInteraction editText = onView(
                allOf(withId(R.id.giftAmountEditText), withText("12.5"),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                4),
                        isDisplayed()));
        editText.check(matches(withText("12.5")));

        // Try to send money to non-existent user

        ViewInteraction appCompatEditText21 = onView(
                allOf(withId(R.id.recipientEditText),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                5),
                        isDisplayed()));
        appCompatEditText21.perform(replaceText("frank"), closeSoftKeyboard());

        ViewInteraction appCompatButton7 = onView(
                allOf(withId(android.R.id.button1), withText("Send gift"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton7.perform(scrollTo(), click());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check error message appears

        ViewInteraction textView7 = onView(
                allOf(withId(R.id.usernameValidText), withText("Recipients username not found in database"),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                6),
                        isDisplayed()));
        textView7.check(matches(withText("Recipients username not found in database")));

        // Switch to SHIL

        ViewInteraction appCompatSpinnerShil = onView(
                allOf(withId(R.id.cryptoSpinner),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                3),
                        isDisplayed()));
        appCompatSpinnerShil.perform(click());

        onView(withText("SHIL"))
                .inRoot(isPlatformPopup())
                .perform(click());

        // Check trade amount corrected to 0 SHIL (amount in balance)

        ViewInteraction editTextShil = onView(
                allOf(withId(R.id.giftAmountEditText), withText("0.0"),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                4),
                        isDisplayed()));
        editTextShil.check(matches(withText("0.0")));
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
