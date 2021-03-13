package github.zerorooot.sixpan.activity;


import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.SavedStateViewModelFactory;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import java.util.ArrayList;

import github.zerorooot.sixpan.adapter.PictureAdapter;
import github.zerorooot.sixpan.bean.FileBean;
import github.zerorooot.sixpan.databinding.ActivityPictureBinding;
import github.zerorooot.sixpan.viewModel.FileViewModel;

public class PictureActivity extends AppCompatActivity {
    public ActivityPictureBinding binding;
    private PictureActivity pictureActivity;
    private int start = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPictureBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        pictureActivity = this;
        Intent intent = getIntent();
        Bundle bundle = intent.getBundleExtra("bundle");
        ArrayList<FileBean> photoList = bundle.getParcelableArrayList("list");
        int position = bundle.getInt("position");
        start = bundle.getInt("start");

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
                //load more
                if ((position + 1) == photoList.size()) {
                    fileViewModel.getPictureFile(photoList.get(position).getParentPath(), start, fileViewModel.getLimitCount()).observe(pictureActivity, fileBeans -> {
                        photoList.addAll(fileBeans);
                        adapter.submitList(photoList);
                        binding.textViewItem.setText((position + 1) + "/" + photoList.size());
                        start = start + fileViewModel.getLimitCount();
                    });
                }
            }

        });


        binding.viewPager.setCurrentItem(position, false);

    }
}
