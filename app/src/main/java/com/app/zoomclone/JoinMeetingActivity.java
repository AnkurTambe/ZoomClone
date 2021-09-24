package com.app.zoomclone;

import android.content.Intent;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.zoomclone.call.utils.Constants;
import com.app.zoomclone.conference.ui.ConferenceCallActivity;

public class JoinMeetingActivity extends BaseActivity {

    private EditText editTextID;
    private TextView textViewName;
    private Button buttonJoinMeeting;
    private ImageView imageViewBack;
    private String name = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join_meeting);

        initViews();
        listener();
        setData();
        textWatcher();
    }



    private void initViews() {
        editTextID = findViewById(R.id.editTextID);
        textViewName = findViewById(R.id.textViewName);
        imageViewBack = findViewById(R.id.imageViewBack);
        buttonJoinMeeting = findViewById(R.id.buttonJoinMeeting);
    }

    private void listener() {
        buttonJoinMeeting.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateToMeetings();
            }
        });
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void textWatcher() {
        editTextID.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.toString().length()>0)
                    enableJoinButton();
                else
                    disableJoinButton();
            }
        });
    }


    private void setData() {
        if (sharedPrefsHelper.hasQbUser()) {
            name = sharedPrefsHelper.getQbUser().getFullName();
        } else {
            name = getDeviceName();
        }
        textViewName.setText(name);
    }


    private void enableJoinButton() {
        buttonJoinMeeting.setEnabled(true);
        buttonJoinMeeting.setAlpha(1f);
    }

    private void disableJoinButton() {
        buttonJoinMeeting.setEnabled(false);
        buttonJoinMeeting.setAlpha(0.6f);
    }


    private void navigateToMeetings() {
        String meetingId= editTextID.getText().toString();
        Intent intent = new Intent(this, ConferenceCallActivity.class);
        intent.putExtra(Constants.ACTION_KEY_CHANNEL_NAME,meetingId);
        intent.putExtra(Constants.ACTION_KEY_USER_NAME,name);
        intent.putExtra(Constants.ACTION_KEY_ENCRYPTION_KEY,"");
        intent.putExtra(Constants.ACTION_KEY_ENCRYPTION_MODE,getString(R.string.encryption_mode_value));
        startActivity(intent);
    }

    public String getDeviceName() {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer)) {
            return capitalize(model);
        } else {
            return capitalize(manufacturer) + " " + model;
        }
    }


    private String capitalize(String s) {
        if (s == null || s.length() == 0) {
            return "";
        }
        char first = s.charAt(0);
        if (Character.isUpperCase(first)) {
            return s;
        } else {
            return Character.toUpperCase(first) + s.substring(1);
        }
    }

}
