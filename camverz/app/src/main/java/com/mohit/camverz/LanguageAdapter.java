package com.mohit.camverz;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class LanguageAdapter extends RecyclerView.Adapter<LanguageAdapter.ViewHolder> {

    public interface OnLanguageClickListener {
        void onLanguageClick(LanguageModel model);
    }

    private final List<LanguageModel> originalList;
    private final List<LanguageModel> filteredList;
    private String selectedLanguageCode;
    private final OnLanguageClickListener listener;

    public LanguageAdapter(List<LanguageModel> list, String currentCode, OnLanguageClickListener listener) {
        this.originalList = new ArrayList<>(list);
        this.filteredList = new ArrayList<>(list);
        this.selectedLanguageCode = currentCode;
        this.listener = listener;
    }

    public void filter(String query) {
        filteredList.clear();
        if (query == null || query.trim().isEmpty()) {
            filteredList.addAll(originalList);
        } else {
            String lower = query.toLowerCase().trim();
            for (LanguageModel model : originalList) {
                if (model.getNameNative().toLowerCase().contains(lower) ||
                    model.getNameEnglish().toLowerCase().contains(lower)) {
                    filteredList.add(model);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void setSelectedLanguageCode(String code) {
        this.selectedLanguageCode = code;
        notifyDataSetChanged();
    }

    public String getSelectedLanguageCode() {
        return selectedLanguageCode;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_language_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LanguageModel item = filteredList.get(position);
        holder.tvFlag.setText(item.getFlagEmoji());
        holder.tvNameNative.setText(item.getNameNative());
        holder.tvNameEnglish.setText(item.getNameEnglish());

        boolean isSelected = item.getCode().equalsIgnoreCase(selectedLanguageCode);
        if (isSelected) {
            holder.ivIndicator.setVisibility(View.VISIBLE);
            holder.card.setBackgroundResource(R.drawable.bg_gender_card_selected);
        } else {
            holder.ivIndicator.setVisibility(View.GONE);
            holder.card.setBackgroundResource(R.drawable.bg_glass_card_premium);
        }

        holder.itemView.setOnClickListener(v -> {
            selectedLanguageCode = item.getCode();
            notifyDataSetChanged();
            if (listener != null) {
                listener.onLanguageClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filteredList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final View card;
        final TextView tvFlag;
        final TextView tvNameNative;
        final TextView tvNameEnglish;
        final ImageView ivIndicator;

        ViewHolder(View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.card_language);
            tvFlag = itemView.findViewById(R.id.tv_language_flag);
            tvNameNative = itemView.findViewById(R.id.tv_language_name_native);
            tvNameEnglish = itemView.findViewById(R.id.tv_language_name_english);
            ivIndicator = itemView.findViewById(R.id.iv_selection_indicator);
        }
    }
}
