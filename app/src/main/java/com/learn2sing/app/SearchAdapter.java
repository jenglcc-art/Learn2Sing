package com.learn2sing.app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for YouTube search results.
 */
public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(VideoItem item);
    }

    private final List<VideoItem> items = new ArrayList<>();
    private final OnItemClickListener listener;

    public SearchAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    public void setItems(List<VideoItem> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public ArrayList<String> getAllVideoIds() {
        ArrayList<String> ids = new ArrayList<>();
        for (VideoItem item : items) ids.add(item.getVideoId());
        return ids;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_video, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        VideoItem item = items.get(position);
        holder.bind(item, listener);
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnail;
        private final TextView  title;
        private final TextView  channel;

        ViewHolder(View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.iv_thumbnail);
            title     = itemView.findViewById(R.id.tv_title);
            channel   = itemView.findViewById(R.id.tv_channel);
        }

        void bind(VideoItem item, OnItemClickListener listener) {
            title.setText(item.getTitle());
            channel.setText(item.getChannelName());

            Glide.with(itemView.getContext())
                    .load(item.getThumbnailUrl())
                    .placeholder(R.drawable.ic_music_placeholder)
                    .into(thumbnail);

            itemView.setOnClickListener(v -> listener.onItemClick(item));
        }
    }
}
