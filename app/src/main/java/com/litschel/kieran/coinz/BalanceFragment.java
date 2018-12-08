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

import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Objects;

// This fragment handles showing the user the balances and provides FABs to send gifts and exchange
// coins with the bank
public class BalanceFragment extends Fragment implements ExecuteTradeTaskCallback, CoinsUpdateTaskCallback {
    private Context activity;
    private FirebaseFirestore db;
    private SharedPreferences settings;
    private String uid;
    private final String[] currencies = new String[]{"GOLD", "PENY", "DOLR", "SHIL", "QUID"};
    private final String[] cryptoCurrencies = new String[]{"PENY", "DOLR", "SHIL", "QUID"};
    // We suppress this warning as it is being shown as the IDE does not pick up on entries being
    // added to currentTexts on its declaration
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
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
        // We keep a record of the GUI elements so that we can change them easier later
        currencyTexts = new HashMap<String, TextView>() {{
            put("GOLD", (view.findViewById(R.id.GOLDText)));
            put("PENY", (view.findViewById(R.id.PENYText)));
            put("DOLR", (view.findViewById(R.id.DOLRText)));
            put("SHIL", (view.findViewById(R.id.SHILText)));
            put("QUID", (view.findViewById(R.id.QUIDText)));
        }};
        currencyValues = new HashMap<>();
        updateCurrencyValues();

        // If the map is stored locally, we extract the currency rates to provide to the fragment
        // that handles exchanging coins with the bank
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

        // When this button is pressed it will bring up a dialog for the user to exchange crypto
        // with the bank
        FloatingActionButton exchangeCryptoFAB = view.findViewById(R.id.exchangeCryptoBtn);
        exchangeCryptoFAB.setOnClickListener(view1 -> {
            // If the map is empty then we don't have exchange rates for today
            if (!mapJSONString.equals("")) {
                // We don't want to create a new trade while another is running, as this could allow the user to exchange coins they don't have
                if (!tradeExecuting) {
                    if (getActivity() != null) {
                        // We setup and display the dialog for exchanging currency with the bank
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
                        System.out.println("ACTIVITY WAS NULL WHEN EXPECTED NON-NULL");
                    }
                } else {
                    Toast.makeText(activity, "Previous exchange being finalized, please wait.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(activity, "Exchanging coins is unavailable as today's exchange rates have not been downloaded", Toast.LENGTH_LONG).show();
            }
        });

        // When this button is pressed it'll bring up a dialog for the user to send crypto to other
        // players
        FloatingActionButton giftCryptoFAB = view.findViewById(R.id.giftCryptoBtn);
        giftCryptoFAB.setOnClickListener(view12 -> {
            // Can only send gifts if there is a network connection, so first we check that
            if (getActivity() != null) {
                if (getActivity().getClass() == MainActivity.class) {
                    if (((MainActivity) getActivity()).isNetworkAvailable()) {
                        String username = settings.getString("username", "");
                        // Next we check if the user has set their username, as this is displayed on
                        // the gift dialog, if they haven't we ask them too create one
                        if (username.equals("")) {
                            createUsernameFragment(username);
                        } else {
                            // We setup and display the gift dialog
                            DialogFragment newFragment = new GiftCryptoDialogFragment();
                            Bundle args = new Bundle();
                            args.putString("uid", uid);
                            args.putString("username", username);
                            for (String currency : cryptoCurrencies) {
                                args.putDouble(currency + "Val", currencyValues.get(currency));
                            }
                            newFragment.setArguments(args);
                            newFragment.setTargetFragment(fragment, DIALOG_FRAGMENT);
                            newFragment.show(getActivity().getSupportFragmentManager(), "gift_crypto_dialog");
                        }
                    } else {
                        Toast.makeText(activity, "You require an internet connection to gift coin.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    System.out.println("ACTIVITY CLASS WAS EXPECTED TO BE MAIN ACTIVITY BUT ISN'T");
                }
            } else {
                System.out.println("ACTIVITY WAS NULL WHEN EXPECTED NON-NULL");
            }
        });
    }

    // This reads the currency values from settings and updates the local hashmap
    private void updateCurrencyValues() {
        for (String currency : currencies) {
            currencyValues.put(currency, Double.parseDouble(settings.getString(currency, "0"))
                    + Double.parseDouble(settings.getString(currency + "Delta", "0")));
        }
    }

    // This updates the values currency values on the UI with the ones stored in the hashmap, we run it on the UI
    // thread as it can be called be threads other than the UI one, and whenever we are modifying the
    // UI we must call these methods on the UI thread
    private void setupValues() {
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> {
                for (String currency : currencies) {
                    double currVal = currencyValues.get(currency);
                    currencyTexts.get(currency).setText(String.format("%s:\n%s\n", currency, currVal));
                }
            });
        } else {
            System.out.println("EXPECTED ACTIVITY WAS NON-NULL BUT FOUND NULL");
        }
    }

    // This is called when a trade is submitted through the trade dialog, and it safely updates the
    // values in the database and settings to execute the trade
    public void executeTrade(String currency, double tradeAmount, double exchangeRate) {
        if (getActivity() != null) {
            if (getActivity().getClass() == MainActivity.class) {
                tradeExecuting = true;
                new Thread(new ExecuteTradeTask(((MainActivity) getActivity()).users, this,
                        ((MainActivity) getActivity()), db, settings, currencyValues,
                        coinsRemainingToday, currency, tradeAmount, exchangeRate))
                        .start();
            } else {
                System.out.println("ACTIVITY CLASS WAS EXPECTED TO BE MAIN ACTIVITY BUT ISN'T");
            }
        } else {
            System.out.println("ACTIVITY WAS NULL WHEN EXPECTED NON-NULL");
        }
    }

    // This is called once the trade has completed and updates all the values so those displayed
    // to the user are up-to date
    @Override
    public void onTradeComplete(double coinsRemainingToday) {
        this.coinsRemainingToday = coinsRemainingToday;
        tradeExecuting = false;
        setupValues();
        if (getActivity() != null) {
            getActivity().runOnUiThread(() -> Toast.makeText(activity, "Trade executed successfully, balances have been updated.", Toast.LENGTH_LONG)
                    .show());
        } else {
            System.out.println("EXPECTED ACTIVITY WAS NON-NULL BUT FOUND NULL");
        }
    }

    // This creates the dialog for the user to create a new username or update their old one

    public void createUsernameFragment(String username) {
        // Recheck internet connection here even though checked in gift FAB as this can be called from
        // GiftCryptoDialogFragment too
        if (getActivity() != null) {
            if (getActivity().getClass() == MainActivity.class) {
                if (((MainActivity) getActivity()).isNetworkAvailable()) {
                    DialogFragment newFragment = new ChangeUsernameDialogFragment();
                    Bundle args = new Bundle();
                    args.putBoolean("isNewUser", true);
                    args.putString("username", username);
                    args.putString("uid", uid);
                    newFragment.setArguments(args);
                    newFragment.show(getActivity().getSupportFragmentManager(), "create_username_dialog");
                } else {
                    getActivity().runOnUiThread(() -> Toast.makeText(activity, "You require an internet connection to gift coin.", Toast.LENGTH_LONG).show());
                }
            } else {
                System.out.println("ACTIVITY CLASS WAS EXPECTED TO BE MAIN ACTIVITY BUT ISN'T");
            }
        } else {
            System.out.println("ACTIVITY WAS NULL WHEN EXPECTED NON-NULL");
        }
    }

    // This is called whenever we complete a coins update task, to ensure the displayed values are
    // up-to date
    @Override
    public void coinsUpdateTaskComplete() {
        updateCurrencyValues();
        setupValues();
    }
}
