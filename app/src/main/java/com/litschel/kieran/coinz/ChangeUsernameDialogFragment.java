package com.litschel.kieran.coinz;

import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

public class ChangeUsernameDialogFragment extends DialogFragment {
    private FirebaseFirestore db;
    private boolean isNewUser;
    private String uid;
    private String username;
    private TextView errorText;
    private AlertDialog dialog;
    private Button positiveBtn;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        db = FirebaseFirestore.getInstance();

        // Get the layout inflater
        LayoutInflater inflater = getActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout

        Bundle args = getArguments();

        isNewUser = args.getBoolean("isNewUser", true);
        uid = args.getString("uid", "");
        username = args.getString("username", "");

        int layout;
        if (isNewUser) {
            layout = R.layout.dialog_create_username;
        } else {
            layout = R.layout.dialog_change_username;
        }

        View view = inflater.inflate(layout, null);

        EditText usernameEditText = view.findViewById(R.id.usernameEditText);
        errorText = view.findViewById(R.id.usernameExistsText);

        builder.setView(view)
                // Set positive button behvaiour later so we can prevent dialog being dismissed
                // when change username button pressed
                .setPositiveButton(R.string.change_username, null)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // Do nothing
                    }
                });

        dialog = builder.create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                positiveBtn = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
                positiveBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        System.out.println("CHANGE USERNAME BUTTON CLICKED");
                        String desiredUsername = usernameEditText.getText().toString();
                        if (!desiredUsername.equals("")){
                            if (desiredUsername.equals(username)) {
                                errorText.setVisibility(View.GONE);
                                errorText.setText(R.string.username_same);
                                errorText.setVisibility(View.VISIBLE);
                            } else if (!desiredUsername.equals("")) {
                                // Make it so button can't be clicked again until operation completes
                                positiveBtn.setEnabled(false);
                                errorText.setVisibility(View.GONE);
                                trySetUsername(desiredUsername);
                            }
                        }
                    }
                });
            }
        });

        return dialog;
    }

    private void trySetUsername(String desiredUsername) {
        db.runTransaction(new Transaction.Function<Boolean>() {
            @Override
            public Boolean apply(Transaction transaction) throws FirebaseFirestoreException {

                // Here we use a usernames document to keep track of username changes to ensure that
                // there is no risk of two users having the same username if they both change to it at the same time

                // Note that this approach of using a single document to keep track of all the usernames
                // is not scalable, as with a large userbase it will take a while for the transaction
                // to complete without any other user changing their name. But as stated in the sepcification
                // this is a prototype, and this approach works with a small scale userbase as changing the
                // username occurs infrequently.

                DocumentSnapshot usernamesRecord = transaction.get(db.collection("users").document("usernames"));

                for (String otherUid : usernamesRecord.getData().keySet()) {
                    if (usernamesRecord.getString(otherUid).equals(desiredUsername)) {
                        return false;
                    }
                }

                transaction.update(db.collection("users").document("usernames"),
                        uid, desiredUsername);
                transaction.update(db.collection("users").document(uid),
                        "username", desiredUsername);

                // Success
                return true;
            }
        }).addOnSuccessListener(new OnSuccessListener<Boolean>() {
            @Override
            public void onSuccess(Boolean usernameUpdated) {
                if (!usernameUpdated) {
                    System.out.println("FOUND USERNAME IN USE");
                    errorText.setText(R.string.username_same);
                    errorText.setVisibility(View.VISIBLE);
                    positiveBtn.setEnabled(true);
                } else {
                    System.out.println("SUCCEEDED IN CHANGING USERNAME");
                    dialog.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String text;
                            if (isNewUser){
                                text = String.format("Succeeded in setting username to %s", desiredUsername);
                            } else {
                                text = String.format("Succeeded in changing username to %s", desiredUsername);
                            }
                            Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();
                        }
                    });
                    SharedPreferences settings = ((MainActivity) getActivity()).settings;
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putString("username", username);
                    editor.apply();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                System.out.printf("FAILED IN CHECKING USERNAME USERNAME VALID WITH EXCEPTION:\n%s\n",e);
                dialog.dismiss();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String text;
                        if (isNewUser){
                            text = "Something went wrong while trying to set your new username, please try again.";
                        } else {
                            text = "Something went wrong while trying to create your username, please try again.";
                        }
                        Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();
                    }
                });
            }
        });


    }
}
