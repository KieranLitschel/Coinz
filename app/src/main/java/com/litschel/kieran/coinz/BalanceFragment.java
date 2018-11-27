package com.litschel.kieran.coinz;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

public class BalanceFragment extends Fragment {
    private Context activity;
    private FirebaseFirestore db;
    private SharedPreferences settings;
    private DocumentReference docRef;
    private final String[] currencies = new String[]{"GOLD", "PENY", "DOLR", "SHIL", "QUID"};
    private final String[] cryptoCurrencies = new String[]{"PENY", "DOLR", "SHIL", "QUID"};
    private HashMap<String, TextView> currencyTexts;
    private HashMap<String, Double> currencyValues;
    private HashMap<String, Double> currencyRates;
    private double goldInExchange;
    private final int DIALOG_FRAGMENT = 1;
    private BalanceFragment fragment = this;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = context;
        db = ((MainActivity) Objects.requireNonNull(getActivity())).db;
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
        currencyTexts = new HashMap<String, TextView>() {{
            put("GOLD", (view.findViewById(R.id.GOLDText)));
            put("PENY", (view.findViewById(R.id.PENYText)));
            put("DOLR", (view.findViewById(R.id.DOLRText)));
            put("SHIL", (view.findViewById(R.id.SHILText)));
            put("QUID", (view.findViewById(R.id.QUIDText)));
        }};
        currencyValues = new HashMap<>();
        for (String currency : currencies) {
            currencyValues.put(currency, Double.parseDouble(settings.getString(currency, "0")));
        }

        String mapJSONString = settings.getString("map", "");
        if (!mapJSONString.equals("")) {
            try {
                currencyRates = new HashMap<>();
                JSONObject ratesJSONObj = new JSONObject(mapJSONString).getJSONObject("rates");
                for (String currency : cryptoCurrencies) {
                    currencyRates.put(currency, ratesJSONObj.getDouble(currency));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        goldInExchange = Double.parseDouble(settings.getString("goldInExchange", "0"));

        setupValues();

        FloatingActionButton exchangeCryptoFAB = (FloatingActionButton) view.findViewById(R.id.exchangeCryptoBtn);
        exchangeCryptoFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mapJSONString.equals("")) {
                    DialogFragment newFragment = new ExchangeCryptoDialogFragment();
                    Bundle args = new Bundle();
                    for (String currency : cryptoCurrencies) {
                        args.putDouble(currency + "Rate", currencyRates.get(currency));
                        args.putDouble(currency + "Val", currencyValues.get(currency));
                    }
                    args.putDouble("goldInExchange", goldInExchange);
                    newFragment.setArguments(args);
                    newFragment.setTargetFragment(fragment,DIALOG_FRAGMENT);
                    newFragment.show(getActivity().getSupportFragmentManager(), "exchange_crypto_dialog");
                } else {
                    Toast.makeText(activity, "Exchanging coins is unavailable as today's exchange rates have not been downloaded", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setupValues() {
        for (String currency : currencies) {
            double currVal = currencyValues.get(currency);
            currencyTexts.get(currency).setText(String.format("%s:\n%s\n", currency, currVal));
        }
    }

    public void executeTrade(String currency, double tradeAmount, double exchangeRate) {
        currencyValues.put(currency, currencyValues.get(currency) - tradeAmount);
        currencyValues.put("GOLD", currencyValues.get("GOLD") + tradeAmount * exchangeRate);
        setupValues();
        goldInExchange -= tradeAmount;
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(currency, Double.toString(currencyValues.get(currency)));
        editor.putString("GOLD", Double.toString(currencyValues.get("GOLD")));
        editor.putString("goldInExchange", Double.toString(goldInExchange));
        editor.apply();
        db.runTransaction(new Transaction.Function<Void>() {
            @Override
            public Void apply(Transaction transaction) throws FirebaseFirestoreException {
                DocumentSnapshot snapshot = transaction.get(docRef);
                // Recalculate new values using value in database in case somehow the local stored
                // value gets out of sync
                Map<String, Object> updatedVals = new HashMap<>();
                updatedVals.put(currency, snapshot.getDouble(currency) - tradeAmount);
                updatedVals.put("GOLD", snapshot.getDouble("GOLD") + tradeAmount * exchangeRate);
                updatedVals.put("goldInExchange", snapshot.getDouble("goldInExchange") - tradeAmount);
                transaction.update(docRef, updatedVals);

                // Success
                return null;
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                System.out.println("Succeeded in updating database post trade");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.println("Failed to update database post trade with exception " + e);
            }
        });
    }
}
