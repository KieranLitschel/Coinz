package com.litschel.kieran.coinz;


import android.support.test.espresso.DataInteraction;
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
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onData;
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
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.is;

// This test tries to break the exchange activity by trading more coin than we have

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BreakSendGiftTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION");

    @Before
    public void beforeTest() {
        DatabaseMethods.resetTestDB();
        DatabaseMethods.setupUser("ROtiCeFTuIZ3xNOhEweThG3htXj1", "bob", new String[][]{
                new String[]{"QUID", "25.1"},
                new String[]{"DOLR", "12.5"}});
        DatabaseMethods.setupUser("8SpoGV9JFlXKlIiuAXkQ22PB0MF3", "jim", new String[][]{});
    }

    @Test
    public void breakSendGiftTest() {
        // Login

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction textInputEditText = onView(
                allOf(withId(R.id.email),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.email_layout),
                                        0),
                                0)));
        textInputEditText.perform(scrollTo(), click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction textInputEditText2 = onView(
                allOf(withId(R.id.email),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.email_layout),
                                        0),
                                0)));
        textInputEditText2.perform(scrollTo(), replaceText("tester1@coinz.litschel.com"), closeSoftKeyboard());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatButton = onView(
                allOf(withId(R.id.button_next), withText("Next"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                1)));
        appCompatButton.perform(scrollTo(), click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction textInputEditText3 = onView(
                allOf(withId(R.id.password),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.password_layout),
                                        0),
                                0)));
        textInputEditText3.perform(scrollTo(), replaceText("test1234"), closeSoftKeyboard());

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.button_done), withText("Sign in"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                4)));
        appCompatButton2.perform(scrollTo(), click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
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

        ViewInteraction appCompatCheckedTextView3 = onView(withText("DOLR"))
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

        ViewInteraction appCompatCheckedTextViewShil = onView(withText("SHIL"))
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

        // CLICK CANCEL BUTTON

        ViewInteraction appCompatButton11 = onView(
                allOf(withId(android.R.id.button2), withText("Cancel"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                2)));
        appCompatButton11.perform(scrollTo(), click());

        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Log out of the app to preprare for the next test

        ViewInteraction appCompatImageButton2 = onView(
                allOf(withContentDescription("Navigate up"),
                        childAtPosition(
                                allOf(withId(R.id.toolbar),
                                        childAtPosition(
                                                withId(R.id.content_frame),
                                                2)),
                                1),
                        isDisplayed()));
        appCompatImageButton2.perform(click());

        ViewInteraction navigationMenuItemView2 = onView(
                allOf(childAtPosition(
                        allOf(withId(R.id.design_navigation_view),
                                childAtPosition(
                                        withId(R.id.nav_view),
                                        0)),
                        4),
                        isDisplayed()));
        navigationMenuItemView2.perform(click());
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