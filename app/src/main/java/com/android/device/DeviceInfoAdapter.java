package com.android.device;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class DeviceInfoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_CATEGORY = 0;
    private static final int TYPE_ITEM = 1;

    private List<Object> items; // Can be String (category) or DeviceInfoItem
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(DeviceInfoItem item);
    }

    public DeviceInfoAdapter(List<Object> items, OnItemClickListener listener) {
        this.listener = listener;
        updateData(items);
    }

    public void updateData(List<Object> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position) instanceof String ? TYPE_CATEGORY : TYPE_ITEM;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_CATEGORY) {
            View view = inflater.inflate(R.layout.item_category, parent, false);
            return new CategoryViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_device_info, parent, false);
            return new ItemViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_CATEGORY) {
            CategoryViewHolder categoryHolder = (CategoryViewHolder) holder;
            String category = (String) items.get(position);
            categoryHolder.categoryText.setText(category);
        } else {
            ItemViewHolder itemHolder = (ItemViewHolder) holder;
            DeviceInfoItem item = (DeviceInfoItem) items.get(position);
            
            itemHolder.keyText.setText(item.getTranslatedKey());
            itemHolder.valueText.setText(item.getDisplayValue());
            itemHolder.originalKeyText.setText("(" + item.getOriginalKey() + ")");
            
            // 设置点击事件
            itemHolder.cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(item);
                }
            });
            
            // 长按显示原始值
            itemHolder.cardView.setOnLongClickListener(v -> {
                Toast.makeText(v.getContext(), item.getOriginalKey(), Toast.LENGTH_SHORT).show();
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryText;

        CategoryViewHolder(@NonNull View itemView) {
            super(itemView);
            categoryText = itemView.findViewById(R.id.category_text);
        }
    }

    static class ItemViewHolder extends RecyclerView.ViewHolder {
        MaterialCardView cardView;
        TextView keyText;
        TextView valueText;
        TextView originalKeyText;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.card_view);
            keyText = itemView.findViewById(R.id.key_text);
            valueText = itemView.findViewById(R.id.value_text);
            originalKeyText = itemView.findViewById(R.id.original_key_text);
        }
    }
}