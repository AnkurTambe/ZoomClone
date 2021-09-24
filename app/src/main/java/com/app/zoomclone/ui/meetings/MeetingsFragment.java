package com.app.zoomclone.ui.meetings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.app.zoomclone.R;
import com.app.zoomclone.call.utils.Constants;
import com.app.zoomclone.call.utils.SharedPrefsHelper;
import com.app.zoomclone.conference.ui.ConferenceCallActivity;
import com.quickblox.users.model.QBUser;

public class MeetingsFragment extends Fragment implements View.OnClickListener {

    private SharedPrefsHelper sharedPrefsHelper;
    private QBUser currentUser;
    private View root;
    private TextView textViewSendInvitation,textViewStart;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        root = inflater.inflate(R.layout.fragment_meetings, container, false);


        return root;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        sharedPrefsHelper = SharedPrefsHelper.getInstance();
        currentUser =sharedPrefsHelper.getQbUser();


        initViews();
        clickListener();
    }


    private void initViews() {
        TextView tvTitle = getActivity().findViewById(R.id.tvTitle);
        TextView textViewMeetingID = root.findViewById(R.id.textViewMeetingID);
         textViewStart = root.findViewById(R.id.textViewStart);
        textViewSendInvitation = root.findViewById(R.id.textViewSendInvitation);

        textViewMeetingID.setText(currentUser.getId().toString());
        tvTitle.setText(getResources().getString(R.string.title_meetings));


    }

    private void clickListener() {
        textViewStart.setOnClickListener(this);
        textViewSendInvitation.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.textViewStart :
                navigateToMeetings();
                break;
            case R.id.textViewSendInvitation:
                shareUrl();
                break;
        }
    }

    private void shareUrl() {
        String name = currentUser.getFullName();
        String id = currentUser.getId()+"";
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        intent.putExtra(Intent.EXTRA_SUBJECT,"Join Zoom Meeting");
        intent.putExtra(Intent.EXTRA_TEXT,
                name + " is inviting you to a scheduled Zoom meeting.\nJoin Zoom Meeting http://zoom.in/share?id="+id);
        startActivity(Intent.createChooser(intent,"Share a link!"));
    }

    private void navigateToMeetings() {
        String meetingId= currentUser.getId().toString();
        Intent intent = new Intent(getActivity(), ConferenceCallActivity.class);
        intent.putExtra(Constants.ACTION_KEY_CHANNEL_NAME,meetingId);
        intent.putExtra(Constants.ACTION_KEY_USER_NAME,currentUser.getFullName());
        intent.putExtra(Constants.ACTION_KEY_ENCRYPTION_KEY,"");
        intent.putExtra(Constants.ACTION_KEY_ENCRYPTION_MODE,getString(R.string.encryption_mode_value));
        startActivity(intent);
    }

}