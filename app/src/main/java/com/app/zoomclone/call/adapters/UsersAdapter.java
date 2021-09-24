package com.app.zoomclone.call.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.app.zoomclone.ProfileActivity;
import com.app.zoomclone.call.utils.UiUtils;
import com.app.zoomclone.R;
import com.quickblox.users.model.QBUser;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class UsersAdapter extends RecyclerView.Adapter<UsersAdapter.ViewHolder> {

    private Context context;
    List<QBUser> usersList;
    List<QBUser> selectedUsers;
    private SelectedItemsCountChangedListener selectedItemsCountChangedListener;


    public UsersAdapter(Context context, List<QBUser> usersList) {
        this.context = context;
        this.usersList = usersList;
        this.selectedUsers = new ArrayList<>();
    }


    public List<QBUser> getSelectedUsers() {
        return selectedUsers;
    }

    public void setSelectedItemsCountsChangedListener(SelectedItemsCountChangedListener selectedItemsCountChanged) {
        if (selectedItemsCountChanged != null) {
            this.selectedItemsCountChangedListener = selectedItemsCountChanged;
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(context).inflate(R.layout.item_opponents_list, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        QBUser user = usersList.get(position);
        holder.opponentName.setText(user.getFullName());

        if (user.getFullName() != null && !user.getFullName().trim().isEmpty()) {
            if (user.getFullName().trim().contains(" ")) {
                String[] eventName = user.getFullName().trim().split(" ");
                holder.tvShortName.setText(eventName[0].substring(0, 1).toUpperCase() + eventName[1].substring(0, 1).toUpperCase());
            } else if (user.getFullName().trim().length() > 2) {
                holder.tvShortName.setText(user.getFullName().trim().substring(0, 2).toUpperCase());
            } else {
                holder.tvShortName.setText(user.getFullName().trim().toUpperCase());
            }
            holder.tvShortName.setBackgroundDrawable(UiUtils.getColorCircleDrawable(user.getId()));
        }


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                context.startActivity(new Intent(context, ProfileActivity.class)
                .putExtra("position",user));
            }
        });

    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    public void updateUsersList(List<QBUser> usersList) {
        this.usersList = usersList;
        notifyDataSetChanged();
    }

    public void addUsers(List<QBUser> users) {
        for (QBUser user : users) {
            if (!usersList.contains(user)) {
                usersList.add(user);
            }
        }
        notifyDataSetChanged();
    }

    private void toggleSelection(QBUser qbUser) {
        if (selectedUsers.contains(qbUser)) {
            selectedUsers.remove(qbUser);
        } else {
            selectedUsers.add(qbUser);
        }
        notifyDataSetChanged();
    }

    public void updateList(List<QBUser> temp) {
        this.usersList = temp;
        notifyDataSetChanged();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView opponentIcon;
        TextView opponentName;
        TextView tvShortName;
        LinearLayout rootLayout;

        public ViewHolder(@NonNull View view) {
            super(view);
            opponentIcon = view.findViewById(R.id.image_opponent_icon);
            opponentName = view.findViewById(R.id.opponents_name);
            tvShortName = view.findViewById(R.id.tvShortName);
            rootLayout = view.findViewById(R.id.root_layout);
        }
    }

    public interface SelectedItemsCountChangedListener {
        void onCountSelectedItemsChanged(Integer count);
    }
}
