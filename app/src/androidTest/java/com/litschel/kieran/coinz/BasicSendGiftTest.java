package com.litschel.kieran.coinz;


import android.support.annotation.NonNull;
import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
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
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION");

    @Before
    public void beforeTest() {
        DatabaseMethods.resetTestDB();
        DatabaseMethods.setupUser("ROtiCeFTuIZ3xNOhEweThG3htXj1", "bob", new String[][]{
                new String[]{"SHIL", "100"}});
        DatabaseMethods.setupUser("8SpoGV9JFlXKlIiuAXkQ22PB0MF3", "jim", new String[][]{});
    }

    @Test
    public void basicSendGiftTest() {
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

        ViewInteraction appCompatCheckedTextView3 = onView(withText("SHIL"))
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
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
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
                    }
                });

        jimsGiftDocument.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
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
                    }
                });

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        DocumentReference jimsGift = db.collection("gifts-test").document(jimsGiftId);
        jimsGift.get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
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
            }
        });

        try {
            Thread.sleep(5000);
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
