package org.tpoly.pendingsubmissionsviewer.adapter;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;

import org.tpoly.pendingsubmissionsviewer.fragments.SubmissionFragment;

public class AssignmentPagerAdapter extends FragmentPagerAdapter {
    final int PAGE_COUNT = 3;
    private final String[] tabTitles = new String[] { "Turned In", "Pending" };
    private final Context context;

    public AssignmentPagerAdapter(FragmentManager fm, final Context context) {
        super(fm);
        this.context = context;
    }

    @Override
    public int getCount() {
        return PAGE_COUNT;
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return SubmissionFragment.newInstance(position);
    }

    @Override
    public CharSequence getPageTitle(int position) {
        // Generate title based on item position
        return tabTitles[position];
    }
}
