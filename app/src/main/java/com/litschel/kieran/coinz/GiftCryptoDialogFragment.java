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
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// This dialog fragment acts as an interface to send gifts

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
    private String users;
    private String gifts;
    private String users_gifts;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Bundle args = getArguments();

        if (getActivity() != null && args != null && getContext() != null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            db = FirebaseFirestore.getInstance();
            LayoutInflater inflater = getActivity().getLayoutInflater();

            // We suppress the lint warning here about a null parent as we do not have a parent layout
            @SuppressLint("InflateParams")
            View view = inflater.inflate(R.layout.dialog_gift_crypto, null);

            users = ((MainActivity) getActivity()).users;
            gifts = ((MainActivity) getActivity()).gifts;
            users_gifts = ((MainActivity) getActivity()).users_gifts;

            giftAmountEditText = view.findViewById(R.id.giftAmountEditText);
            recipientEditText = view.findViewById(R.id.recipientEditText);
            errorText = view.findViewById(R.id.usernameValidText);

            String username = args.getString("username");
            ((TextView) view.findViewById(R.id.usernameText)).setText(
                    String.format("Your username is:\n%s", username));

            uid = args.getString("uid");

            currencyVals = new HashMap<>();
            for (String currency : cryptoCurrencies) {
                currencyVals.put(currency, args.getDouble(currency + "Val"));
            }

            giftAmount = 0;

            // We make the spinner behave as it did in the exchange dialog fragment

            Spinner spinner = view.findViewById(R.id.cryptoSpinner);
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                    selectedCurrency = cryptoCurrencies[i];
                    String strgiftAmount = giftAmountEditText.getText().toString();
                    updateGiftAmount(strgiftAmount);
                }

                @Override
                public void onNothingSelected(AdapterView<?> adapterView) {
                    // No need to change anything if nothing selected
                }
            });

            // We make the edit text behave as it did in the exchange dialog fragment

            giftAmountEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    String strgiftAmount = charSequence.toString();
                    updateGiftAmount(strgiftAmount);
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });

            // We set up the spinner as we did in the exchange crypto dialog
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                    R.array.crypto_array, R.layout.spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinner.setAdapter(adapter);

            builder.setView(view)
                    // Set no listener for positive button as we will write on click action seperately
                    // so we can control the dialog being dismissed
                    .setPositiveButton(R.string.send_gift, null)
                    .setNegativeButton(R.string.cancel, (dialog, id) -> {
                    });

            dialog = builder.create();

            // If the user clicks the change username button we dismiss the current fragment and
            // show them the change username dialog instead
            view.findViewById(R.id.changeUsernameBtn).setOnClickListener(view12 -> {
                dialog.dismiss();
                if (getTargetFragment() != null) {
                    if (getTargetFragment().getClass() == BalanceFragment.class) {
                        ((BalanceFragment) getTargetFragment()).updateUsernameFragment(username, false);
                    } else {
                        System.out.println("EXPECTED TARGET FRAGMENTS CLASS TO BE BALANCE FRAGMENT BUT IS NOT");
                    }
                } else {
                    System.out.println("TARGET FRAGMENT IS NULL WHEN EXPECTED NON-NULL");
                }
            });

            dialog.setOnShowListener(dialogInterface -> {
                positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveBtn.setOnClickListener(view1 -> {
                    System.out.println("CHANGE USERNAME BUTTON CLICKED");
                    String recipent = recipientEditText.getText().toString();
                    // If the recipient box is empty we do nothing
                    if (!recipent.equals("") && giftAmount > 0) {
                        // We disable them clicking the send button again until we have finished handling
                        // the current send gift request
                        positiveBtn.setEnabled(false);
                        errorText.setVisibility(View.GONE);
                        // Start chain of database queries to send gift
                        findRecipientByUid(recipent, selectedCurrency, giftAmount);
                    }
                });
            });

            return dialog;
        } else {
            if (getActivity() != null) {
                System.out.println("ACTIVITY OF EXCHANGE DIALOG FRAGMENT IS NULL WHEN EXPECTED NON-NULL");
            }
            if (args != null) {
                System.out.println("ARGS OF EXCHANGE DIALOG FRAGMENT IS NULL WHEN EXPECTED NON-NULL");
            }
            if (getContext() != null) {
                System.out.println("CONTEXT OF EXCHANGE DIALOG FRAGMENT IS NULL WHEN EXPECTED NON-NULL");
            }
            return super.onCreateDialog(savedInstanceState);
        }
    }

    // We update the edit text as we did in the exchange dialog fragment
    // This suppresses the warning caused to Double.toString which we use for the same reason
    // as discussed in the exchange dialog fragment
    @SuppressLint("SetTextI18n")
    private void updateGiftAmount(String strgiftAmount) {
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
            // This ensures players can't steal coins from others if there balance becomes negative somehow
            if (giftAmount < 0) {
                giftAmountEditText.setText("");
                giftAmount = 0;
            }
        }
    }

    // First we find the uid of the recipient using the specified username
    private void findRecipientByUid(String recipent, String selectedCurrency, double giftAmount) {
        db.collection(users)
                .whereEqualTo("username", recipent)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.getResult().isEmpty()) {
                        // If we can't find them we tell the user they do not exist
                        System.out.println("RECIPIENT NOT FOUND IN DATABASE");
                        errorText.setVisibility(View.VISIBLE);
                        positiveBtn.setEnabled(true);
                    } else {
                        // If they do exist then only one document will be returned (as usernames are
                        // unique), so we take the first document from the results and id of this
                        // is the recipients uid
                        DocumentSnapshot snapshot = task.getResult().getDocuments().get(0);
                        String recipientUid = snapshot.getId();
                        sendGiftToRecipient(recipientUid, recipent, selectedCurrency, giftAmount);
                    }
                });
    }

    // Next we try to send the recipient the gift
    private void sendGiftToRecipient(String recipientUid, String recipient, String selectedCurrency, double giftAmount) {
        final DocumentReference senderRef = db.collection(users).document(uid);
        final DocumentReference recipientRef = db.collection(users).document(recipientUid);
        final DocumentReference recipientGiftRef = db.collection(users_gifts).document(recipientUid);
        final DocumentReference giftRef = db.collection(gifts).document();
        db.runTransaction((Transaction.Function<Void>) transaction -> {
            DocumentSnapshot recipientGiftSnapshot = transaction.get(recipientGiftRef);

            // First we update the sender and recipients balances

            // If someone has sent themself a gift we don't need to update the balances. We allow
            // people to send themselves gifts as it allows testing the gift listener using espresso tests
            if (!recipientUid.equals(uid)) {
                DocumentSnapshot recipientSnapshot = transaction.get(recipientRef);
                DocumentSnapshot senderSnapshot = transaction.get(senderRef);
                Double sendersAmntSelectedCurrency = senderSnapshot.getDouble(selectedCurrency);
                Double recipientsAmntSelectedCurrency = recipientSnapshot.getDouble(selectedCurrency);
                if (sendersAmntSelectedCurrency != null & recipientsAmntSelectedCurrency != null) {
                    transaction.update(senderRef, selectedCurrency, sendersAmntSelectedCurrency - giftAmount);
                    transaction.update(recipientRef, selectedCurrency, recipientsAmntSelectedCurrency + giftAmount);
                } else {
                    System.out.println(String.format("RECIPIENTS AMOUNT OF %1$s, OR SENDERS AMOUNT OF %1$s IS NOT DECLARED IN FIRESTORE", selectedCurrency));
                }
            }

            // Next we create the gift document used to display to the user who they recieved a gift
            // from and how much

            Map<String, Object> giftInfo = new HashMap<>();
            // We store the UID rather than the username in case the sender changes their username
            // before the recipient is notified of the gift
            giftInfo.put("senderUid", uid);
            giftInfo.put("currency", selectedCurrency);
            giftInfo.put("amount", giftAmount);

            transaction.set(giftRef, giftInfo);

            // Finally we update the users array of gifts that they haven't seen yet to include the
            // new gift

            Object giftsObj = recipientGiftSnapshot.get("gifts");
            if (giftsObj != null) {
                try {
                    // We suppress warning here as impossible to fail, but IDE gives a warning falsely about unchecked cast
                    @SuppressWarnings("unchecked")
                    List<String> giftsList = (List<String>) giftsObj;
                    giftsList.add(giftRef.getId());
                    transaction.update(recipientGiftRef, "gifts", giftsList);
                } catch (ClassCastException e) {
                    System.out.println("GIFTS IS NOT AN ARRAY LIST");
                }
            } else {
                System.out.println("COULD NOT GET GIFTS FROM USERS-GIFTS DOC FOR JIM");
            }


            return null;
        }).addOnSuccessListener(aVoid -> {
            System.out.println("UPDATED COINS IN DATABASE");
            dialog.dismiss();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), String.format("Gifted %s %s to %s", giftAmount, selectedCurrency, recipient), Toast.LENGTH_LONG).show());
                // We assert getActivity is not null here as line above will not make getActivity null
                if (Objects.requireNonNull(getActivity()).getClass() == MainActivity.class){
                    // We call the coinUpdateExecutor to update the local values
                    MainActivity mainActivity = ((MainActivity) getActivity());
                    HashMap<String, Double> currencyChanges = new HashMap<>();
                    currencyChanges.put(selectedCurrency, -giftAmount);
                    mainActivity.coinsUpdateExecutor.submit(new CoinsUpdateTask(mainActivity, mainActivity.settingsWriteLock, mainActivity.settings, currencyChanges));
                } else {
                    System.out.println("ACTIVITY OF EXCHANGE DIALOG FRAGMENT EXPECTED TO BE OF MAINACTIVITY CLASS BUT ISN'T");
                }
            } else {
                System.out.println("ACTIVITY OF EXCHANGE DIALOG FRAGMENT IS NULL WHEN EXPECTED NON-NULL");
            }
        }).addOnFailureListener(e -> {
            System.out.printf("FAILED TO SEND GIFT WITH EXCEPTION:\n%s\n", e);
            dialog.dismiss();
            if (getActivity()!=null){
                getActivity().runOnUiThread(() -> Toast.makeText(getActivity(), "Something went wrong when sending the gift, please try again.", Toast.LENGTH_LONG).show());
            } else {
                System.out.println("ACTIVITY OF EXCHANGE DIALOG FRAGMENT IS NULL WHEN EXPECTED NON-NULL");
            }
        });
    }
}
