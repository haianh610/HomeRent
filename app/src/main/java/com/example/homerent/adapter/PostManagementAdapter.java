package com.example.homerent.adapter; // Thay đổi package name

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // Thêm thư viện Glide vào build.gradle
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.homerent.R; // Thay R
import com.example.homerent.model.Post;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PostManagementAdapter extends RecyclerView.Adapter<PostManagementAdapter.PostViewHolder> {

    private Context context;
    private List<Post> postList;
    private OnPostActionListener listener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));


    // Interface để xử lý click trong Activity
    public interface OnPostActionListener {
        void onPostClick(int position);
        void onEditClick(int position);
        void onDeleteClick(int position);
    }

    public PostManagementAdapter(Context context, List<Post> postList, OnPostActionListener listener) {
        this.context = context;
        this.postList = postList;
        this.listener = listener;
        currencyFormat.setMaximumFractionDigits(0); // Bỏ phần thập phân (vd: 11.000.000 ₫)
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_management, parent, false);
        return new PostViewHolder(view, listener);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.textViewTitle.setText(post.getTitle());

        // Format giá tiền
        holder.textViewPrice.setText(currencyFormat.format(post.getPrice()) + "/tháng");

        // Ghép địa chỉ
        String address = String.join(", ", post.getAddress(), post.getWard(), post.getDistrict(), post.getCity());
        // Loại bỏ các phần null hoặc rỗng nếu có (optional)
        address = address.replaceAll("(, )+", ", ").replaceAll("^, |, $", "");
        holder.textViewAddress.setText(address);


        // Format ngày đăng
        if (post.getTimestamp() != null) {
            holder.textViewDate.setText(dateFormat.format(post.getTimestamp().toDate()));
        } else {
            holder.textViewDate.setText("N/A");
        }

        // Load hình ảnh đầu tiên (nếu có) bằng Glide
        if (post.getImageUrls() != null && !post.getImageUrls().isEmpty() && post.getImageUrls().get(0) != null) {
            Glide.with(context)
                    .load(post.getImageUrls().get(0))
                    .placeholder(R.drawable.ic_placeholder_image) // Ảnh chờ load
                    .error(R.drawable.ic_image_error) // Ảnh khi lỗi load
                    .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache ảnh
                    .into(holder.imageViewPost);
        } else {
            // Nếu không có ảnh, hiển thị ảnh placeholder
            holder.imageViewPost.setImageResource(R.drawable.ic_placeholder_image);
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    // --- ViewHolder ---
    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewPost;
        TextView textViewTitle, textViewPrice, textViewAddress, textViewDate;
        ImageButton buttonEdit, buttonDelete;

        public PostViewHolder(@NonNull View itemView, OnPostActionListener listener) {
            super(itemView);
            imageViewPost = itemView.findViewById(R.id.imageViewPostManage);
            textViewTitle = itemView.findViewById(R.id.textViewTitleManage);
            textViewPrice = itemView.findViewById(R.id.textViewPriceManage);
            textViewAddress = itemView.findViewById(R.id.textViewAddressManage);
            textViewDate = itemView.findViewById(R.id.textViewDateManage);
            buttonEdit = itemView.findViewById(R.id.buttonEditPost);
            buttonDelete = itemView.findViewById(R.id.buttonDeletePost);

            // Gán listener cho cả item view, nút sửa, nút xóa
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onPostClick(position);
                    }
                }
            });

            buttonEdit.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onEditClick(position);
                    }
                }
            });

            buttonDelete.setOnClickListener(v -> {
                if (listener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        listener.onDeleteClick(position);
                    }
                }
            });
        }
    }
}