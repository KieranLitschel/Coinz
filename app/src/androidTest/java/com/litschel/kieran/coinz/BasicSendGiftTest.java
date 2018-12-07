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

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

// Tests sending a gift under expected use, we confirm the gift sent successfully by querying the firestore
// and using assertions to ensure the database updated correctly

@LargeTest
@RunWith(AndroidJUnit4.class)
public class BasicSendGiftTest {

    private String jimsGiftId = "";

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<MainActivity>(MainActivity.class) {
        @Override
        protected void beforeActivityLaunched() {
            Context context = InstrumentationRegistry.getTargetContext();
            TestSetupMethods.resetSettings(context);
            TestSetupMethods.setTester1LoggedIn(context);
            TestSetupMethods.resetTestDB();
            TestSetupMethods.setupUser("ROtiCeFTuIZ3xNOhEweThG3htXj1", "bob", new String[][]{
                    new String[]{"SHIL", "100"}});
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
    public void basicSendGiftTest() {

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

        // Open the gift dialog fragment

        ViewInteraction floatingActionButton = onView(
                allOf(withId(R.id.giftCryptoBtn),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.flContent),
                                        0),
                                2),
                        isDisplayed()));
        floatingActionButton.perform(click());

        // Select SHIL in spinner

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

        onView(withText("SHIL"))
                .inRoot(isPlatformPopup())
                .perform(click());

        // Input gift amount as 50

        ViewInteraction appCompatEditText12 = onView(
                allOf(withId(R.id.giftAmountEditText),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                4),
                        isDisplayed()));
        appCompatEditText12.perform(replaceText("50"));

        // Input recipient as jim

        ViewInteraction appCompatEditText21 = onView(
                allOf(withId(R.id.recipientEditText),
                        childAtPosition(
                                allOf(withId(R.id.constraintLayout),
                                        childAtPosition(
                                                withId(android.R.id.custom),
                                                0)),
                                5),
                        isDisplayed()));
        appCompatEditText21.perform(replaceText("jim"), closeSoftKeyboard());

        // Send gift

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

        // Check SHIL balance has updated correctly

        ViewInteraction textView4 = onView(
                allOf(withId(R.id.SHILText), withText("SHIL:\n50.0\n"),
                        childAtPosition(
                                allOf(withId(R.id.SHILRow),
                                        childAtPosition(
                                                withId(R.id.CoinsTable),
                                                3)),
                                0),
                        isDisplayed()));
        textView4.check(matches(withText("SHIL:\n50.0\n")));

        // Check the trade was submitted correctly to the database

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        DocumentReference jimsDocument = db.collection("users-test").document("8SpoGV9JFlXKlIiuAXkQ22PB0MF3");
        DocumentReference jimsGiftDocument = db.collection("users_gifts-test").document("8SpoGV9JFlXKlIiuAXkQ22PB0MF3");

        jimsDocument.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()){
                        DocumentSnapshot document = task.getResult();
                        Double shil = document.getDouble("SHIL");
                        if (shil != null){
                            assertEquals(shil,50.0,0.0);
                        } else {
                            fail("COULD NOT FIND SHIL IN JIMS DOCUMENT");
                        }
                    } else {
                        fail("FAILED TO GET JIMS DOCUMENT");
                    }
                });

        jimsGiftDocument.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()){
                        DocumentSnapshot document = task.getResult();
                        Object giftsObj = document.get("gifts");
                        if (giftsObj != null){
                            try {
                                // We suppress warning here as impossible to fail, but IDE gives a warning falsely about unchecked cast
                                @SuppressWarnings("unchecked")
                                ArrayList<String> gifts = (ArrayList<String>) giftsObj;
                                if (!gifts.isEmpty()){
                                    jimsGiftId = gifts.get(0);
                                } else {
                                    fail("NO GIFTS FOUND IN ARRAY LIST");
                                }
                            } catch (ClassCastException e){
                                fail("GIFTS IS NOT AN ARRAY LIST");
                            }
                        } else {
                            fail("COULD NOT GET GIFTS FROM USERS-GIFTS DOC FOR JIM");
                        }
                    } else {
                        fail("FAILED TO FIND JIMS GIFTS DOCUMENT");
                    }
                });

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        DocumentReference jimsGift = db.collection("gifts-test").document(jimsGiftId);
        jimsGift.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()){
                        DocumentSnapshot document = task.getResult();
                        Double amount = document.getDouble("amount");
                        String currency = document.getString("currency");
                        String senderUid = document.getString("senderUid");
                        if (amount != null & currency != null & senderUid != null){
                            assertEquals(amount,50.0,0.0);
                            assertEquals(currency,"SHIL");
                            assertEquals(senderUid,"ROtiCeFTuIZ3xNOhEweThG3htXj1");
                        } else {
                            fail("AT LEAST ONE OF THE GIFTS FIELDS DOES NOT EXIST");
                        }
                    } else {
                        fail("FAILED TO GET JIMS GIFT");
                    }
                });

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
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
