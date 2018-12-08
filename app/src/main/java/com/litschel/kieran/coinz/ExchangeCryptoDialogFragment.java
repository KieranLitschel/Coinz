package com.litschel.kieran.coinz;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Locale;

// This class creates a dialog fragment which allows the user to enter how much crypto they'd like
// to exchange with the bank and submit the exchange

public class ExchangeCryptoDialogFragment extends DialogFragment {

    private final String[] cryptoCurrencies = new String[]{"PENY", "DOLR", "SHIL", "QUID"};
    private HashMap<String, Double> exchangeRates;
    private HashMap<String, Double> currencyVals;
    private String selectedCurrency;
    private double tradeAmount;
    private double coinsRemainingToday;
    private TextView exchangeRateText;
    private TextView valueInGoldText;
    private TextView coinsRemainingTodayText;
    private EditText tradeAmountEditText;
    private Locale locale;

    // This sets up the dialog fragment for display and how it behaves

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Bundle args = getArguments();

        if (getActivity()!=null && args != null && getContext() != null){
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

            // Use locales to ensure if we expanded the game to other countries, deicmal system would be
            // correct (e.g. in Europe they use commas instead of dots to signify the start of decimal
            // places)
            locale = getResources().getConfiguration().getLocales().get(0);

            LayoutInflater inflater = getActivity().getLayoutInflater();

            // We suppress the lint warning here about a null parent as we do not have a parent layout
            @SuppressLint("InflateParams")
            View view = inflater.inflate(R.layout.dialog_exchange_crypto, null);

            exchangeRateText = view.findViewById(R.id.exchangeRateText);
            valueInGoldText = view.findViewById(R.id.offeredGoldText);
            coinsRemainingTodayText = view.findViewById(R.id.coinsRemainingToday);
            tradeAmountEditText = view.findViewById(R.id.tradeAmountEditText);

            // Get the values passed through the bundle and store them
            exchangeRates = new HashMap<>();
            currencyVals = new HashMap<>();
            for (String currency : cryptoCurrencies) {
                exchangeRates.put(currency, args.getDouble(currency + "Rate"));
                currencyVals.put(currency, args.getDouble(currency + "Val"));
            }
            coinsRemainingToday = args.getDouble("coinsRemainingToday");
            coinsRemainingTodayText.setText(String.format("Remaining coins bank will accept today:\n%s",
                    coinsRemainingToday));

            tradeAmount = 0;

            Spinner spinner = view.findViewById(R.id.cryptoSpinner);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    selectedCurrency = cryptoCurrencies[i];
                    // If the user changes the selected currency we need to show the exchange rate
                    // for the new currency
                    exchangeRateText.setText(String.format("Exchange rate:\n%s",
                            exchangeRates.get(selectedCurrency)));

                    // We may need to update the amount of coin put forward for the trade if they have less of
                    // the newly selected currency than the trade amount put forward in the edit box

                    String strTradeAmount = tradeAmountEditText.getText().toString();
                    updateTradeAmount(strTradeAmount);

                    // Finally we need to update the amount of gold the user would recieve for the trade
                    setvalueInGold();
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    // No need to change anything if nothing selected
                }
            });

            tradeAmountEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                // We use onTextChanged as this allows values to update as the user types, which
                // makes for a more user friendly experience

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    String strTradeAmount = charSequence.toString();
                    updateTradeAmount(strTradeAmount);
                    coinsRemainingTodayText.setText(String.format("Remaining crypto bank will accept today:\n%s",
                            coinsRemainingToday - tradeAmount));
                    setvalueInGold();
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });

            // Here we set the formatting for the crypto selector spinner and what items should be
            // in the spinner
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.crypto_array, R.layout.spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            builder.setView(view)
                    // Add action buttons
                    .setPositiveButton(R.string.accept_trade, (dialog, id) -> {
                        // This ensures if the player tries to trade 0 crypto with the bank we don't waste resources updating values
                        if (tradeAmount>0){
                            if (getTargetFragment()!=null){
                                if (getTargetFragment().getClass() == BalanceFragment.class){
                                    // We have checked the input is valid whenever the user edits the input, so we can just
                                    // execute the trade when they press the accept trade button
                                    ((BalanceFragment) getTargetFragment()).executeTrade(selectedCurrency, tradeAmount,
                                            exchangeRates.get(selectedCurrency));
                                } else {
                                    System.out.println("EXPECTED TARGET FRAGMENTS CLASS TO BE BALANCE FRAGMENT BUT IS NOT");
                                }
                            } else {
                                System.out.println("TARGET FRAGMENT IS NULL WHEN EXPECTED NON-NULL");
                            }
                        }
                    })
                    .setNegativeButton(R.string.cancel, (dialog, id) -> {
                        // We don't need to do anything here as the dialog will just close as desired
                    });

            return builder.create();
        } else {
            if (getActivity()!=null){
                System.out.println("ACTIVITY OF EXCHANGE DIALOG FRAGMENT IS NULL WHEN EXPECTED NON-NULL");
            }
            if (args != null){
                System.out.println("ARGS OF EXCHANGE DIALOG FRAGMENT IS NULL WHEN EXPECTED NON-NULL");
            }
            if (getContext() != null){
                System.out.println("CONTEXT OF EXCHANGE DIALOG FRAGMENT IS NULL WHEN EXPECTED NON-NULL");
            }
            return super.onCreateDialog(savedInstanceState);
        }
    }

    // Run this whenever we change selectedCurrency or tradeAmount in order to ensure the coins
    // in the edit box are a legal value
    private void updateTradeAmount(String strTradeAmount){
        if (strTradeAmount.equals("")) {
            tradeAmount = 0;
        } else {
            tradeAmount = Double.parseDouble(strTradeAmount);
            // Make sure the user hasn't asked to trade more currency than they have
            if (currencyVals.get(selectedCurrency) - tradeAmount < 0) {
                // Reduce the amount in the input box and read as input to the maximum they can trade
                tradeAmountEditText.setText(String.format(locale,"%f",currencyVals.get(selectedCurrency)));
                tradeAmount = currencyVals.get(selectedCurrency);
            }
            // Make sure the user hasn't asked to trade more currency than the bank will accept
            if (coinsRemainingToday - tradeAmount < 0) {
                tradeAmountEditText.setText(String.format(locale,"%f",coinsRemainingToday));
                tradeAmount = coinsRemainingToday;
            }
            // This ensures they can't take crypto from the bank in exchange for gold if their crypto balance becomes negative somehow
            if (tradeAmount < 0){
                tradeAmountEditText.setText("");
                tradeAmount = 0;
            }
        }
    }

    // This updates the value offered in gold by the exchange, we update it whenever the trade amount
    // changes or the spinner changes
    private void setvalueInGold() {
        double valueInGold = exchangeRates.get(selectedCurrency) * tradeAmount;
        valueInGoldText.setText(String.format("Offered gold for crypto:\n%s",
                valueInGold));
    }
}
