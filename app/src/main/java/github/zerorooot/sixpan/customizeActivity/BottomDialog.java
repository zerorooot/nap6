package github.zerorooot.sixpan.customizeActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import github.zerorooot.sixpan.R;
import github.zerorooot.sixpan.bean.FileBean;
import github.zerorooot.sixpan.viewModel.FileViewModel;
import lombok.Setter;
import me.shaohui.bottomdialog.BaseBottomDialog;

@Setter
public class BottomDialog extends BaseBottomDialog {
    private final FileViewModel fileViewModel;
    private FileBean currentFileBean;
    private int position;
    private BottomDialogInterface bottomDialogInterface = null;
    private final TextDialog textDialog = new TextDialog();


    public BottomDialog(FileViewModel fileViewModel) {
        this.fileViewModel = fileViewModel;
    }

    public interface BottomDialogInterface {
        void deleteFile(List<FileBean> fileBeanList, int position);

        void remove(View v, FileBean fileBean);

        void forcePlayVideo(FileBean fileBean);

        void forceReadText(FileBean fileBean, TextDialog textDialog);

        void forceViewImage(FileBean fileBean);

        void rename(Context c, FileBean fileBean);
    }

    @Override
    public int getLayoutRes() {
        return R.layout.bottom_dialog;
    }

    @Override
    public void bindView(View v) {
        TextView fileName = v.findViewById(R.id.bottm_dialog_file_name);
        fileName.setText(currentFileBean.getName());

        v.findViewById(R.id.mRlDelete).setOnClickListener(e -> {
            List<FileBean> fileBeanList = new ArrayList<>();
            fileBeanList.add(currentFileBean);
            bottomDialogInterface.deleteFile(fileBeanList, position);
            this.dismiss();
        });

        v.findViewById(R.id.mRlDownload).setOnClickListener(e -> {
            download(e);
            this.dismiss();
        });

        v.findViewById(R.id.mRlRemove).setOnClickListener(e -> {
            bottomDialogInterface.remove(v, currentFileBean);
            this.dismiss();
        });

        v.findViewById(R.id.mRlRename).setOnClickListener(e -> {
            //showRenameDialog(requireContext());
            bottomDialogInterface.rename(requireContext(), currentFileBean);
            this.dismiss();
        });

        v.findViewById(R.id.mRlFileInfo).setOnClickListener(e -> {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(requireContext());
            materialAlertDialogBuilder
                    .setTitle(currentFileBean.getName())
                    .setMessage(currentFileBean.toString())
                    .setPositiveButton("确定", null).show();
            this.dismiss();
        });

        v.findViewById(R.id.mRlVideo).setOnClickListener(e -> {
            bottomDialogInterface.forcePlayVideo(currentFileBean);
            this.dismiss();
        });
        v.findViewById(R.id.mRlPicture).setOnClickListener(e -> {
            bottomDialogInterface.forceViewImage(currentFileBean);
            this.dismiss();
        });

        v.findViewById(R.id.mRlText).setOnClickListener(e -> {
            bottomDialogInterface.forceReadText(currentFileBean, textDialog);
            this.dismiss();
        });


    }

    @Override
    public float getDimAmount() {
        return 0.4f;
    }


    private void download(View v) {
        ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);

        fileViewModel.download(currentFileBean,true).observe(requireActivity(), s -> {
            if (s == null) {
                return;
            }
            ClipData clip = ClipData.newPlainText("downloadSingle", s);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(v.getContext(), "下载链接已输出到剪贴板", Toast.LENGTH_SHORT).show();
        });
    }

}