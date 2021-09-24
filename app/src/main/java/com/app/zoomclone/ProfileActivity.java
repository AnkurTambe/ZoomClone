package com.app.zoomclone;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.app.zoomclone.call.activities.CallActivity;
import com.app.zoomclone.call.activities.PermissionsActivity;
import com.app.zoomclone.call.db.QbUsersDbManager;
import com.app.zoomclone.call.services.LoginService;
import com.app.zoomclone.call.utils.CollectionsUtils;
import com.app.zoomclone.call.utils.Consts;
import com.app.zoomclone.call.utils.PermissionsChecker;
import com.app.zoomclone.call.utils.PushNotificationSender;
import com.app.zoomclone.call.utils.ToastUtils;
import com.app.zoomclone.call.utils.UiUtils;
import com.app.zoomclone.call.utils.WebRtcSessionManager;
import com.app.zoomclone.chat.ChatActivity;
import com.app.zoomclone.chat.ChatHelper;
import com.app.zoomclone.chat.DialogsActivity;
import com.app.zoomclone.chat.managers.DialogsManager;
import com.app.zoomclone.chat.qb.QbDialogHolder;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBSystemMessagesManager;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.model.QBUser;
import com.quickblox.videochat.webrtc.QBRTCClient;
import com.quickblox.videochat.webrtc.QBRTCSession;
import com.quickblox.videochat.webrtc.QBRTCTypes;

import java.util.ArrayList;
import java.util.List;

import static com.app.zoomclone.chat.qb.QbDialogUtils.createDialog;

public class ProfileActivity extends BaseActivity implements View.OnClickListener, DialogsManager.ManagingDialogsCallbacks {

    private TextView textViewShortName, textViewEmail;
    private ImageView imageViewMeet, imageViewCall, imageViewChat, imageViewBack;
    private QBUser opponentUser;
    private QBUser currentUser;
    private QbUsersDbManager dbManager;
    private PermissionsChecker checker;
    private String TAG = ProfileActivity.class.getSimpleName();
    private static final int REQUEST_DIALOG_ID_FOR_UPDATE = 165;
    private DialogsManager dialogsManager = new DialogsManager();
    private QBSystemMessagesManager systemMessagesManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        dbManager = QbUsersDbManager.getInstance(getApplicationContext());
        checker = new PermissionsChecker(getApplicationContext());

        systemMessagesManager = QBChatService.getInstance().getSystemMessagesManager();

        dialogsManager.addManagingDialogsCallbackListener(this);

        initViews();
        clickListeners();
        getIntentValues();
        setDate();
    }


    private void getIntentValues() {
        opponentUser = (QBUser) getIntent().getSerializableExtra("position");
        currentUser = sharedPrefsHelper.getQbUser();
    }

    private void setDate() {
        if (opponentUser.getFullName() != null && !opponentUser.getFullName().trim().isEmpty()) {
            if (opponentUser.getFullName().trim().contains(" ")) {
                String[] eventName = opponentUser.getFullName().trim().split(" ");
                textViewShortName.setText(eventName[0].substring(0, 1).toUpperCase() + eventName[1].substring(0, 1).toUpperCase());
            } else if (opponentUser.getFullName().trim().length() > 2) {
                textViewShortName.setText(opponentUser.getFullName().trim().substring(0, 2).toUpperCase());
            } else {
                textViewShortName.setText(opponentUser.getFullName().trim().toUpperCase());
            }
            textViewShortName.setBackgroundDrawable(UiUtils.getColorCircleDrawable(opponentUser.getId()));
        }

        textViewEmail.setText(opponentUser.getLogin());
    }


    private void initViews() {
        imageViewBack = findViewById(R.id.imageViewBack);
        textViewShortName = findViewById(R.id.textViewShortName);
        textViewEmail = findViewById(R.id.textViewEmail);
        imageViewMeet = findViewById(R.id.imageViewMeet);
        imageViewCall = findViewById(R.id.imageViewPhone);
        imageViewChat = findViewById(R.id.imageViewChat);
    }

    private void clickListeners() {
        imageViewBack.setOnClickListener(this);
        imageViewMeet.setOnClickListener(this);
        imageViewCall.setOnClickListener(this);
        imageViewChat.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {

            case R.id.imageViewBack:
                finish();
                break;

            case R.id.imageViewPhone:
                if (checkIsLoggedInChat()) {
                    startCall(false);
                }
                if (checker.lacksPermissions(Consts.PERMISSIONS[1])) {
                    startPermissionsActivity(true);
                }
                break;

            case R.id.imageViewMeet:
                if (checkIsLoggedInChat()) {
                    startCall(true);
                }
                if (checker.lacksPermissions(Consts.PERMISSIONS[1])) {
                    startPermissionsActivity(true);
                }
                break;

            case R.id.imageViewChat:
                navigateToChat();
                break;


        }
    }

    private void navigateToChat() {
        ArrayList<QBUser> selectedUsers = new ArrayList<>();
        selectedUsers.add(opponentUser);
        String chatName = opponentUser.getFullName();
        if (isPrivateDialogExist(selectedUsers)) {
            selectedUsers.remove(ChatHelper.getCurrentUser());
            QBChatDialog existingPrivateDialog = QbDialogHolder.getInstance().getPrivateDialogWithUser(selectedUsers.get(0));
            if (existingPrivateDialog != null) {
                ChatActivity.startForResult(this, REQUEST_DIALOG_ID_FOR_UPDATE, existingPrivateDialog);
            }
        } else {
            showProgressDialog(R.string.create_chat);
            if (TextUtils.isEmpty(chatName)) {
                chatName = "";
            }
            createDialog(selectedUsers, chatName);
        }
    }


    private void createDialog(final ArrayList<QBUser> selectedUsers, String chatName) {
        Log.d(TAG, "Creating Dialog");
        ChatHelper.getInstance().createDialogWithSelectedUsers(selectedUsers, chatName,
                new QBEntityCallback<QBChatDialog>() {
                    @Override
                    public void onSuccess(QBChatDialog dialog, Bundle args) {
                        Log.d(TAG, "Creating Dialog Successful");
                        dialogsManager.sendSystemMessageAboutCreatingDialog(systemMessagesManager, dialog);
                        ArrayList<QBChatDialog> dialogs = new ArrayList<>();
                        dialogs.add(dialog);
                        new DialogsActivity.DialogJoinerAsyncTask( dialogs).execute();
                        ChatActivity.startForResult(ProfileActivity.this, REQUEST_DIALOG_ID_FOR_UPDATE, dialog, true);
                        hideProgressDialog();
                    }

                    @Override
                    public void onError(QBResponseException error) {
                        Log.d(TAG, "Creating Dialog Error: " + error.getMessage());
                        hideProgressDialog();
                        showErrorSnackbar(R.string.dialogs_creation_error, error, null);
                    }
                }
        );
    }



    private boolean isPrivateDialogExist(ArrayList<QBUser> allSelectedUsers) {
        ArrayList<QBUser> selectedUsers = new ArrayList<>();
        selectedUsers.addAll(allSelectedUsers);
        selectedUsers.remove(ChatHelper.getCurrentUser());
        return selectedUsers.size() == 1 && QbDialogHolder.getInstance().hasPrivateDialogWithUser(selectedUsers.get(0));
    }



    private boolean checkIsLoggedInChat() {
        if (!QBChatService.getInstance().isLoggedIn()) {
            startLoginService();
            ToastUtils.shortToast(R.string.dlg_relogin_wait);
            return false;
        }
        return true;
    }

    private void startLoginService() {
        if (sharedPrefsHelper.hasQbUser()) {
            QBUser qbUser = sharedPrefsHelper.getQbUser();
            LoginService.start(this, qbUser);
        }
    }

    private void startCall(boolean isVideoCall) {
        Log.d(TAG, "Starting Call");
        List<QBUser> selectedUsers = new ArrayList<>();
        selectedUsers.add(opponentUser);

        ArrayList<Integer> opponentsList = CollectionsUtils.getIdsSelectedOpponents(selectedUsers);
        QBRTCTypes.QBConferenceType conferenceType = isVideoCall
                ? QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_VIDEO
                : QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_AUDIO;
        Log.d(TAG, "conferenceType = " + conferenceType);

        QBRTCClient qbrtcClient = QBRTCClient.getInstance(getApplicationContext());
        QBRTCSession newQbRtcSession = qbrtcClient.createNewSessionWithOpponents(opponentsList, conferenceType);
        WebRtcSessionManager.getInstance(this).setCurrentSession(newQbRtcSession);

        // Make Users FullName Strings and ID's list for iOS VOIP push
        String newSessionID = newQbRtcSession.getSessionID();
        ArrayList<String> opponentsIDsList = new ArrayList<>();
        ArrayList<String> opponentsNamesList = new ArrayList<>();
        List<QBUser> usersInCall = selectedUsers;

        // the Caller in exactly first position is needed regarding to iOS 13 functionality
        usersInCall.add(0, currentUser);

        for (QBUser user : usersInCall) {
            String userId = user.getId().toString();
            String userName = "";
            if (TextUtils.isEmpty(user.getFullName())) {
                userName = user.getLogin();
            } else {
                userName = user.getFullName();
            }

            opponentsIDsList.add(userId);
            opponentsNamesList.add(userName);
        }

        String opponentsIDsString = TextUtils.join(",", opponentsIDsList);
        String opponentNamesString = TextUtils.join(",", opponentsNamesList);

        Log.d(TAG, "New Session with ID: " + newSessionID + "\n Users in Call: " + "\n" + opponentsIDsString + "\n" + opponentNamesString);
        PushNotificationSender.sendPushMessage(opponentsList, currentUser.getFullName(), newSessionID, opponentsIDsString, opponentNamesString, isVideoCall);
        CallActivity.start(this, false);
    }

    private void startPermissionsActivity(boolean checkOnlyAudio) {
        PermissionsActivity.startActivity(this, checkOnlyAudio, Consts.PERMISSIONS);
    }


    @Override
    public void onDialogCreated(QBChatDialog chatDialog) {

    }

    @Override
    public void onDialogUpdated(String chatDialog) {

    }

    @Override
    public void onNewDialogLoaded(QBChatDialog chatDialog) {

    }
}
