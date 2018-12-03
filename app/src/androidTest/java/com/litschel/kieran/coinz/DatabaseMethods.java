package com.litschel.kieran.coinz;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

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
}
