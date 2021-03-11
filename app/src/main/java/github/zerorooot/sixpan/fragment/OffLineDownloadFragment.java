package github.zerorooot.sixpan.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import github.zerorooot.sixpan.adapter.OffLineDownloadAdapter;
import github.zerorooot.sixpan.bean.OffLineParse;
import github.zerorooot.sixpan.databinding.FragmentOfflineDownloadBinding;
import github.zerorooot.sixpan.viewModel.FileViewModel;

public class OffLineDownloadFragment extends Fragment implements OffLineDownloadAdapter.ClickInterface {
    private FragmentOfflineDownloadBinding binding;
    private FileViewModel fileViewModel;
    private final MutableLiveData<List<OffLineParse>> offLineParseLiveData = new MutableLiveData<>();
    private OffLineDownloadAdapter adapter;
    private String offLinePath = "/";

    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentOfflineDownloadBinding.inflate(inflater, container, false);
        fileViewModel = new ViewModelProvider(requireActivity(), new SavedStateViewModelFactory(requireActivity().getApplication(), this)).get(FileViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new OffLineDownloadAdapter();
        adapter.setClickInterface(this);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerView.setAdapter(adapter);

        binding.recyclerView.setLayoutManager(linearLayoutManager);

        offLineParseLiveData.observe(getViewLifecycleOwner(), o -> {
            adapter.submitList(o);
        });

        fileViewModel.getPathLiveDate().observe(getViewLifecycleOwner(), path -> {
            offLinePath = path;
            binding.offlinePath.setText(path);
        });

        adapter.setOffLineSwipe(binding.offLineSwipe);
    }


    @Override
    public void parseClick(String links, String password) {
        String[] split = links.split("\n");
        List<OffLineParse> offLineParseList = new ArrayList<>();
        for (String s : split) {
            if (!"".equals(s)) {
                OffLineParse parse = new OffLineParse();
                parse.setTextLink(s.replace(" ", ""));
                offLineParseList.add(parse);
            }
        }

        offLineParseLiveData.setValue(offLineParseList);


        if ("".equals(password)) {
            password = null;
        }

        for (int i = 0; i < offLineParseList.size(); i++) {
            int finalI = i;
            fileViewModel.offLineParse(offLineParseList.get(i).getTextLink(), password).observe(getViewLifecycleOwner(), o -> {
                offLineParseLiveData.getValue().set(finalI, o);
                adapter.notifyItemChanged(finalI + 1);
            });
        }
        binding.recyclerView.scrollToPosition(0);
    }

    @Override
    public void downloadClick(TextInputEditText links, TextInputEditText password) {
        if (offLineParseLiveData.getValue() == null) {
            parseClick(links.getText().toString(), password.getText().toString());
            return;
        }
        List<String> collect = offLineParseLiveData.getValue().stream().filter(s -> !s.isReady()).map(OffLineParse::getTextLink).collect(Collectors.toList());
        if (collect.size() != 0) {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(requireContext());
            materialAlertDialogBuilder.setMessage("有 " + collect.size() + " 个无法解析的内容，是否继续离线？")
                    .setNeutralButton("输出", (dialog, which) -> {
                        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        StringBuilder stringBuilder = new StringBuilder();
                        collect.forEach(s -> {
                            stringBuilder.append(s).append("\n");
                        });
                        ClipData clip = ClipData.newPlainText("link", stringBuilder.toString().substring(0, stringBuilder.toString().length() - 1));
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(requireContext(), "无法解析的内容已输出到剪贴板", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", (dialog, which) -> {
                        checkRootPath(links, password);
                    })
                    .show();

        } else {
            checkRootPath(links, password);
        }
    }

    private void checkRootPath(TextInputEditText links, TextInputEditText password) {
        List<String> collect = offLineParseLiveData.getValue().stream().filter(OffLineParse::isReady).map(OffLineParse::getHash).collect(Collectors.toList());
        if ("/".equals(offLinePath)) {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(requireContext());
            materialAlertDialogBuilder.setMessage("是否将文件保存至根目录?")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", (dialog, which) -> {
                        download(offLinePath, collect, links, password);
                    })
                    .show();
            return;
        }
        download(offLinePath, collect, links, password);
    }

    private void download(String path, List<String> collect, TextInputEditText links, TextInputEditText password) {
        fileViewModel.offLineDownload(path, collect).observe(getViewLifecycleOwner(), i -> {
            if (i == collect.size()) {
                Toast.makeText(requireContext(), "离线下载成功", Toast.LENGTH_SHORT).show();
                offLineParseLiveData.setValue(null);
                adapter.notifyDataSetChanged();
                links.setText("");
                password.setText("");
                //刷新列表
                fileViewModel.getOffLineList();
            } else {
                Toast.makeText(requireContext(), "离线下载失败~", Toast.LENGTH_SHORT).show();
            }
        });
    }

}