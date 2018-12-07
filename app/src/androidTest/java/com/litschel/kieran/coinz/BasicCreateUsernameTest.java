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

// Tests creating a username under expected use

// Also note we do not need to test basic functionality of changing username as the create username
// and change username dialogs are the same with just a couple cosmetic tweaks

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BasicCreateUsernameTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<MainActivity>(MainActivity.class) {

        // These just ensure at the start and end of each test the settings are wiped

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
    public void basicCreateUsernameTest() {

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

        // Open the gift dialog fragment, which as there is no username will open the create
        // username dialog

        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.giftCryptoBtn),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.flContent),
                                        0),
                                2),
                        isDisplayed()));
        floatingActionButton.perform(click());

        // Change username

        ViewInteraction appCompatEditText8 = onView(
                allOf(withId(R.id.usernameEditText),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                2),
                        isDisplayed()));
        appCompatEditText8.perform(replaceText("bob"), closeSoftKeyboard());

        ViewInteraction appCompatButton5 = onView(
                allOf(withId(android.R.id.button1), withText("Create username"),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(is("android.widget.ScrollView")),
                                        0),
                                3)));
        appCompatButton5.perform(scrollTo(), click());

        try {
            Thread.sleep(2500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Open the gift dialog fragment

        ViewInteraction floatingActionButton2 = onView(
                allOf(withId(R.id.giftCryptoBtn),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.flContent),
                                        0),
                                2),
                        isDisplayed()));
        floatingActionButton2.perform(click());

        // Check the username is updated correctly

        ViewInteraction textView6 = onView(
                allOf(withId(R.id.usernameText), withText("Your username is:\nbob"),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                1),
                        isDisplayed()));
        textView6.check(matches(withText("Your username is:\nbob")));
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
