package github.zerorooot.sixpan.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import java.util.Objects;

import github.zerorooot.sixpan.R;
import github.zerorooot.sixpan.activity.FileActivity;
import github.zerorooot.sixpan.bean.TokenBean;
import github.zerorooot.sixpan.util.SharedPreferencesUtil;
import github.zerorooot.sixpan.viewModel.LoginViewModel;


public class CheckFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_check, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        NavController navController = Navigation.findNavController(view);
        SharedPreferencesUtil sharedPreferencesUtil = new SharedPreferencesUtil(requireContext());
        LoginViewModel loginViewModel = new ViewModelProvider(this, new SavedStateViewModelFactory(requireActivity().getApplication(), this)).get(LoginViewModel.class);
        //版本检测
        loginViewModel.checkUpdate();
        TokenBean tokenBean = sharedPreferencesUtil.get();
        if (requireActivity().getIntent().getBooleanExtra("exit", false)) {
            navController.navigate(R.id.action_checkFragment_to_loginFragment);
            return;
        }
        if (Objects.nonNull(tokenBean.getToken())) {
            String token = tokenBean.getToken();
            //如果没过期，则自动登录
            if (loginViewModel.checkTime(token)) {
                loginViewModel.checkToken(token).observe(getViewLifecycleOwner(), check -> {
                    if (check) {
                        Intent intent = new Intent(requireActivity(), FileActivity.class);
                        //禁止返回到登录页面
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.putExtra("token", token);
                        startActivity(intent);
                    } else {
                        navController.navigate(R.id.action_checkFragment_to_loginFragment);
                    }
                });
            } else {
                navController.navigate(R.id.action_checkFragment_to_loginFragment);
            }
        } else {
            navController.navigate(R.id.action_checkFragment_to_loginFragment);
        }
    }
}