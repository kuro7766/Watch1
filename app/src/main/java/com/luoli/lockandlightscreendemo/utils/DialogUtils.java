package com.luoli.lockandlightscreendemo.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.DrawableRes;
import androidx.appcompat.app.AlertDialog;
import com.luoli.lockandlightscreendemo.R;


public class DialogUtils {

    public static AlertDialog createDialog(Context context, @DrawableRes int iconRes, String title, String message, String positiveTitle, DialogInterface.OnClickListener positiveClickListener,
                                           String negativeTitle, DialogInterface.OnClickListener negativeClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogCustom);
        builder.setIcon(iconRes);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(true);
        builder.setPositiveButton(positiveTitle, positiveClickListener);
        builder.setNegativeButton(negativeTitle, negativeClickListener);
        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialog -> {
            Button positiveButton = ((AlertDialog) dialog)
                    .getButton(AlertDialog.BUTTON_POSITIVE);
//                positiveButton.setBackgroundColor(Color.BLUE);
            positiveButton.setTextColor(Color.RED);

            Button negativeButton = ((AlertDialog) dialog)
                    .getButton(AlertDialog.BUTTON_NEGATIVE);
//                positiveButton.setBackgroundColor(Color.BLUE);
            negativeButton.setTextColor(Color.LTGRAY);

        });


        return alertDialog;
    }

}
