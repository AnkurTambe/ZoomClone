package com.app.zoomclone.call.fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.app.zoomclone.R;
import com.app.zoomclone.call.db.QbUsersDbManager;
import com.app.zoomclone.call.utils.CollectionsUtils;
import com.app.zoomclone.call.utils.RingtonePlayer;
import com.app.zoomclone.call.utils.UiUtils;
import com.app.zoomclone.call.utils.UsersUtils;
import com.app.zoomclone.call.utils.WebRtcSessionManager;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;
import com.quickblox.videochat.webrtc.QBRTCSession;
import com.quickblox.videochat.webrtc.QBRTCTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;

/**
 * QuickBlox team
 */
public class IncomeCallFragment extends Fragment implements Serializable, View.OnClickListener {
    private static final String TAG = IncomeCallFragment.class.getSimpleName();

    private static final int PER_PAGE_SIZE_100 = 100;
    private static final String ORDER_RULE = "order";
    private static final String ORDER_DESC_UPDATED = "desc string updated_at";

    private static final long CLICK_DELAY = TimeUnit.SECONDS.toMillis(2);
    private TextView callTypeTextView;
    private ImageButton rejectButton;
    private ImageButton takeButton;
    private TextView callerNameTextView;
    private ProgressBar progressUserName;

    private List<Integer> opponentsIds;
    private Vibrator vibrator;
    private QBRTCTypes.QBConferenceType conferenceType;
    private long lastClickTime = 0l;
    private RingtonePlayer ringtonePlayer;
    private IncomeCallFragmentCallbackListener incomeCallFragmentCallbackListener;
    private QBRTCSession currentSession;
    private QbUsersDbManager qbUserDbManager;
    private TextView alsoOnCallText;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        try {
            incomeCallFragmentCallbackListener = (IncomeCallFragmentCallbackListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnCallEventsController");
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRetainInstance(true);

        Log.d(TAG, "onCreate() from IncomeCallFragment");
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_income_call, container, false);

        initFields();
        hideToolBar();
        initUI(view);
        setDisplayedTypeCall(conferenceType);
        initButtonsListener();
        ringtonePlayer = new RingtonePlayer(getActivity());

        return view;
    }

    private void initFields() {
        currentSession = WebRtcSessionManager.getInstance(getActivity()).getCurrentSession();
        qbUserDbManager = QbUsersDbManager.getInstance(getActivity().getApplicationContext());
        if (currentSession != null) {
            opponentsIds = currentSession.getOpponents();
            conferenceType = currentSession.getConferenceType();
            Log.d(TAG, conferenceType.toString() + "From onCreateView()");
        } else {
            getActivity().getSupportFragmentManager().popBackStack();
            Log.d(TAG, "Current session is not exist. Pop BackStack");
        }
    }

    public void hideToolBar() {
        Toolbar toolbar = getActivity().findViewById(R.id.toolbar_call);
        toolbar.setVisibility(View.GONE);
    }

    @Override
    public void onStart() {
        super.onStart();
        startCallNotification();
    }

    private void initButtonsListener() {
        rejectButton.setOnClickListener(this);
        takeButton.setOnClickListener(this);
    }

    private void initUI(View view) {
        callTypeTextView = view.findViewById(R.id.call_type);
        progressUserName = view.findViewById(R.id.progress_bar_opponent_name);
        ImageView callerAvatarImageView = view.findViewById(R.id.image_caller_avatar);
        callerNameTextView = view.findViewById(R.id.text_caller_name);
        TextView otherIncUsersTextView = view.findViewById(R.id.text_other_inc_users);
        alsoOnCallText = view.findViewById(R.id.text_also_on_call);
        rejectButton = view.findViewById(R.id.image_button_reject_call);
        takeButton = view.findViewById(R.id.image_button_accept_call);

        if (currentSession != null) {
            callerAvatarImageView.setBackgroundDrawable(getBackgroundForCallerAvatar(currentSession.getCallerID()));

            QBUser callerUser = qbUserDbManager.getUserById(currentSession.getCallerID());
            if (callerUser != null && !TextUtils.isEmpty(callerUser.getFullName())) {
                callerNameTextView.setText(callerUser.getFullName());
            } else {
                callerNameTextView.setText(String.valueOf(currentSession.getCallerID()));
                updateUserFromServer();
            }

            otherIncUsersTextView.setText(getOtherIncUsersNames());
            setVisibilityAlsoOnCallTextView();
        }
    }

    private void updateUserFromServer() {
        progressUserName.setVisibility(View.VISIBLE);
        Integer callerID = currentSession.getCallerID();
        QBUsers.getUser(callerID).performAsync(new QBEntityCallback<QBUser>() {
            @Override
            public void onSuccess(QBUser qbUser, Bundle bundle) {
                if (qbUser != null) {
                    qbUserDbManager.saveUser(qbUser);
                    String callerName = TextUtils.isEmpty(qbUser.getFullName())? qbUser.getLogin() : qbUser.getFullName();
                    callerNameTextView.setText(callerName);
                }
                progressUserName.setVisibility(View.GONE);
            }

            @Override
            public void onError(QBResponseException e) {
                progressUserName.setVisibility(View.GONE);
                e.printStackTrace();
            }
        });
    }

    private void setVisibilityAlsoOnCallTextView() {
        if (opponentsIds.size() < 2) {
            alsoOnCallText.setVisibility(View.INVISIBLE);
        }
    }

    private Drawable getBackgroundForCallerAvatar(int callerId) {
        return UiUtils.getColorCircleDrawable(callerId);
    }

    private void startCallNotification() {
        Log.d(TAG, "startCallNotification()");

        ringtonePlayer.play(false);
        vibrator = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);
        long[] vibrationCycle = {0, 1000, 1000};
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(vibrationCycle, 1);
        }

    }

    private void stopCallNotification() {
        Log.d(TAG, "stopCallNotification()");

        if (ringtonePlayer != null) {
            ringtonePlayer.stop();
        }

        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private String getOtherIncUsersNames() {
        ArrayList<QBUser> usersFromDb = qbUserDbManager.getUsersByIds(opponentsIds);
        ArrayList<QBUser> opponents = new ArrayList<>();
        opponents.addAll(UsersUtils.getListAllUsersFromIds(usersFromDb, opponentsIds));

        opponents.remove(QBChatService.getInstance().getUser());
        Log.d(TAG, "opponentsIds = " + opponentsIds);
        return CollectionsUtils.makeStringFromUsersFullNames(opponents);
    }

    private void setDisplayedTypeCall(QBRTCTypes.QBConferenceType conferenceType) {
        boolean isVideoCall = conferenceType == QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_VIDEO;

        callTypeTextView.setText(isVideoCall ? R.string.text_incoming_video_call : R.string.text_incoming_audio_call);
        takeButton.setImageResource(isVideoCall ? R.drawable.ic_video_white : R.drawable.ic_call);
    }

    @Override
    public void onStop() {
        stopCallNotification();
        super.onStop();
        Log.d(TAG, "onStop() from IncomeCallFragment");
    }

    @Override
    public void onClick(View v) {

        if ((SystemClock.uptimeMillis() - lastClickTime) < CLICK_DELAY) {
            return;
        }
        lastClickTime = SystemClock.uptimeMillis();

        switch (v.getId()) {
            case R.id.image_button_reject_call:
                reject();
                break;

            case R.id.image_button_accept_call:
                accept();
                break;

            default:
                break;
        }
    }

    private void accept() {
        enableButtons(false);
        stopCallNotification();

        incomeCallFragmentCallbackListener.onAcceptCurrentSession();
        Log.d(TAG, "Call is started");
    }

    private void reject() {
        enableButtons(false);
        stopCallNotification();

        incomeCallFragmentCallbackListener.onRejectCurrentSession();
        Log.d(TAG, "Call is rejected");
    }

    private void enableButtons(boolean enable) {
        takeButton.setEnabled(enable);
        rejectButton.setEnabled(enable);
    }
}