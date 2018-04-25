/*
 *     LM videodownloader is a browser app for android, made to easily
 *     download videos.
 *     Copyright (C) 2018 Loremar Marabillas
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc.,
 *     51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package marabillas.loremar.lmvideodownloader.download_feature;

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.format.Formatter;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import marabillas.loremar.lmvideodownloader.LMvdActivity;
import marabillas.loremar.lmvideodownloader.LMvdFragment;
import marabillas.loremar.lmvideodownloader.R;
import marabillas.loremar.lmvideodownloader.Utils;

public class Downloads extends LMvdFragment implements LMvdActivity.OnBackPressedListener, Tracking, DownloadsInProgress.OnNumDownloadsInProgressChangeListener, DownloadsCompleted.OnNumDownloadsCompletedChangeListener, DownloadsInactive.OnNumDownloadsInactiveChangeListener {
    private TextView downloadSpeed;
    private TextView remaining;
    private Handler mainHandler;
    private Tracking tracking;

    private TabLayout tabs;
    private ViewPager pager;
    private DownloadsInProgress downloadsInProgress;
    private DownloadsCompleted downloadsCompleted;
    private DownloadsInactive downloadsInactive;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.downloads, container, false);

        final DrawerLayout layout = getActivity().findViewById(R.id.drawer);
        ImageView menu = view.findViewById(R.id.menuButton);
        menu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                layout.openDrawer(Gravity.START);
            }
        });

        downloadSpeed = view.findViewById(R.id.downloadSpeed);
        remaining = view.findViewById(R.id.remaining);

        getLMvdActivity().setOnBackPressedListener(this);

        mainHandler = new Handler(Looper.getMainLooper());
        tracking = new Tracking();

        tabs = view.findViewById(R.id.downloadsTabs);
        pager = view.findViewById(R.id.downloadsPager);

        tabs.addTab(tabs.newTab());
        tabs.addTab(tabs.newTab());
        tabs.addTab(tabs.newTab());

        pager.setAdapter(new PagerAdapter());
        pager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabs));
        tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                pager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });
        pager.setOffscreenPageLimit(2);//default is 1 which would make Inactive tab not diplay

        downloadsInProgress = new DownloadsInProgress();
        downloadsCompleted = new DownloadsCompleted();
        downloadsInactive = new DownloadsInactive();

        getFragmentManager().beginTransaction().add(pager.getId(), downloadsInProgress,
                "downloadsInProgress").commit();
        getFragmentManager().beginTransaction().add(pager.getId(), downloadsCompleted,
                "downloadsCompleted").commit();
        getFragmentManager().beginTransaction().add(pager.getId(), downloadsInactive,
                "downloadsInactive").commit();

        downloadsInProgress.setTracking(this);

        downloadsInProgress.setOnAddDownloadedVideoToCompletedListener(downloadsCompleted);
        downloadsInProgress.setOnAddDownloadItemToInactiveListener(downloadsInactive);
        downloadsInactive.setOnDownloadWithNewLinkListener(downloadsInProgress);

        downloadsInProgress.setOnNumDownloadsInProgressChangeListener(this);
        downloadsCompleted.setOnNumDownloadsCompletedChangeListener(this);
        downloadsInactive.setOnNumDownloadsInactiveChangeListener(this);

        return view;
    }

    @Override
    public void onDestroyView() {
        getFragmentManager().beginTransaction().remove(downloadsInProgress).commit();
        getFragmentManager().beginTransaction().remove(downloadsCompleted).commit();
        getFragmentManager().beginTransaction().remove(downloadsInactive).commit();
        downloadsInProgress = null;
        downloadsCompleted = null;
        downloadsInactive = null;

        super.onDestroyView();
    }

    @Override
    public void onBackpressed() {
        getLMvdActivity().getBrowserManager().unhideCurrentWindow();
        getFragmentManager().beginTransaction().remove(this).commit();
    }

    @Override
    public void onNumDownloadsInProgressChange() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TabLayout.Tab tab = tabs.getTabAt(0);
                if (tab != null) {
                    SpannableStringBuilder tabText = createStyledTabText(12, 13,
                            downloadsInProgress.getNumDownloadsInProgress(), "In Progress " +
                                    downloadsInProgress.getNumDownloadsInProgress());
                    tab.setText(tabText);
                }
            }
        });
    }

    @Override
    public void onNumDownloadsCompletedChange() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TabLayout.Tab tab = tabs.getTabAt(1);
                if (tab != null) {
                    SpannableStringBuilder tabText = createStyledTabText(10, 11,
                            downloadsCompleted.getNumDownloadsCompleted(), "Completed " +
                                    downloadsCompleted.getNumDownloadsCompleted());
                    tab.setText(tabText);
                }
            }
        });
    }

    @Override
    public void onNumDownloadsInactiveChange() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TabLayout.Tab tab = tabs.getTabAt(2);
                if (tab != null) {
                    SpannableStringBuilder tabText = createStyledTabText(9, 10,
                            downloadsInactive.getNumDownloadsInactive(), "Inactive " +
                                    downloadsInactive.getNumDownloadsInactive());
                    tab.setText(tabText);
                }
            }
        });
    }

    private SpannableStringBuilder createStyledTabText(int start, int end, int num, String text) {
        SpannableStringBuilder sb = new SpannableStringBuilder(text);
        ForegroundColorSpan fcs;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            fcs = new ForegroundColorSpan(getResources().getColor(R.color.green));
        } else {
            fcs = new ForegroundColorSpan(getResources().getColor(R.color.green, null));
        }
        sb.setSpan(fcs, start, end + num / 10, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
        return sb;
    }

    class Tracking implements Runnable {

        @Override
        public void run() {
            long downloadSpeedValue = DownloadManager.getDownloadSpeed();
            String downloadSpeedText = "Speed:" + Formatter.formatShortFileSize(getActivity(),
                    downloadSpeedValue) + "/s";

            downloadSpeed.setText(downloadSpeedText);

            if (downloadSpeedValue > 0) {
                long remainingMills = DownloadManager.getRemaining();
                String remainingText = "Remaining:" + Utils.getHrsMinsSecs(remainingMills);
                remaining.setText(remainingText);
            } else {
                remaining.setText(R.string.remaining_undefine);
            }

            if (getFragmentManager().findFragmentByTag("downloadsInProgress") != null) {
                downloadsInProgress.updateDownloadItem();
            }
            mainHandler.postDelayed(this, 1000);
        }
    }

    public void startTracking() {
        getActivity().runOnUiThread(tracking);
    }

    public void stopTracking() {
        mainHandler.removeCallbacks(tracking);
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                downloadSpeed.setText(R.string.speed_0);
                remaining.setText(R.string.remaining_undefine);
                if (getFragmentManager().findFragmentByTag("downloadsInProgress") != null) {
                    downloadsInProgress.updateDownloadItem();
                }
            }
        });
    }

    class PagerAdapter extends android.support.v4.view.PagerAdapter {
        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {
            switch (position) {
                case 0:
                    return downloadsInProgress;
                case 1:
                    return downloadsCompleted;
                case 2:
                    return downloadsInactive;
                default:
                    return downloadsInProgress;
            }
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return ((Fragment) object).getView() == view;
        }
    }
}
