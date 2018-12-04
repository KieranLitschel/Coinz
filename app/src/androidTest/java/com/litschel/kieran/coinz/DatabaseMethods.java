package com.litschel.kieran.coinz;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

class DatabaseMethods {
    static void resetTestDB() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users-test")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            ArrayList<String> testUsers = new ArrayList<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                if (!document.getId().equals("DEFAULT") && !document.getId().equals("usernames")) {
                                    testUsers.add(document.getId());
                                }
                            }
                            db.collection("gifts-test")
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                ArrayList<String> testGifts = new ArrayList<>();
                                                for (QueryDocumentSnapshot document : task.getResult()) {
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
                                                batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Void> task) {
                                                        System.out.println("DELETED USERS FROM TEST DB");
                                                    }
                                                });
                                            } else {
                                                System.out.println("FAILED TO DELETE USERS TEST DB");
                                            }
                                        }
                                    });
                        } else {
                            System.out.println("FAILED TO DELETE USERS TEST DB");
                        }
                    }
                });
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    static void setupTester1WithCurrency(String[][] currenciesNdAmounts){
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        final String[] currencies = new String[]{"GOLD", "PENY", "DOLR", "SHIL", "QUID"};
        String uid = "ROtiCeFTuIZ3xNOhEweThG3htXj1";
        WriteBatch batch = db.batch();

        DocumentReference userDocRef = db.collection("users-test").document(uid);
        Map<String, Object> user_defaults = new HashMap<>();
        user_defaults.put("username", "");
        for (String aCurrency : currencies) {
            boolean set = false;
            for (String[] currencyNdAmount : currenciesNdAmounts){
                if (aCurrency.equals(currencyNdAmount[0])){
                    user_defaults.put(aCurrency, Double.parseDouble(currencyNdAmount[1]));
                    set = true;
                    break;
                }
            }
            if (!set){
                user_defaults.put(aCurrency, 0.0);
            }
        }
        user_defaults.put("coinsRemainingToday", 0.0);
        user_defaults.put("map", "");
        user_defaults.put("lastDownloadDate", LocalDate.MIN.toString());
        batch.set(userDocRef, user_defaults);

        // We store gifts in a separate document to make listening for changes simpler
        DocumentReference userGiftDocRef = db.collection("users_gifts-test").document(uid);
        Map<String, Object> user_gift_defaults = new HashMap<>();
        user_gift_defaults.put("gifts", new ArrayList<String>());
        batch.set(userGiftDocRef, user_gift_defaults);

        batch.commit()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        System.out.println("SUCCESSFULLY ADDED USER TO DATABASE");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        System.out.println("FAILED TO ADD USER TO DATABASE");
                    }
                });
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
