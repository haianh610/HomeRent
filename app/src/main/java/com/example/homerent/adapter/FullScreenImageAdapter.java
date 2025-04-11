package com.example.homerent.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.homerent.R;
import com.github.chrisbanes.photoview.PhotoView; // Import PhotoView

import java.util.List;

public class FullScreenImageAdapter extends RecyclerView.Adapter<FullScreenImageAdapter.FullScreenViewHolder> {

    private Context context;
    private List<String> imageUrls;

    public FullScreenImageAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public FullScreenViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Tạo layout item_fullscreen_image.xml chỉ chứa PhotoView
        View view = LayoutInflater.from(context).inflate(R.layout.item_fullscreen_image, parent, false);
        return new FullScreenViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FullScreenViewHolder holder, int position) {
        String imageUrl = imageUrls.get(position);
        Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder_image) // Ảnh chờ
                .error(R.drawable.ic_image_error) // Ảnh lỗi
                .into(holder.photoView); // Load vào PhotoView
    }

    @Override
    public int getItemCount() {
        return imageUrls == null ? 0 : imageUrls.size();
    }

    static class FullScreenViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView; // Sử dụng PhotoView

        FullScreenViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photoViewItem); // ID trong item_fullscreen_image.xml
        }
    }
}