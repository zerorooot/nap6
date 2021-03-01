package github.zerorooot.sixpan.fragment;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Date;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.unit.DataSizeUtil;
import github.zerorooot.sixpan.BuildConfig;
import github.zerorooot.sixpan.MainActivity;
import github.zerorooot.sixpan.R;
import github.zerorooot.sixpan.bean.ApiUrl;
import github.zerorooot.sixpan.bean.TokenBean;
import github.zerorooot.sixpan.bean.UserInfoBean;
import github.zerorooot.sixpan.databinding.FragmentAboutMeBinding;
import github.zerorooot.sixpan.uitl.SharedPreferencesUtil;
import github.zerorooot.sixpan.viewModel.FileViewModel;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class AboutMeFragment extends Fragment {
    private FragmentAboutMeBinding binding;
    private MutableLiveData<UserInfoBean> liveData = new MutableLiveData<>();
    private FileViewModel fileViewModel;


    @Override
    public View onCreateView(@NotNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentAboutMeBinding.inflate(inflater, container, false);
        fileViewModel = new ViewModelProvider(requireActivity(), new SavedStateViewModelFactory(requireActivity().getApplication(), this)).get(FileViewModel.class);

        getUserInfo();
        fileViewModel.quota().observe(getViewLifecycleOwner(), bean -> {
            binding.aboutMeOfflineQuota.setText("配额(" + "今日已用 " + bean.getDailyUsed() + " 次，剩余 " + bean.getAvailable() + " / " + bean.getDailyQuota() + ")");
        });
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        liveData.observe(getViewLifecycleOwner(), userInfoBean -> {
            binding.aboutMeName.setText(userInfoBean.getName());
            Glide.with(requireContext())
                    .load(userInfoBean.getIcon())
                    .apply(RequestOptions.bitmapTransform(new CircleCrop()))
                    .thumbnail(0.1f)
                    .into(binding.aboutMeUserIcon);
            binding.aboutMeVipTime.setText("订阅有效期至：" + userInfoBean.getVipExpireTimeString());
            binding.aboutMeFileQuota.setText("配额(" + userInfoBean.getSpaceUsedSize() + "/" + userInfoBean.getSpaceCapacitySize() + ")");
        });

        binding.aboutMeExit.setOnClickListener(e -> {
            SharedPreferencesUtil sharedPreferencesUtil = new SharedPreferencesUtil(requireContext());
            TokenBean tokenBean = sharedPreferencesUtil.get();
            tokenBean.setToken(null);
            sharedPreferencesUtil.save(tokenBean);
            //登出
            fileViewModel.logout();
            Intent intent = new Intent(requireActivity(), MainActivity.class);
            //禁止返回
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("exit", true);
            startActivity(intent);
        });
        //文件管理
        binding.mainFileManagerCard.setOnClickListener(v -> {
            fileViewModel.getBottomNavigationView().setSelectedItemId(R.id.fileFragment);
        });
        //离线
        binding.mainOfflineDownloadCard.setOnClickListener(e -> {
            fileViewModel.getBottomNavigationView().setSelectedItemId(R.id.offLineListAndDownloadFragment);
        });
        //版本信息
        binding.versionTextView.setText("当前版本 : " + BuildConfig.VERSION_NAME);
    }

    private void getUserInfo() {
        OkHttpClient okHttpClient = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        Request request = new Request.Builder()
                .url(ApiUrl.USER_INFO)
                .post(RequestBody.create("{}", mediaType))
                .addHeader("authorization", fileViewModel.getToken())
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                UserInfoBean userInfoBean = new Gson().fromJson(response.body().string(), UserInfoBean.class);
                userInfoBean.setSpaceCapacitySize(DataSizeUtil.format(userInfoBean.getSpaceCapacity()));
                userInfoBean.setSpaceUsedSize(DataSizeUtil.format(userInfoBean.getSpaceUsed()));
                userInfoBean.setVipExpireTimeString(DateUtil.format(new Date(userInfoBean.getVipExpireTime()), "yyyy-MM-dd"));
                liveData.postValue(userInfoBean);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

}