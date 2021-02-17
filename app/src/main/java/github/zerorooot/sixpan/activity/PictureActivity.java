package github.zerorooot.sixpan.activity;


import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import android.content.Intent;

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import java.util.ArrayList;

import github.zerorooot.sixpan.R;
import github.zerorooot.sixpan.adapter.PictureAdapter;
import github.zerorooot.sixpan.bean.FileBean;
import github.zerorooot.sixpan.customizeActivity.ZoomageView;
import github.zerorooot.sixpan.databinding.ActivityPictureBinding;
import github.zerorooot.sixpan.viewModel.FileViewModel;

public class PictureActivity extends AppCompatActivity {
    public ActivityPictureBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPictureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("bundle");
        ArrayList<FileBean> photoList = bundle.getParcelableArrayList("list");
        int position = bundle.getInt("position");

        String token = bundle.getString("token");
        FileViewModel fileViewModel = new ViewModelProvider(this, new SavedStateViewModelFactory(getApplication(), this)).get(FileViewModel.class);
        fileViewModel.setToken(token);

        PictureAdapter adapter = new PictureAdapter();
        adapter.setActivity(this);
        adapter.setFileViewModel(fileViewModel);
        adapter.submitList(photoList);

        binding.viewPager.setAdapter(adapter);
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                binding.textViewItem.setText((position + 1) + "/" + photoList.size());
                binding.textViewTitle.setText(photoList.get(position).getName());
            }

        });


        binding.viewPager.setCurrentItem(position, false);

    }


}
