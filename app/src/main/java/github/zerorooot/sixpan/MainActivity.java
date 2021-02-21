package github.zerorooot.sixpan;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.autofill.AutofillManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;

import java.util.Objects;

import github.zerorooot.sixpan.activity.FileActivity;
import github.zerorooot.sixpan.bean.TokenBean;
import github.zerorooot.sixpan.databinding.ActivityMainBinding;
import github.zerorooot.sixpan.uitl.SharedPreferencesUtil;
import github.zerorooot.sixpan.viewModel.LoginViewModel;

public class MainActivity extends AppCompatActivity {
    ActivityMainBinding binding;
    private EditText userName;
    private EditText password;
    private Button button;
    private SharedPreferencesUtil sharedPreferencesUtil;
    private LoginViewModel loginViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        //use only indicator
        binding.swipeRefreshLogin.setEnabled(false);

        Objects.requireNonNull(getSupportActionBar()).setTitle("登录");
        userName = binding.editTextUserName;
        password = binding.editTextPassword;
        button = binding.button;


        sharedPreferencesUtil = new SharedPreferencesUtil(getApplicationContext());
        loginViewModel = new ViewModelProvider(this, new SavedStateViewModelFactory(getApplication(), this)).get(LoginViewModel.class);

        TokenBean tokenBean = sharedPreferencesUtil.get();
        Intent intent = getIntent();
        boolean reload = intent.getBooleanExtra("reload", false);

        if (Objects.nonNull(tokenBean.getToken()) && !reload) {
            String token = tokenBean.getToken();
            //如果没过期，则自动登录
            if (loginViewModel.check(token)) {
                binding.swipeRefreshLogin.setRefreshing(true);
                jump(token);
                return;
            }
        }

        //
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


    @RequiresApi(api = Build.VERSION_CODES.O)
    private void login(View view) {
        binding.swipeRefreshLogin.setRefreshing(true);
        button.setEnabled(false);
        TokenBean tokenBean = new TokenBean();
        tokenBean.setAccount(userName.getText().toString());
        tokenBean.setPassword(password.getText().toString());
        loginViewModel.login(tokenBean).observe(this, token -> {
            if (Objects.nonNull(token)) {
                tokenBean.setToken(token);
                sharedPreferencesUtil.save(tokenBean);
                jump(token);
                //hide keyboard
                InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
            } else {
                binding.swipeRefreshLogin.setRefreshing(false);
                runOnUiThread(() -> {
                    button.setEnabled(true);
                });
            }
        });
    }

    private void jump(String token) {
        Intent intent = new Intent(this, FileActivity.class);
        //禁止返回到登录页面
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("token", token);
        binding.swipeRefreshLogin.setRefreshing(false);
        startActivity(intent);
    }


}