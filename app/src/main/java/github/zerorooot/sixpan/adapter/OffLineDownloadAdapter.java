package github.zerorooot.sixpan.adapter;

import android.graphics.Color;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;

import github.zerorooot.sixpan.R;
import github.zerorooot.sixpan.bean.OffLineParse;


public class OffLineDownloadAdapter extends ListAdapter<OffLineParse, OffLineDownloadAdapter.OffLineDownloadViewHolder> {
    private final int HEAD_VIEW = 0;
    private OffLineDownloadAdapter.ClickInterface clickInterface;
    private String links;
    private String password;
    private SwipeRefreshLayout offLineSwipe;
    private String externalLink;

    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }

    public String getLinks() {
        return links;
    }

    public void setClickInterface(ClickInterface clickInterface) {
        this.clickInterface = clickInterface;
    }


    public interface ClickInterface {
        void parseClick(String links, String password);

        void downloadClick(TextInputEditText links, TextInputEditText password);
    }

    public void setOffLineSwipe(SwipeRefreshLayout offLineSwipe) {
        this.offLineSwipe = offLineSwipe;
    }

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
        OffLineDownloadViewHolder viewHolder;
        View inflate;
        if (viewType == HEAD_VIEW) {
            inflate = layoutInflater.inflate(R.layout.offline_download_head, parent, false);
            viewHolder = new OffLineDownloadViewHolder(inflate);
            viewHolder.offline_by_links_editText_links.addTextChangedListener(new TextWatcherAdapter() {
                @Override
                public void afterTextChanged(Editable s) {
                    links = s.toString();
                }
            });
            viewHolder.offline_by_links_editText_password.addTextChangedListener(new TextWatcherAdapter() {
                @Override
                public void afterTextChanged(Editable s) {
                    password = s.toString();
                }
            });

            //设置外部分享来的链接
            if (Objects.nonNull(externalLink)) {
                viewHolder.offline_by_links_editText_links.setText(externalLink);
            }

            viewHolder.offline_new_by_links_button_parse.setOnClickListener(e -> {
                clickInterface.parseClick(links, password);
            });
            viewHolder.offline_new_by_links_button_download.setOnClickListener(e -> {
                clickInterface.downloadClick(viewHolder.offline_by_links_editText_links, viewHolder.offline_by_links_editText_password);
            });
            offLineSwipe.setOnRefreshListener(() -> {
                viewHolder.offline_by_links_editText_links.setText("");
                viewHolder.offline_by_links_editText_password.setText("");
                this.submitList(null);
                externalLink = null;
                offLineSwipe.setRefreshing(false);
            });

            return viewHolder;
        }

        inflate = layoutInflater.inflate(R.layout.offline_download_cell, parent, false);
        viewHolder = new OffLineDownloadViewHolder(inflate);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull OffLineDownloadViewHolder holder, int position) {
        if (position == 0) {
            holder.offline_by_links_editText_links.setText(links);
            holder.offline_by_links_editText_password.setText(password);
            return;
        }
        OffLineParse item = getItem(holder.getAdapterPosition() - 1);
        holder.offLineParseNameTextView.setText(item.getName());
        holder.offLineParseTextLink.setText(item.getTextLink());
        holder.offLineParseSizeTextView.setText(item.getSizeString());
        holder.cardView.setCardBackgroundColor(item.isReady() ? Color.CYAN : Color.RED);
    }

    @Override
    public int getItemCount() {
        return super.getItemCount() + 1;
    }

    @Override
    public void onViewAttachedToWindow(@NonNull OffLineDownloadViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        if (Objects.nonNull(holder.offline_by_links_editText_links)) {
            //https://stackoverflow.com/questions/37566303/edittext-giving-error-textview-does-not-support-text-selection-selection-canc
            holder.offline_by_links_editText_password.setEnabled(false);
            holder.offline_by_links_editText_password.setEnabled(true);

            holder.offline_by_links_editText_links.setEnabled(false);
            holder.offline_by_links_editText_links.setEnabled(true);
        }

    }

    @Override
    public int getItemViewType(int position) {
        return position == 0 ? HEAD_VIEW : 1;
    }

    public static class OffLineDownloadViewHolder extends RecyclerView.ViewHolder {
        private final TextView offLineParseNameTextView;
        private final TextView offLineParseTextLink;
        private final TextView offLineParseSizeTextView;
        private final CardView cardView;
        //--------------------------------------
        private final TextInputEditText offline_by_links_editText_links;
        private final TextInputEditText offline_by_links_editText_password;
        private final MaterialButton offline_new_by_links_button_parse;
        private final MaterialButton offline_new_by_links_button_download;

        public OffLineDownloadViewHolder(@NonNull View itemView) {
            super(itemView);
            offLineParseNameTextView = itemView.findViewById(R.id.offLineParseNameTextView);
            offLineParseTextLink = itemView.findViewById(R.id.offLineParseTextLink);
            offLineParseSizeTextView = itemView.findViewById(R.id.offLineParseSizeTextView);
            cardView = itemView.findViewById(R.id.offline_download_cardview);

            //-----------------------------------
            offline_by_links_editText_links = itemView.findViewById(R.id.offline_by_links_editText_links);
            offline_by_links_editText_password = itemView.findViewById(R.id.offline_by_links_editText_password);
            offline_new_by_links_button_parse = itemView.findViewById(R.id.offline_new_by_links_button_parse);
            offline_new_by_links_button_download = itemView.findViewById(R.id.offline_new_by_links_button_download);
        }
    }
}
