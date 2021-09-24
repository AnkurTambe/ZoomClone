package com.app.zoomclone.welcome;

import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import com.app.zoomclone.R;

public class WelcomePagerAdapter extends FragmentPagerAdapter {

    private Activity activity;


    public WelcomePagerAdapter(Activity activity, @NonNull FragmentManager fm) {
        super(fm);
        this.activity = activity;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        switch (position) {

            case 1:
                return WelcomeFragmentViewPager.newInstance(activity.getString(R.string.share_your_content),
                        activity.getString(R.string.they_see), R.drawable.welcome_2);
            case 2:
                return WelcomeFragmentViewPager.newInstance(activity.getString(R.string.manage_your_team),
                        activity.getString(R.string.send_texts), R.drawable.welcome_3);
            case 3:
                return WelcomeFragmentViewPager.newInstance(activity.getString(R.string.get_zooming),
                        activity.getString(R.string.work_anywhere), R.drawable.welcome_4);

            case 0:
            default:
                return WelcomeFragmentViewPager.newInstance(activity.getString(R.string.start_a_meeting),
                        activity.getString(R.string.start_or_join_video), R.drawable.welcome_1);
        }
    }

    @Override
    public int getCount() {
        return 4;
    }
}
