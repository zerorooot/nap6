package github.zerorooot.sixpan.fragment;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
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
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
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

        fileViewModel.getExternalLinkLiveDate().observe(getViewLifecycleOwner(), externalLink -> {
            String links = adapter.getLinks();
            if (Objects.nonNull(links) && !"".equals(links)) {
                links = links + "\n" + externalLink;
            } else {
                links = externalLink;
            }
            Set<String> collect = Arrays.stream(links.split("\n")).collect(Collectors.toSet());
            StringJoiner stringJoiner = new StringJoiner("\n");
            collect.forEach(stringJoiner::add);
            adapter.setExternalLink(stringJoiner.toString());
            binding.recyclerView.setAdapter(adapter);
            parseClick(stringJoiner.toString(), null);

            setDefaultOffLinePath();
        });

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

        swipeDelete();
    }

    private void swipeDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.END) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int adapterPosition = viewHolder.getAdapterPosition();
                if (adapterPosition == 0) {
                    adapter.notifyItemChanged(0);
                    return;
                }
                OffLineParse offLineParse = offLineParseLiveData.getValue().get(adapterPosition - 1);
                Snackbar make = Snackbar.make(requireView(), "是否删除 " + offLineParse.getName() + " ?", BaseTransientBottomBar.LENGTH_SHORT);
                make.setAction("是", view -> {
                    ArrayList<String> arrayList = new ArrayList<>(Arrays.asList(adapter.getLinks().split("\n")));
                    //remove
                    arrayList.remove(adapterPosition - 1);
                    StringJoiner stringJoiner = new StringJoiner("\n");
                    arrayList.forEach(stringJoiner::add);
                    adapter.setExternalLink(stringJoiner.toString());

                    binding.recyclerView.setAdapter(adapter);
                    offLineParseLiveData.getValue().remove(adapterPosition - 1);
                });
                //恢复
                make.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar snackbar, int event) {
                        adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                    }
                });
                make.setAnchorView(fileViewModel.getBottomNavigationView());
                make.show();
            }
        }).attachToRecyclerView(binding.recyclerView);
    }

    private void setDefaultOffLinePath() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String defaultOffLinePath = sharedPreferences.getString("offLinePath", "/");
        if ("/".equals(offLinePath)) {
            offLinePath = defaultOffLinePath;
            binding.offlinePath.setText(defaultOffLinePath);
        }
    }

    @Override
    public void parseClick(String links, String password) {
        String[] split = links.split("\n");
        List<OffLineParse> offLineParseList = new ArrayList<>();
        for (String s : split) {
            if (!"".equals(s)) {
                String link = s.replace(" ", "");
                OffLineParse parse = getOffLineParse(link, password);
                offLineParseList.add(parse);
            }
        }

        offLineParseLiveData.setValue(offLineParseList);


        for (int i = 0; i < offLineParseList.size(); i++) {
            int finalI = i;
            OffLineParse parse = offLineParseList.get(i);
            fileViewModel.offLineParse(parse.getTextLink(), parse.getPassword()).observe(getViewLifecycleOwner(), o -> {
                offLineParseLiveData.getValue().set(finalI, o);
                adapter.notifyItemChanged(finalI + 1);
            });
        }
        binding.recyclerView.scrollToPosition(0);
    }

    private OffLineParse getOffLineParse(String link, String password) {
        OffLineParse parse = new OffLineParse();
        if ("".equals(password)) {
            password = null;
        }

        //链接:https://xxxxx提取码:xxx
        if (link.startsWith("链接:")) {
            parse.setTextLink(link.substring("链接:".length(), link.indexOf("提取码:")));
            parse.setPassword(link.substring(link.indexOf("提取码:") + "提取码:".length(), link.indexOf("提取码:") + 8));
        } else {
            parse.setTextLink(link);
            parse.setPassword(password);
        }
        return parse;
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
        fileViewModel.showBottomNavigationView();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}