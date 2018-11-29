package com.litschel.kieran.coinz;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GiftCryptoDialogFragment extends DialogFragment {

    private final String[] cryptoCurrencies = new String[]{"PENY", "DOLR", "SHIL", "QUID"};
    private FirebaseFirestore db;
    private String uid;
    private HashMap<String, Double> currencyVals;
    private EditText giftAmountEditText;
    private EditText recipientEditText;
    private TextView errorText;
    private double giftAmount;
    private String selectedCurrency;
    private AlertDialog dialog;
    private Button positiveBtn;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        db = FirebaseFirestore.getInstance();
        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_gift_crypto, null);

        giftAmountEditText = view.findViewById(R.id.giftAmountEditText);
        recipientEditText = view.findViewById(R.id.recipientEditText);
        errorText = view.findViewById(R.id.usernameValidText);

        Bundle args = getArguments();

        String username = args.getString("username");
        ((TextView) view.findViewById(R.id.usernameText)).setText(
                String.format("Your username is:\n%s", username));

        uid = args.getString("uid");

        currencyVals = new HashMap<>();
        for (String currency : cryptoCurrencies) {
            currencyVals.put(currency, args.getDouble(currency + "Val"));
        }

        giftAmount = 0;

        Spinner spinner = view.findViewById(R.id.cryptoSpinner);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                selectedCurrency = cryptoCurrencies[i];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                // No need to change anything if nothing selected
            }
        });

        giftAmountEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                String strgiftAmount = charSequence.toString();
                if (strgiftAmount.equals("")) {
                    giftAmount = 0;
                } else {
                    giftAmount = Double.parseDouble(strgiftAmount);
                    // Make sure the user hasn't asked to trade more currency than they have
                    if (currencyVals.get(selectedCurrency) - giftAmount < 0) {
                        // Reduce the amount in the input box and read as input to the maximum they can trade
                        giftAmountEditText.setText(Double.toString(currencyVals.get(selectedCurrency)));
                        giftAmount = currencyVals.get(selectedCurrency);
                    }
                }
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

        builder.setView(view)
                // Set no listener for positive button as we will write on click action seperately
                // so we can control the dialog being dismissed
                .setPositiveButton(R.string.accept_trade, null)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });

        dialog = builder.create();

        view.findViewById(R.id.changeUsernameBtn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
                ((BalanceFragment) getTargetFragment()).updateUsernameFragment(username, false);
            }
        });

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        System.out.println("CHANGE USERNAME BUTTON CLICKED");
                        String recipent = recipientEditText.getText().toString();
                        if (!recipent.equals("")) {
                            positiveBtn.setEnabled(false);
                            errorText.setVisibility(View.GONE);
                            findRecipientByUid(recipent, selectedCurrency, giftAmount);
                        }
                    }
                });
            }
        });

        return dialog;
    }

    private void findRecipientByUid(String recipent, String selectedCurrency, double giftAmount) {
        db.collection("users")
                .whereEqualTo("username", recipent)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.getResult().isEmpty()) {
                            System.out.println("RECIPIENT NOT FOUND IN DATABASE");
                            errorText.setVisibility(View.VISIBLE);
                            positiveBtn.setEnabled(true);
                        } else {
                            DocumentSnapshot snapshot = task.getResult().getDocuments().get(0);
                            String recipientUid = snapshot.getString("uid");
                            sendGiftToRecipient(recipientUid, selectedCurrency, giftAmount);
                        }
                    }
                });
    }

    private void sendGiftToRecipient(String recipientUid, String selectedCurrency, double giftAmount) {
        final DocumentReference senderRef = db.collection("users").document(uid);
        final DocumentReference recipientRef = db.collection("users").document(recipientUid);
        final DocumentReference giftRef = db.collection("gifts").document();
        db.runTransaction(new Transaction.Function<Void>() {
            @Nullable
            @Override
            public Void apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
                DocumentSnapshot senderSnapshot = transaction.get(senderRef);
                DocumentSnapshot recipientSnapshot = transaction.get(recipientRef);

                transaction.update(senderRef, selectedCurrency, senderSnapshot.getDouble(selectedCurrency) - giftAmount);

                Map<String, Object> giftInfo = new HashMap<>();
                giftInfo.put("senderUid", uid);
                giftInfo.put("recipientUid", recipientUid);
                giftInfo.put("currency", selectedCurrency);
                giftInfo.put("amount", giftAmount);

                transaction.set(giftRef, giftInfo);

                Map<String, Object> recipientInfo = new HashMap<>();
                recipientInfo.put("gifts", ((List<String>) recipientSnapshot.get("gifts")).add(giftRef.getId()));
                recipientInfo.put(selectedCurrency, recipientSnapshot.getDouble(selectedCurrency) + giftAmount);

                transaction.update(recipientRef, recipientInfo);

                return null;
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.printf("FAILED TO SEND GIFT WITH EXCEPTION:\n%s\n", e);
                dialog.dismiss();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), "Something went wrong when sending the gift, please try again.", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
