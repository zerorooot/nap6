package github.zerorooot.sixpan.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import github.zerorooot.sixpan.R;
import github.zerorooot.sixpan.bean.OffLineBean;
import lombok.Setter;

@Setter
public class OffLineFileAdapter extends ListAdapter<OffLineBean, OffLineFileAdapter.OffLineViewHolder> {

    private OffLineFileAdapter.OnItemClickListener onItemClickListener = null;

    public interface OnItemClickListener {
        void onItemClick(View view, int position);

        void onMenuClick(View v, int position, float x, float y);
    }

//    public interface OnItemLongClickListener {
//        boolean onItemLongClick(View view, int position);
//    }

    public OffLineFileAdapter() {
        super(new DiffUtil.ItemCallback<OffLineBean>() {
            @Override
            public boolean areItemsTheSame(@NonNull OffLineBean oldItem, @NonNull OffLineBean newItem) {
                return oldItem.getAccessIdentity().equals(newItem.getAccessIdentity());
            }

            @Override
            public boolean areContentsTheSame(@NonNull OffLineBean oldItem, @NonNull OffLineBean newItem) {
                return oldItem.toString().equals(newItem.toString());
            }
        });
    }

    @NonNull
    @Override
    public OffLineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View inflate = layoutInflater.inflate(R.layout.offline_file_cell, parent, false);
        OffLineViewHolder myViewHolder = new OffLineViewHolder(inflate);
//        myViewHolder.offlineProgressBar.setOnClickListener(view -> {
//
//        });
        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull OffLineViewHolder holder, int position) {
        OffLineBean offLineBean = getItem(holder.getAdapterPosition());

        holder.offlineFileProgressTextView.setText(offLineBean.getProgress() + "%");
        holder.offlineFileNameTextView.setText(offLineBean.getName());
        holder.offlineFileSizeTextView.setText(offLineBean.getSizeString());
        holder.offlineFileTimeTextView.setText(offLineBean.getTime());

        setImage(offLineBean, holder);
//        holder.offlineCardView.setCardBackgroundColor(offLineBean.isSelect() ? Color.CYAN : Color.WHITE);

        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(v, holder.getAdapterPosition()));
        holder.offlineImageButton.setOnClickListener(v -> {
            onItemClickListener.onMenuClick(v, holder.getAdapterPosition(), v.getX(), v.getY());
        });
    }

    private void setImage(OffLineBean offLineBean, OffLineViewHolder offlineViewHolder) {
        if (offLineBean.getFileMime().contains("image")) {
            offlineViewHolder.offlineImageView.setImageResource(R.drawable.ic_baseline_image_24);
            return;
        }
        if (offLineBean.getFileMime().contains("video")) {
            offlineViewHolder.offlineImageView.setImageResource(R.drawable.ic_baseline_videocam_24);
            return;
        }
        if (offLineBean.getFileMime().equals("text/plain") || offLineBean.getName().contains(".txt")) {
            offlineViewHolder.offlineImageView.setImageResource(R.drawable.ic_baseline_text_24);
            return;
        }
        if (offLineBean.isDirectory()) {
            offlineViewHolder.offlineImageView.setImageResource(R.drawable.ic_round_folder_24);
        } else {
            offlineViewHolder.offlineImageView.setImageResource(R.drawable.ic_round_insert_drive_file_24);
        }
    }

    public static class OffLineViewHolder extends RecyclerView.ViewHolder {
        private final ImageView offlineImageView;
        private final TextView offlineFileNameTextView;
        private final TextView offlineFileSizeTextView;
        private final TextView offlineFileTimeTextView;
        private final TextView offlineFileProgressTextView;
        private final ImageButton offlineImageButton;
        private final CardView offlineCardView;

        public OffLineViewHolder(@NonNull View itemView) {
            super(itemView);
            offlineImageView = itemView.findViewById(R.id.offlineImageView);
            offlineFileNameTextView = itemView.findViewById(R.id.offlineFileNameTextView);
            offlineFileSizeTextView = itemView.findViewById(R.id.offlineFileSizeTextView);
            offlineFileTimeTextView = itemView.findViewById(R.id.offlineFileTimeTextView);
            offlineImageButton = itemView.findViewById(R.id.offlineImageButton);
            offlineFileProgressTextView = itemView.findViewById(R.id.offlineFileProgressTextView);
            offlineCardView = itemView.findViewById(R.id.offlineCardView);
        }

    }
}
