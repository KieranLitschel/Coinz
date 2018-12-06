package com.litschel.kieran.coinz;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.Transaction;
import com.google.firebase.firestore.WriteBatch;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.fail;

// This is a set of database methods that we use to support testing the database

class DatabaseMethods {

    // Resets the test database to the default state

    static void resetTestDB() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users-test")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ArrayList<String> testUsers = new ArrayList<>();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            if (!document.getId().equals("DEFAULT") && !document.getId().equals("usernames")) {
                                testUsers.add(document.getId());
                            }
                        }
                        db.collection("gifts-test")
                                .get()
                                .addOnCompleteListener(task1 -> {
                                    if (task1.isSuccessful()) {
                                        ArrayList<String> testGifts = new ArrayList<>();
                                        for (QueryDocumentSnapshot document : task1.getResult()) {
                                            if (!document.getId().equals("DEFAULT")) {
                                                testGifts.add(document.getId());
                                            }
                                        }

                                        WriteBatch batch = db.batch();
                                        for (String testUser : testUsers) {
                                            DocumentReference usersRef = db.collection("users-test").document(testUser);
                                            batch.delete(usersRef);
                                            DocumentReference usersGiftsRef = db.collection("users_gifts-test").document(testUser);
                                            batch.delete(usersGiftsRef);
                                        }
                                        Map<String, Object> usernamesData = new HashMap<>();
                                        usernamesData.put("DEFAULT", "");
                                        batch.set(db.collection("users-test").document("usernames"), usernamesData);
                                        for (String gift : testGifts) {
                                            DocumentReference giftRef = db.collection("gifts-test").document(gift);
                                            batch.delete(giftRef);
                                        }
                                        batch.commit().addOnCompleteListener(task11 -> System.out.println("DELETED USERS FROM TEST DB"));
                                    } else {
                                        fail("FAILED TO DELETE USERS TEST DB");
                                    }
                                });
                    } else {
                        fail("FAILED TO DELETE USERS TEST DB");
                    }
                });
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Sets up a user in the database with a specifiable username and amount of each currency, is
    // a modified version of setFirstTimeUser from MainActivity

    static void setupUser(String uid, String username, String[][] currenciesNdAmounts) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final String[] currencies = new String[]{"GOLD", "PENY", "DOLR", "SHIL", "QUID"};
        WriteBatch batch = db.batch();

        DocumentReference userDocRef = db.collection("users-test").document(uid);
        Map<String, Object> user_defaults = new HashMap<>();
        user_defaults.put("username", username);
        for (String aCurrency : currencies) {
            boolean set = false;
            for (String[] currencyNdAmount : currenciesNdAmounts) {
                if (aCurrency.equals(currencyNdAmount[0])) {
                    user_defaults.put(aCurrency, Double.parseDouble(currencyNdAmount[1]));
                    set = true;
                    break;
                }
            }
            if (!set) {
                user_defaults.put(aCurrency, 0.0);
            }
        }
        user_defaults.put("coinsRemainingToday", 0.0);
        user_defaults.put("map", "");
        user_defaults.put("lastDownloadDate", LocalDate.MIN.toString());
        batch.set(userDocRef, user_defaults);

        if (!username.equals("")) {
            DocumentReference usernamesDocRef = db.collection("users-test").document("usernames");
            batch.update(usernamesDocRef, uid, username);
        }

        DocumentReference userGiftDocRef = db.collection("users_gifts-test").document(uid);
        Map<String, Object> user_gift_defaults = new HashMap<>();
        user_gift_defaults.put("gifts", new ArrayList<String>());
        batch.set(userGiftDocRef, user_gift_defaults);

        batch.commit()
                .addOnSuccessListener(aVoid -> System.out.println("SUCCESSFULLY ADDED USER TO DATABASE"))
                .addOnFailureListener(e -> fail("FAILED TO ADD USER TO DATABASE"));
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Modified version of send gift to recipient from GiftCryptoDialogFragment

    // We suppress the same parameter warning as it makes sense to keep them flexible as future tests
    // may want to use different users and currencies
    @SuppressWarnings("SameParameterValue")
    static void sendGiftToRecipient(String senderUid, String recipientUid, String selectedCurrency, double giftAmount) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final DocumentReference senderRef = db.collection("users-test").document(senderUid);
        final DocumentReference recipientRef = db.collection("users-test").document(recipientUid);
        final DocumentReference recipientGiftRef = db.collection("users_gifts-test").document(recipientUid);
        final DocumentReference giftRef = db.collection("gifts-test").document();
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot recipientGiftSnapshot = transaction.get(recipientGiftRef);

            // If someone has sent themself a gift we don't need to update the balances. We allow
            // people to send themselves gifts as it allows testing the gift listener using espresso tests
            if (!recipientUid.equals(senderUid)) {
                DocumentSnapshot recipientSnapshot = transaction.get(recipientRef);
                DocumentSnapshot senderSnapshot = transaction.get(senderRef);
                Double sendersAmntSelectedCurrency = senderSnapshot.getDouble(selectedCurrency);
                Double recipientsAmntSelectedCurrency = recipientSnapshot.getDouble(selectedCurrency);
                if (sendersAmntSelectedCurrency != null & recipientsAmntSelectedCurrency != null) {
                    transaction.update(senderRef, selectedCurrency, sendersAmntSelectedCurrency - giftAmount);
                    transaction.update(recipientRef, selectedCurrency, recipientsAmntSelectedCurrency + giftAmount);
                } else {
                    fail(String.format("RECIPIENTS AMOUNT OF %1$s, OR SENDERS AMOUNT OF %1$s IS NOT DECLARED IN FIRESTORE", selectedCurrency));
                }
            }

            Map<String, Object> giftInfo = new HashMap<>();
            // We store the UID rather than the username in case the sender changes their username
            // before the recipient is notified of the gift
            giftInfo.put("senderUid", senderUid);
            giftInfo.put("currency", selectedCurrency);
            giftInfo.put("amount", giftAmount);

            transaction.set(giftRef, giftInfo);

            Object giftsObj = recipientGiftSnapshot.get("gifts");
            if (giftsObj != null){
                try {
                    // We suppress warning here as impossible to fail, but IDE gives a warning falsely about unchecked cast
                    @SuppressWarnings("unchecked")
                    List<String> giftsList = (List<String>) giftsObj;
                    giftsList.add(giftRef.getId());
                    transaction.update(recipientGiftRef, "gifts", giftsList);
                } catch (ClassCastException e){
                    fail("GIFTS IS NOT AN ARRAY LIST");
                }
            } else {
                fail("COULD NOT GET GIFTS FROM USERS-GIFTS DOC FOR JIM");
            }

            return null;
        }).addOnSuccessListener(aVoid -> System.out.println("GIFT SENT SUCCESSFULLY")).addOnFailureListener(e -> fail("FAILED SENDING GIFT TO RECIPIENT"));

        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
