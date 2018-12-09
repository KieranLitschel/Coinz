package com.litschel.kieran.coinz;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

public class NoInternetDialogFragment extends DialogFragment {

    public interface NoInternetDialogCallback {
        void tryInternetAgain();

        void closeApp();
    }

    // Use this instance of the interface to deliver action events
    NoInternetDialogCallback mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoInternetDialogCallback so we can send events to the host
            mListener = (NoInternetDialogCallback) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException("Activity must implement NoInternetDialogCallback");
        }
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.no_login_dialog_message)
                .setPositiveButton(R.string.try_again, (dialog, id) -> mListener.tryInternetAgain())
                .setNegativeButton(R.string.close_app, (dialog, id) -> mListener.closeApp());
        // Create the AlertDialog object and return it
        return builder.create();
    }
}

