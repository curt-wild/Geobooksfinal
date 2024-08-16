package com.example.geobooks2;
import android.app.Dialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

//This class is used to create a custom dialog box for the about page

public class AboutDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(R.layout.custom_dialog);
        
        Dialog dialog = builder.create();
        dialog.show();

        ImageView closeButton = dialog.findViewById(R.id.closeButton);

        // Set an OnClickListener on the ImageView
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
        
        return dialog;
    }
}
