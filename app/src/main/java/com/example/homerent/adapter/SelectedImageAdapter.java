package com.example.homerent.adapter;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.homerent.R;

import java.util.ArrayList;
import java.util.List;

public class SelectedImageAdapter extends RecyclerView.Adapter<SelectedImageAdapter.ImageViewHolder> {

    private Context context;
    private List<Object> imageItems;
    private OnImageRemoveListener listener;

    public interface OnImageRemoveListener {
        void onImageRemoved(int position);
    }

    public SelectedImageAdapter(Context context, List<Object> imageItems, OnImageRemoveListener listener) {
        this.context = context;
        this.imageItems = imageItems;
        this.listener = listener;
    }


    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_selected_image, parent, false);
        return new ImageViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Object item = imageItems.get(position);

        // Check the type of the item and load accordingly
        if (item instanceof Uri) {
            // It's a new image selected from the device
            Glide.with(context)
                    .load((Uri) item)
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_image_error)
                    .centerCrop()
                    .into(holder.imageView);
        } else if (item instanceof String) {
            // It's an existing image URL from Firebase Storage
            Glide.with(context)
                    .load((String) item)
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_image_error)
                    .centerCrop()
                    .into(holder.imageView);
        } else {
            // Handle unexpected type or show placeholder
            holder.imageView.setImageResource(R.drawable.ic_image_error);
        }
    }

    @Override
    public int getItemCount() {
        // Use the new list
        return imageItems.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ImageButton removeButton;

        ImageViewHolder(@NonNull View itemView, OnImageRemoveListener listener) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivSelectedImage);
            removeButton = itemView.findViewById(R.id.ibRemoveImage);

            removeButton.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onImageRemoved(position);
                    }
                }
            });
        }
    }
}