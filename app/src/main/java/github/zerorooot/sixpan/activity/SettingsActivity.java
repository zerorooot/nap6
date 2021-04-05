package github.zerorooot.sixpan.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import java.io.File;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.unit.DataSizeUtil;
import github.zerorooot.sixpan.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle("高级设置");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean glideCache = sharedPreferences.getBoolean("appCache", false);
        if (glideCache) {
            SharedPreferences.Editor edit = sharedPreferences.edit();
            edit.putBoolean("appCache", false);
            edit.apply();
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.setting_preferences, rootKey);

            setLimitCountPreference(findPreference("limitCount"));

            setSearchNumber(findPreference("searchNumber"));

            setOffLinePath(findPreference("offLinePath"));

            setAppCachePreference(findPreference("appCache"));

        }


        private void setLimitCountPreference(@NonNull EditTextPreference limitCountPreference) {
            limitCountPreference.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

            limitCountPreference.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) preference -> {
                String text = preference.getText();
                if (TextUtils.isEmpty(text)) {
                    text = "20";
                }
                return "每次请求" + text + "个文件";
            });

        }

        private void setSearchNumber(@NonNull EditTextPreference searchNumberPreference) {
            searchNumberPreference.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED));

            searchNumberPreference.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) preference -> {
                String text = preference.getText();
                if (TextUtils.isEmpty(text)) {
                    return "-1表示搜索时不限文件数量";
                }
                if ("-1".equals(text)) {
                    return "搜索时不限文件数量";
                }
                return "每次搜索" + text + "个文件";
            });
        }

        private void setAppCachePreference(@NonNull SwitchPreferenceCompat appCachePreference) {
            String reminder = "缓存已占用%s";
            File file = new File(requireContext().getCacheDir() + "/");
            String size = DataSizeUtil.format(FileUtil.size(file));
            appCachePreference.setSummaryOff(String.format(reminder, size));

            appCachePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                SwitchPreferenceCompat switchPreferenceCompat = (SwitchPreferenceCompat) preference;
                boolean off = (boolean) newValue;
                if (off) {
                    switchPreferenceCompat.setSummaryOff(String.format(reminder, size));
                } else {
                    FileUtil.del(file);
                    file.mkdirs();
                    switchPreferenceCompat.setSummaryOn("清除完毕");
                }
                return true;
            });
        }

        private void setOffLinePath(@NonNull EditTextPreference offLinePathPreference) {
            offLinePathPreference.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) preference -> {
                String text = preference.getText();
                if (TextUtils.isEmpty(text)) {
                    return "默认离线位置，长按文件页面的目录可复制";
                }
                return "默认离线位置为: " + text;
            });

        }


    }
}