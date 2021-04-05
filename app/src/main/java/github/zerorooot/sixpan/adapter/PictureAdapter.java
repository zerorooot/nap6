package github.zerorooot.sixpan.adapter;

import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
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
        fileViewModel.downloadSingle(item.getIdentity(), true).observe(activity, s -> {
            Glide.with(activity)
                    .load(s)
                    .placeholder(R.drawable.ic_baseline_image_24)
                    .thumbnail(0.1f)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                            holder.swipeRefreshLayout.setRefreshing(false);
                            return false;
                        }
                    })
                    .into(holder.pagerPhoto);

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
