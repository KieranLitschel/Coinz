package com.litschel.kieran.coinz;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;

// This fragment is used to inform the user they must be logged in to use the app, but there is
// no internet to log them in

public class NoInternetDialogFragment extends DialogFragment {

    // The interface we use for callbacks when deploying NoInternetDialogFragment
    public interface NoInternetDialogCallback {
        void tryInternetAgain();
        void closeApp();
    }

    NoInternetDialogCallback mListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (NoInternetDialogCallback) context;
        } catch (ClassCastException e) {
            throw new ClassCastException("Activity must implement NoInternetDialogCallback");
        }
    }


    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.no_login_dialog_message)
                .setPositiveButton(R.string.try_again, (dialog, id) -> mListener.tryInternetAgain())
                .setNegativeButton(R.string.close_app, (dialog, id) -> mListener.closeApp());
        return builder.create();
    }
}

