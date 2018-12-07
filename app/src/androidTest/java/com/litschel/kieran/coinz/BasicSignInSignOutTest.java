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
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

// This tests that user details are downloaded correctly on sign in, and that signing in and out
// works as expected

// Note that we don't need to test the sign in interface beyond this as it is the default implemented
// with firebase authentication, so it will have already been extensively tested

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BasicSignInSignOutTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<MainActivity>(MainActivity.class) {
        @Override
        protected void beforeActivityLaunched() {
            Context context = InstrumentationRegistry.getTargetContext();
            TestSetupMethods.resetSettings(context);
            TestSetupMethods.resetTestDB();
            TestSetupMethods.setupUser("ROtiCeFTuIZ3xNOhEweThG3htXj1", "",
                    new String[][]{
                            new String[]{"GOLD", "50.0"},
                            new String[]{"PENY", "6.679450014816521"},
                            new String[]{"DOLR", "6.780261952440848"},
                            new String[]{"SHIL", "27.139268122070483"},
                            new String[]{"QUID", "15.475834848170207"}});
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
    public void basicSignInSignOutTest() {

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

        // Check the balances are as expected

        ViewInteraction textView = onView(
                allOf(withId(R.id.GOLDText), withText("GOLD:\n50.0\n"),
                        childAtPosition(
                                allOf(withId(R.id.GOLDRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                0)),
                                0),
                        isDisplayed()));
        textView.check(matches(withText("GOLD:\n50.0\n")));

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

        ViewInteraction textInputEditTextLoginAgain = onView(
                allOf(withId(R.id.email),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.email_layout),
                                        0),
                                0)));
        textInputEditTextLoginAgain.perform(scrollTo(), click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction textInputEditTextLogin2Again = onView(
                allOf(withId(R.id.email),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.email_layout),
                                        0),
                                0)));
        textInputEditTextLogin2Again.perform(scrollTo(), replaceText("tester2@coinz.litschel.com"), closeSoftKeyboard());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction appCompatButtonLoginAgain = onView(
                allOf(withId(R.id.button_next), withText("Next"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                1)));
        appCompatButtonLoginAgain.perform(scrollTo(), click());

        // Added a sleep statement to match the app's execution delay.
        // The recommended way to handle such scenarios is to use Espresso idling resources:
        // https://google.github.io/android-testing-support-library/docs/espresso/idling-resource/index.html
        try {
            Thread.sleep(3500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        ViewInteraction textInputEditTextLogin3Again = onView(
                allOf(withId(R.id.password),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.password_layout),
                                        0),
                                0)));
        textInputEditTextLogin3Again.perform(scrollTo(), replaceText("test1234"), closeSoftKeyboard());

        ViewInteraction appCompatButtonLogin2Again = onView(
                allOf(withId(R.id.button_done), withText("Sign in"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                4)));
        appCompatButtonLogin2Again.perform(scrollTo(), click());

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

        // Check the balances are as expected

        // Note that the value we expect isn't exactly the value we'd get if we calculated the result
        // in a calculator, this is because the true value is larger than a double, so the value is
        // rounded.

        ViewInteraction textView6 = onView(
                allOf(withId(R.id.GOLDText), withText("GOLD:\n0.0\n"),
                        childAtPosition(
                                allOf(withId(R.id.GOLDRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                0)),
                                0),
                        isDisplayed()));
        textView6.check(matches(withText("GOLD:\n0.0\n")));

        ViewInteraction textView7 = onView(
                allOf(withId(R.id.PENYText), withText("PENY:\n0.0\n"),
                        childAtPosition(
                                allOf(withId(R.id.PENYRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                1)),
                                0),
                        isDisplayed()));
        textView7.check(matches(withText("PENY:\n0.0\n")));

        ViewInteraction textView8 = onView(
                allOf(withId(R.id.DOLRText), withText("DOLR:\n0.0\n"),
                        childAtPosition(
                                allOf(withId(R.id.DOLRRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                2)),
                                0),
                        isDisplayed()));
        textView8.check(matches(withText("DOLR:\n0.0\n")));

        ViewInteraction textView9 = onView(
                allOf(withId(R.id.SHILText), withText("SHIL:\n0.0\n"),
                        childAtPosition(
                                allOf(withId(R.id.SHILRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                3)),
                                0),
                        isDisplayed()));
        textView9.check(matches(withText("SHIL:\n0.0\n")));

        ViewInteraction textView10 = onView(
                allOf(withId(R.id.QUIDText), withText("QUID:\n0.0\n"),
                        childAtPosition(
                                allOf(withId(R.id.QUIDRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                4)),
                                0),
                        isDisplayed()));
        textView10.check(matches(withText("QUID:\n0.0\n")));
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
