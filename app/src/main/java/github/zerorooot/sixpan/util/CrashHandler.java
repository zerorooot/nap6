package github.zerorooot.sixpan.util;


import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.hutool.core.date.DateUtil;
import github.zerorooot.sixpan.BuildConfig;

public class CrashHandler implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "CrashHandler";
    private Context mContext;
    private static final CrashHandler mInstance = new CrashHandler();
    private final String path = "/sdcard/crash/";

    private CrashHandler() {
    }

    /**
     * 单例模式，保证只有一个CrashHandler实例存在
     */
    public static CrashHandler getInstance() {
        return mInstance;
    }

    /**
     * 异常发生时，系统回调的函数，我们在这里处理一些操作
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        showCrashToast();
        saveCrashReport2SD(ex);
    }

    /**
     * 为我们的应用程序设置自定义Crash处理
     */
    public void init(Context context) {
        mContext = context;
        Thread.setDefaultUncaughtExceptionHandler(this);
    }


    private void showCrashToast() {
        new Thread(() -> {
            Looper.prepare();
            String message = "程序出现异常,请将 %s 位置下开头为 %s 崩溃日志发送给开发者";
            Toast.makeText(getInstance().mContext, String.format(message, path, BuildConfig.APPLICATION_ID),
                    Toast.LENGTH_LONG).show();
            Looper.loop();
        }).start();
    }

    /**
     * 获取一些简单的信息,软件版本，手机版本，型号等信息存放在LinkedHashMap中
     */
    private String obtainSimpleInfo() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put("品牌", Build.BRAND);
        map.put("型号", Build.MODEL);
        map.put("SDK版本", String.valueOf(Build.VERSION.SDK_INT));
        map.put("versionCode", BuildConfig.VERSION_NAME);
        map.put("crash时间", DateUtil.now());

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            sb.append(key).append(" = ").append(value).append("\n");
        }
        return sb.toString();
    }


    /**
     * 获取系统未捕捉的错误信息
     */
    private String obtainExceptionInfo(Throwable throwable) {
        StringWriter mStringWriter = new StringWriter();
        PrintWriter mPrintWriter = new PrintWriter(mStringWriter);
        throwable.printStackTrace(mPrintWriter);
        mPrintWriter.close();

        Log.e(TAG, mStringWriter.toString());
        return mStringWriter.toString();
    }

    /**
     * 保存获取的 软件信息，设备信息和出错信息保存在SDcard中
     *
     * @param ex
     */
    private void saveCrashReport2SD(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        sb.append(obtainSimpleInfo());
        sb.append(obtainExceptionInfo(ex));

        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            try {
                String fileName = dir.toString() + File.separator + BuildConfig.APPLICATION_ID + "-" + DateUtil.now().replace(" ", "-") + ".txt";
                FileOutputStream fos = new FileOutputStream(fileName);
                fos.write(sb.toString().getBytes());
                fos.flush();
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

