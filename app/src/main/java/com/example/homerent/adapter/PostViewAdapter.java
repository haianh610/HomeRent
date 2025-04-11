package com.example.homerent.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat; // Thêm import này
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy; // Thêm import này
import com.example.homerent.R;
import com.example.homerent.model.Post;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set; // Thêm import này

public class PostViewAdapter extends RecyclerView.Adapter<PostViewAdapter.PostViewHolder> {

    private Context context;
    private List<Post> postList;
    private Set<String> savedPostIds; // Set chứa ID các tin đã lưu
    private OnPostActionListener listener; // Dùng interface chung
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    // Interface chung cho cả click vào item và click nút save
    public interface OnPostActionListener {
        void onPostClick(int position);
        void onSaveClick(int position); // Dùng tên này cho nhất quán
    }

    // Constructor cập nhật
    public PostViewAdapter(Context context, List<Post> postList, Set<String> savedPostIds, OnPostActionListener listener) {
        this.context = context;
        this.postList = postList;
        this.savedPostIds = savedPostIds; // Nhận Set ID đã lưu
        this.listener = listener;
        currencyFormat.setMaximumFractionDigits(0);
    }

    // Hàm để cập nhật savedPostIds từ Fragment
    public void updateSavedPostIds(Set<String> newSavedIds) {
        this.savedPostIds = newSavedIds;
        // Không cần notifyDataSetChanged() ở đây, Fragment sẽ gọi sau khi cập nhật
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_view, parent, false);
        return new PostViewHolder(view, listener); // Truyền listener
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.textViewTitle.setText(post.getTitle());
        holder.textViewPrice.setText(currencyFormat.format(post.getPrice()) + "/tháng");

        // Ghép địa chỉ ngắn gọn hơn cho list view (chỉ Quận/Huyện, Tỉnh/TP)
        String shortAddress = String.join(", ", notNull(post.getDistrict()), notNull(post.getCity()));
        shortAddress = shortAddress.replaceAll("(, )+", ", ").replaceAll("^, |, $", "");
        holder.textViewAddress.setText(shortAddress.isEmpty() ? "N/A" : shortAddress);

        if (post.getTimestamp() != null) {
            holder.textViewDate.setText(dateFormat.format(post.getTimestamp().toDate()));
        } else {
            holder.textViewDate.setText("N/A");
        }

        // Load ảnh
        if (post.getImageUrls() != null && !post.getImageUrls().isEmpty() && post.getImageUrls().get(0) != null) {
            Glide.with(context)
                    .load(post.getImageUrls().get(0))
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_image_error)
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache ảnh
                    .into(holder.imageViewPost);
        } else {
            holder.imageViewPost.setImageResource(R.drawable.ic_placeholder_image);
        }

        // Cập nhật icon bookmark dựa trên trạng thái lưu
        if (savedPostIds != null && savedPostIds.contains(post.getPostId())) {
            holder.buttonSave.setImageResource(R.drawable.bookmark_filled_24px); // Icon đã lưu
            holder.buttonSave.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray)); // Set màu nếu cần
        } else {
            holder.buttonSave.setImageResource(R.drawable.bookmark_24px); // Icon chưa lưu
            holder.buttonSave.setColorFilter(ContextCompat.getColor(context, android.R.color.darker_gray)); // Màu mặc định
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    // Hàm helper tránh null
    private String notNull(String value) {
        return value != null ? value : "";
    }


    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewPost;
        TextView textViewTitle, textViewPrice, textViewAddress, textViewDate;
        ImageView imageViewLocationIcon; // Thêm icon địa chỉ
        ImageButton buttonSave; // Nút bookmark

        // Constructor ViewHolder cập nhật
        public PostViewHolder(@NonNull View itemView, OnPostActionListener listener) {
            super(itemView);
            imageViewPost = itemView.findViewById(R.id.imageViewPostView);
            textViewTitle = itemView.findViewById(R.id.textViewTitleView);
            textViewPrice = itemView.findViewById(R.id.textViewPriceView);
            imageViewLocationIcon = itemView.findViewById(R.id.imageViewLocationIconView); // Ánh xạ icon
            textViewAddress = itemView.findViewById(R.id.textViewAddressView);
            textViewDate = itemView.findViewById(R.id.textViewDateView);
            buttonSave = itemView.findViewById(R.id.buttonSavePost);

            // Listener cho cả item view (mở chi tiết)
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onPostClick(position);
                    }
                }
            });

            // Listener cho nút bookmark
            buttonSave.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onSaveClick(position); // Gọi phương thức của interface
                    }
                }
            });
        }
    }
}