package github.zerorooot.sixpan.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import github.zerorooot.sixpan.adapter.OffLineDownloadAdapter;
import github.zerorooot.sixpan.bean.OffLineParse;
import github.zerorooot.sixpan.databinding.FragmentOfflineDownloadBinding;
import github.zerorooot.sixpan.viewModel.FileViewModel;

public class OffLineDownloadFragment extends Fragment {
    private FragmentOfflineDownloadBinding binding;
    private FileViewModel fileViewModel;
    private MutableLiveData<List<OffLineParse>> offLineParseLiveData = new MutableLiveData<>();
    private OffLineDownloadAdapter adapter;

    private OffLineDownloadFragment() {
    }

    public static OffLineDownloadFragment newInstance() {
        OffLineDownloadFragment fragment = new OffLineDownloadFragment();
        return fragment;
    }


    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentOfflineDownloadBinding.inflate(inflater, container, false);
        fileViewModel = new ViewModelProvider(requireActivity(), new SavedStateViewModelFactory(requireActivity().getApplication(), this)).get(FileViewModel.class);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fileViewModel.getPathLiveDate().observe(getViewLifecycleOwner(), path -> {
            binding.offlinePath.setText(path);
        });

        binding.offlineNewByLinksButtonParse.setOnClickListener(this::parse);
        binding.offlineNewByLinksButtonDownload.setOnClickListener(this::buttonDownload);

        adapter = new OffLineDownloadAdapter();
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
        binding.recyclerView.setAdapter(adapter);

        binding.recyclerView.setLayoutManager(linearLayoutManager);

        offLineParseLiveData.observe(getViewLifecycleOwner(), o -> {
            adapter.submitList(o);
        });


    }

    private void buttonDownload(View e) {
        if (offLineParseLiveData.getValue() == null) {
            parse(e);
            return;
        }
        List<String> collect = offLineParseLiveData.getValue().stream().filter(s -> !s.isReady()).map(OffLineParse::getTextLink).collect(Collectors.toList());
        if (collect.size() != 0) {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(requireContext());
            materialAlertDialogBuilder.setMessage("有 " + collect.size() + " 个无法解析的内容，是否继续离线？")
                    .setNeutralButton("输出", (dialog, which) -> {
                        ClipboardManager clipboard = (ClipboardManager) e.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
                        StringBuilder stringBuilder = new StringBuilder();
                        collect.forEach(s -> {
                            stringBuilder.append(s).append("\n");
                        });
                        ClipData clip = ClipData.newPlainText("link", stringBuilder.toString().substring(0, stringBuilder.toString().length() - 1));
                        clipboard.setPrimaryClip(clip);
                        Toast.makeText(e.getContext(), "无法解析的内容已输出到剪贴板", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", (dialog, which) -> {
                        download();
                    })
                    .show();

            return;
        }
        download();
    }

    private void download() {
        List<String> collect = offLineParseLiveData.getValue().stream().filter(OffLineParse::isReady).map(OffLineParse::getHash).collect(Collectors.toList());
        String path = binding.offlinePath.getText().toString();
        if ("/".equals(path)) {
            MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(requireContext());
            materialAlertDialogBuilder.setMessage("是否将文件保存至根目录?")
                    .setNegativeButton("取消", null)
                    .setPositiveButton("确定", (dialog, which) -> {
                        fileViewModel.offLineDownload(path, collect);
                    })
                    .show();
            return;
        }

        fileViewModel.offLineDownload(path, collect).observe(getViewLifecycleOwner(), i -> {
            if (i == collect.size()) {
                Toast.makeText(getContext(), "离线下载成功", Toast.LENGTH_SHORT).show();
                offLineParseLiveData.postValue(null);
                adapter.notifyDataSetChanged();
                binding.offlineByLinksEditTextLinks.setText("");
                binding.offlineByLinksEditTextPassword.setText("");
            } else {
                Toast.makeText(getContext(), "离线下载失败~", Toast.LENGTH_SHORT).show();
            }
        });
    }


    private void parse(View view) {
//        binding.offlineNewByLinksButtonParse.setEnabled(false);
        String[] split = binding.offlineByLinksEditTextLinks.getText().toString().split("\n");
        List<OffLineParse> offLineParseList = new ArrayList<>();
        for (String s : split) {
            if (!"".equals(s)) {
                OffLineParse parse = new OffLineParse();
                parse.setTextLink(s.replace(" ", ""));
                offLineParseList.add(parse);
            }
        }

        offLineParseLiveData.postValue(offLineParseList);

        String password = null;
        if (!"".equals(binding.offlineByLinksEditTextPassword.getText().toString())) {
            password = binding.offlineByLinksEditTextPassword.getText().toString();
        }

        for (int i = 0; i < offLineParseList.size(); i++) {
            int finalI = i;
            fileViewModel.offLineParse(offLineParseList.get(i).getTextLink(), password).observe(getViewLifecycleOwner(), o -> {
                offLineParseLiveData.getValue().set(finalI, o);
                adapter.notifyItemChanged(finalI);
            });
        }

//        for (String s : linkList) {
//            fileViewModel.offLineParse(s, password).observe(getViewLifecycleOwner(), p -> {
//                offLineParseList.add(p);
//            });
//        }
//        offLineParseLiveData.postValue(offLineParseList);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}