package github.zerorooot.sixpan.activity;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import java.io.File;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.unit.DataSizeUtil;
import github.zerorooot.sixpan.BuildConfig;
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
        boolean recoverDefault = sharedPreferences.getBoolean("recoverDefault", false);
        if (recoverDefault) {
            String file = this.getCacheDir().getParent() + "/shared_prefs/" + BuildConfig.APPLICATION_ID + "_preferences.xml";
            FileUtil.del(file);
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

            setThemePreference(findPreference("theme"));

            setItemSelect(findPreference("itemSelect"));

            setItemSelectTitle(findPreference("itemSelectTitle"));

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
                if (!off) {
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
                    return "长按文件页面目录可复制当前路径";
                }
                return "默认离线位置为: " + text;
            });

        }

        private void setThemePreference(@NonNull ListPreference themePreference) {
            String[] entry = {"普通模式", "暗黑模式", "跟随系统"};
            String[] entryValue = {"1", "2", "-1"};
            themePreference.setEntries(entry);
            themePreference.setEntryValues(entryValue);
            themePreference.setSummaryProvider((Preference.SummaryProvider<ListPreference>) ListPreference::getEntry);
            themePreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String mode = newValue.toString();
                switch (mode) {
                    case "2":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                        break;
                    case "-1":
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                        break;
                    default:
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                }
                return true;
            });
        }

        private void setItemSelect(@NonNull ListPreference itemSelectPreference) {
            String[] entry = {"启用反选", "启用全选", "全部启用"};
            String[] entryValue = {"1", "2", "0"};
            itemSelectPreference.setEntries(entry);
            itemSelectPreference.setEntryValues(entryValue);
            itemSelectPreference.setSummaryProvider((Preference.SummaryProvider<ListPreference>) ListPreference::getEntry);
        }

        private void setItemSelectTitle(@NonNull ListPreference itemSelectTitlePreference) {
            String[] entry = {"显示选中数量", "显示选中大小", "显示数量大小"};
            String[] entryValue = {"0", "1", "2"};
            itemSelectTitlePreference.setEntries(entry);
            itemSelectTitlePreference.setEntryValues(entryValue);
            itemSelectTitlePreference.setSummaryProvider((Preference.SummaryProvider<ListPreference>) ListPreference::getEntry);
        }

    }
}