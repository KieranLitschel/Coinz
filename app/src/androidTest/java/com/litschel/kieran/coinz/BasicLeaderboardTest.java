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
import org.junit.Before;
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

// Tests the leaderboard under expected use

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BasicLeaderboardTest {

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<MainActivity>(MainActivity.class) {
        @Override
        protected void beforeActivityLaunched() {
            Context context = InstrumentationRegistry.getTargetContext();
            DatabaseMethods.resetSettings(context);
            super.beforeActivityLaunched();
        }

        @Override
        protected void afterActivityFinished() {
            Context context = InstrumentationRegistry.getTargetContext();
            DatabaseMethods.resetSettings(context);
            super.afterActivityFinished();
        }
    };

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION");

    @Before
    public void beforeTest(){
        DatabaseMethods.resetTestDB();
        // Setup the first 5 users for the leaderboard
        DatabaseMethods.setupUser("ROtiCeFTuIZ3xNOhEweThG3htXj1", "Alex", new String[][]{
                new String[]{"GOLD", "10"}
        });
        DatabaseMethods.setupUser("8SpoGV9JFlXKlIiuAXkQ22PB0MF3", "Ben", new String[][]{
                new String[]{"GOLD", "654646"}
        });
        DatabaseMethods.setupUser("dUfwCZBX2EW1QIHwHkhNZoqOWf52", "Charlie", new String[][]{
                new String[]{"GOLD", "21523353"}
        });
        DatabaseMethods.setupUser("ceFCMjlFOhXqrAHyuhKwEZGUUHn1", "Daniel", new String[][]{
                new String[]{"GOLD", "446448"}
        });
        DatabaseMethods.setupUser("7SaETL8QlXUQtgJMaJb9xO5JVAY2", "Ellie", new String[][]{
                new String[]{"GOLD", "7794"}
        });
    }

    @Test
    public void basicLeaderboardTest() {
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

        // Navigate to leaderboard fragment

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
                        3),
                        isDisplayed()));
        navigationMenuItemView2.perform(click());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check values are as expected

        ViewInteraction textView = onView(
                allOf(withId(R.id.Top10Text), withText("TOP 5"),
                        childAtPosition(
                                allOf(withId(R.id.Top10Row),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                3)),
                                0),
                        isDisplayed()));
        textView.check(matches(withText("TOP 5")));

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.HeaderRank), withText("RANK"),
                        childAtPosition(
                                allOf(withId(R.id.HeaderRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                4)),
                                0),
                        isDisplayed()));
        textView2.check(matches(withText("RANK")));

        ViewInteraction textView3 = onView(
                allOf(withId(R.id.HeaderName), withText("USERNAME"),
                        childAtPosition(
                                allOf(withId(R.id.HeaderRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                4)),
                                1),
                        isDisplayed()));
        textView3.check(matches(withText("USERNAME")));

        ViewInteraction textView4 = onView(
                allOf(withId(R.id.HeaderGold), withText("GOLD"),
                        childAtPosition(
                                allOf(withId(R.id.HeaderRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                4)),
                                2),
                        isDisplayed()));
        textView4.check(matches(withText("GOLD")));

        ViewInteraction textView5 = onView(
                allOf(withId(R.id.FirstRank), withText("1"),
                        childAtPosition(
                                allOf(withId(R.id.FirstRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                5)),
                                0),
                        isDisplayed()));
        textView5.check(matches(withText("1")));

        ViewInteraction textView6 = onView(
                allOf(withId(R.id.FirstName), withText("Charlie"),
                        childAtPosition(
                                allOf(withId(R.id.FirstRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                5)),
                                1),
                        isDisplayed()));
        textView6.check(matches(withText("Charlie")));

        ViewInteraction textView7 = onView(
                allOf(withId(R.id.FirstGold), withText("21.523 M"),
                        childAtPosition(
                                allOf(withId(R.id.FirstRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                5)),
                                2),
                        isDisplayed()));
        textView7.check(matches(withText("21.523 M")));

        ViewInteraction textView8 = onView(
                allOf(withId(R.id.SecondRank), withText("2"),
                        childAtPosition(
                                allOf(withId(R.id.SecondRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                6)),
                                0),
                        isDisplayed()));
        textView8.check(matches(withText("2")));

        ViewInteraction textView9 = onView(
                allOf(withId(R.id.SecondName), withText("Ben"),
                        childAtPosition(
                                allOf(withId(R.id.SecondRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                6)),
                                1),
                        isDisplayed()));
        textView9.check(matches(withText("Ben")));

        ViewInteraction textView10 = onView(
                allOf(withId(R.id.SecondGold), withText("654.646 K"),
                        childAtPosition(
                                allOf(withId(R.id.SecondRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                6)),
                                2),
                        isDisplayed()));
        textView10.check(matches(withText("654.646 K")));

        ViewInteraction textView11 = onView(
                allOf(withId(R.id.ThirdRank), withText("3"),
                        childAtPosition(
                                allOf(withId(R.id.ThirdRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                7)),
                                0),
                        isDisplayed()));
        textView11.check(matches(withText("3")));

        ViewInteraction textView12 = onView(
                allOf(withId(R.id.ThirdName), withText("Daniel"),
                        childAtPosition(
                                allOf(withId(R.id.ThirdRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                7)),
                                1),
                        isDisplayed()));
        textView12.check(matches(withText("Daniel")));

        ViewInteraction textView13 = onView(
                allOf(withId(R.id.ThirdGold), withText("446.448 K"),
                        childAtPosition(
                                allOf(withId(R.id.ThirdRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                7)),
                                2),
                        isDisplayed()));
        textView13.check(matches(withText("446.448 K")));

        ViewInteraction textView14 = onView(
                allOf(withId(R.id.FourthRank), withText("4"),
                        childAtPosition(
                                allOf(withId(R.id.FourthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                8)),
                                0),
                        isDisplayed()));
        textView14.check(matches(withText("4")));

        ViewInteraction textView15 = onView(
                allOf(withId(R.id.FourthName), withText("Ellie"),
                        childAtPosition(
                                allOf(withId(R.id.FourthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                8)),
                                1),
                        isDisplayed()));
        textView15.check(matches(withText("Ellie")));

        ViewInteraction textView16 = onView(
                allOf(withId(R.id.FourthGold), withText("7.794 K"),
                        childAtPosition(
                                allOf(withId(R.id.FourthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                8)),
                                2),
                        isDisplayed()));
        textView16.check(matches(withText("7.794 K")));

        ViewInteraction textView17 = onView(
                allOf(withId(R.id.FifthRank), withText("5"),
                        childAtPosition(
                                allOf(withId(R.id.FifthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                9)),
                                0),
                        isDisplayed()));
        textView17.check(matches(withText("5")));

        ViewInteraction textView18 = onView(
                allOf(withId(R.id.FifthName), withText("Alex"),
                        childAtPosition(
                                allOf(withId(R.id.FifthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                9)),
                                1),
                        isDisplayed()));
        textView18.check(matches(withText("Alex")));

        ViewInteraction textView19 = onView(
                allOf(withId(R.id.FifthGold), withText("10.000"),
                        childAtPosition(
                                allOf(withId(R.id.FifthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                9)),
                                2),
                        isDisplayed()));
        textView19.check(matches(withText("10.000")));

        // Add users to fill up the leaderboard

        DatabaseMethods.setupUser("VLtSELSe31cLPvNsvF692VyO5Py1", "Fiona", new String[][]{
                new String[]{"GOLD", "465465235289"}
        });
        DatabaseMethods.setupUser("d9X1huNfATNk0tsDM8T8AWLM4qb2", "Georgia", new String[][]{
                new String[]{"GOLD", "4545677"}
        });
        DatabaseMethods.setupUser("nXt3iQm2uOhpwtAPocgd2ii9imi2", "Harriet", new String[][]{
                new String[]{"GOLD", "798465"}
        });
        DatabaseMethods.setupUser("GwNuEg0pEUQvpuaBXiCU5uWip0G2", "India", new String[][]{
                new String[]{"GOLD", "7846548"}
        });
        DatabaseMethods.setupUser("9dAfz9WWB2SWalZdcHmLhjrGdvT2", "James", new String[][]{
                new String[]{"GOLD", "3435486564"}
        });

        // Refresh leaderboard

        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.refreshButton),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.flContent),
                                        0),
                                1),
                        isDisplayed()));
        floatingActionButton.perform(click());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check values are correct

        ViewInteraction textView21 = onView(
                allOf(withId(R.id.Top10Text), withText("TOP 10"),
                        childAtPosition(
                                allOf(withId(R.id.Top10Row),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                3)),
                                0),
                        isDisplayed()));
        textView21.check(matches(withText("TOP 10")));

        ViewInteraction textView22 = onView(
                allOf(withId(R.id.HeaderRank), withText("RANK"),
                        childAtPosition(
                                allOf(withId(R.id.HeaderRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                4)),
                                0),
                        isDisplayed()));
        textView22.check(matches(withText("RANK")));

        ViewInteraction textView23 = onView(
                allOf(withId(R.id.HeaderName), withText("USERNAME"),
                        childAtPosition(
                                allOf(withId(R.id.HeaderRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                4)),
                                1),
                        isDisplayed()));
        textView23.check(matches(withText("USERNAME")));

        ViewInteraction textView24 = onView(
                allOf(withId(R.id.HeaderGold), withText("GOLD"),
                        childAtPosition(
                                allOf(withId(R.id.HeaderRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                4)),
                                2),
                        isDisplayed()));
        textView24.check(matches(withText("GOLD")));

        ViewInteraction textView25 = onView(
                allOf(withId(R.id.FirstRank), withText("1"),
                        childAtPosition(
                                allOf(withId(R.id.FirstRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                5)),
                                0),
                        isDisplayed()));
        textView25.check(matches(withText("1")));

        ViewInteraction textView26 = onView(
                allOf(withId(R.id.FirstName), withText("Fiona"),
                        childAtPosition(
                                allOf(withId(R.id.FirstRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                5)),
                                1),
                        isDisplayed()));
        textView26.check(matches(withText("Fiona")));

        ViewInteraction textView27 = onView(
                allOf(withId(R.id.FirstGold), withText("465.465 B"),
                        childAtPosition(
                                allOf(withId(R.id.FirstRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                5)),
                                2),
                        isDisplayed()));
        textView27.check(matches(withText("465.465 B")));

        ViewInteraction textView28 = onView(
                allOf(withId(R.id.SecondRank), withText("2"),
                        childAtPosition(
                                allOf(withId(R.id.SecondRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                6)),
                                0),
                        isDisplayed()));
        textView28.check(matches(withText("2")));

        ViewInteraction textView29 = onView(
                allOf(withId(R.id.SecondName), withText("James"),
                        childAtPosition(
                                allOf(withId(R.id.SecondRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                6)),
                                1),
                        isDisplayed()));
        textView29.check(matches(withText("James")));

        ViewInteraction textView30 = onView(
                allOf(withId(R.id.SecondGold), withText("3.435 B"),
                        childAtPosition(
                                allOf(withId(R.id.SecondRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                6)),
                                2),
                        isDisplayed()));
        textView30.check(matches(withText("3.435 B")));

        ViewInteraction textView31 = onView(
                allOf(withId(R.id.ThirdRank), withText("3"),
                        childAtPosition(
                                allOf(withId(R.id.ThirdRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                7)),
                                0),
                        isDisplayed()));
        textView31.check(matches(withText("3")));

        ViewInteraction textView32 = onView(
                allOf(withId(R.id.ThirdName), withText("Charlie"),
                        childAtPosition(
                                allOf(withId(R.id.ThirdRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                7)),
                                1),
                        isDisplayed()));
        textView32.check(matches(withText("Charlie")));

        ViewInteraction textView33 = onView(
                allOf(withId(R.id.ThirdGold), withText("21.523 M"),
                        childAtPosition(
                                allOf(withId(R.id.ThirdRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                7)),
                                2),
                        isDisplayed()));
        textView33.check(matches(withText("21.523 M")));

        ViewInteraction textView34 = onView(
                allOf(withId(R.id.FourthRank), withText("4"),
                        childAtPosition(
                                allOf(withId(R.id.FourthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                8)),
                                0),
                        isDisplayed()));
        textView34.check(matches(withText("4")));

        ViewInteraction textView35 = onView(
                allOf(withId(R.id.FourthName), withText("India"),
                        childAtPosition(
                                allOf(withId(R.id.FourthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                8)),
                                1),
                        isDisplayed()));
        textView35.check(matches(withText("India")));

        ViewInteraction textView36 = onView(
                allOf(withId(R.id.FourthGold), withText("7.847 M"),
                        childAtPosition(
                                allOf(withId(R.id.FourthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                8)),
                                2),
                        isDisplayed()));
        textView36.check(matches(withText("7.847 M")));

        ViewInteraction textView37 = onView(
                allOf(withId(R.id.FifthRank), withText("5"),
                        childAtPosition(
                                allOf(withId(R.id.FifthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                9)),
                                0),
                        isDisplayed()));
        textView37.check(matches(withText("5")));

        ViewInteraction textView38 = onView(
                allOf(withId(R.id.FifthName), withText("Georgia"),
                        childAtPosition(
                                allOf(withId(R.id.FifthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                9)),
                                1),
                        isDisplayed()));
        textView38.check(matches(withText("Georgia")));

        ViewInteraction textView39 = onView(
                allOf(withId(R.id.FifthGold), withText("4.546 M"),
                        childAtPosition(
                                allOf(withId(R.id.FifthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                9)),
                                2),
                        isDisplayed()));
        textView39.check(matches(withText("4.546 M")));

        ViewInteraction textView40 = onView(
                allOf(withId(R.id.SixthRank), withText("6"),
                        childAtPosition(
                                allOf(withId(R.id.SixthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                10)),
                                0),
                        isDisplayed()));
        textView40.check(matches(withText("6")));

        ViewInteraction textView41 = onView(
                allOf(withId(R.id.SixthName), withText("Harriet"),
                        childAtPosition(
                                allOf(withId(R.id.SixthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                10)),
                                1),
                        isDisplayed()));
        textView41.check(matches(withText("Harriet")));

        ViewInteraction textView42 = onView(
                allOf(withId(R.id.SixthGold), withText("798.465 K"),
                        childAtPosition(
                                allOf(withId(R.id.SixthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                10)),
                                2),
                        isDisplayed()));
        textView42.check(matches(withText("798.465 K")));

        ViewInteraction textView43 = onView(
                allOf(withId(R.id.SeventhRank), withText("7"),
                        childAtPosition(
                                allOf(withId(R.id.SeventhRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                11)),
                                0),
                        isDisplayed()));
        textView43.check(matches(withText("7")));

        ViewInteraction textView44 = onView(
                allOf(withId(R.id.SeventhName), withText("Ben"),
                        childAtPosition(
                                allOf(withId(R.id.SeventhRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                11)),
                                1),
                        isDisplayed()));
        textView44.check(matches(withText("Ben")));

        ViewInteraction textView45 = onView(
                allOf(withId(R.id.SeventhGold), withText("654.646 K"),
                        childAtPosition(
                                allOf(withId(R.id.SeventhRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                11)),
                                2),
                        isDisplayed()));
        textView45.check(matches(withText("654.646 K")));

        ViewInteraction textView46 = onView(
                allOf(withId(R.id.EighthRank), withText("8"),
                        childAtPosition(
                                allOf(withId(R.id.EighthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                12)),
                                0),
                        isDisplayed()));
        textView46.check(matches(withText("8")));

        ViewInteraction textView47 = onView(
                allOf(withId(R.id.EighthName), withText("Daniel"),
                        childAtPosition(
                                allOf(withId(R.id.EighthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                12)),
                                1),
                        isDisplayed()));
        textView47.check(matches(withText("Daniel")));

        ViewInteraction textView48 = onView(
                allOf(withId(R.id.EighthGold), withText("446.448 K"),
                        childAtPosition(
                                allOf(withId(R.id.EighthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                12)),
                                2),
                        isDisplayed()));
        textView48.check(matches(withText("446.448 K")));

        ViewInteraction textView49 = onView(
                allOf(withId(R.id.NinthRank), withText("9"),
                        childAtPosition(
                                allOf(withId(R.id.NinthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                13)),
                                0),
                        isDisplayed()));
        textView49.check(matches(withText("9")));

        ViewInteraction textView50 = onView(
                allOf(withId(R.id.NinthName), withText("Ellie"),
                        childAtPosition(
                                allOf(withId(R.id.NinthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                13)),
                                1),
                        isDisplayed()));
        textView50.check(matches(withText("Ellie")));

        ViewInteraction textView51 = onView(
                allOf(withId(R.id.NinthGold), withText("7.794 K"),
                        childAtPosition(
                                allOf(withId(R.id.NinthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                13)),
                                2),
                        isDisplayed()));
        textView51.check(matches(withText("7.794 K")));

        ViewInteraction textView52 = onView(
                allOf(withId(R.id.TenthRank), withText("10"),
                        childAtPosition(
                                allOf(withId(R.id.TenthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                14)),
                                0),
                        isDisplayed()));
        textView52.check(matches(withText("10")));

        ViewInteraction textView53 = onView(
                allOf(withId(R.id.TenthName), withText("Alex"),
                        childAtPosition(
                                allOf(withId(R.id.TenthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                14)),
                                1),
                        isDisplayed()));
        textView53.check(matches(withText("Alex")));

        ViewInteraction textView54 = onView(
                allOf(withId(R.id.TenthGold), withText("10.000"),
                        childAtPosition(
                                allOf(withId(R.id.TenthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                14)),
                                2),
                        isDisplayed()));
        textView54.check(matches(withText("10.000")));

        // Add another user to push Alex (tester 1 off the leaderboard)

        DatabaseMethods.setupUser("EdrUWa2aEjNP0PAnyzi34AZRKGG3", "Kyle", new String[][]{
                new String[]{"GOLD", "98798546547"}
        });

        // Refresh leaderboard

        ViewInteraction floatingActionButton2 = onView(
                allOf(withId(R.id.refreshButton),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.flContent),
                                        0),
                                1),
                        isDisplayed()));
        floatingActionButton2.perform(click());

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Check values are correct

        ViewInteraction textView56 = onView(
                allOf(withId(R.id.YouText), withText("YOU"),
                        childAtPosition(
                                allOf(withId(R.id.YouRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                0)),
                                0),
                        isDisplayed()));
        textView56.check(matches(withText("YOU")));

        ViewInteraction textView57 = onView(
                allOf(withId(R.id.UserHeaderRank), withText("RANK"),
                        childAtPosition(
                                allOf(withId(R.id.UserHeaderRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                1)),
                                0),
                        isDisplayed()));
        textView57.check(matches(withText("RANK")));

        ViewInteraction textView58 = onView(
                allOf(withId(R.id.UserHeaderName), withText("USERNAME"),
                        childAtPosition(
                                allOf(withId(R.id.UserHeaderRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                1)),
                                1),
                        isDisplayed()));
        textView58.check(matches(withText("USERNAME")));

        ViewInteraction textView59 = onView(
                allOf(withId(R.id.UserHeaderGold), withText("GOLD"),
                        childAtPosition(
                                allOf(withId(R.id.UserHeaderRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                1)),
                                2),
                        isDisplayed()));
        textView59.check(matches(withText("GOLD")));

        ViewInteraction textView60 = onView(
                allOf(withId(R.id.UserRank), withText("11"),
                        childAtPosition(
                                allOf(withId(R.id.UserRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                2)),
                                0),
                        isDisplayed()));
        textView60.check(matches(withText("11")));

        ViewInteraction textView61 = onView(
                allOf(withId(R.id.UserName), withText("Alex"),
                        childAtPosition(
                                allOf(withId(R.id.UserRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                2)),
                                1),
                        isDisplayed()));
        textView61.check(matches(withText("Alex")));

        ViewInteraction textView62 = onView(
                allOf(withId(R.id.UserGold), withText("10.000"),
                        childAtPosition(
                                allOf(withId(R.id.UserRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                2)),
                                2),
                        isDisplayed()));
        textView62.check(matches(withText("10.000")));

        ViewInteraction textView63 = onView(
                allOf(withId(R.id.Top10Text), withText("TOP 10"),
                        childAtPosition(
                                allOf(withId(R.id.Top10Row),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                3)),
                                0),
                        isDisplayed()));
        textView63.check(matches(withText("TOP 10")));

        ViewInteraction textView64 = onView(
                allOf(withId(R.id.HeaderRank), withText("RANK"),
                        childAtPosition(
                                allOf(withId(R.id.HeaderRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                4)),
                                0),
                        isDisplayed()));
        textView64.check(matches(withText("RANK")));

        ViewInteraction textView65 = onView(
                allOf(withId(R.id.HeaderName), withText("USERNAME"),
                        childAtPosition(
                                allOf(withId(R.id.HeaderRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                4)),
                                1),
                        isDisplayed()));
        textView65.check(matches(withText("USERNAME")));

        ViewInteraction textView66 = onView(
                allOf(withId(R.id.HeaderGold), withText("GOLD"),
                        childAtPosition(
                                allOf(withId(R.id.HeaderRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                4)),
                                2),
                        isDisplayed()));
        textView66.check(matches(withText("GOLD")));

        ViewInteraction textView67 = onView(
                allOf(withId(R.id.FirstRank), withText("1"),
                        childAtPosition(
                                allOf(withId(R.id.FirstRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                5)),
                                0),
                        isDisplayed()));
        textView67.check(matches(withText("1")));

        ViewInteraction textView68 = onView(
                allOf(withId(R.id.FirstName), withText("Fiona"),
                        childAtPosition(
                                allOf(withId(R.id.FirstRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                5)),
                                1),
                        isDisplayed()));
        textView68.check(matches(withText("Fiona")));

        ViewInteraction textView69 = onView(
                allOf(withId(R.id.FirstGold), withText("465.465 B"),
                        childAtPosition(
                                allOf(withId(R.id.FirstRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                5)),
                                2),
                        isDisplayed()));
        textView69.check(matches(withText("465.465 B")));

        ViewInteraction textView70 = onView(
                allOf(withId(R.id.SecondRank), withText("2"),
                        childAtPosition(
                                allOf(withId(R.id.SecondRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                6)),
                                0),
                        isDisplayed()));
        textView70.check(matches(withText("2")));

        ViewInteraction textView71 = onView(
                allOf(withId(R.id.SecondName), withText("Kyle"),
                        childAtPosition(
                                allOf(withId(R.id.SecondRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                6)),
                                1),
                        isDisplayed()));
        textView71.check(matches(withText("Kyle")));

        ViewInteraction textView72 = onView(
                allOf(withId(R.id.SecondGold), withText("98.799 B"),
                        childAtPosition(
                                allOf(withId(R.id.SecondRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                6)),
                                2),
                        isDisplayed()));
        textView72.check(matches(withText("98.799 B")));

        ViewInteraction textView73 = onView(
                allOf(withId(R.id.ThirdRank), withText("3"),
                        childAtPosition(
                                allOf(withId(R.id.ThirdRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                7)),
                                0),
                        isDisplayed()));
        textView73.check(matches(withText("3")));

        ViewInteraction textView74 = onView(
                allOf(withId(R.id.ThirdName), withText("James"),
                        childAtPosition(
                                allOf(withId(R.id.ThirdRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                7)),
                                1),
                        isDisplayed()));
        textView74.check(matches(withText("James")));

        ViewInteraction textView75 = onView(
                allOf(withId(R.id.ThirdGold), withText("3.435 B"),
                        childAtPosition(
                                allOf(withId(R.id.ThirdRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                7)),
                                2),
                        isDisplayed()));
        textView75.check(matches(withText("3.435 B")));

        ViewInteraction textView76 = onView(
                allOf(withId(R.id.FourthRank), withText("4"),
                        childAtPosition(
                                allOf(withId(R.id.FourthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                8)),
                                0),
                        isDisplayed()));
        textView76.check(matches(withText("4")));

        ViewInteraction textView77 = onView(
                allOf(withId(R.id.FourthName), withText("Charlie"),
                        childAtPosition(
                                allOf(withId(R.id.FourthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                8)),
                                1),
                        isDisplayed()));
        textView77.check(matches(withText("Charlie")));

        ViewInteraction textView78 = onView(
                allOf(withId(R.id.FourthGold), withText("21.523 M"),
                        childAtPosition(
                                allOf(withId(R.id.FourthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                8)),
                                2),
                        isDisplayed()));
        textView78.check(matches(withText("21.523 M")));

        ViewInteraction textView79 = onView(
                allOf(withId(R.id.FifthRank), withText("5"),
                        childAtPosition(
                                allOf(withId(R.id.FifthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                9)),
                                0),
                        isDisplayed()));
        textView79.check(matches(withText("5")));

        ViewInteraction textView80 = onView(
                allOf(withId(R.id.FifthName), withText("India"),
                        childAtPosition(
                                allOf(withId(R.id.FifthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                9)),
                                1),
                        isDisplayed()));
        textView80.check(matches(withText("India")));

        ViewInteraction textView81 = onView(
                allOf(withId(R.id.FifthGold), withText("7.847 M"),
                        childAtPosition(
                                allOf(withId(R.id.FifthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                9)),
                                2),
                        isDisplayed()));
        textView81.check(matches(withText("7.847 M")));

        ViewInteraction textView82 = onView(
                allOf(withId(R.id.SixthRank), withText("6"),
                        childAtPosition(
                                allOf(withId(R.id.SixthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                10)),
                                0),
                        isDisplayed()));
        textView82.check(matches(withText("6")));

        ViewInteraction textView83 = onView(
                allOf(withId(R.id.SixthName), withText("Georgia"),
                        childAtPosition(
                                allOf(withId(R.id.SixthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                10)),
                                1),
                        isDisplayed()));
        textView83.check(matches(withText("Georgia")));

        ViewInteraction textView84 = onView(
                allOf(withId(R.id.SixthGold), withText("4.546 M"),
                        childAtPosition(
                                allOf(withId(R.id.SixthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                10)),
                                2),
                        isDisplayed()));
        textView84.check(matches(withText("4.546 M")));

        ViewInteraction textView85 = onView(
                allOf(withId(R.id.SeventhRank), withText("7"),
                        childAtPosition(
                                allOf(withId(R.id.SeventhRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                11)),
                                0),
                        isDisplayed()));
        textView85.check(matches(withText("7")));

        ViewInteraction textView86 = onView(
                allOf(withId(R.id.SeventhName), withText("Harriet"),
                        childAtPosition(
                                allOf(withId(R.id.SeventhRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                11)),
                                1),
                        isDisplayed()));
        textView86.check(matches(withText("Harriet")));

        ViewInteraction textView87 = onView(
                allOf(withId(R.id.SeventhGold), withText("798.465 K"),
                        childAtPosition(
                                allOf(withId(R.id.SeventhRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                11)),
                                2),
                        isDisplayed()));
        textView87.check(matches(withText("798.465 K")));

        ViewInteraction textView88 = onView(
                allOf(withId(R.id.EighthRank), withText("8"),
                        childAtPosition(
                                allOf(withId(R.id.EighthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                12)),
                                0),
                        isDisplayed()));
        textView88.check(matches(withText("8")));

        ViewInteraction textView89 = onView(
                allOf(withId(R.id.EighthName), withText("Ben"),
                        childAtPosition(
                                allOf(withId(R.id.EighthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                12)),
                                1),
                        isDisplayed()));
        textView89.check(matches(withText("Ben")));

        ViewInteraction textView90 = onView(
                allOf(withId(R.id.EighthGold), withText("654.646 K"),
                        childAtPosition(
                                allOf(withId(R.id.EighthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                12)),
                                2),
                        isDisplayed()));
        textView90.check(matches(withText("654.646 K")));

        ViewInteraction textView91 = onView(
                allOf(withId(R.id.NinthRank), withText("9"),
                        childAtPosition(
                                allOf(withId(R.id.NinthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                13)),
                                0),
                        isDisplayed()));
        textView91.check(matches(withText("9")));

        ViewInteraction textView92 = onView(
                allOf(withId(R.id.NinthName), withText("Daniel"),
                        childAtPosition(
                                allOf(withId(R.id.NinthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                13)),
                                1),
                        isDisplayed()));
        textView92.check(matches(withText("Daniel")));

        ViewInteraction textView93 = onView(
                allOf(withId(R.id.NinthGold), withText("446.448 K"),
                        childAtPosition(
                                allOf(withId(R.id.NinthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                13)),
                                2),
                        isDisplayed()));
        textView93.check(matches(withText("446.448 K")));

        ViewInteraction textView94 = onView(
                allOf(withId(R.id.TenthRank), withText("10"),
                        childAtPosition(
                                allOf(withId(R.id.TenthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                14)),
                                0),
                        isDisplayed()));
        textView94.check(matches(withText("10")));

        ViewInteraction textView95 = onView(
                allOf(withId(R.id.TenthName), withText("Ellie"),
                        childAtPosition(
                                allOf(withId(R.id.TenthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                14)),
                                1),
                        isDisplayed()));
        textView95.check(matches(withText("Ellie")));

        ViewInteraction textView96 = onView(
                allOf(withId(R.id.TenthGold), withText("7.794 K"),
                        childAtPosition(
                                allOf(withId(R.id.TenthRow),
                                        childAtPosition(
                                                withId(R.id.leaderboardTable),
                                                14)),
                                2),
                        isDisplayed()));
        textView96.check(matches(withText("7.794 K")));
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
