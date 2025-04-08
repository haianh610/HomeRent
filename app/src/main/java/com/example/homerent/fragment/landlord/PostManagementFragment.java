package com.example.homerent.fragment.landlord;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homerent.R;
//import com.example.homerent.activity.landlord.CreatePostActivity;
//import com.example.homerent.activity.landlord.EditPostActivity;
//import com.example.homerent.activity.common.PostDetailLandlordActivity;
import com.example.homerent.activity.landlord.CreatePostActivity;
import com.example.homerent.activity.landlord.PostDetailLandlordActivity;
import com.example.homerent.adapter.PostManagementAdapter;
import com.example.homerent.model.Post;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.Timestamp; // Thêm import này
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Collections; // Thêm import này
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean; // Thêm import này

public class PostManagementFragment extends Fragment implements PostManagementAdapter.OnPostActionListener {

    private static final String TAG = "PostManagementFragment";

    private RecyclerView recyclerViewManagePosts;
    private FloatingActionButton fabAddPost;
    private ProgressBar progressBar;
    private TextView textViewNoPosts;

    private PostManagementAdapter adapter;
    private List<Post> postList;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private FirebaseUser currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post_management, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();
        currentUser = mAuth.getCurrentUser();

        recyclerViewManagePosts = view.findViewById(R.id.recyclerViewManagePostsFrag);
        fabAddPost = view.findViewById(R.id.fabAddPostFrag);
        progressBar = view.findViewById(R.id.progressBarManagePostsFrag);
        textViewNoPosts = view.findViewById(R.id.textViewNoPostsFrag);

        postList = new ArrayList<>();
        adapter = new PostManagementAdapter(requireContext(), postList, this);
        recyclerViewManagePosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewManagePosts.setAdapter(adapter);

        // --- Sự kiện nút FAB ---
        // Sự kiện click thông thường (mở màn hình tạo tin)
        fabAddPost.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), CreatePostActivity.class);
            startActivity(intent);
        });

        // *** Sự kiện nhấn giữ (Long Click) để tạo tin mẫu ***
        fabAddPost.setOnLongClickListener(v -> {
            if (currentUser != null) {
                createAndSaveSamplePost(currentUser.getUid());
            } else {
                Toast.makeText(requireContext(), "Vui lòng đăng nhập để tạo tin mẫu", Toast.LENGTH_SHORT).show();
            }
            return true; // Quan trọng: trả về true để báo rằng sự kiện long click đã được xử lý
            // và không kích hoạt sự kiện onClick thông thường sau đó.
        });


        if (currentUser != null) {
            loadUserPosts(currentUser.getUid());
        } else {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
        }
    }

    // --- Hàm tạo và lưu tin mẫu ---
    private void createAndSaveSamplePost(String userId) {
        Log.d(TAG, "Creating sample post for user: " + userId);
        Toast.makeText(requireContext(), "Đang tạo tin mẫu...", Toast.LENGTH_SHORT).show();

        // Tạo dữ liệu mẫu
        long currentTimeMillis = System.currentTimeMillis(); // Để tạo title khác nhau
        Post samplePost = new Post();
        samplePost.setUserId(userId);
        samplePost.setTitle("Tin đăng Mẫu - " + currentTimeMillis);
        samplePost.setDescription("Đây là mô tả chi tiết cho tin đăng mẫu. Nhà đẹp, giá tốt, liên hệ ngay!");
        samplePost.setAddress("123 Đường Mẫu");
        samplePost.setWard("Phường Mẫu");
        samplePost.setDistrict("Quận Thử Nghiệm");
        samplePost.setCity("Thành phố Demo");
        samplePost.setArea(65.5); // Diện tích mẫu
        samplePost.setPrice(12000000); // Giá mẫu: 12 triệu
        samplePost.setBedrooms(2); // Số phòng ngủ mẫu
        samplePost.setFloors(4); // Số tầng mẫu
        samplePost.setImageUrls(Collections.emptyList()); // Không có ảnh mẫu hoặc dùng placeholder URL nếu có
        samplePost.setTimestamp(Timestamp.now()); // Thời gian hiện tại
        samplePost.setLatitude(0); // Vĩ độ mẫu
        samplePost.setLongitude(0); // Kinh độ mẫu
        samplePost.setAvailable(true); // Mặc định là còn trống
        long oneMonthInMillis = 30L * 24 * 60 * 60 * 1000; // Khoảng 1 tháng
        samplePost.setStartDate(Timestamp.now()); // Ngày bắt đầu là hiện tại
        samplePost.setEndDate(new Timestamp(new Date(System.currentTimeMillis() + oneMonthInMillis)));

        // Lưu vào Firestore
        db.collection("posts")
                .add(samplePost) // Dùng add() để Firestore tự tạo ID
                .addOnSuccessListener(documentReference -> {
                    if (!isAdded()) return; // Kiểm tra fragment còn tồn tại
                    Log.d(TAG, "Sample post added with ID: " + documentReference.getId());
                    Toast.makeText(requireContext(), "Đã tạo tin mẫu thành công!", Toast.LENGTH_SHORT).show();
                    // Load lại danh sách để hiển thị tin mới
                    loadUserPosts(userId);
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    Log.w(TAG, "Error adding sample post", e);
                    Toast.makeText(requireContext(), "Lỗi khi tạo tin mẫu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    @Override
    public void onResume() {
        super.onResume();
        if (currentUser != null && isAdded()) {
            loadUserPosts(currentUser.getUid());
        }
    }

    // --- Các hàm loadUserPosts, onPostClick, onEditClick, onDeleteClick, deletePostFromFirestore, deletePostImagesFromStorage, handleImageDeletionCompletion ---
    // (Giữ nguyên các hàm này như trước)
    private void loadUserPosts(String userId) {
        if (!isAdded()) return;
        Log.d(TAG, "Loading posts for user: " + userId);
        // --- Tạm ẩn ProgressBar và NoPosts Text khi load ---
        //progressBar.setVisibility(View.VISIBLE);
        //textViewNoPosts.setVisibility(View.GONE);
        //recyclerViewManagePosts.setVisibility(View.GONE);
        // --- Hiển thị ProgressBar khi bắt đầu load ---
        progressBar.setVisibility(View.VISIBLE);
        textViewNoPosts.setVisibility(View.GONE);
        recyclerViewManagePosts.setVisibility(View.GONE);


        db.collection("posts")
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE); // Ẩn ProgressBar khi load xong
                    if (task.isSuccessful()) {
                        postList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            try {
                                Post post = document.toObject(Post.class);
                                post.setPostId(document.getId());
                                postList.add(post);
                            } catch (Exception e) {
                                Log.e(TAG, "Error converting document: " + document.getId(), e);
                                // Có thể hiển thị thông báo lỗi hoặc bỏ qua tin này
                            }
                        }
                        adapter.notifyDataSetChanged();

                        if (postList.isEmpty()) {
                            textViewNoPosts.setVisibility(View.VISIBLE);
                            recyclerViewManagePosts.setVisibility(View.GONE);
                            Log.d(TAG, "No posts found.");
                        } else {
                            textViewNoPosts.setVisibility(View.GONE);
                            recyclerViewManagePosts.setVisibility(View.VISIBLE);
                            Log.d(TAG, "Total posts loaded: " + postList.size());
                        }
                    } else {
                        Log.w(TAG, "Error getting documents.", task.getException());
                        Toast.makeText(requireContext(), "Lỗi tải tin đăng: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        textViewNoPosts.setVisibility(View.VISIBLE);
                        textViewNoPosts.setText("Không thể tải tin đăng");
                        recyclerViewManagePosts.setVisibility(View.GONE);
                    }
                });
    }

    @Override
    public void onPostClick(int position) {
        if (!isAdded() || position < 0 || position >= postList.size()) return;
        Post selectedPost = postList.get(position);
        Intent intent = new Intent(getActivity(), PostDetailLandlordActivity.class);
        intent.putExtra("POST_ID", selectedPost.getPostId());
        startActivity(intent);
        Log.d(TAG, "Clicked post: " + selectedPost.getTitle());
    }

    @Override
    public void onEditClick(int position) {
        if (!isAdded() || position < 0 || position >= postList.size()) return;
        Post selectedPost = postList.get(position);
//        Intent intent = new Intent(getActivity(), EditPostActivity.class);
//        intent.putExtra("POST_ID", selectedPost.getPostId());
//        startActivity(intent);
        Log.d(TAG, "Edit post: " + selectedPost.getTitle());
    }

    @Override
    public void onDeleteClick(int position) {
        if (!isAdded() || position < 0 || position >= postList.size()) return;
        Post postToDelete = postList.get(position);
        Log.d(TAG, "Attempting to delete post: " + postToDelete.getTitle());

        new AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận xóa")
                .setMessage("Bạn có chắc chắn muốn xóa tin đăng '" + postToDelete.getTitle() + "' không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    // Kiểm tra lại vị trí trước khi xóa
                    if (position >= 0 && position < postList.size()) {
                        deletePostFromFirestore(postList.get(position), position);
                    } else {
                        Log.e(TAG, "Invalid position on delete confirmation: " + position);
                        Toast.makeText(requireContext(), "Lỗi vị trí tin đăng, vui lòng thử lại.", Toast.LENGTH_SHORT).show();
                        if (currentUser != null) loadUserPosts(currentUser.getUid()); // Load lại list cho chắc
                    }
                })
                .setNegativeButton("Hủy", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void deletePostFromFirestore(Post post, int position) {
        if (!isAdded()) return;
        String postId = post.getPostId();
        if (postId == null || postId.isEmpty()) {
            Toast.makeText(requireContext(), "Không thể xóa tin (ID không hợp lệ)", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        // --- Sử dụng AtomicBoolean để đảm bảo chỉ gọi xóa ảnh 1 lần ---
        AtomicBoolean imageDeletionStarted = new AtomicBoolean(false);

        db.collection("posts").document(postId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    if (!isAdded()) return;
                    Log.d(TAG, "Post document successfully deleted: " + postId);
                    // Chỉ gọi xóa ảnh nếu chưa gọi trước đó
                    if (imageDeletionStarted.compareAndSet(false, true)) {
                        deletePostImagesFromStorage(post.getImageUrls(), postId, position);
                    }
                })
                .addOnFailureListener(e -> {
                    if (!isAdded()) return;
                    progressBar.setVisibility(View.GONE);
                    Log.w(TAG, "Error deleting post document: " + postId, e);
                    Toast.makeText(requireContext(), "Lỗi xóa tin đăng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void deletePostImagesFromStorage(List<String> imageUrls, String postId, int originalPosition) {
        if (!isAdded()) return;

        // Xác định lại vị trí hiện tại của item trong list trước khi xóa ảnh
        // Vì list có thể đã thay đổi do load lại hoặc thao tác khác
        int currentPosition = -1;
        for(int i=0; i<postList.size(); i++){
            if(postList.get(i).getPostId() != null && postList.get(i).getPostId().equals(postId)){
                currentPosition = i;
                break;
            }
        }
        final int finalCurrentPosition = currentPosition; // Vị trí cuối cùng để xóa khỏi adapter

        if (imageUrls == null || imageUrls.isEmpty()) {
            Log.d(TAG, "No images to delete for post: " + postId);
            handleImageDeletionCompletion(false, finalCurrentPosition, postId); // Gọi hàm xử lý hoàn thành ngay
            return;
        }

        final int totalImages = imageUrls.size();
        // Sử dụng AtomicInteger thay vì mảng int[] để đảm bảo thread-safe khi cập nhật count
        final java.util.concurrent.atomic.AtomicInteger deletedCount = new java.util.concurrent.atomic.AtomicInteger(0);
        final AtomicBoolean errorOccurred = new AtomicBoolean(false);

        for (String imageUrl : imageUrls) {
            if (imageUrl == null || imageUrl.trim().isEmpty()) {
                if(deletedCount.incrementAndGet() == totalImages && isAdded()){
                    handleImageDeletionCompletion(errorOccurred.get(), finalCurrentPosition, postId);
                }
                continue;
            }

            if (!isAdded()) return;

            try {
                StorageReference photoRef = storage.getReferenceFromUrl(imageUrl);
                photoRef.delete().addOnCompleteListener(task -> {
                    if (!isAdded()) return; // Kiểm tra lại sau khi task hoàn thành
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Successfully deleted image: " + imageUrl);
                    } else {
                        Log.w(TAG, "Error deleting image: " + imageUrl, task.getException());
                        errorOccurred.set(true);
                    }
                    // Dù thành công hay thất bại, tăng count và kiểm tra hoàn thành
                    if (deletedCount.incrementAndGet() == totalImages) {
                        handleImageDeletionCompletion(errorOccurred.get(), finalCurrentPosition, postId);
                    }
                });
            } catch (IllegalArgumentException e){
                Log.e(TAG, "Invalid image URL, skipping deletion: " + imageUrl, e);
                errorOccurred.set(true);
                if(deletedCount.incrementAndGet() == totalImages && isAdded()){
                    handleImageDeletionCompletion(errorOccurred.get(), finalCurrentPosition, postId);
                }
            }
        }
    }

    private void handleImageDeletionCompletion(boolean errorOccurred, int position, String postId) {
        if (!isAdded()) return;

        requireActivity().runOnUiThread(() -> {
            if (!isAdded()) return;

            progressBar.setVisibility(View.GONE);

            // Kiểm tra lại vị trí và ID trước khi xóa khỏi adapter
            if (position != -1 && position < postList.size() && Objects.equals(postList.get(position).getPostId(), postId)) {
                postList.remove(position);
                adapter.notifyItemRemoved(position);
                // Cập nhật lại vị trí các item còn lại sau khi xóa
                adapter.notifyItemRangeChanged(position, postList.size() - position);
                if (postList.isEmpty()) {
                    textViewNoPosts.setVisibility(View.VISIBLE);
                    recyclerViewManagePosts.setVisibility(View.GONE);
                }
            } else {
                Log.w(TAG,"Position (" + position + ") invalid or item mismatch during image deletion completion for post " + postId + ". List size: " + postList.size());
                // Không xóa item khỏi adapter nếu vị trí không hợp lệ, nhưng vẫn thông báo
            }

            if (errorOccurred) {
                Toast.makeText(requireContext(), "Đã xóa tin đăng (có lỗi khi xóa một số ảnh)", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(requireContext(), "Đã xóa tin đăng và ảnh thành công", Toast.LENGTH_SHORT).show();
            }

            // Cân nhắc load lại toàn bộ list nếu có lỗi hoặc vị trí không khớp để đảm bảo đồng bộ
            // if (errorOccurred || position == -1) {
            //     if (currentUser != null) loadUserPosts(currentUser.getUid());
            // }
        });
    }

}