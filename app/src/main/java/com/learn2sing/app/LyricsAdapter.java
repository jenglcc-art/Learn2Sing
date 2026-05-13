package com.learn2sing.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for time-synced lyrics.
 * The active (current) line is highlighted in purple.
 */
public class LyricsAdapter extends RecyclerView.Adapter<LyricsAdapter.ViewHolder> {

    private final List<LyricsLine> lines = new ArrayList<>();
    private int activeIndex = -1;

    public void setLines(List<LyricsLine> newLines) {
        lines.clear();
        if (newLines != null) lines.addAll(newLines);
        activeIndex = -1;
        notifyDataSetChanged();
    }

    /**
     * Update the highlighted line based on current playback position.
     *
     * @param currentSeconds playback time in seconds
     * @return the index of the newly active line, or -1 if unchanged
     */
    public int updateActiveIndex(double currentSeconds) {
        int newIndex = -1;
        for (int i = lines.size() - 1; i >= 0; i--) {
            if (lines.get(i).getTimeSeconds() <= currentSeconds) {
                newIndex = i;
                break;
            }
        }
        if (newIndex != activeIndex) {
            int old = activeIndex;
            activeIndex = newIndex;
            if (old >= 0) notifyItemChanged(old);
            if (newIndex >= 0) notifyItemChanged(newIndex);
            return newIndex;
        }
        return -1;
    }

    public int getActiveIndex() { return activeIndex; }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_lyrics, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LyricsLine line = lines.get(position);
        holder.bind(line, position == activeIndex);
    }

    @Override
    public int getItemCount() { return lines.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvLyric;

        ViewHolder(View itemView) {
            super(itemView);
            tvLyric = itemView.findViewById(R.id.tv_lyric);
        }

        void bind(LyricsLine line, boolean isActive) {
            Context ctx = itemView.getContext();
            tvLyric.setText(line.getText());

            if (isActive) {
                tvLyric.setTextColor(ContextCompat.getColor(ctx, R.color.lyric_active));
                tvLyric.setTextSize(18f);
                tvLyric.setAlpha(1.0f);
            } else {
                tvLyric.setTextColor(ContextCompat.getColor(ctx, R.color.lyric_inactive));
                tvLyric.setTextSize(16f);
                tvLyric.setAlpha(0.65f);
            }
        }
    }
}
