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

// This dialog is used to allow the user to change their username or create a new one
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

        LayoutInflater inflater = getActivity().getLayoutInflater();

        Bundle args = getArguments();

        // We use isNewUser to differentiate between whether the user is creating a new username or
        // if they are updating an old one, as we display different things in each case
        isNewUser = args.getBoolean("isNewUser", true);
        uid = args.getString("uid", "");
        username = args.getString("username", "");

        int layout;
        int positiveTextId;
        if (isNewUser) {
            layout = R.layout.dialog_create_username;
            positiveTextId = R.string.create_username;
        } else {
            layout = R.layout.dialog_change_username;
            positiveTextId = R.string.change_username;
        }

        View view = inflater.inflate(layout, null);

        // In addition to requiring a user to have a username to gift coins, we also require them to
        // have one when displaying the leaderboard, and this handles showing a slightly different
        // message in each of teh cases
        if (args.getBoolean("isLeaderboard",false)){
            ((TextView) view.findViewById(R.id.infoText)).setText(R.string.new_user_username_leaderboard);
        }

        EditText usernameEditText = view.findViewById(R.id.usernameEditText);
        errorText = view.findViewById(R.id.usernameExistsText);

        builder.setView(view)
                // Set positive button behvaiour later so we can prevent dialog being dismissed
                // when change username button pressed
                .setPositiveButton(positiveTextId, null)
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
                        // If the desired new username is empty we don't react to the button press
                        if (!desiredUsername.equals("")) {
                            // If their new username is the same as their old one we tell them that
                            // their new username must be different
                            if (desiredUsername.equals(username)) {
                                errorText.setVisibility(View.GONE);
                                errorText.setText(R.string.username_same);
                                errorText.setVisibility(View.VISIBLE);
                            } else {
                                // Make it so button can't be clicked again until operation completes,
                                // as we have to query the database which can take a while
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
        // We use a transaction here in case two different users try to set their username to the same
        // one at the same time
        db.runTransaction(new Transaction.Function<Void>() {
            @Override
            public Void apply(Transaction transaction) throws FirebaseFirestoreException {

                // Here we use a usernames document to keep track of username changes to ensure that
                // there is no risk of two users having the same username if they both change to it at the same time

                // Note that this approach of using a single document to keep track of all the usernames
                // is not scalable, as with a large userbase it will take a while for the transaction
                // to complete without any other user changing their name. But as stated in the sepcification
                // this is a prototype, and this approach works with a small scale userbase as changing the
                // username occurs infrequently.

                DocumentSnapshot usernamesRecord = transaction.get(db.collection("users").document("usernames"));

                // We check the username is not already in use, in which case we throw an exception so we can inform the user
                for (String otherUid : usernamesRecord.getData().keySet()) {
                    if (usernamesRecord.getString(otherUid).equals(desiredUsername)) {
                        throw new FirebaseFirestoreException("USERNAME ALREADY EXISTS IN DB", FirebaseFirestoreException.Code.ABORTED);
                    }
                }

                // We update the users document and the username document with the new username
                transaction.update(db.collection("users").document("usernames"),
                        uid, desiredUsername);
                transaction.update(db.collection("users").document(uid),
                        "username", desiredUsername);

                // Success
                return null;
            }
        }).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                System.out.println("SUCCEEDED IN CHANGING USERNAME");
                // We store the new username locally for other fragments to use
                SharedPreferences settings = ((MainActivity) getActivity()).settings;
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("username", desiredUsername);
                editor.apply();
                // We get rid of the dialog and inform them of the success
                dialog.dismiss();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String text;
                        if (isNewUser) {
                            text = String.format("Succeeded in setting username to %s", desiredUsername);
                        } else {
                            text = String.format("Succeeded in changing username to %s", desiredUsername);
                        }
                        Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                if (e.getMessage().equals("USERNAME ALREADY EXISTS IN DB")) {
                    // If the exception had the message stated above, we found another user with the
                    // desired username, so we inform the user the username is already taken
                    System.out.println("FOUND USERNAME IN USE");
                    errorText.setText(R.string.username_in_use);
                    errorText.setVisibility(View.VISIBLE);
                    positiveBtn.setEnabled(true);
                } else {
                    // If there was another exception something else went wrong, so we inform the user
                    // the change was not sucessful and ask them to try again
                    System.out.printf("FAILED IN CHECKING USERNAME USERNAME VALID WITH EXCEPTION:\n%s\n", e);
                    dialog.dismiss();
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            String text;
                            if (isNewUser) {
                                text = "Something went wrong while trying to set your new username, please try again.";
                            } else {
                                text = "Something went wrong while trying to create your username, please try again.";
                            }
                            Toast.makeText(getActivity(), text, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });


    }
}
