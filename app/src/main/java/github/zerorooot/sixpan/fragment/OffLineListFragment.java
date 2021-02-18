package github.zerorooot.sixpan.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;

import java.util.List;

import github.zerorooot.sixpan.activity.VideoActivity;
import github.zerorooot.sixpan.adapter.OffLineFileAdapter;
import github.zerorooot.sixpan.bean.OffLineBean;
import github.zerorooot.sixpan.databinding.FragmentOfflineListBinding;
import github.zerorooot.sixpan.viewModel.FileViewModel;

public class OffLineListFragment extends Fragment implements OffLineFileAdapter.OnItemClickListener {
    private FragmentOfflineListBinding binding;
    private FileViewModel fileViewModel;
    private OffLineFileAdapter adapter;
    private MutableLiveData<List<OffLineBean>> offLineLiveData;

    private OffLineListFragment() {
    }

    public static OffLineListFragment newInstance() {
        OffLineListFragment fragment = new OffLineListFragment();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentOfflineListBinding.inflate(inflater, container, false);
        fileViewModel = new ViewModelProvider(requireActivity(), new SavedStateViewModelFactory(requireActivity().getApplication(), this)).get(FileViewModel.class);
        offLineLiveData = fileViewModel.getOffLineLiveData();
        fileViewModel.getOffLineList();
        binding.offlineSwipe.setRefreshing(true);

        return binding.getRoot();
    }


    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        adapter = new OffLineFileAdapter();
        adapter.setOnItemClickListener(this);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(requireContext());
        binding.offlineRecyclerView.setLayoutManager(linearLayoutManager);
        binding.offlineRecyclerView.setAdapter(adapter);

        offLineLiveData.observe(getViewLifecycleOwner(), offLineBeans -> {
            if (binding.offlineSwipe.isRefreshing()) {
                binding.offlineSwipe.setRefreshing(false);
            }
            adapter.submitList(offLineBeans);
        });

        binding.offlineSwipe.setOnRefreshListener(() -> {
            fileViewModel.getOffLineList();
        });

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                OffLineBean offLineBean = offLineLiveData.getValue().get(viewHolder.getAdapterPosition());

                Snackbar snackbar = Snackbar.make(requireView(), "是否删除 " + offLineBean.getName() + " ?", BaseTransientBottomBar.LENGTH_SHORT)
                        .setAction("是", view -> {
                            fileViewModel.deleteOfflineFile(offLineBean);
                            List<OffLineBean> value = offLineLiveData.getValue();
                            value.remove(viewHolder.getAdapterPosition());
                            adapter.submitList(value);
                            adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                        });
                snackbar.addCallback(new Snackbar.Callback() {
                    @Override
                    public void onDismissed(Snackbar transientBottomBar, int event) {
                        adapter.notifyItemChanged(viewHolder.getAdapterPosition());
                    }
                });
                snackbar.show();


            }
        }).attachToRecyclerView(binding.offlineRecyclerView);
    }

    @Override
    public void onItemClick(View view, int position) {
        OffLineBean offLineBean = offLineLiveData.getValue().get(position);
        if (offLineBean.isDirectory()) {
            fileViewModel.setPath(offLineBean.getAccessPath());
            fileViewModel.getLiveData().setValue(null);
            fileViewModel.getFile(offLineBean.getAccessPath(), 0, fileViewModel.getLimitCount());
            fileViewModel.getViewPager2().setCurrentItem(0);
            return;
        }
        if (offLineBean.getFileMime().contains("video")) {
            Intent intent = new Intent(requireActivity(), VideoActivity.class);
            fileViewModel.downloadSingle(offLineBean.getAccessIdentity()).observe(this, s -> {
                intent.putExtra("address", s);
                intent.putExtra("title", offLineBean.getName());
                startActivity(intent);
            });
            return;
        }
        Toast.makeText(requireContext(), offLineBean.toString(), Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onMenuClick(View v, int position, float x, float y) {
        OffLineBean offLineBean = offLineLiveData.getValue().get(position);
        v.setOnCreateContextMenuListener((menu, v1, menuInfo) -> {

            MenuItem taskInfo = menu.add("查看任务详情");
            taskInfo.setOnMenuItemClickListener(e -> {
                MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(requireContext());
                materialAlertDialogBuilder.setTitle(offLineBean.getName())
                        .setMessage(offLineBean.toString())
                        .setNegativeButton("确定", null).show();
                return true;
            });

            MenuItem deleteCurrent = menu.add("删除当前任务");
            deleteCurrent.setOnMenuItemClickListener(e -> {
                fileViewModel.deleteOfflineFile(offLineBean);

                List<OffLineBean> value = offLineLiveData.getValue();
                value.remove(position);
                adapter.submitList(value);
                adapter.notifyItemRemoved(position);

                return true;
            });

            MenuItem deleteComplete = menu.add("删除完成任务");
            deleteComplete.setOnMenuItemClickListener(e -> {
                fileViewModel.deleteCompleteOffline();

                return true;
            });
        });

        v.showContextMenu(x, y);

    }
}