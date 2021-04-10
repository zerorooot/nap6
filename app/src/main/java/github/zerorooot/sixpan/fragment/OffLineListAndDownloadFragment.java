package github.zerorooot.sixpan.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import java.util.Objects;

import github.zerorooot.sixpan.R;


public class OffLineListAndDownloadFragment extends Fragment {
    private String externalLink;
    private OffLineDownloadFragment offLineDownloadFragment;
    private ViewPager2 viewPager2;

    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
        if (Objects.isNull(offLineDownloadFragment)) {
            return;
        }
        if (viewPager2.getCurrentItem() == 1) {
            viewPager2.setCurrentItem(0);
        }
        offLineDownloadFragment.setExternalLink(externalLink);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_offline_list_and_download, container, false);
        viewPager2 = view.findViewById(R.id.fragment_offline_list_and_download_viewPager2);
        viewPager2.setAdapter(new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return 2;
            }

            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        offLineDownloadFragment = new OffLineDownloadFragment();
                        offLineDownloadFragment.setExternalLink(externalLink);
                        return offLineDownloadFragment;
                    case 1:
                        return new OffLineListFragment();

                }
                offLineDownloadFragment.setExternalLink(externalLink);
                return offLineDownloadFragment;
            }
        });

        new TabLayoutMediator(view.findViewById(R.id.fragment_offline_list_and_download_tabLayout), viewPager2, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setText("离线下载");
                    break;
                case 1:
                    tab.setText("离线列表");
                    break;

            }
        }).attach();

        boolean downloadAndListSwitch =  PreferenceManager.getDefaultSharedPreferences(requireContext()).getBoolean("downloadAndListSwitch", true);
        viewPager2.setUserInputEnabled(downloadAndListSwitch);

        return view;
    }


}