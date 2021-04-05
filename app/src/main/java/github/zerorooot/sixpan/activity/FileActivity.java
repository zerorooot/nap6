package github.zerorooot.sixpan.activity;


import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import github.zerorooot.sixpan.R;
import github.zerorooot.sixpan.customizeActivity.SingleDownloadBroadcastReceiver;
import github.zerorooot.sixpan.databinding.ActivityFileBinding;
import github.zerorooot.sixpan.fragment.AboutMeFragment;
import github.zerorooot.sixpan.fragment.FileFragment;
import github.zerorooot.sixpan.fragment.OffLineListAndDownloadFragment;
import github.zerorooot.sixpan.util.SharedPreferencesUtil;
import github.zerorooot.sixpan.viewModel.FileViewModel;

public class FileActivity extends AppCompatActivity {
    private ActivityFileBinding binding;
    private boolean mBackKeyPressed = false;
    private Fragment currentFragment;
    private final FragmentManager fm = getSupportFragmentManager();
    private FileFragment fileFragment;
    private OffLineListAndDownloadFragment offLineListAndDownloadFragment;
    private AboutMeFragment aboutMeFragment;
    private final BroadcastReceiver singleDownloadBroadcastReceiver = new SingleDownloadBroadcastReceiver();

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFileBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        BottomNavigationView bottomNavigationView = binding.bottomNavigationView;


        Intent intent = getIntent();
        String token = intent.getStringExtra("token");
        String externalLink = null;
        //通过分享启动
        if (Objects.isNull(token)) {
            token = new SharedPreferencesUtil(getBaseContext()).get().getToken();
            externalLink = getIntentData(intent);
        }

        FileViewModel fileViewModel = new ViewModelProvider(this, new SavedStateViewModelFactory(getApplication(), this)).get(FileViewModel.class);
        fileViewModel.setToken(token);
        fileViewModel.setBottomNavigationView(bottomNavigationView);

        preventFragmentOverlaps(savedInstanceState);

        //左上角返回
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        setBottomNavigationViewSelected(bottomNavigationView);

        //外部分享来的链接
        if (Objects.nonNull(externalLink)) {
            setExtraMessage(externalLink);
        }



        IntentFilter filter = new IntentFilter();
        filter.addAction(SingleDownloadBroadcastReceiver.TAG);
        registerReceiver(singleDownloadBroadcastReceiver, filter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(singleDownloadBroadcastReceiver);
    }

    /**
     * 设置bottomNavigationView跳转
     *
     * @param bottomNavigationView
     */
    @SuppressWarnings("all")
    private void setBottomNavigationViewSelected(BottomNavigationView bottomNavigationView) {
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.fileFragment:
                    fm.beginTransaction().hide(currentFragment).show(fileFragment).commit();
                    currentFragment = fileFragment;
                    return true;
                case R.id.offLineListAndDownloadFragment:
                    fm.beginTransaction().hide(currentFragment).show(offLineListAndDownloadFragment).commit();
                    currentFragment = offLineListAndDownloadFragment;
                    return true;
                case R.id.aboutMeFragment:
                    fm.beginTransaction().hide(currentFragment).show(aboutMeFragment).commit();
                    //刷新离线配额
                    aboutMeFragment.setOfflineQuota();
                    currentFragment = aboutMeFragment;
                    return true;
            }
            return false;
        });
        //双击返回顶部
        bottomNavigationView.setOnNavigationItemReselectedListener(item -> {
            if (item.getItemId() == R.id.fileFragment) {
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
        });
    }

    /**
     * 防止fragment重叠
     *
     * @param savedInstanceState savedInstanceState
     */
    private void preventFragmentOverlaps(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            fileFragment = new FileFragment();
            currentFragment = fileFragment;
            offLineListAndDownloadFragment = new OffLineListAndDownloadFragment();
            aboutMeFragment = new AboutMeFragment();
            fm.beginTransaction().add(R.id.fragment2, aboutMeFragment, "3").hide(aboutMeFragment).commit();
            fm.beginTransaction().add(R.id.fragment2, offLineListAndDownloadFragment, "2").hide(offLineListAndDownloadFragment).commit();
            fm.beginTransaction().add(R.id.fragment2, fileFragment, "1").commit();
        } else {
            fileFragment = (FileFragment) getSupportFragmentManager().getFragment(savedInstanceState, "1");
            offLineListAndDownloadFragment = (OffLineListAndDownloadFragment) getSupportFragmentManager().getFragment(savedInstanceState, "2");
            aboutMeFragment = (AboutMeFragment) getSupportFragmentManager().getFragment(savedInstanceState, "3");
            currentFragment = getSupportFragmentManager().getFragment(savedInstanceState, "4");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            int selectedItemId = binding.bottomNavigationView.getSelectedItemId();
            if (selectedItemId != R.id.fileFragment) {
                runOnUiThread(() -> {
                    fileFragment.showBottomNavigationView();
                });
                binding.bottomNavigationView.setSelectedItemId(R.id.fileFragment);
            } else {
                fileFragment.onBackPressed(fileFragment.backPressedCallback);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public void onBackPressed() {
        if (binding.bottomNavigationView.getSelectedItemId() != R.id.fileFragment) {
            fileFragment.showBottomNavigationView();
            binding.bottomNavigationView.setSelectedItemId(R.id.fileFragment);
        } else {
            fileFragment.onBackPressed(fileFragment.backPressedCallback);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        fm.putFragment(outState, "1", fileFragment);
        fm.putFragment(outState, "2", offLineListAndDownloadFragment);
        fm.putFragment(outState, "3", aboutMeFragment);
        fm.putFragment(outState, "4", currentFragment);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        String intentData = getIntentData(intent);
        setExtraMessage(intentData);
    }

    private void setExtraMessage(String externalLink) {
        if (currentFragment != offLineListAndDownloadFragment) {
            fm.beginTransaction().hide(currentFragment).show(offLineListAndDownloadFragment).commit();
            currentFragment = offLineListAndDownloadFragment;
            binding.bottomNavigationView.setSelectedItemId(R.id.offLineListAndDownloadFragment);
        }
        offLineListAndDownloadFragment.setExternalLink(externalLink);
    }

    private String getIntentData(Intent intent) {
        //android.intent.action.SEND   extra-text
        //android.intent.action.VIEW   getDataString
        if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            return intent.getDataString();
        }
        return intent.getStringExtra(Intent.EXTRA_TEXT);
    }

}
