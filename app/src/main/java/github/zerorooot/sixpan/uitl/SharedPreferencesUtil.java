package github.zerorooot.sixpan.uitl;

import android.content.Context;
import android.content.SharedPreferences;

import github.zerorooot.sixpan.bean.TokenBean;

public class SharedPreferencesUtil {
    private final Context context;
    private final String FILENAME = "token";

    public SharedPreferencesUtil(Context context) {
        this.context = context;
    }

    public void save(TokenBean tokenBean) {
        SharedPreferences preferences = context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = preferences.edit();
        edit.putString("account", tokenBean.getAccount());
        edit.putString("password", tokenBean.getPassword());
        edit.putString("token", tokenBean.getToken());
        edit.apply();
    }

    public TokenBean get() {
        TokenBean tokenBean = new TokenBean();
        SharedPreferences preferences = context.getSharedPreferences(FILENAME, Context.MODE_PRIVATE);
        tokenBean.setAccount(preferences.getString("account", null));
        tokenBean.setPassword(preferences.getString("password", null));
        tokenBean.setToken(preferences.getString("token", null));
        return tokenBean;
    }
}
