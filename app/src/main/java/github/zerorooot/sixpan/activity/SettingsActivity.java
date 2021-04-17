package github.zerorooot.sixpan.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.io.File;
import java.util.Map;

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


    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.setting_preferences, rootKey);

            setLimitCountPreference(findPreference("limitCount"));

            setSearchNumber(findPreference("searchNumber"));

            setOffLinePath(findPreference("offLinePath"));

            setAppCachePreference(findPreference("appCache"));

            setRecoverDefault(findPreference("recoverDefault"));

            setReboot(findPreference("reboot"));

            setThemePreference(findPreference("theme"));

            setItemSelect(findPreference("itemSelect"));

            setItemSelectTitle(findPreference("itemSelectTitle"));

            setBackUp(findPreference("backUp"));

            setRecover(findPreference("recover"));

        }


        private void setLimitCountPreference(@NonNull EditTextPreference limitCountPreference) {
            limitCountPreference.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER));

            limitCountPreference.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) preference -> "每次请求" + preference.getText() + "个文件");

        }

        private void setSearchNumber(@NonNull EditTextPreference searchNumberPreference) {
            searchNumberPreference.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED));

            searchNumberPreference.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) preference -> {
                String text = preference.getText();
                if ("-1".equals(text)) {
                    return "搜索时不限文件数量";
                }
                return "每次搜索" + text + "个文件";
            });
        }

        private void setAppCachePreference(@NonNull Preference appCachePreference) {
            String reminder = "缓存已占用%s";
            File file = new File(requireContext().getCacheDir() + "/");
            String size = DataSizeUtil.format(FileUtil.size(file));
            appCachePreference.setSummary(String.format(reminder, size));

            appCachePreference.setOnPreferenceClickListener(preference -> {
                FileUtil.del(file);
                file.mkdirs();
                appCachePreference.setSummary("清除完毕");
                return true;
            });
        }

        private void setRecoverDefault(@NonNull Preference recoverDefaultPreference) {
            recoverDefaultPreference.setOnPreferenceClickListener(preference -> {
                String file = requireContext().getCacheDir().getParent() + "/shared_prefs/" + BuildConfig.APPLICATION_ID + "_preferences.xml";
                String toast = "恢复默认设置失败";
                if (FileUtil.del(file)) {
                    toast = "恢复默认设置成功";
                }
                Toast.makeText(requireContext(), toast, Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        private void setReboot(@NonNull Preference rebootPreference) {
            rebootPreference.setOnPreferenceClickListener(preference -> {
//                Intent mainIntent = IntentCompat.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_LAUNCHER);
//                mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//                requireContext().getApplicationContext().startActivity(mainIntent);
//                System.exit(0);


                PackageManager packageManager = requireContext().getPackageManager();
                Intent intent = packageManager.getLaunchIntentForPackage(requireContext().getPackageName());
                ComponentName componentName = intent.getComponent();
                Intent mainIntent = Intent.makeRestartActivityTask(componentName);
                startActivity(mainIntent);
                Runtime.getRuntime().exit(0);
                return true;
            });
        }

        private void setOffLinePath(@NonNull EditTextPreference offLinePathPreference) {
            offLinePathPreference.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) preference -> {
                String text = preference.getText();
                if ("/".equals(text)) {
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

        private void setBackUp(@NonNull Preference backUpPreference) {
            backUpPreference.setOnPreferenceClickListener(preference -> {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
                Map<String, ?> all = sharedPreferences.getAll();
                String json = new Gson().toJson(all);
                ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("backUp", json);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(requireContext(), "配置信息已输出到剪贴板", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

        private void setRecover(@NonNull EditTextPreference recoverPreference) {
            recoverPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                JsonObject jsonObject = new Gson().fromJson(newValue.toString(), JsonObject.class);
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
                SharedPreferences.Editor edit = sharedPreferences.edit();
                Map<String, ?> all = sharedPreferences.getAll();
                all.forEach((k, v) -> {
                    JsonPrimitive jsonPrimitive = jsonObject.getAsJsonPrimitive(k);
                    if (jsonPrimitive.isJsonNull()) {
                        return;
                    }
                    if (jsonPrimitive.isBoolean()) {
                        edit.putBoolean(k, jsonPrimitive.getAsBoolean());
                    }
                    if (jsonPrimitive.isNumber()) {
                        edit.putInt(k, jsonPrimitive.getAsInt());
                    }
                    if (jsonPrimitive.isString()) {
                        edit.putString(k, jsonPrimitive.getAsString());
                    }
                });
                edit.apply();
                Toast.makeText(requireContext(), "恢复成功,请重启后生效", Toast.LENGTH_SHORT).show();
                return true;
            });
        }

    }
}