package com.app.zoomclone.ui.contacts;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.app.zoomclone.R;
import com.app.zoomclone.call.activities.CallActivity;
import com.app.zoomclone.call.adapters.UsersAdapter;
import com.app.zoomclone.call.db.QbUsersDbManager;
import com.app.zoomclone.call.services.CallService;
import com.app.zoomclone.call.utils.Consts;
import com.app.zoomclone.call.utils.ErrorUtils;
import com.app.zoomclone.call.utils.PermissionsChecker;
import com.app.zoomclone.call.utils.SharedPrefsHelper;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.GenericQueryRule;
import com.quickblox.core.request.QBPagedRequestBuilder;
import com.quickblox.messages.services.QBPushManager;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import java.util.ArrayList;
import java.util.List;

public class ContactsFragment extends Fragment {

    private static final int PER_PAGE_SIZE_100 = 100;
    private static final String ORDER_RULE = "order";
    private static final String ORDER_DESC_UPDATED = "desc date updated_at";
    public static final String TOTAL_PAGES_BUNDLE_PARAM = "total_pages";

    private RecyclerView usersRecyclerview;
    private QBUser currentUser;
    private UsersAdapter usersAdapter;
    private int currentPage = 0;
    private Boolean isLoading = false;
    private Boolean hasNextPage = true;

    private QbUsersDbManager dbManager;
    private PermissionsChecker checker;
    private String TAG = ContactsFragment.class.getSimpleName();
    private SharedPrefsHelper sharedPrefsHelper;
    private ProgressDialog progressDialog;
    private SearchView searchView;
    private TextView textViewTitle;


    public View onCreateView(@NonNull LayoutInflater inflater,
            ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_contacts, container, false);


        usersRecyclerview = root.findViewById(R.id.list_select_users);
        searchView = root.findViewById(R.id.searchView);
        textViewTitle = getActivity().findViewById(R.id.tvTitle);

        textViewTitle.setText("Contacts");

        return root;
    }


    @Override
    public void onResume() {
        super.onResume();
        loadUsers();
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        sharedPrefsHelper = SharedPrefsHelper.getInstance();
        currentUser = SharedPrefsHelper.getInstance().getQbUser();
        dbManager = QbUsersDbManager.getInstance(getActivity());
        checker = new PermissionsChecker(getActivity());

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                filter(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                filter(s);
                return false;
            }
        });

    }

    private void filter(String query) {
        List<QBUser> temp = new ArrayList<>();

        for (QBUser  qbUser: dbManager.getAllUsers()){
            if (qbUser.getFullName().toLowerCase().contains(query.toLowerCase())){
                temp.add(qbUser);
            }
        }
        usersAdapter.updateList(temp);
    }

    private void initUsersList() {
        List<QBUser> currentOpponentsList = dbManager.getAllUsers();
        Log.d(TAG, "initUsersList currentOpponentsList= " + currentOpponentsList);
        currentOpponentsList.remove(sharedPrefsHelper.getQbUser());
        if (usersAdapter == null) {
            usersAdapter = new UsersAdapter(getActivity(), currentOpponentsList);

            usersRecyclerview.setLayoutManager(new LinearLayoutManager(getActivity()));
            usersRecyclerview.setAdapter(usersAdapter);
            usersRecyclerview.addOnScrollListener(new ScrollListener((LinearLayoutManager) usersRecyclerview.getLayoutManager()));
        } else {
            usersAdapter.updateUsersList(currentOpponentsList);
        }
    }


    private class ScrollListener extends RecyclerView.OnScrollListener {
        LinearLayoutManager layoutManager;

        ScrollListener(LinearLayoutManager layoutManager) {
            this.layoutManager = layoutManager;
        }

        @Override
        public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
            if (!isLoading && hasNextPage && dy > 0) {
                int visibleItemCount = layoutManager.getChildCount();
                int totalItemCount = layoutManager.getItemCount();
                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();

                boolean needToLoadMore = ((visibleItemCount * 2) + firstVisibleItem) >= totalItemCount;
                if (needToLoadMore) {
                    loadUsers();
                }
            }
        }
    }

    private void loadUsers() {
        isLoading = true;
        showProgressDialog(R.string.dlg_loading_opponents);
        currentPage +=1;
        ArrayList<GenericQueryRule> rules = new ArrayList<>();
        rules.add(new GenericQueryRule(ORDER_RULE, ORDER_DESC_UPDATED));

        QBPagedRequestBuilder qbPagedRequestBuilder = new QBPagedRequestBuilder();
        qbPagedRequestBuilder.setRules(rules);
        qbPagedRequestBuilder.setPerPage(PER_PAGE_SIZE_100);
        qbPagedRequestBuilder.setPage(currentPage);

        QBUsers.getUsers(qbPagedRequestBuilder).performAsync(new QBEntityCallback<ArrayList<QBUser>>() {
            @Override
            public void onSuccess(ArrayList<QBUser> qbUsers, Bundle bundle) {
                Log.d(TAG, "Successfully loaded users");
                dbManager.saveAllUsers(qbUsers, true);

                int totalPagesFromParams = (int) bundle.get(TOTAL_PAGES_BUNDLE_PARAM);
                if (currentPage >= totalPagesFromParams) {
                    hasNextPage = false;
                }

                if (currentPage == 1) {
                    initUsersList();
                } else {
                    usersAdapter.addUsers(qbUsers);
                }
                hideProgressDialog();
                isLoading = false;
            }

            @Override
            public void onError(QBResponseException e) {
                Log.d(TAG, "Error load users" + e.getMessage());
                hideProgressDialog();
                isLoading = false;
                currentPage -=1;
                showErrorSnackbar(R.string.loading_users_error, e, v -> loadUsers());
            }
        });
    }

    protected void showErrorSnackbar(@StringRes int resId, Exception e,
                                     View.OnClickListener clickListener) {
        View rootView = getActivity().getWindow().getDecorView().findViewById(android.R.id.content);
        if (rootView != null) {
            ErrorUtils.showSnackbar(rootView, resId, e, R.string.dlg_retry, clickListener);
        }
    }

    void showProgressDialog(@StringRes int messageId) {
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
        progressDialog.show();
    }

    void hideProgressDialog() {
        if (progressDialog != null) {
            try {
                progressDialog.dismiss();
            } catch (IllegalArgumentException ignored) {

            }
        }
    }

}