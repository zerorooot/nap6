package github.zerorooot.sixpan.adapter;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import github.zerorooot.sixpan.R;
import github.zerorooot.sixpan.activity.PictureActivity;
import github.zerorooot.sixpan.bean.FileBean;
import github.zerorooot.sixpan.customizeActivity.ZoomageView;
import github.zerorooot.sixpan.viewModel.FileViewModel;
import lombok.Setter;

@Setter
public class PictureAdapter extends ListAdapter<FileBean, PictureAdapter.PictureViewHolder> {
    private FileViewModel fileViewModel;
    private PictureActivity activity;

    public PictureAdapter() {
        super(new DiffUtil.ItemCallback<FileBean>() {
            @Override
            public boolean areItemsTheSame(@NonNull FileBean oldItem, @NonNull FileBean newItem) {
                return oldItem == newItem;
            }

            @Override
            public boolean areContentsTheSame(@NonNull FileBean oldItem, @NonNull FileBean newItem) {
                return oldItem.getIdentity().equals(newItem.getIdentity());
            }
        });
    }

    @NonNull
    @Override
    public PictureViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.picture_cell, parent, false);
        PictureViewHolder holder = new PictureViewHolder(itemView);
        holder.pagerPhoto.setViewPager2(activity.binding.viewPager);

        //use only indicator
        holder.swipeRefreshLayout.setEnabled(false);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull PictureViewHolder holder, int position) {
        FileBean item = getItem(position);
        holder.swipeRefreshLayout.setRefreshing(true);
        fileViewModel.downloadSingle(item.getIdentity()).observe(activity, s -> {
            Glide.with(activity)
                    .load(s)
                    .placeholder(R.drawable.ic_baseline_image_24)
                    .thumbnail(0.1f)
                    .into(holder.pagerPhoto);
            holder.swipeRefreshLayout.setRefreshing(false);

        });

    }

    static class PictureViewHolder extends RecyclerView.ViewHolder {
        ZoomageView pagerPhoto;
        SwipeRefreshLayout swipeRefreshLayout;

        public PictureViewHolder(@NonNull View itemView) {
            super(itemView);
            pagerPhoto = itemView.findViewById(R.id.pagerPhoto);
            swipeRefreshLayout = itemView.findViewById(R.id.swipeRefreshLayout_photo);
        }
    }

}
