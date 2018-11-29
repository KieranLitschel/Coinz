package com.litschel.kieran.coinz;

import android.content.Context;
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

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Objects;

public class BalanceFragment extends Fragment implements ExecuteTradeTaskCallback {
    private Context activity;
    private FirebaseFirestore db;
    private SharedPreferences settings;
    private String uid;
    private final String[] currencies = new String[]{"GOLD", "PENY", "DOLR", "SHIL", "QUID"};
    private final String[] cryptoCurrencies = new String[]{"PENY", "DOLR", "SHIL", "QUID"};
    private HashMap<String, TextView> currencyTexts;
    private HashMap<String, Double> currencyValues;
    private HashMap<String, Double> currencyRates;
    private double coinsRemainingToday;
    private final int DIALOG_FRAGMENT = 1;
    private BalanceFragment fragment = this;
    private boolean tradeExecuting = false;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        activity = context;
        db = ((MainActivity) Objects.requireNonNull(getActivity())).db;
        settings = ((MainActivity) getActivity()).settings;
        uid = ((MainActivity) getActivity()).uid;
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
            currencyValues.put(currency, Double.parseDouble(settings.getString(currency, "0"))
                    + Double.parseDouble(settings.getString(currency + "Delta", "0")));
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

        coinsRemainingToday = Double.parseDouble(settings.getString("coinsRemainingToday", "0"));

        setupValues();

        FloatingActionButton exchangeCryptoFAB = view.findViewById(R.id.exchangeCryptoBtn);
        exchangeCryptoFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mapJSONString.equals("")) {
                    if (!tradeExecuting) {
                        DialogFragment newFragment = new ExchangeCryptoDialogFragment();
                        Bundle args = new Bundle();
                        for (String currency : cryptoCurrencies) {
                            args.putDouble(currency + "Rate", currencyRates.get(currency));
                            args.putDouble(currency + "Val", currencyValues.get(currency));
                        }
                        args.putDouble("coinsRemainingToday", coinsRemainingToday);
                        newFragment.setArguments(args);
                        newFragment.setTargetFragment(fragment, DIALOG_FRAGMENT);
                        newFragment.show(getActivity().getSupportFragmentManager(), "exchange_crypto_dialog");
                    } else {
                        Toast.makeText(activity, "Previous exchange being finalized, please wait.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(activity, "Exchanging coins is unavailable as today's exchange rates have not been downloaded", Toast.LENGTH_LONG).show();
                }
            }
        });

        FloatingActionButton giftCryptoFAB = view.findViewById(R.id.giftCryptoBtn);
        giftCryptoFAB.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                if (((MainActivity) getActivity()).isNetworkAvailable()) {
                    String username = settings.getString("username", "");
                    if (username.equals("")) {
                        DialogFragment newFragment = new ChangeUsernameDialogFragment();
                        Bundle args = new Bundle();
                        args.putBoolean("isNewUser", true);
                        args.putString("username", username);
                        args.putString("uid", uid);
                        newFragment.setArguments(args);
                        newFragment.show(getActivity().getSupportFragmentManager(), "create_username_dialog");
                    }
                } else {
                    Toast.makeText(activity, "You require an internet connection to gift coin.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setupValues() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                for (String currency : currencies) {
                    double currVal = currencyValues.get(currency);
                    currencyTexts.get(currency).setText(String.format("%s:\n%s\n", currency, currVal));
                }
            }
        });
    }

    public void executeTrade(String currency, double tradeAmount, double exchangeRate) {
        tradeExecuting = true;
        new Thread(new ExecuteTradeTask(this, ((MainActivity) getActivity()), db, settings, currencyValues, coinsRemainingToday, currency, tradeAmount, exchangeRate)).start();
    }

    @Override
    public void onTradeComplete(double coinsRemainingToday) {
        this.coinsRemainingToday = coinsRemainingToday;
        tradeExecuting = false;
        setupValues();
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(activity, "Trade executed successfully, balances have been updated.", Toast.LENGTH_LONG)
                        .show();
            }
        });
    }
}
