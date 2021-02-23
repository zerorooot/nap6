package github.zerorooot.sixpan.customizeActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.shuyu.gsyvideoplayer.utils.CommonUtil;
import com.shuyu.gsyvideoplayer.utils.Debuger;
import com.shuyu.gsyvideoplayer.utils.GSYVideoType;
import com.shuyu.gsyvideoplayer.utils.OrientationUtils;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.unit.DataSizeUtil;
import cn.hutool.core.io.unit.DataUnit;
import github.zerorooot.sixpan.R;

public class MyGSYVideoPlayer extends StandardGSYVideoPlayer {
    private TextView mMoreScale;
    private TextView switchSpeed;
    //记住切换数据源类型
    private int mType = 0;


    long forwardRewindIncrementMs = 15000;
    private TextView netSpeedTextViewPortrait;
    private TextView netSpeedTextViewLandscape;
    private TextView batteryTextView;
    private TextView timeTextView;

    private OrientationUtils orientationUtils;

    private Timer timer;


    private TextView dlanTextView;
    public MyGSYVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextView getDlanTextView() {
        return dlanTextView;
    }


    @Override
    protected void init(Context context) {
        super.init(context);
        //恢复默认播放模式
        GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT);
        initView();
    }


    private void initView() {
        dlanTextView = findViewById(R.id.dlanTextView);
        netSpeedTextViewPortrait = findViewById(R.id.netSpeedTextViewPortrait);
        netSpeedTextViewLandscape = findViewById(R.id.netSpeedTextViewLandscape);
        batteryTextView = findViewById(R.id.batteryTextView);
        timeTextView = findViewById(R.id.timeTextView);

        mMoreScale = findViewById(R.id.moreScale);
        switchSpeed = findViewById(R.id.switchSpeed);
        //切换清晰度
        mMoreScale.setOnClickListener(v -> {
            if (!mHadPlay) {
                return;
            }
            if (mType == 0) {
                mType = 1;
            } else if (mType == 1) {
                mType = 2;
            } else if (mType == 2) {
                mType = 3;
            } else if (mType == 3) {
                mType = 4;
            } else if (mType == 4) {
                mType = 0;
            }
            resolveTypeUI();
        });

        switchSpeed.setOnClickListener(v -> {
            v.setOnCreateContextMenuListener((menu, v1, menuInfo) -> {
                MenuItem speed5 = menu.add("× 0.5");
                speed5.setOnMenuItemClickListener(e -> {
                    getCurrentPlayer().setSpeed(0.5f, true);
                    switchSpeed.setText("0.5 倍速");
                    return true;
                });
                MenuItem speed1 = menu.add("× 1");
                speed1.setOnMenuItemClickListener(e -> {
                    getCurrentPlayer().setSpeed(1f, true);
                    switchSpeed.setText("速度");
                    return true;
                });
                MenuItem speed15 = menu.add("× 1.5");
                speed15.setOnMenuItemClickListener(e -> {
                    getCurrentPlayer().setSpeed(1.5f, true);
                    switchSpeed.setText("1.5 倍速");
                    return true;
                });
                MenuItem speed2 = menu.add("× 2");
                speed2.setOnMenuItemClickListener(e -> {
                    getCurrentPlayer().setSpeed(2f, true);
                    switchSpeed.setText("2 倍速");
                    return true;
                });
            });
            v.showContextMenu(v.getX(), v.getY());
        });
    }

    @Override
    public int getLayoutId() {
        return R.layout.video_layout_preview;
    }

    @Override
    protected void setStateAndUi(int state) {
        super.setStateAndUi(state);
        setBatteryAndTime();
    }


    @Override
    protected void showDragProgressTextOnSeekBar(boolean fromUser, int progress) {
        super.showDragProgressTextOnSeekBar(fromUser, progress);
        setBatteryAndTime();
    }

    private void setBatteryAndTime() {
        //电池
        Intent batteryStatus = getContext().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
        int batteryPct = level * 100 / scale;
        batteryTextView.setText(batteryPct + "%");
        //时间
        timeTextView.setText(DateUtil.format(new Date(), "HH:mm"));
    }

    @Override
    protected void changeUiToPlayingBufferingShow() {
        super.changeUiToPlayingBufferingShow();
        showNetSpeed();
    }

    @Override
    public void startPlayLogic() {
        super.startPlayLogic();
        showNetSpeed();
    }

    private void showNetSpeed() {
        if (orientationUtils.getScreenType() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            netSpeedTextViewPortrait.setVisibility(VISIBLE);
        } else {
            netSpeedTextViewLandscape.setVisibility(VISIBLE);
        }

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).post(() -> {
                    netSpeedTextViewPortrait.setText(DataSizeUtil.format(getNetSpeed()) + "/s");
                    netSpeedTextViewLandscape.setText(DataSizeUtil.format(getNetSpeed()) + "/s");
                });
            }
        }, 0, 800);
    }

    @Override
    protected void changeUiToPlayingShow() {
        super.changeUiToPlayingShow();
        netSpeedTextViewPortrait.setVisibility(GONE);
        netSpeedTextViewLandscape.setVisibility(GONE);
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    public void setOrientationUtils(OrientationUtils orientationUtils) {
        this.orientationUtils = orientationUtils;
    }


    @Override
    protected void touchSurfaceMove(float deltaX, float deltaY, float y) {
        int curWidth = 0;
        int curHeight = 0;
        if (getActivityContext() != null) {
            curWidth = CommonUtil.getCurrentScreenLand((Activity) getActivityContext()) ? mScreenHeight : mScreenWidth;
            curHeight = CommonUtil.getCurrentScreenLand((Activity) getActivityContext()) ? mScreenWidth : mScreenHeight;
        }
        if (mChangePosition) {
            int totalTimeDuration = getDuration();
            mSeekTimePosition = (int) (mDownPosition + (deltaX * totalTimeDuration / curWidth) / mSeekRatio);
            if (mSeekTimePosition > totalTimeDuration)
                mSeekTimePosition = totalTimeDuration;
            String seekTime = CommonUtil.stringForTime(mSeekTimePosition);
            String totalTime = CommonUtil.stringForTime(totalTimeDuration);
            showProgressDialog(deltaX, seekTime, mSeekTimePosition, totalTime, totalTimeDuration);
        } else if (mChangeVolume) {
            deltaY = -deltaY;
            int max = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int deltaV = (int) (max * deltaY / curHeight);

            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mGestureDownVolume + deltaV, 0);

            int volumePercent = (int) (mGestureDownVolume * 100 / max + deltaY * 100 / curHeight);

            showVolumeDialog(-deltaY, volumePercent);
        } else if (mBrightness) {
            if (Math.abs(deltaY) > mThreshold) {
                float percent = (-deltaY / curHeight);
                onBrightnessSlide(percent);
                mDownY = y;
            }
        }
    }

    /**
     * 显示比例
     * 注意，GSYVideoType.setShowType是全局静态生效，除非重启APP。
     */
    private void resolveTypeUI() {
        if (!mHadPlay) {
            return;
        }
        if (mType == 1) {
            mMoreScale.setText("16:9");
            GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_16_9);
        } else if (mType == 2) {
            mMoreScale.setText("4:3");
            GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_4_3);
        } else if (mType == 3) {
            mMoreScale.setText("全屏");
            GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_FULL);
        } else if (mType == 4) {
            mMoreScale.setText("拉伸");
            GSYVideoType.setShowType(GSYVideoType.SCREEN_MATCH_FULL);
        } else if (mType == 0) {
            mMoreScale.setText("默认");
            GSYVideoType.setShowType(GSYVideoType.SCREEN_TYPE_DEFAULT);
        }
        changeTextureViewShowType();
        if (mTextureView != null)
            mTextureView.requestLayout();
    }

    @Override
    public void touchDoubleUp(MotionEvent event) {
        float x = event.getX();
        int screenWidth = mScreenHeight;

        //竖屏
        if (orientationUtils.getScreenType() == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
            screenWidth = mScreenWidth;
        }

        if (x <= screenWidth * 0.3) {
            //快退
            forwardOrRewind(forwardRewindIncrementMs * (-1));
        }

        if (x > screenWidth * 0.3 && x < screenWidth * 0.6) {
            if (!mHadPlay) {
                return;
            }
            clickStartIcon();
        }
        if (x >= screenWidth * 0.6) {
            //快进
            forwardOrRewind(forwardRewindIncrementMs);
        }

    }

    public void forwardOrRewind(long time) {
        int totalTimeDuration = getDuration();

        mSeekTimePosition = (int) (getGSYVideoManager().getCurrentPosition() + time);
        if (mSeekTimePosition > totalTimeDuration) {
            mSeekTimePosition = totalTimeDuration;
        }
        String seekTime = CommonUtil.stringForTime(mSeekTimePosition);
        String totalTime = CommonUtil.stringForTime(totalTimeDuration);
        getGSYVideoManager().seekTo(mSeekTimePosition);


        new Handler(Looper.myLooper()).postDelayed(() -> {
            showProgressDialog(time, seekTime, mSeekTimePosition, totalTime, totalTimeDuration);
        }, 100);
        new Handler(Looper.myLooper()).postDelayed(this::dismissProgressDialog, 600);
    }


    public void playNext(String url, String title) {
        setUp(url, mCache, null, title, true);
        mTitleTextView.setText(title);
        startPlayLogic();
    }
}
