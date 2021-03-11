package github.zerorooot.sixpan.fragment;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.behavior.HideBottomViewOnScrollBehavior;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import github.zerorooot.sixpan.R;
import github.zerorooot.sixpan.activity.PictureActivity;
import github.zerorooot.sixpan.activity.VideoActivity;
import github.zerorooot.sixpan.adapter.FileAdapter;
import github.zerorooot.sixpan.bean.FileBean;
import github.zerorooot.sixpan.customizeActivity.BottomDialog;
import github.zerorooot.sixpan.customizeActivity.TextDialog;
import github.zerorooot.sixpan.databinding.FragmentFileBinding;
import github.zerorooot.sixpan.viewModel.FileViewModel;


public class FileFragment extends Fragment implements BottomDialog.BottomDialogInterface {
    public FragmentFileBinding binding;
    private FileAdapter adapter;
    private FileViewModel fileViewModel;
    private MutableLiveData<List<FileBean>> liveData;
    public final OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
        @Override
        public void handleOnBackPressed() {
            onBackPressed(this);
        }
    };
    private android.view.ActionMode actionMode = null;
    private SearchView searchView;
    private LinearLayoutManager linearLayoutManager;
    private ArrayList<FileBean> beSelectFileBean;
    private boolean search = false;
    private boolean mBackKeyPressed = false;

    private Menu menu;


    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentFileBinding.inflate(inflater, container, false);
        View view = binding.getRoot();
        fileViewModel = new ViewModelProvider(requireActivity(), new SavedStateViewModelFactory(requireActivity().getApplication(), this)).get(FileViewModel.class);
        //创建菜单
        setHasOptionsMenu(true);


        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fileViewModel.getFile("/", 0, fileViewModel.getLimitCount());
        binding.swipeRefreshLayout.setRefreshing(true);
        liveData = fileViewModel.getLiveData();

        adapter = new FileAdapter();


        BottomDialog dialog = new BottomDialog(fileViewModel);
        dialog.setBottomDialogInterface(this);

        linearLayoutManager = new LinearLayoutManager(requireContext());

        binding.recycleView.setLayoutManager(linearLayoutManager);

        //用于排序时定位
        fileViewModel.setPosition("/", linearLayoutManager);

        adapter.setOnItemClickListener(this::onClick);
        adapter.setOnItemLongClickListener(this::onLongClick);
        adapter.setDialog(dialog);

        adapter.setSupportFragmentManager(requireActivity().getSupportFragmentManager());
        binding.recycleView.setAdapter(adapter);


        liveData.observe(getViewLifecycleOwner(), this::liveDataObserve);

        binding.swipeRefreshLayout.setOnRefreshListener(this::swipeRefresh);

        binding.floatingAddActionButton.setOnClickListener(this::floatingAddActionButtonClick);

        binding.floatingCutActionButton.setOnClickListener(this::floatingCutActionButtonClick);


        binding.recycleView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                //dy > 0 向下滚动
                if (binding.floatingCutActionButton.getVisibility() == View.GONE) {
                    if (dy > 0 && binding.floatingAddActionButton.isShown()) {
                        binding.floatingAddActionButton.hide();
                    } else {
                        binding.floatingAddActionButton.show();
                    }
                }
                // LOAD MORE
                if (!recyclerView.canScrollVertically(1) && dy > 0 && !search) {
                    binding.swipeRefreshLayout.setRefreshing(true);
                    fileViewModel.setPosition(fileViewModel.getPath(), linearLayoutManager);
                    fileViewModel.getFile(fileViewModel.getPath(), adapter.getItemCount(), fileViewModel.getLimitCount());
                }
            }
        });

        //监听返回键
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);

        swipeDelete();
    }

    /**
     * 滑动删除
     */
    private void swipeDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.START | ItemTouchHelper.END) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                FileBean fileBean = liveData.getValue().get(viewHolder.getAdapterPosition());
                List<FileBean> fileBeanList = new ArrayList<>();
                fileBeanList.add(fileBean);
                Snackbar make = Snackbar.make(requireView(), "是否删除 " + fileBean.getName() + " ?", BaseTransientBottomBar.LENGTH_SHORT);
                make.setAction("是", view -> {
                    itemDelete(fileBeanList, viewHolder.getAdapterPosition());
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
        }).attachToRecyclerView(binding.recycleView);
    }

    private void liveDataObserve(List<FileBean> fileBean) {
        //证明证明清空了live data,为进入下一页做准备,但清空 live data会触发此观察，所以直接返回
        if (fileBean == null) {
            //gotoLogin();
            return;
        }
        adapter.submitList(fileBean);
        adapter.notifyItemChanged(adapter.getItemCount());

        String path = fileViewModel.getPath();
        binding.filePath.setText(path);

        if (!search) {
            fileViewModel.getFileListCache().put(path, fileBean);
            fileViewModel.scroll(binding.filePath.getText().toString(), binding.recycleView);
        }

        if (binding.swipeRefreshLayout.isRefreshing()) {
            binding.swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void swipeRefresh() {
        clearMenuItemIcon();

        if (search) {
            removeSearch();
        }

        if (actionMode != null) {
            actionMode.finish();
        }

        if (!binding.floatingAddActionButton.isShown()) {
            binding.floatingAddActionButton.show();
        }

        String path = binding.filePath.getText().toString();
        //remove cache
        Set<String> filePathSet = adapter.getCurrentList().stream().map(FileBean::getPath).collect(Collectors.toSet());
        fileViewModel.getFileListCache().keySet().removeAll(filePathSet);

        liveData.setValue(null);
        fileViewModel.getFile(path, 0, fileViewModel.getLimitCount());
        //adapter.notifyDataSetChanged();
        fileViewModel.setPosition(path, linearLayoutManager);

    }

    private void floatingCutActionButtonClick(View v) {
        if (Objects.nonNull(beSelectFileBean)) {
            String path = binding.filePath.getText().toString();
            fileViewModel.move(beSelectFileBean, path).observe(getViewLifecycleOwner(), fresh -> {
                binding.swipeRefreshLayout.setRefreshing(true);
                swipeRefresh();
            });
        }
        binding.floatingAddActionButton.setImageResource(R.drawable.ic_baseline_add_24);
        binding.floatingCutActionButton.setVisibility(View.GONE);
    }

    private void floatingAddActionButtonClick(View v) {
        if (binding.floatingCutActionButton.getVisibility() == View.GONE) {
            createNewFile(v.getContext(), v.findViewById(R.id.dialog_layout));
        } else {
            beSelectFileBean = null;
            binding.floatingAddActionButton.setImageResource(R.drawable.ic_baseline_add_24);
            binding.floatingCutActionButton.setVisibility(View.GONE);
        }
    }


    private void createNewFile(Context c, FrameLayout dialog) {
        View inflate = LayoutInflater.from(c).inflate(R.layout.material_dialog, dialog, false);
        TextInputLayout textInputLayout = inflate.findViewById(R.id.dialog_textInput);
        textInputLayout.setHint("文件名称");
        TextInputEditText editInputLayout = inflate.findViewById(R.id.dialog_editText);
        editInputLayout.setFocusable(true);
        editInputLayout.requestFocus();
        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(c);
        materialAlertDialogBuilder.setTitle("新建文件夹")
                .setView(inflate)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (dialog1, which) -> {
                    String newName = editInputLayout.getText().toString();
                    fileViewModel.createFolder(binding.filePath.getText().toString(), newName).observe(getViewLifecycleOwner(), fresh -> {
                        if (fresh) {
                            swipeRefresh();
                        }
                    });
                })
                .show();
    }


    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search_menu, menu);
        //支持菜单图标
        if (menu instanceof MenuBuilder) {
            ((MenuBuilder) menu).setOptionalIconsVisible(true);
        }
        this.menu = menu;

        searchView = (SearchView) menu.findItem(R.id.app_bar_search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return search(query);
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
    }

    private boolean search(String query) {
        search = true;
        binding.swipeRefreshLayout.setRefreshing(true);
        Objects.requireNonNull(((AppCompatActivity) requireActivity()).getSupportActionBar()).setTitle("搜索");
        String path = binding.filePath.getText().toString();
        fileViewModel.search(path, query).observe(getViewLifecycleOwner(), fileBean -> {
            liveData.postValue(fileBean);
            binding.swipeRefreshLayout.setRefreshing(false);
        });
        //阻止进一步处理
        searchView.onActionViewCollapsed();
        return true;
    }

    private void clearMenuItemIcon() {
        //排除搜索item
        for (int i = 0; i < menu.size() - 1; i++) {
            menu.getItem(i).setIcon(null);
        }
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        //清除图片
        clearMenuItemIcon();

        String path = binding.filePath.getText().toString();
        fileViewModel.setPosition(path, linearLayoutManager);

        List<FileBean> value = liveData.getValue();
        if (value == null || value.size() <= 1) {
            return true;
        }

        FileBean firstBean = value.get(0);
        FileBean secondBean = value.get(1);

        switch (item.getItemId()) {
            //处理左上角返回的逻辑在FileActivity,因为官方建议，且可能会存在不在此fragment里调用的情况，即点击后没反应
            case R.id.item_getAll:
                if (search) {
                    removeSearch();
                }
                binding.swipeRefreshLayout.setRefreshing(true);
                fileViewModel.getLiveData().postValue(null);
                fileViewModel.getAllFile(binding.filePath.getText().toString());
                return true;
            case R.id.item_name:
                orderByName(item, firstBean, secondBean);
                return true;
            case R.id.item_size:
                orderBySize(item, firstBean, secondBean);
                return true;
            case R.id.item_time:
                orderByTime(item, firstBean, secondBean);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void orderByTime(MenuItem item, FileBean firstBean, FileBean secondBean) {
        order(item, Comparator.comparing(FileBean::getAtime), firstBean.getAtime() > secondBean.getAtime());
    }

    private void orderByName(MenuItem item, FileBean firstBean, FileBean secondBean) {
        order(item, Comparator.comparing(FileBean::getName), firstBean.getName().compareTo(secondBean.getName()) > 0);
    }

    private void orderBySize(MenuItem item, FileBean firstBean, FileBean secondBean) {
        order(item, Comparator.comparing(FileBean::getSize), firstBean.getSize() > secondBean.getSize());
    }

    private void order(MenuItem item, Comparator<FileBean> comparator, boolean condition) {
        List<FileBean> collect;
        List<FileBean> value = liveData.getValue();

        if (condition) {
            collect = value.stream().sorted(comparator).collect(Collectors.toList());
            //imageView.setImageResource(R.drawable.ic_baseline_arrow_upward_24);
            item.setIcon(R.drawable.ic_baseline_arrow_upward_24);
        } else {
            collect = value.stream().sorted(comparator.reversed()).collect(Collectors.toList());
            //imageView.setImageResource(R.drawable.ic_baseline_arrow_downward_24);
            item.setIcon(R.drawable.ic_baseline_arrow_downward_24);
        }

//        item.setActionView(imageView);
        liveData.postValue(collect);
    }

    private void onClick(View view, int position) {
        FileBean fileBean;
        try {
            List<FileBean> value = adapter.getCurrentList();
            fileBean = value.get(position);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "请刷新", Toast.LENGTH_SHORT).show();
            return;
        }

        //searchView.onActionViewCollapsed();

        if (actionMode == null) {
            if (fileBean.isDirectory()) {
                gotoNext(fileBean);
            } else {
                playOrRead(fileBean);
            }
        } else {
            select(fileBean, position);
        }


    }

    private void gotoNext(FileBean fileBean) {
        binding.swipeRefreshLayout.setRefreshing(true);
        clearMenuItemIcon();
        showBottomNavigationView();
        if (search) {
            removeSearch();
        }

        if (!binding.floatingAddActionButton.isShown()) {
            binding.floatingAddActionButton.show();
        }

        binding.filePath.setText(fileBean.getPath());
        fileViewModel.setPath(fileBean.getPath());

        //当进入下一级时，记录当前的位置
        fileViewModel.setPosition(fileBean.getParentPath(), linearLayoutManager);


        if (fileViewModel.getFileListCache().get(fileBean.getPath()) != null && !search) {
            liveData.postValue(fileViewModel.getFileListCache().get(fileBean.getPath()));
            return;
        }


        //更新
        liveData.setValue(null);
        fileViewModel.getFile(fileBean.getPath(), 0, fileViewModel.getLimitCount());
    }

    @SuppressWarnings("all")
    private void showBottomNavigationView() {
        BottomNavigationView bottomNavigationView = fileViewModel.getBottomNavigationView();
        CoordinatorLayout.LayoutParams layoutParams = (CoordinatorLayout.LayoutParams) bottomNavigationView.getLayoutParams();
        HideBottomViewOnScrollBehavior<BottomNavigationView> behavior = (HideBottomViewOnScrollBehavior<BottomNavigationView>) layoutParams.getBehavior();
        behavior.slideUp(bottomNavigationView);
    }

    /**
     * 当退出搜索后，需要清除它
     */
    private void removeSearch() {
        search = false;
        Objects.requireNonNull(((AppCompatActivity) getActivity()).getSupportActionBar()).setTitle("6pan");
    }

    private void playOrRead(FileBean fileBean) {
        if (fileBean.getMime().contains("video")) {
            playVideo(fileBean);
            return;
        }
        if (fileBean.getMime().contains("image")) {
            viewPhoto(fileBean);
            return;
        }
        if (fileBean.getMime().equals("text/plain") || fileBean.getName().contains(".txt")) {
            TextDialog textDialog = new TextDialog();

            forceReadText(fileBean, textDialog);
            return;
        }
        Toast.makeText(requireContext(), fileBean.toString(), Toast.LENGTH_SHORT).show();
    }

    private void playVideo(FileBean fileBean) {
        Intent intent = new Intent(requireActivity(), VideoActivity.class);
        fileViewModel.downloadSingle(fileBean.getIdentity()).observe(this, s -> {
            intent.putExtra("address", s);
            intent.putExtra("title", fileBean.getName());
            startActivity(intent);
        });
    }

    private void viewPhoto(FileBean fileBean) {
        List<FileBean> photoList = liveData.getValue().stream()
                .filter(s -> s.getMime().contains("image")).collect(Collectors.toList());

        int position = 0;
        for (int i = 0; i < photoList.size(); i++) {
            if (photoList.get(i).getIdentity().equals(fileBean.getIdentity())) {
                position = i;
                break;
            }
        }
        viewPhoto((ArrayList<FileBean>) photoList, position);

    }

    private void viewPhoto(ArrayList<FileBean> photoList, int position) {
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("list", photoList);
        bundle.putString("token", fileViewModel.getToken());
        bundle.putInt("position", position);
        Intent intent = new Intent(requireActivity(), PictureActivity.class);
        intent.putExtra("bundle", bundle);
        startActivity(intent);
    }


    private void select(FileBean fileBean, int position) {
        fileBean.setSelect(!fileBean.isSelect());
        try {
            Objects.requireNonNull(liveData.getValue()).set(position, fileBean);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString();
            Toast.makeText(requireContext(), sStackTrace, Toast.LENGTH_SHORT).show();
            return;
        }
        adapter.notifyDataSetChanged();

        int size = liveData.getValue().stream().filter(FileBean::isSelect).toArray().length;
        actionMode.setTitle(size + " ");
        if (size == 0) {
            actionMode.finish();
        }

    }

    private boolean onLongClick(View view, int position) {
        FileBean fileBean;
        try {
            fileBean = liveData.getValue().get(position);
        } catch (Exception e) {
            Toast.makeText(requireContext(), e.getMessage() + "\n请刷新", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return true;
        }

        if (actionMode == null) {
            actionMode = requireActivity().startActionMode(getCallBack());
        }

        fileBean.setSelect(!fileBean.isSelect());
        liveData.getValue().set(position, fileBean);
        adapter.notifyDataSetChanged();

        binding.floatingAddActionButton.setImageResource(R.drawable.ic_baseline_add_24);
        binding.floatingCutActionButton.setVisibility(View.GONE);
        beSelectFileBean = null;

        int size = liveData.getValue().stream().filter(FileBean::isSelect).toArray().length;
        actionMode.setTitle(size + " ");
        return true;
    }

    private ActionMode.Callback getCallBack() {
        ActionMode.Callback callback = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                MenuInflater inflater = mode.getMenuInflater();
                inflater.inflate(R.menu.file_funtion_menu, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                if (item.getItemId() == R.id.item_all) {
                    itemAll();
                }

                if (item.getItemId() == R.id.item_move) {
                    itemMove();

                    mode.finish();
                    return true;
                }

                if (item.getItemId() == R.id.item_download) {
                    itemDownload();

                    mode.finish();
                    return true;
                }

                if (item.getItemId() == R.id.item_delete) {
                    List<FileBean> value = liveData.getValue();
                    if (value == null) {
                        Toast.makeText(requireContext(), "获取数据失败，请刷新后重试", Toast.LENGTH_SHORT).show();
                        return true;
                    }

                    List<FileBean> collect = value.stream().filter(FileBean::isSelect).collect(Collectors.toList());
                    int startPosition = 0;
                    for (int i = 0; i < value.size(); i++) {
                        if (value.get(i).getIdentity().equals(collect.get(0).getIdentity())) {
                            startPosition = i;
                            break;
                        }
                    }
                    collect.forEach(f -> {
                        f.setSelect(false);
                    });

                    itemDelete(collect, startPosition);

                    mode.finish();
                    return true;
                }

                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode mode) {
                Objects.requireNonNull(liveData.getValue()).forEach(s -> {
                    s.setSelect(false);
                });
                adapter.notifyDataSetChanged();
                actionMode = null;

            }
        };
        return callback;
    }

    private void itemAll() {
        List<FileBean> value = liveData.getValue();
        //记录位置，防止乱移
        fileViewModel.setPosition(fileViewModel.getPath(), linearLayoutManager);
        assert value != null;
        value.forEach(s -> {
            s.setSelect(true);
        });
        liveData.postValue(value);
        adapter.notifyDataSetChanged();
        int size = liveData.getValue().stream().filter(FileBean::isSelect).toArray().length;
        actionMode.setTitle(size + " ");
    }

    private void itemMove() {
        List<FileBean> collect;
        try {
            collect = Objects.requireNonNull(liveData.getValue()).stream().filter(FileBean::isSelect).collect(Collectors.toList());
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString();
            Toast.makeText(requireContext(), sStackTrace, Toast.LENGTH_SHORT).show();
            return;
        }
        beSelectFileBean = (ArrayList<FileBean>) collect;

        binding.floatingAddActionButton.setImageResource(R.drawable.ic_baseline_clear_24);
        binding.floatingCutActionButton.setVisibility(View.VISIBLE);
        if (!binding.floatingAddActionButton.isShown()) {
            binding.floatingAddActionButton.show();
        }
        //清除搜索
        if (search) {
            onBackPressed(backPressedCallback);
        }
    }

    private void itemDownload() {
        ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        List<FileBean> collect;
        try {
            collect = Objects.requireNonNull(liveData.getValue()).stream().filter(FileBean::isSelect).collect(Collectors.toList());
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString();
            Toast.makeText(requireContext(), sStackTrace, Toast.LENGTH_SHORT).show();
            return;
        }

        fileViewModel.downloadZip((ArrayList<FileBean>) collect).observe(getViewLifecycleOwner(), s -> {
            ClipData clip = ClipData.newPlainText("downloadSingle", s);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "下载链接已输出到剪贴板", Toast.LENGTH_SHORT).show();
        });
    }

    private void itemDelete(List<FileBean> collect, int startPosition) {
        String path = binding.filePath.getText().toString();

        List<FileBean> arrayList = fileViewModel.getFileListCache().get(path);
        arrayList.removeAll(collect);
        //仅不再搜索状态时刷新，因为当在搜索状态并删除后，会重新刷新数据
        if (!search) {
            adapter.notifyItemRangeRemoved(startPosition, collect.size());
        }

        liveData.postValue(arrayList);

        //注意后台不会更新缓存，为了防止排序删除时排序打乱
        fileViewModel.delete(collect);
        //设置位置，防止排序删除的情况下xjb跑
        fileViewModel.setPosition(path, linearLayoutManager);
        //清除缓存
        collect.forEach(s -> {
            if (fileViewModel.getFileListCache().get(s.getParentPath()) != null) {
                fileViewModel.getFileListCache().get(s.getParentPath()).remove(s);
            }
        });

        //清除搜索
        if (search) {
            onBackPressed(backPressedCallback);
        }
    }

    public void onBackPressed(OnBackPressedCallback callback) {
        clearMenuItemIcon();
        showBottomNavigationView();
        if (!searchView.isIconified()) {
            searchView.onActionViewCollapsed();
            return;
        }

        if (!binding.floatingAddActionButton.isShown()) {
            binding.floatingAddActionButton.show();
        }

        String path = binding.filePath.getText().toString();

        //如果直接点返回，则不更新数据，直接返回原来的数据
        if (search) {
            List<FileBean> fileBeans = fileViewModel.getFileListCache().get(path);
            removeSearch();
            liveData.postValue(fileBeans);
            return;
        }


        if (path.equals("/")) {
            if (binding.floatingCutActionButton.getVisibility() == View.VISIBLE) {
                binding.floatingAddActionButton.setImageResource(R.drawable.ic_baseline_add_24);
                binding.floatingCutActionButton.setVisibility(View.GONE);
                beSelectFileBean = null;
                return;
            }
            if (!mBackKeyPressed) {
                Toast.makeText(requireContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
                mBackKeyPressed = true;
                new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                        mBackKeyPressed = false;
                    }
                }, 3000);
            } else {
                callback.setEnabled(false);
                //回到桌面
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addCategory(Intent.CATEGORY_HOME);
                startActivity(intent);
            }
            return;
        }

        binding.swipeRefreshLayout.setRefreshing(true);

        fileViewModel.setPosition(path, linearLayoutManager);


        if (path.lastIndexOf("/") == 0) {
            path = "/";
        } else {
            path = path.substring(0, path.lastIndexOf("/"));
        }
        binding.filePath.setText(path);
        fileViewModel.setPath(path);

        if (fileViewModel.getFileListCache().get(path) != null) {
            liveData.postValue(fileViewModel.getFileListCache().get(path));
            return;
        }
        liveData.setValue(null);
        fileViewModel.getFile(path, 0, fileViewModel.getLimitCount());

    }

    @Override
    public void deleteFile(List<FileBean> fileBeanList, int position) {
        itemDelete(fileBeanList, position);
    }

    @Override
    public void remove(View v, FileBean fileBean) {
        beSelectFileBean = new ArrayList<>();
        beSelectFileBean.add(fileBean);

        binding.floatingAddActionButton.setImageResource(R.drawable.ic_baseline_clear_24);
        binding.floatingCutActionButton.setVisibility(View.VISIBLE);
        if (!binding.floatingAddActionButton.isShown()) {
            binding.floatingAddActionButton.show();
        }
        //清除搜索
        if (search) {
            onBackPressed(backPressedCallback);
        }
    }


    @Override
    public void forcePlayVideo(FileBean fileBean) {
        playVideo(fileBean);
    }

    @Override
    public void forceReadText(FileBean fileBean, TextDialog textDialog) {
        if (fileBean.isDirectory()) {
            Toast.makeText(requireContext(), "不能查看文件夹", Toast.LENGTH_SHORT).show();
            return;
        }
        //文件大小大于1m
        if (fileBean.getSize() > 1048576) {
            Toast.makeText(requireContext(), "文件太大啦~请下载后查看", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.swipeRefreshLayout.setRefreshing(true);

        fileViewModel.downloadSingle(fileBean.getIdentity()).observe(this, url -> {
            fileViewModel.downloadFile(url).observe(this, content -> {
                binding.swipeRefreshLayout.setRefreshing(false);
                textDialog.setContent(content);
                textDialog.show(requireActivity().getSupportFragmentManager());
            });
        });
    }

    @Override
    public void forceViewImage(FileBean fileBean) {
        ArrayList<FileBean> fileBeanList = new ArrayList<>();
        fileBeanList.add(fileBean);
        viewPhoto(fileBeanList, 1);
    }

    @Override
    public void rename(Context c, FileBean fileBean) {
        TextInputEditText textInputEditText = new TextInputEditText(c);
        textInputEditText.setText(fileBean.getName());
        MaterialAlertDialogBuilder materialAlertDialogBuilder = new MaterialAlertDialogBuilder(requireContext());
        materialAlertDialogBuilder.setTitle("重命名文件")
                .setView(textInputEditText)
                .setNegativeButton("取消", null)
                .setPositiveButton("确定", (dialog, which) -> {
                    String path = fileBean.getParentPath();
                    if (search) {
                        //清除缓存
                        fileViewModel.getFileListCache().remove(path);

                        path = fileViewModel.getPath();
                        List<FileBean> fileBeans = fileViewModel.getFileListCache().get(path);
                        removeSearch();
                        //退出搜索，恢复之前的样子
                        liveData.postValue(fileBeans);
                    }

                    String newName = textInputEditText.getText().toString();
                    fileViewModel.rename(fileBean, newName, path);
                }).show();
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}