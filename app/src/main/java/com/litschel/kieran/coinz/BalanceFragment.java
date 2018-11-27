package com.litschel.kieran.coinz;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Objects;

public class BalanceFragment extends Fragment {
    private FirebaseFirestore db;
    private SharedPreferences settings;
    private DocumentReference docRef;
    private String[] currencies;
    private HashMap<String,TextView> currencyTexts;
    private HashMap<String,Double> currencyValues;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        db = ((MainActivity) getActivity()).db;
        settings = ((MainActivity) getActivity()).settings;
        docRef = db.collection("users").document(((MainActivity) getActivity()).uid);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_balance, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // Listen for changes in document as opposed to just getting values on creation, as gift may change balance while its being viewed
        currencies = new String[]{"GOLD", "PENY", "DOLR", "SHIL", "QUID"};
        currencyTexts = new HashMap<String,TextView>()
        {{
            put("GOLD",(view.findViewById(R.id.GOLDText)));
            put("PENY",(view.findViewById(R.id.PENYText)));
            put("DOLR",(view.findViewById(R.id.DOLRText)));
            put("SHIL",(view.findViewById(R.id.SHILText)));
            put("QUID",(view.findViewById(R.id.QUIDText)));
        }};
        currencyValues = new HashMap<>();
        for (String currency : currencies){
            currencyValues.put(currency,Double.parseDouble(settings.getString(currency+"Value","0")));
        }
        setupValues();
    }

    private void setupValues(){
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        SharedPreferences.Editor editor = settings.edit();
                        for (String currency : currencies){
                            String currValStr = String.valueOf(currencyValues.get(currency));
                            currencyTexts.get(currency).setText(String.format("%s:\n%s\n",currency,currValStr));
                            editor.putString(currency+"Value",currValStr);
                        }
                        editor.apply();
                        setupDocListener();
                    } else {
                        System.out.println("GETTING INITIAL VALUES COULD NOT FIND USER IN DATABASE");
                    }
                } else {
                    System.out.printf("GETTING INITIAL VALUES FAILED WITH EXCEPTION: %s,\n", task.getException());
                }
            }
        });
    }

    private void setupDocListener(){
        docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot document,
                                @Nullable FirebaseFirestoreException e) {
                if (e != null) {
                    System.out.printf("LISTEN FOR BALANCE CHANGE FAILED WITH ERROR MESSAGE: %s\n",e);
                }

                if (document != null && document.exists()) {
                    ArrayList<String> changedCurrencies = new ArrayList<>();
                    for (String currency : currencies){
                        if (!Objects.equals(document.getDouble(currency), currencyValues.get(currency))){
                            currencyValues.put(currency,document.getDouble(currency));
                            changedCurrencies.add(currency);
                        }
                    }
                    SharedPreferences.Editor editor = settings.edit();
                    for (String currency : changedCurrencies){
                        String currValStr = String.valueOf(currencyValues.get(currency));
                        currencyTexts.get(currency).setText(String.format("%s\n: %s\n",currency,currValStr));
                        editor.putString(currency+"Value",currValStr);
                    }
                    editor.apply();
                } else {
                    System.out.printf("LISTEN FOR BALANCE CHANGE FOUND NULL DOCUMENT");
                }
            }
        });
    }
}
