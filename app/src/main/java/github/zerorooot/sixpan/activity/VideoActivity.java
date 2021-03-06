package github.zerorooot.sixpan.activity;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.model.VideoOptionModel;
import com.shuyu.gsyvideoplayer.player.IjkPlayerManager;

import org.cybergarage.upnp.Device;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import github.zerorooot.sixpan.R;
import github.zerorooot.sixpan.adapter.VideoAllCallBackAdapter;
import github.zerorooot.sixpan.customizeActivity.MyGSYVideoPlayer;
import github.zerorooot.sixpan.customizeActivity.MyOrientationUtils;
import github.zerorooot.sixpan.dlan.DLNADeviceManager;
import github.zerorooot.sixpan.dlan.IController;
import github.zerorooot.sixpan.dlan.MultiPointController;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;


public class VideoActivity extends AppCompatActivity {
    MyGSYVideoPlayer videoPlayer;
    MyOrientationUtils orientationUtils;
    boolean pip;
    private String address;

    private DLNADeviceManager dlnaDeviceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);
        Intent intent = getIntent();
        address = intent.getStringExtra("address");
        String title = intent.getStringExtra("title");
        videoPlayer = findViewById(R.id.pre_video_player);

        //ijk关闭log
        IjkPlayerManager.setLogLevel(IjkMediaPlayer.IJK_LOG_SILENT);
        //accurate-seek
        VideoOptionModel videoOptionModel = new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_PLAYER, "enable-accurate-seek", 1);
        List<VideoOptionModel> list = new ArrayList<>();
        list.add(videoOptionModel);
        GSYVideoManager.instance().setOptionModelList(list);

//        设置是否自动横屏
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String autoLandscapeRotationVideoString = sharedPreferences.getString("autoLandscapeRotationVideo", "0");
        boolean autoLandscapeRotationVideo = false;
        switch (autoLandscapeRotationVideoString) {
            case "1":
                autoLandscapeRotationVideo = true;
                break;
            case "2":
                //电池
                Intent batteryStatus = getBaseContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
                //isCharging
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                autoLandscapeRotationVideo= status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
                break;
        }

        //设置旋转
        orientationUtils = new MyOrientationUtils(this, videoPlayer, autoLandscapeRotationVideo);
        videoPlayer.setUp(address, false, title);

        //增加title
        videoPlayer.getTitleTextView().setVisibility(View.VISIBLE);
        videoPlayer.getTitleTextView().setSelected(true);
        videoPlayer.setSeekRatio(10);
        //设置返回键
        videoPlayer.getBackButton().setVisibility(View.VISIBLE);
        videoPlayer.setOrientationUtils(orientationUtils);

        //设置默认横屏
        orientationUtils.resolveByClick(false);
        videoPlayer.getFullscreenButton().setOnClickListener(v -> {
            orientationUtils.resolveByClick(true);
        });


        //设置返回按键功能
        videoPlayer.getBackButton().setOnClickListener(v -> onBackPressed());
        videoPlayer.startPlayLogic();


        videoPlayer.setVideoAllCallBack(new VideoAllCallBackAdapter() {
            @Override
            public void onPrepared(String url, Object... objects) {
                int videoHeight = videoPlayer.getCurrentVideoHeight();
                int videoWidth = videoPlayer.getCurrentVideoWidth();
                //设置竖屏
                if (videoWidth < videoHeight) {
                    orientationUtils.resolveByClick(false);
                }
            }

            @Override
            public void onPlayError(String url, Object... objects) {
                super.onPlayError(url, objects);
                Toast.makeText(getBaseContext(), "播放失败~", Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        videoPlayer.getDlanTextView().setOnClickListener(this::startDlan);

    }

    private void startDlan(View v) {
        dlnaDeviceManager = new DLNADeviceManager();

        MutableLiveData<List<Device>> listMutableLiveData = searchDevice();
        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(this);
        listMutableLiveData.observe(this, deviceList -> {
            stopDiscovery(listMutableLiveData);

            materialAlertDialogBuilder
                    .setTitle("请选择设备")
                    .setItems(deviceList.stream().map(Device::getFriendlyName).toArray(String[]::new), (dialog, which) -> {
                        Device device = deviceList.get(which);
                        play(device);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    private void play(Device device) {
        IController mController = new MultiPointController();
        mController.play(device, address);
    }

    private MutableLiveData<List<Device>> searchDevice() {
        Toast.makeText(this, "开始搜索", Toast.LENGTH_SHORT).show();
        MutableLiveData<List<Device>> listMutableLiveData = new MutableLiveData<>();
        DLNADeviceManager.MediaRenderDeviceChangeListener mListener = new DLNADeviceManager.MediaRenderDeviceChangeListener() {
            @Override
            public void onStarted() {
            }

            @Override
            public void onDeviceListChanged(List<Device> list) {
                listMutableLiveData.postValue(list);
            }

            @Override
            public void onFinished() {
                if (Objects.nonNull(dlnaDeviceManager)) {
                    stopDiscovery(listMutableLiveData);
                }
            }
        };

        dlnaDeviceManager.startDiscovery(mListener);

        return listMutableLiveData;
    }

    private void stopDiscovery(MutableLiveData<List<Device>> listMutableLiveData) {
        dlnaDeviceManager.stopDiscovery();
        int size = 0;
        if (listMutableLiveData.getValue() != null) {
            size = listMutableLiveData.getValue().size();
        }
        Toast.makeText(VideoActivity.this, "搜索结束，共找到 " + size + " 个设备", Toast.LENGTH_SHORT).show();
        dlnaDeviceManager = null;
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        pip = isInPictureInPictureMode;
        View start = findViewById(R.id.start);
        if (isInPictureInPictureMode) {
            // Hide the full-screen UI (controls, etc.) while in picture-in-picture mode.
            findViewById(R.id.layout_top).setVisibility(View.GONE);
            findViewById(R.id.layout_bottom).setVisibility(View.GONE);
            if (start != null) {
                start.setVisibility(View.GONE);
            }

        } else {
            // Restore the full-screen UI.
            findViewById(R.id.layout_top).setVisibility(View.VISIBLE);
            findViewById(R.id.layout_bottom).setVisibility(View.VISIBLE);
            if (start != null) {
                start.setVisibility(View.VISIBLE);
            }

        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (pip) {
            GSYVideoManager.releaseAllVideos();
            if (orientationUtils != null) {
                orientationUtils.releaseListener();
            }
        }
        if (Objects.nonNull(dlnaDeviceManager)) {
            dlnaDeviceManager.stopDiscovery();
        }
    }

    @Override
    public void onUserLeaveHint() {
        pip = isInPictureInPictureMode();
        if (!isInPictureInPictureMode()) {
            enterPictureInPictureMode();
        }
    }

    @Override
    public void onBackPressed() {
        videoPlayer.setVideoAllCallBack(null);
        super.onBackPressed();
        if (Objects.nonNull(dlnaDeviceManager)) {
            dlnaDeviceManager.stopDiscovery();
        }
        GSYVideoManager.releaseAllVideos();
        if (orientationUtils != null) {
            orientationUtils.releaseListener();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        address = intent.getStringExtra("address");
        String title = intent.getStringExtra("title");
        videoPlayer.playNext(address, title);
    }
}