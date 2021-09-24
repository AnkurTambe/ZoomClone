package com.app.zoomclone.ui.meetChat;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.view.ActionMode;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.app.zoomclone.JoinMeetingActivity;
import com.app.zoomclone.R;
import com.app.zoomclone.call.utils.Constants;
import com.app.zoomclone.call.utils.ErrorUtils;
import com.app.zoomclone.call.utils.SharedPrefsHelper;
import com.app.zoomclone.call.utils.ToastUtils;
import com.app.zoomclone.call.utils.UiUtils;
import com.app.zoomclone.chat.ChatActivity;
import com.app.zoomclone.chat.ChatHelper;
import com.app.zoomclone.chat.DialogsActivity;
import com.app.zoomclone.chat.DialogsAdapter;
import com.app.zoomclone.chat.FcmConsts;
import com.app.zoomclone.chat.async.BaseAsyncTask;
import com.app.zoomclone.chat.managers.DialogsManager;
import com.app.zoomclone.chat.qb.QbChatDialogMessageListenerImp;
import com.app.zoomclone.chat.qb.QbDialogHolder;
import com.app.zoomclone.chat.qb.VerboseQbChatConnectionListener;
import com.app.zoomclone.chat.qb.callback.QbEntityCallbackImpl;
import com.app.zoomclone.conference.ui.ConferenceCallActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayout;
import com.orangegangsters.github.swipyrefreshlayout.library.SwipyRefreshLayoutDirection;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBIncomingMessagesManager;
import com.quickblox.chat.QBSystemMessagesManager;
import com.quickblox.chat.exception.QBChatException;
import com.quickblox.chat.listeners.QBChatDialogMessageListener;
import com.quickblox.chat.listeners.QBSystemMessageListener;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBRequestGetBuilder;
import com.quickblox.users.model.QBUser;

import org.jivesoftware.smack.ConnectionListener;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MeetChatFragment extends Fragment implements View.OnClickListener, DialogsManager.ManagingDialogsCallbacks {

    private TextView textViewShortName, textViewFullName;
    private EditText editTextSearch;
    private LinearLayout linearLayoutJoin, linearLayoutNewMeeting;
    private SharedPrefsHelper sharedPrefsHelper;
    private QBUser currentUser;
    private View root;
    private SwipyRefreshLayout refreshLayout;
    private ProgressBar progress;
    public static final int DIALOGS_PER_PAGE = 100;
    private static final int REQUEST_SELECT_PEOPLE = 174;
    private static final int REQUEST_DIALOG_ID_FOR_UPDATE = 165;
    private static final int PLAY_SERVICES_REQUEST_CODE = 9000;

    private boolean isProcessingResultInProgress = false;
    private BroadcastReceiver pushBroadcastReceiver;
    private ConnectionListener chatConnectionListener;

    private DialogsAdapter dialogsAdapter;
    private QBChatDialogMessageListener allDialogsMessagesListener = new AllDialogsMessageListener();
    private SystemMessagesListener systemMessagesListener = new SystemMessagesListener();
    private QBSystemMessagesManager systemMessagesManager;
    private QBIncomingMessagesManager incomingMessagesManager;
    private DialogsManager dialogsManager = new DialogsManager();
    private ActionMode currentActionMode;
    private boolean hasMoreDialogs = true;
    private final Set<DialogsActivity.DialogJoinerAsyncTask> joinerTasksSet = new HashSet<>();
    private ProgressDialog progressDialog;
    private String TAG = MeetChatFragment.class.getSimpleName();
    private ArrayList<QBChatDialog> dialogs;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_meetchat, container, false);



        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        sharedPrefsHelper = SharedPrefsHelper.getInstance();
        currentUser =sharedPrefsHelper.getQbUser();

        initUI();
        setData();
        clickListener();
        initConnectionListener();
        textWatcher();

    }



    private void initUI() {
        textViewShortName = root.findViewById(R.id.textViewShortName);
        textViewFullName = root.findViewById(R.id.textViewName);
        textViewFullName = root.findViewById(R.id.textViewName);
        linearLayoutJoin = root.findViewById(R.id.llJoin);
        linearLayoutNewMeeting = root.findViewById(R.id.llNewMeeting);
        editTextSearch = root.findViewById(R.id.editTextSearch);
        TextView tvTitle = getActivity().findViewById(R.id.tvTitle);

        tvTitle.setText(getResources().getString(R.string.title_meet_chat));

        LinearLayout emptyHintLayout = root.findViewById(R.id.ll_chat_empty);
            ListView dialogsListView = root.findViewById(R.id.list_dialogs_chats);
            refreshLayout = root.findViewById(R.id.swipy_refresh_layout);
            progress = root.findViewById(R.id.pb_dialogs);

            dialogs = new ArrayList<>(QbDialogHolder.getInstance().getDialogs().values());
            dialogsAdapter = new DialogsAdapter(getActivity(), dialogs);

            dialogsListView.setEmptyView(emptyHintLayout);
            dialogsListView.setAdapter(dialogsAdapter);

            dialogsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final QBChatDialog selectedDialog = (QBChatDialog) parent.getItemAtPosition(position);
                    if (currentActionMode != null) {
                        dialogsAdapter.toggleSelection(selectedDialog);
                        String subtitle = "";
                        if (dialogsAdapter.getSelectedItems().size() != 1) {
                            subtitle = getString(R.string.dialogs_actionmode_subtitle, String.valueOf(dialogsAdapter.getSelectedItems().size()));
                        } else {
                            subtitle = getString(R.string.dialogs_actionmode_subtitle_single, String.valueOf(dialogsAdapter.getSelectedItems().size()));
                        }
                        currentActionMode.setSubtitle(subtitle);
                        currentActionMode.getMenu().getItem(0).setVisible(dialogsAdapter.getSelectedItems().size() >= 1);
                    } else if (ChatHelper.getInstance().isLogged()) {
                        showProgressDialog(R.string.dlg_login);
                        ChatHelper.getInstance().loginToChat(currentUser, new QBEntityCallback<Void>() {
                            @Override
                            public void onSuccess(Void aVoid, Bundle bundle) {
                                hideProgressDialog();
                                ChatActivity.startForResult(getActivity(), REQUEST_DIALOG_ID_FOR_UPDATE, selectedDialog);
                            }

                            @Override
                            public void onError(QBResponseException e) {
                                hideProgressDialog();
                                ToastUtils.shortToast(R.string.login_chat_login_error);
                            }
                        });
                    }
                }
            });


            refreshLayout.setOnRefreshListener(new SwipyRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh(SwipyRefreshLayoutDirection direction) {
                    cancelTasks();
                    loadDialogsFromQb(true, true);
                }
            });
            refreshLayout.setColorSchemeResources(R.color.color_new_blue, R.color.random_color_2, R.color.random_color_3, R.color.random_color_7);
        }


    private void textWatcher() {
        editTextSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                filter(editable.toString());
            }
        });
    }

    private void filter(String text) {
        List<QBChatDialog> temp = new ArrayList<>();
        for (QBChatDialog qbChatDialog : dialogs){
            if (qbChatDialog.getName().toLowerCase().contains(text.toLowerCase())){
                temp.add(qbChatDialog);
            }
        }
        dialogsAdapter.updateList(temp);
    }

    private void initConnectionListener() {
        View rootView = root.findViewById(R.id.layout_root);
        chatConnectionListener = new VerboseQbChatConnectionListener(rootView) {
            @Override
            public void reconnectionSuccessful() {
                super.reconnectionSuccessful();
                loadDialogsFromQb(false, true);
            }
        };
    }


    private void updateDialogsAdapter() {
        dialogs = new ArrayList<>(QbDialogHolder.getInstance().getDialogs().values());
        dialogsAdapter.updateList(dialogs);
    }

    @Override
    public void onDialogCreated(QBChatDialog chatDialog) {
        loadDialogsFromQb(true, true);
    }

    @Override
    public void onDialogUpdated(String chatDialog) {
        updateDialogsAdapter();
    }

    @Override
    public void onNewDialogLoaded(QBChatDialog chatDialog) {
        updateDialogsAdapter();
    }


    private class PushBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra(FcmConsts.EXTRA_FCM_MESSAGE);
            Log.v(TAG, "Received broadcast " + intent.getAction() + " with data: " + message);
            loadDialogsFromQb(false, false);
        }
    }

    private class SystemMessagesListener implements QBSystemMessageListener {
        @Override
        public void processMessage(final QBChatMessage qbChatMessage) {
            dialogsManager.onSystemMessageReceived(qbChatMessage);
        }

        @Override
        public void processError(QBChatException ignored, QBChatMessage qbChatMessage) {

        }
    }

    private class AllDialogsMessageListener extends QbChatDialogMessageListenerImp {
        @Override
        public void processMessage(final String dialogId, final QBChatMessage qbChatMessage, Integer senderId) {
            Log.d(TAG, "Processing received Message: " + qbChatMessage.getBody());
            if (!senderId.equals(currentUser.getId())) {
                dialogsManager.onGlobalMessageReceived(dialogId, qbChatMessage);
            }
        }
    }


    private void setData() {
        if (currentUser.getFullName() != null && !currentUser.getFullName().trim().isEmpty()) {
            if (currentUser.getFullName().trim().contains(" ")) {
                String[] eventName = currentUser.getFullName().trim().split(" ");
                textViewShortName.setText(eventName[0].substring(0, 1).toUpperCase() + eventName[1].substring(0, 1).toUpperCase());
            } else if (currentUser.getFullName().trim().length() > 2) {
                textViewShortName.setText(currentUser.getFullName().trim().substring(0, 2).toUpperCase());
            } else {
                textViewShortName.setText(currentUser.getFullName().trim().toUpperCase());
            }
            textViewShortName.setBackgroundDrawable(UiUtils.getColorCircleDrawable(currentUser.getId()));
        }

        textViewFullName.setText(currentUser.getFullName());

    }

    private void clickListener() {
        linearLayoutJoin.setOnClickListener(this);
        linearLayoutNewMeeting.setOnClickListener(this);
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.llJoin:
                navigateToJoinMeeting();
                break;
            case R.id.llNewMeeting:
                navigateToMeetings();
                break;
        }
    }

    private void navigateToJoinMeeting() {
        Intent intent = new Intent(getActivity(), JoinMeetingActivity.class);
        startActivity(intent);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

    }

    private void navigateToMeetings() {
        String meetingId= currentUser.getId().toString();
        Intent intent = new Intent(getActivity(), ConferenceCallActivity.class);
        intent.putExtra(Constants.ACTION_KEY_CHANNEL_NAME,meetingId);
        intent.putExtra(Constants.ACTION_KEY_USER_NAME,currentUser.getFullName());
        intent.putExtra(Constants.ACTION_KEY_ENCRYPTION_KEY,"");
        intent.putExtra(Constants.ACTION_KEY_ENCRYPTION_MODE,getString(R.string.encryption_mode_value));
        startActivity(intent);
        getActivity().overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void loadDialogsFromQb(final boolean silentUpdate, final boolean clearDialogHolder) {
        isProcessingResultInProgress = true;
        if (silentUpdate) {
            progress.setVisibility(View.VISIBLE);
        } else {
            showProgressDialog(R.string.dlg_loading);
        }
        QBRequestGetBuilder requestBuilder = new QBRequestGetBuilder();
        requestBuilder.setLimit(DIALOGS_PER_PAGE);
        requestBuilder.setSkip(clearDialogHolder ? 0 : QbDialogHolder.getInstance().getDialogs().size());

        ChatHelper.getInstance().getDialogs(requestBuilder, new QBEntityCallback<ArrayList<QBChatDialog>>() {
            @Override
            public void onSuccess(ArrayList<QBChatDialog> dialogs, Bundle bundle) {
                if (dialogs.size() < DIALOGS_PER_PAGE) {
                    hasMoreDialogs = false;
                }
                if (clearDialogHolder) {
                    QbDialogHolder.getInstance().clear();
                    hasMoreDialogs = true;
                }
                QbDialogHolder.getInstance().addDialogs(dialogs);
                updateDialogsAdapter();

                DialogsActivity.DialogJoinerAsyncTask joinerTask =
                        new DialogsActivity.DialogJoinerAsyncTask( dialogs);
                joinerTasksSet.add(joinerTask);
                joinerTask.execute();

                disableProgress();
                if (hasMoreDialogs) {
                    loadDialogsFromQb(true, false);
                }
            }

            @Override
            public void onError(QBResponseException e) {
                disableProgress();
                ToastUtils.shortToast(e.getMessage());
            }
        });
    }

    private void disableProgress() {
        isProcessingResultInProgress = false;
        hideProgressDialog();
        refreshLayout.setRefreshing(false);
        progress.setVisibility(View.GONE);
    }

    protected void showProgressDialog(@StringRes Integer messageId) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setIndeterminate(true);
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);

            // Disable the back button
            DialogInterface.OnKeyListener keyListener = new DialogInterface.OnKeyListener() {
                @Override
                public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                    return keyCode == KeyEvent.KEYCODE_BACK;
                }
            };
            progressDialog.setOnKeyListener(keyListener);
        }
        progressDialog.setMessage(getString(messageId));
        try {
            progressDialog.show();
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
    }




    protected void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }


    private void reloginToChat() {
        showProgressDialog(R.string.dlg_relogin);
        if (SharedPrefsHelper.getInstance().hasQbUser()) {
            ChatHelper.getInstance().loginToChat(SharedPrefsHelper.getInstance().getQbUser(), new QBEntityCallback<Void>() {
                @Override
                public void onSuccess(Void aVoid, Bundle bundle) {
                    Log.d(TAG, "Relogin Successful");
                    checkPlayServicesAvailable();
                    registerQbChatListeners();
                    loadDialogsFromQb(false, false);
                }

                @Override
                public void onError(QBResponseException e) {
                    Log.d(TAG, "Relogin Failed " + e.getMessage());
                    hideProgressDialog();
                    getActivity().finish();
                }
            });
        }
    }

    private void checkPlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(getActivity());
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(getActivity(), resultCode, PLAY_SERVICES_REQUEST_CODE).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                getActivity().finish();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ChatHelper.getInstance().removeConnectionListener(chatConnectionListener);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(pushBroadcastReceiver);
    }

    @Override
    public void onStop() {
        super.onStop();
        cancelTasks();
        unregisterQbChatListeners();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterQbChatListeners();
    }

    private void registerQbChatListeners() {
        ChatHelper.getInstance().addConnectionListener(chatConnectionListener);
        try {
            systemMessagesManager = QBChatService.getInstance().getSystemMessagesManager();
            incomingMessagesManager = QBChatService.getInstance().getIncomingMessagesManager();
        } catch (Exception e) {
            Log.d(TAG, "Can not get SystemMessagesManager. Need relogin. " + e.getMessage());
            reloginToChat();
            return;
        }
        if (incomingMessagesManager == null) {
            reloginToChat();
            return;
        }

        systemMessagesManager.addSystemMessageListener(systemMessagesListener);
        incomingMessagesManager.addDialogMessageListener(allDialogsMessagesListener);
        dialogsManager.addManagingDialogsCallbackListener(this);

        pushBroadcastReceiver = new PushBroadcastReceiver();
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(pushBroadcastReceiver,
                new IntentFilter(FcmConsts.ACTION_NEW_FCM_EVENT));
    }
    private void unregisterQbChatListeners() {
        if (incomingMessagesManager != null) {
            incomingMessagesManager.removeDialogMessageListrener(allDialogsMessagesListener);
        }

        if (systemMessagesManager != null) {
            systemMessagesManager.removeSystemMessageListener(systemMessagesListener);
        }

        dialogsManager.removeManagingDialogsCallbackListener(this);
    }

    private void cancelTasks() {
        for (DialogsActivity.DialogJoinerAsyncTask task : joinerTasksSet) {
            task.cancel(true);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (ChatHelper.getInstance().isLogged()) {
            checkPlayServicesAvailable();
            registerQbChatListeners();
            if (!QbDialogHolder.getInstance().getDialogs().isEmpty()) {
                loadDialogsFromQb(true, true);
            } else {
                loadDialogsFromQb(false, true);
            }
        } else {
            reloginToChat();
        }
    }
}