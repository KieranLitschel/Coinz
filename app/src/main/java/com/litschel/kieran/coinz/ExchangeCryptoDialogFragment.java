package com.litschel.kieran.coinz;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
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

public class ExchangeCryptoDialogFragment extends DialogFragment {

    private final String[] cryptoCurrencies = new String[]{"PENY", "DOLR", "SHIL", "QUID"};
    private HashMap<String, Double> exchangeRates;
    private HashMap<String, Double> currencyVals;
    private String selectedCurrency;
    private double tradeAmount;
    private double valueInGold;
    private double coinsRemainingToday;
    private TextView exchangeRateText;
    private TextView valueInGoldText;
    private TextView coinsRemainingTodayText;
    private EditText tradeAmountEditText;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_exchange_crypto, null);

        exchangeRateText = view.findViewById(R.id.exchangeRateText);
        valueInGoldText = view.findViewById(R.id.offeredGoldText);
        coinsRemainingTodayText = view.findViewById(R.id.coinsRemainingToday);
        tradeAmountEditText = view.findViewById(R.id.tradeAmountEditText);

        Bundle args = getArguments();

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
                exchangeRateText.setText(String.format("Exchange rate:\n%s",
                        exchangeRates.get(selectedCurrency)));
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

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String strTradeAmount = charSequence.toString();
                if (strTradeAmount.equals("")) {
                    tradeAmount = 0;
                } else {
                    tradeAmount = Double.parseDouble(strTradeAmount);
                    // Make sure the user hasn't asked to trade more currency than they have
                    if (currencyVals.get(selectedCurrency) - tradeAmount < 0) {
                        // Reduce the amount in the input box and read as input to the maximum they can trade
                        tradeAmountEditText.setText(Double.toString(currencyVals.get(selectedCurrency)));
                        tradeAmount = currencyVals.get(selectedCurrency);
                    }
                    // Make sure the user hasn't asked to trade more currency than the bank will accept
                    if (coinsRemainingToday - tradeAmount < 0) {
                        tradeAmountEditText.setText(Double.toString(coinsRemainingToday));
                        tradeAmount = coinsRemainingToday;
                    }
                }
                coinsRemainingTodayText.setText(String.format("Remaining crypto bank will accept today:\n%s",
                        coinsRemainingToday - tradeAmount));
                setvalueInGold();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        // Create an ArrayAdapter using the string array and a custom spinner layout to match other font size
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.crypto_array, R.layout.spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        builder.setView(view)
                // Add action buttons
                .setPositiveButton(R.string.accept_trade, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        ((BalanceFragment) getTargetFragment()).executeTrade(selectedCurrency, tradeAmount,
                                exchangeRates.get(selectedCurrency));
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        return builder.create();
    }

    private void setvalueInGold() {
        valueInGold = exchangeRates.get(selectedCurrency) * tradeAmount;
        valueInGoldText.setText(String.format("Offered gold for crypto:\n%s",
                valueInGold));
    }
}
