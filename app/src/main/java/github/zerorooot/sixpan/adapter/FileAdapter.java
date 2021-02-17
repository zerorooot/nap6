package github.zerorooot.sixpan.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;


import github.zerorooot.sixpan.R;
import github.zerorooot.sixpan.bean.FileBean;
import github.zerorooot.sixpan.customizeActivity.BottomDialog;
import lombok.Setter;


@Setter
public class FileAdapter extends ListAdapter<FileBean, FileAdapter.MyViewHolder> {
    private BottomDialog dialog;
    private FragmentManager supportFragmentManager;
    private OnItemClickListener onItemClickListener = null;
    private OnItemLongClickListener onItemLongClickListener = null;


    public interface OnItemClickListener {
        void onItemClick(View view, int position);
    }

    public interface OnItemLongClickListener {
        boolean onItemLongClick(View view, int position);
    }

    public FileAdapter() {
        super(new DiffUtil.ItemCallback<FileBean>() {
            @Override
            public boolean areItemsTheSame(@NonNull FileBean oldItem, @NonNull FileBean newItem) {
                return oldItem.getIdentity().equals(newItem.getIdentity());
            }

            @Override
            public boolean areContentsTheSame(@NonNull FileBean oldItem, @NonNull FileBean newItem) {
                return oldItem.toString().equals(newItem.toString());
            }
        });
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View inflate = layoutInflater.inflate(R.layout.file_cell, parent, false);
        MyViewHolder myViewHolder = new MyViewHolder(inflate);
        myViewHolder.moreButton.setOnClickListener(view -> {
            int adapterPosition = myViewHolder.getAdapterPosition();
            FileBean item = getItem(adapterPosition);
            dialog.setPosition(adapterPosition);
            dialog.setCurrentFileBean(item);
            dialog.show(supportFragmentManager);
        });

        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        FileBean currentFileBean = getItem(position);
        holder.fileNameTextView.setText(currentFileBean.getName());
        holder.fileTimeTextView.setText(currentFileBean.getDateTime());
        holder.fileSizeTextView.setText(currentFileBean.getSizeString());

        setImage(currentFileBean, holder);

        holder.cardView.setCardBackgroundColor(currentFileBean.isSelect() ? Color.CYAN : Color.WHITE);

        holder.itemView.setOnClickListener(v -> onItemClickListener.onItemClick(v, holder.getAdapterPosition()));
        holder.itemView.setOnLongClickListener(v -> onItemLongClickListener.onItemLongClick(v, holder.getAdapterPosition()));
    }

    private void setImage(FileBean currentFileBean, MyViewHolder holder) {
        if (currentFileBean.getMime().contains("image")) {
            holder.imageView.setImageResource(R.drawable.ic_baseline_image_24);
            return;
        }
        if (currentFileBean.getMime().contains("video")) {
            holder.imageView.setImageResource(R.drawable.ic_baseline_videocam_24);
            return;
        }

        if (currentFileBean.getMime().equals("text/plain") || currentFileBean.getName().contains(".txt")) {
            holder.imageView.setImageResource(R.drawable.ic_baseline_text_24);
            return;
        }

        if (currentFileBean.isDirectory()) {
            holder.imageView.setImageResource(R.drawable.ic_round_folder_24);
        } else {
            holder.imageView.setImageResource(R.drawable.ic_round_insert_drive_file_24);
        }

    }


    public static class MyViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView fileNameTextView;
        private final TextView fileSizeTextView;
        private final TextView fileTimeTextView;
        private final ImageButton moreButton;
        private final CardView cardView;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.fileImageView);
            fileNameTextView = itemView.findViewById(R.id.fileNameTextView);
            fileNameTextView.setSelected(true);

            fileSizeTextView = itemView.findViewById(R.id.fileSizeTextView);
            fileTimeTextView = itemView.findViewById(R.id.fileTimeTextView);
            moreButton = itemView.findViewById(R.id.fileImageButton);
            cardView = itemView.findViewById(R.id.cardView);

        }
    }
}
