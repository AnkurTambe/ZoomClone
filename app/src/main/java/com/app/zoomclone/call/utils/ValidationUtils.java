package com.app.zoomclone.call.utils;

import android.content.Context;
import android.text.TextUtils;
import android.util.Patterns;
import android.widget.EditText;

import com.app.zoomclone.R;
import com.quickblox.chat.model.QBAttachment;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValidationUtils {

    private ValidationUtils() {

    }

    public static  boolean validEmail(String email) {
        Pattern pattern = Patterns.EMAIL_ADDRESS;
        return pattern.matcher(email).matches();
    }


    public static boolean isAttachmentValid(QBAttachment qbAttachment) {
        Boolean isCorrect = false;
        if (qbAttachment != null && !TextUtils.isEmpty(qbAttachment.getName()) && !TextUtils.isEmpty(qbAttachment.getType())) {
            isCorrect = true;
        }
        return isCorrect;
    }

    private static boolean isEnteredTextValid(Context context, EditText editText, int resFieldName, int maxLength, boolean checkLogin) {
        boolean isCorrect = false;
        Pattern pattern;
        if (checkLogin) {
            pattern = Pattern.compile("^[a-zA-Z][a-zA-Z0-9]{2," + (maxLength - 1) + "}+$");
        } else {
            pattern = Pattern.compile("^[a-zA-Z][a-zA-Z 0-9]{2," + (maxLength - 1) + "}+$");
        }

        if (!TextUtils.isEmpty(editText.getText().toString())) {
            Matcher matcher = pattern.matcher(editText.getText().toString().trim());
            isCorrect = matcher.matches();
        }

        if (!isCorrect) {
            editText.setError(String.format(context.getString(R.string.error_name_must_not_contain_special_characters_from_app), context.getString(resFieldName), String.valueOf(maxLength)));
        }

        return isCorrect;
    }

    public static boolean isLoginValid(Context context, EditText editText) {
        return isEnteredTextValid(context, editText, R.string.field_name_user_login, Consts.MAX_LOGIN_LENGTH, true);
    }

    public static boolean isFoolNameValid(Context context, EditText editText) {
        return isEnteredTextValid(context, editText, R.string.field_name_user_fullname, Consts.MAX_FULLNAME_LENGTH, false);
    }
}