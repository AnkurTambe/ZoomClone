package com.app.zoomclone;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener {

    private TextView textViewVersion;
    private RelativeLayout relativeLayoutTellOthers;
    private RelativeLayout relativeLayoutRate;
    private RelativeLayout relativeLayoutPrivacyPolicy;
    private ImageView imageViewBack;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        initViews();
        listeners();
    }

    private void initViews() {
        textViewVersion = findViewById(R.id.textViewVersion);
        imageViewBack = findViewById(R.id.imageViewBack);
        relativeLayoutTellOthers = findViewById(R.id.rlTellOthers);
        relativeLayoutRate = findViewById(R.id.rlRate);
        relativeLayoutPrivacyPolicy = findViewById(R.id.rlPrivacyPolicy);

        textViewVersion.setText(BuildConfig.VERSION_NAME);

    }

    private void listeners() {
        imageViewBack.setOnClickListener(this);
        relativeLayoutTellOthers.setOnClickListener(this);
        relativeLayoutRate.setOnClickListener(this);
        relativeLayoutPrivacyPolicy.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.imageViewBack:
                finish();
                break;

            case R.id.rlTellOthers:
                shareApp();
                break;

            case R.id.rlRate:
                rateAppOnStore();
                break;

            case R.id.rlPrivacyPolicy:
                openBrowser();
                break;

        }
    }

    private void openBrowser() {
        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse("http://www.google.com"));
        startActivity(intent);
    }

    private void rateAppOnStore() {
        Uri uri = Uri.parse("market://details?id=us.zoom.videomeetings");
        Intent intent = new Intent(Intent.ACTION_VIEW,uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
        intent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        startActivity(intent);
    }

    private void shareApp() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT,
                "Hey checkout my app at: https://play.google.com/store/apps/details?id=us.zoom.videomeetings");
        intent.setType("text/plain");
        startActivity(intent);
    }
}
