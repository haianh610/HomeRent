package com.example.homerent.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.homerent.R;
import com.example.homerent.model.Post;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class PostViewAdapter extends RecyclerView.Adapter<PostViewAdapter.PostViewHolder> {

    private Context context;
    private List<Post> postList;
    private OnPostClickListener postClickListener;
    private OnSaveClickListener saveClickListener;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));

    public interface OnPostClickListener {
        void onPostClick(int position);
    }

    public interface OnSaveClickListener {
        void onSaveClick(int position);
    }

    public PostViewAdapter(Context context, List<Post> postList, OnPostClickListener postatihClickListener, OnSaveClickListener saveClickListener) {
        this.context = context;
        this.postList = postList;
        this.postClickListener = postClickListener;
        this.saveClickListener = saveClickListener;
        currencyFormat.setMaximumFractionDigits(0);
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_view, parent, false);
        return new PostViewHolder(view, postClickListener, saveClickListener);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = postList.get(position);

        holder.textViewTitle.setText(post.getTitle());
        holder.textViewPrice.setText(currencyFormat.format(post.getPrice()) + "/tháng");

        String address = String.join(", ", post.getAddress(), post.getWard(), post.getDistrict(), post.getCity());
        address = address.replaceAll("(, )+", ", ").replaceAll("^, |, $", "");
        holder.textViewAddress.setText(address);

        if (post.getTimestamp() != null) {
            holder.textViewDate.setText(dateFormat.format(post.getTimestamp().toDate()));
        } else {
            holder.textViewDate.setText("N/A");
        }

        if (post.getImageUrls() != null && !post.getImageUrls().isEmpty() && post.getImageUrls().get(0) != null) {
            Glide.with(context)
                    .load(post.getImageUrls().get(0))
                    .placeholder(R.drawable.ic_placeholder_image)
                    .error(R.drawable.ic_image_error)
                    .into(holder.imageViewPost);
        } else {
            holder.imageViewPost.setImageResource(R.drawable.ic_placeholder_image);
        }
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView imageViewPost;
        TextView textViewTitle, textViewPrice, textViewAddress, textViewDate;
        ImageButton buttonSave;

        public PostViewHolder(@NonNull View itemView, OnPostClickListener postClickListener, OnSaveClickListener saveClickListener) {
            super(itemView);
            imageViewPost = itemView.findViewById(R.id.imageViewPostView);
            textViewTitle = itemView.findViewById(R.id.textViewTitleView);
            textViewPrice = itemView.findViewById(R.id.textViewPriceView);
            textViewAddress = itemView.findViewById(R.id.textViewAddressView);
            textViewDate = itemView.findViewById(R.id.textViewDateView);
            buttonSave = itemView.findViewById(R.id.buttonSavePost);

            itemView.setOnClickListener(v -> {
                if (postClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        postClickListener.onPostClick(position);
                    }
                }
            });

            buttonSave.setOnClickListener(v -> {
                if (saveClickListener != null) {
                    int position = getAdapterPosition();
                    if (position != RecyclerView.NO_POSITION) {
                        saveClickListener.onSaveClick(position);
                    }
                }
            });
        }
    }
}