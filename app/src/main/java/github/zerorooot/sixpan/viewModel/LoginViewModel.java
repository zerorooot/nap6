package github.zerorooot.sixpan.viewModel;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import cn.hutool.core.codec.Base64;
import github.zerorooot.sixpan.bean.ApiUrl;
import github.zerorooot.sixpan.bean.TokenBean;

import okhttp3.*;


public class LoginViewModel extends AndroidViewModel {

    public LoginViewModel(@NonNull Application application) {
        super(application);
    }

    public MutableLiveData<String> login(TokenBean tokenBean) {
        MutableLiveData<String> liveData = new MutableLiveData<>();
        String md5Password = toMd5(tokenBean.getPassword());
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("countryCode", "86");
        jsonObject.addProperty("user", tokenBean.getAccount());
        jsonObject.addProperty("password", md5Password);

        OkHttpClient okHttpClient = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json");
        Request request = new Request.Builder()
                .url(ApiUrl.LOGIN)
                .post(RequestBody.create(jsonObject.toString().getBytes(), mediaType))
                .build();


        okHttpClient.newCall(request).enqueue(new okhttp3.Callback() {
            @Override
            public void onFailure(@NotNull okhttp3.Call call, @NotNull IOException e) {
            }

            @Override
            public void onResponse(@NotNull okhttp3.Call call, @NotNull okhttp3.Response response) throws IOException {
                String cookie = response.header("Set-Cookie");
                if (Objects.nonNull(cookie)) {
                    Request request2 = new Request.Builder()
                            .url(ApiUrl.CHECK_COOKIE)
                            .addHeader("cookie", cookie)
                            .build();
                    okHttpClient.newCall(request2).enqueue(new Callback() {
                        @Override
                        public void onFailure(@NotNull okhttp3.Call call, @NotNull IOException e) {
                        }

                        @Override
                        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                            //[token=xx; path=/; expires=Sun, 07 Mar 2021 11:58:59 GMT; httponly, token.sig=xxx; path=/; expires=Sun, 07 Mar 2021 11:58:59 GMT; httponly]
                            String[] cookie = response.headers("Set-Cookie").toString().replace("[", "").replace("]", "").split(";");

                            for (String s : cookie) {
                                if (s.contains("token=")) {
                                    String token = s.split("token=")[1];
                                    token = "Bearer " + token;
                                    liveData.postValue(token);
                                    break;
                                }
                            }
                        }
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getApplication(), "账号或密码错误", Toast.LENGTH_SHORT).show();
                    });
                    liveData.postValue(null);
                }

            }
        });


        return liveData;
    }


    public boolean check(String token) {
        String[] tokens = token.split("\\.");
        String info = Base64.decodeStr(tokens[1]);
        JsonObject jsonObject = new Gson().fromJson(info, JsonObject.class);
        int exp = jsonObject.get("exp").getAsInt();
        return (System.currentTimeMillis() / 1000) < exp;
    }

    private String toMd5(String str) {
        StringBuilder hexString = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(str.getBytes());
            byte[] hash = md.digest();
            for (byte b : hash) {
                if ((0xff & b) < 0x10) {
                    hexString.append("0").append(Integer.toHexString((0xFF & b)));
                } else {
                    hexString.append(Integer.toHexString(0xFF & b));
                }
            }
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return hexString.toString();
    }
}
