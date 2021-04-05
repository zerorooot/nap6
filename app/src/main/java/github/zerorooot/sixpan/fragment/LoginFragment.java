package github.zerorooot.sixpan.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.Objects;

import github.zerorooot.sixpan.activity.FileActivity;
import github.zerorooot.sixpan.bean.TokenBean;
import github.zerorooot.sixpan.databinding.FragmentLoginBinding;
import github.zerorooot.sixpan.util.SharedPreferencesUtil;
import github.zerorooot.sixpan.viewModel.LoginViewModel;

public class LoginFragment extends Fragment {
    private FragmentLoginBinding binding;

    private EditText userName;
    private EditText password;
    private Button button;
    private SharedPreferencesUtil sharedPreferencesUtil;
    private LoginViewModel loginViewModel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(inflater, container, false);

        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                this.setEnabled(false);
                requireActivity().finish();
            }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), backPressedCallback);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        //use only indicator
        binding.swipeRefreshLogin.setEnabled(false);
        userName = binding.editTextUserName;
        password = binding.editTextPassword;
        button = binding.button;
        sharedPreferencesUtil = new SharedPreferencesUtil(requireContext());
        loginViewModel = new ViewModelProvider(this, new SavedStateViewModelFactory(requireActivity().getApplication(), this)).get(LoginViewModel.class);
        TokenBean tokenBean = sharedPreferencesUtil.get();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            userName.setAutofillHints(View.AUTOFILL_HINT_USERNAME);
            password.setAutofillHints(View.AUTOFILL_HINT_PASSWORD);
        }

        button.setOnClickListener(this::login);


        if (Objects.nonNull(tokenBean.getAccount())) {
            userName.setText(tokenBean.getAccount());
        }

        if (Objects.nonNull(tokenBean.getPassword())) {
            password.setText(tokenBean.getPassword());
        }

    }

    private void login(View view) {
        if (binding.swipeRefreshLogin.isRefreshing()) {
            Toast.makeText(requireContext(), "登录中......", Toast.LENGTH_SHORT).show();
            return;
        }
        binding.swipeRefreshLogin.setRefreshing(true);
        TokenBean tokenBean = new TokenBean();
        tokenBean.setAccount(userName.getText().toString());
        tokenBean.setPassword(password.getText().toString());
        loginViewModel.login(tokenBean).observe(this, token -> {
            if (Objects.nonNull(token)) {
                tokenBean.setToken(token);
                sharedPreferencesUtil.save(tokenBean);
                jump(token);
                //hide keyboard
                InputMethodManager inputMethodManager = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            } else {
                binding.swipeRefreshLogin.setRefreshing(false);
            }
        });
    }

    private void jump(String token) {
        Intent intent = new Intent(requireActivity(), FileActivity.class);
        //禁止返回到登录页面
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("token", token);
        binding.swipeRefreshLogin.setRefreshing(false);
        startActivity(intent);
    }

}