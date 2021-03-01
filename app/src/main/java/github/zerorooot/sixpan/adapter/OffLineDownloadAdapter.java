package github.zerorooot.sixpan.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import github.zerorooot.sixpan.R;
import github.zerorooot.sixpan.bean.OffLineParse;

public class OffLineDownloadAdapter extends ListAdapter<OffLineParse, OffLineDownloadAdapter.OffLineDownloadViewHolder> {

    public OffLineDownloadAdapter() {
        super(new DiffUtil.ItemCallback<OffLineParse>() {
            @Override
            public boolean areItemsTheSame(@NonNull OffLineParse oldItem, @NonNull OffLineParse newItem) {
                return oldItem.getTextLink().equals(newItem.getTextLink());
            }

            @Override
            public boolean areContentsTheSame(@NonNull OffLineParse oldItem, @NonNull OffLineParse newItem) {
                return oldItem.toString().equals(newItem.toString());
            }
        });
    }

    @NonNull
    @Override
    public OffLineDownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View inflate = layoutInflater.inflate(R.layout.offline_download_cell, parent, false);
        OffLineDownloadViewHolder viewHolder = new OffLineDownloadViewHolder(inflate);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull OffLineDownloadViewHolder holder, int position) {
        OffLineParse item = getItem(holder.getAdapterPosition());
        holder.offLineParseNameTextView.setText(item.getName());
        holder.offLineParseTextLink.setText(item.getTextLink());
        holder.offLineParseSizeTextView.setText(item.getSizeString());
        holder.cardView.setCardBackgroundColor(item.isReady() ? Color.CYAN : Color.RED);
    }

    public static class OffLineDownloadViewHolder extends RecyclerView.ViewHolder {
        private final TextView offLineParseNameTextView;
        private final TextView offLineParseTextLink;
        private final TextView offLineParseSizeTextView;
        private final CardView cardView;

        public OffLineDownloadViewHolder(@NonNull View itemView) {
            super(itemView);
            offLineParseNameTextView = itemView.findViewById(R.id.offLineParseNameTextView);
            offLineParseTextLink = itemView.findViewById(R.id.offLineParseTextLink);
            offLineParseSizeTextView = itemView.findViewById(R.id.offLineParseSizeTextView);
            cardView = itemView.findViewById(R.id.offline_download_cardview);
        }
    }
}
