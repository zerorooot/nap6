package github.zerorooot.sixpan.customizeActivity;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import androidx.core.app.NotificationManagerCompat;

public class SingleDownloadBroadcastReceiver extends BroadcastReceiver {
    public static String TAG = "SingleDownloadBroadcastReceiver";
    public static int ID = 110;
    public static String ERROR = "error";
    public static String OUTPUT = "outPut";
    public static String EXIT = "exit";


    @Override
    public void onReceive(Context context, Intent intent) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        String error = intent.getStringExtra(ERROR);
        String outPut = intent.getStringExtra(OUTPUT);
        String exit = intent.getStringExtra(EXIT);
        if (error != null) {
            ClipData clip = ClipData.newPlainText("ErrorSingleDownload", error);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "下载失败的文件名已输出到剪贴板", Toast.LENGTH_SHORT).show();
        }
        if (outPut != null) {
            ClipData clip = ClipData.newPlainText("SuccessSingleDownload", outPut);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "下载链接已输出到剪贴板", Toast.LENGTH_SHORT).show();
        }
        if (exit != null) {
            closeNotification(context);
        }

    }

    private void closeNotification(Context context) {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.cancel(ID);
    }
}
