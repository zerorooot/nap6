package github.zerorooot.sixpan.activity;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Slide;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;
import androidx.viewpager2.adapter.FragmentStateAdapter;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;


import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;


import java.util.Timer;
import java.util.TimerTask;

import github.zerorooot.sixpan.R;
import github.zerorooot.sixpan.databinding.ActivityFileBinding;
import github.zerorooot.sixpan.fragment.AboutMeFragment;
import github.zerorooot.sixpan.fragment.FileFragment;
import github.zerorooot.sixpan.fragment.OffLineListAndDownloadFragment;
import github.zerorooot.sixpan.viewModel.FileViewModel;

public class FileActivity extends AppCompatActivity implements FileFragment.RecyclerViewOnScrollListener {
    private ActivityFileBinding binding;
    private FileFragment fileFragment;
    private boolean mBackKeyPressed = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        Intent intent = getIntent();
        String token = intent.getStringExtra("token");
        FileViewModel fileViewModel = new ViewModelProvider(this, new SavedStateViewModelFactory(getApplication(), this)).get(FileViewModel.class);
        fileViewModel.setToken(token);
        fileViewModel.setViewPager2(binding.fileActivityViewpager2);
        fileFragment = FileFragment.newInstance();

        fileFragment.setRecyclerViewOnScrollListener(this);
        //禁止左右滚动
        binding.fileActivityViewpager2.setUserInputEnabled(false);


        binding.fileActivityViewpager2.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return fileFragment;
                    case 1:
                        return OffLineListAndDownloadFragment.newInstance();
                    case 2:
                        return new AboutMeFragment();
                }
                return fileFragment;
            }

            @Override
            public int getItemCount() {
                return 3;
            }
        });

        //左上角返回
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //double click back to top
        binding.fileActivityTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    if (!mBackKeyPressed) {
                        mBackKeyPressed = true;
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                mBackKeyPressed = false;
                            }
                        }, 3000);
                    } else {
                        fileFragment.binding.recycleView.smoothScrollToPosition(0);
                    }
                }
            }
        });

        new TabLayoutMediator(binding.fileActivityTabLayout, binding.fileActivityViewpager2, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setIcon(R.drawable.ic_baseline_folder_about_me_24);
                    tab.setText("文件");
                    break;
                case 1:
                    tab.setIcon(R.drawable.ic_baseline_cloud_download_24);
                    tab.setText("离线");
                    break;
                case 2:
                    tab.setIcon(R.drawable.ic_baseline_face_24);
                    tab.setText("我的");
                    break;
            }
        }).attach();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (binding.fileActivityViewpager2.getCurrentItem() != 0) {
                binding.fileActivityViewpager2.setCurrentItem(0);
            } else {
                fileFragment.onBackPressed(fileFragment.backPressedCallback);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBackPressed() {
        if (binding.fileActivityViewpager2.getCurrentItem() != 0) {
            binding.fileActivityViewpager2.setCurrentItem(0);
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void scrollListener(RecyclerView recyclerView, int dx, int dy) {
        binding.fileActivityTabLayout.clearAnimation();
        if (dy > 0) {
            binding.fileActivityTabLayout.animate()
                    .alpha(0f)
                    .setDuration(50)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            binding.fileActivityTabLayout.setVisibility(View.GONE);
                        }
                    });
        } else {
            binding.fileActivityTabLayout.animate()
                    .alpha(1f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            binding.fileActivityTabLayout.setVisibility(View.VISIBLE);
                        }
                    });
        }
    }


}
