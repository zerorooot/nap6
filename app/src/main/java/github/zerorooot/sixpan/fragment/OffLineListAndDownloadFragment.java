package github.zerorooot.sixpan.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayoutMediator;

import github.zerorooot.sixpan.R;


public class OffLineListAndDownloadFragment extends Fragment {
//    private static OffLineListAndDownloadFragment instance = new OffLineListAndDownloadFragment();
//
//
//
//    public static OffLineListAndDownloadFragment newInstance() {
//        return instance;
//    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_offline_list_and_download, container, false);
        ViewPager2 viewPager2 = view.findViewById(R.id.fragment_offline_list_and_download_viewPager2);
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
                        return new OffLineDownloadFragment();
                    case 1:
                        return new OffLineListFragment();

                }
                return new OffLineDownloadFragment();
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
        return view;
    }

}