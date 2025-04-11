package com.example.homerent.fragment.tenant;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils; // Thêm import này
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast; // Thêm import này

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homerent.R;
import com.example.homerent.activity.tenant.PostDetailTenantActivity; // Import activity detail tenant
import com.example.homerent.activity.tenant.TenantHomeActivity; // Import để dùng interface
import com.example.homerent.adapter.PostViewAdapter;
import com.example.homerent.model.Post;
import com.google.firebase.auth.FirebaseAuth; // Thêm import này
import com.google.firebase.auth.FirebaseUser; // Thêm import này
import com.google.firebase.firestore.DocumentReference; // Thêm import này
import com.google.firebase.firestore.FieldValue; // Thêm import này
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query; // Thêm import này
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions; // Thêm import này


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet; // Thêm import này
import java.util.List;
import java.util.Locale; // Thêm import này
import java.util.Set; // Thêm import này
import java.util.stream.Collectors; // Thêm import này (Java 8+)

// Implement interface SearchableFragment
public class PostViewFragment extends Fragment implements TenantHomeActivity.SearchableFragment, PostViewAdapter.OnPostActionListener {

    public static final String TAG = "PostViewFragment"; // Thêm TAG để dùng trong Activity

    private RecyclerView recyclerViewPostView;
    private ProgressBar progressBar;
    private TextView textViewNoPosts;
    private PostViewAdapter adapter;
    private List<Post> allPostList; // Danh sách chứa tất cả bài post gốc
    private List<Post> displayedPostList; // Danh sách hiển thị (sau khi lọc)
    private FirebaseFirestore db;
    private FirebaseAuth mAuth; // Thêm FirebaseAuth
    private FirebaseUser currentUser; // Thêm FirebaseUser

    private Set<String> savedPostIds; // Set để lưu ID các tin đã lưu

    // Phương thức khởi tạo để Activity có thể tạo instance mới
    public static PostViewFragment newInstance() {
        return new PostViewFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        recyclerViewPostView = view.findViewById(R.id.recyclerViewPostView);
        progressBar = view.findViewById(R.id.progressBarPostView);
        textViewNoPosts = view.findViewById(R.id.textViewNoPostsView);

        allPostList = new ArrayList<>();
        displayedPostList = new ArrayList<>();
        savedPostIds = new HashSet<>();

        // Khởi tạo adapter với listener mới và danh sách hiển thị
        adapter = new PostViewAdapter(requireContext(), displayedPostList, savedPostIds, this); // Truyền savedPostIds và this làm listener
        recyclerViewPostView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewPostView.setAdapter(adapter);

        // Load dữ liệu lần đầu
        loadInitialData();
    }

    private void loadInitialData() {
        if (currentUser != null) {
            loadSavedPostIdsAndThenPosts(); // Load ID đã lưu trước, sau đó load posts
        } else {
            loadAllPosts(null); // Load posts mà không cần biết trạng thái lưu
        }
    }

    // Load danh sách ID bài đăng đã lưu của người dùng hiện tại
    private void loadSavedPostIdsAndThenPosts() {
        if (currentUser == null) {
            loadAllPosts(null); // Nếu không có user, cứ load posts
            return;
        }
        progressBar.setVisibility(View.VISIBLE); // Hiện progress khi load IDs
        db.collection("users").document(currentUser.getUid()).collection("savedPosts")
                .get()
                .addOnCompleteListener(task -> {
                    if (isAdded() && getActivity() != null) { // Kiểm tra fragment còn gắn với activity không
                        if (task.isSuccessful()) {
                            savedPostIds.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                savedPostIds.add(document.getId());
                            }
                            Log.d(TAG, "Loaded saved post IDs: " + savedPostIds.size());
                        } else {
                            Log.w(TAG, "Error getting saved posts IDs.", task.getException());
                            // Có thể hiển thị lỗi nhưng vẫn tiếp tục load posts
                        }
                        // Sau khi load xong ID (thành công hoặc thất bại), load danh sách posts
                        loadAllPosts(savedPostIds);
                    }
                });
    }


    // Load tất cả bài đăng và cập nhật trạng thái lưu nếu có
    private void loadAllPosts(@Nullable Set<String> currentSavedIds) {
        // progressBar đã được bật từ loadSavedPostIds hoặc bật ở đây nếu không có user
        if (currentUser == null) {
            progressBar.setVisibility(View.VISIBLE);
        }
        textViewNoPosts.setVisibility(View.GONE);
        recyclerViewPostView.setVisibility(View.GONE);

        db.collection("posts")
                .whereEqualTo("available", true) // Chỉ lấy tin còn trống
                .orderBy("timestamp", Query.Direction.DESCENDING) // Sắp xếp mới nhất lên đầu
                .get()
                .addOnCompleteListener(task -> {
                    if (isAdded() && getActivity() != null) {
                        progressBar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            allPostList.clear();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                try {
                                    Post post = document.toObject(Post.class);
                                    post.setPostId(document.getId());
                                    allPostList.add(post);
                                } catch (Exception e) {
                                    Log.e(TAG,"Error converting post: " + document.getId(), e);
                                }
                            }
                            Log.d(TAG, "Loaded all posts: " + allPostList.size());
                            // Ban đầu hiển thị tất cả
                            filterAndDisplayPosts("");

                        } else {
                            Log.w(TAG, "Error getting posts.", task.getException());
                            textViewNoPosts.setVisibility(View.VISIBLE);
                            textViewNoPosts.setText("Lỗi tải tin đăng");
                        }
                    }
                });
    }

    // Lọc và cập nhật RecyclerView
    private void filterAndDisplayPosts(String query) {
        displayedPostList.clear();
        if (TextUtils.isEmpty(query)) {
            displayedPostList.addAll(allPostList); // Hiển thị tất cả nếu query rỗng
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
            // Lọc theo tiêu đề, mô tả, địa chỉ (có thể mở rộng)
            List<Post> filteredList = allPostList.stream()
                    .filter(post -> (post.getTitle() != null && post.getTitle().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) ||
                            (post.getDescription() != null && post.getDescription().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) ||
                            (post.getAddress() != null && post.getAddress().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) ||
                            (post.getWard() != null && post.getWard().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) ||
                            (post.getDistrict() != null && post.getDistrict().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) ||
                            (post.getCity() != null && post.getCity().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) )
                    .collect(Collectors.toList());
            displayedPostList.addAll(filteredList);
        }

        if (displayedPostList.isEmpty()) {
            textViewNoPosts.setVisibility(View.VISIBLE);
            textViewNoPosts.setText(TextUtils.isEmpty(query) ? "Không có tin đăng nào." : "Không tìm thấy kết quả.");
            recyclerViewPostView.setVisibility(View.GONE);
        } else {
            textViewNoPosts.setVisibility(View.GONE);
            recyclerViewPostView.setVisibility(View.VISIBLE);
        }
        // Cập nhật lại savedPostIds cho adapter phòng trường hợp load bất đồng bộ
        adapter.updateSavedPostIds(this.savedPostIds);
        adapter.notifyDataSetChanged(); // Cập nhật adapter với danh sách đã lọc
    }


    // --- Triển khai Interface từ Activity ---
    @Override
    public void onSearchQuery(String query) {
        Log.d(TAG, "Search query received: " + query);
        filterAndDisplayPosts(query); // Gọi hàm lọc khi có query mới
    }

    // --- Triển khai Interface từ Adapter ---
    @Override
    public void onPostClick(int position) {
        if (position >= 0 && position < displayedPostList.size()) {
            Post clickedPost = displayedPostList.get(position);
            Intent intent = new Intent(getActivity(), PostDetailTenantActivity.class);
            intent.putExtra("POST_ID", clickedPost.getPostId());
            startActivity(intent);
        }
    }

    @Override
    public void onSaveClick(int position) {
        if (currentUser == null) {
            Toast.makeText(requireContext(), "Vui lòng đăng nhập để lưu tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (position >= 0 && position < displayedPostList.size()) {
            Post postToToggle = displayedPostList.get(position);
            String postId = postToToggle.getPostId();
            if (postId == null) return;

            DocumentReference savedPostRef = db.collection("users").document(currentUser.getUid())
                    .collection("savedPosts").document(postId);

            boolean isCurrentlySaved = savedPostIds.contains(postId);

            if (isCurrentlySaved) {
                // Thực hiện bỏ lưu
                savedPostRef.delete()
                        .addOnSuccessListener(aVoid -> {
                            if (isAdded() && getActivity() != null) {
                                savedPostIds.remove(postId);
                                adapter.notifyItemChanged(position); // Chỉ cập nhật item đó
                                Toast.makeText(requireContext(), "Đã bỏ lưu tin", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Post unsaved from list: " + postId);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (isAdded() && getActivity() != null) {
                                Toast.makeText(requireContext(), "Lỗi khi bỏ lưu", Toast.LENGTH_SHORT).show();
                                Log.w(TAG, "Error unsaving post from list", e);
                            }
                        });
            } else {
                // Thực hiện lưu
                // Lưu ID và có thể thêm timestamp hoặc thông tin cơ bản khác nếu muốn
                savedPostRef.set(new HashMap<String, Object>() {{
                            put("savedAt", FieldValue.serverTimestamp());
                            // put("title", postToToggle.getTitle()); // Ví dụ
                        }}, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            if (isAdded() && getActivity() != null) {
                                savedPostIds.add(postId);
                                adapter.notifyItemChanged(position); // Chỉ cập nhật item đó
                                Toast.makeText(requireContext(), "Đã lưu tin", Toast.LENGTH_SHORT).show();
                                Log.d(TAG, "Post saved from list: " + postId);
                            }
                        })
                        .addOnFailureListener(e -> {
                            if (isAdded() && getActivity() != null) {
                                Toast.makeText(requireContext(), "Lỗi khi lưu", Toast.LENGTH_SHORT).show();
                                Log.w(TAG, "Error saving post from list", e);
                            }
                        });
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cân nhắc load lại saved IDs khi quay lại fragment,
        // phòng trường hợp lưu/bỏ lưu ở màn hình chi tiết
        if (currentUser != null) {
            // Có thể chỉ load lại IDs và cập nhật adapter thay vì load lại toàn bộ posts
            loadSavedPostIdsAndUpdateAdapter();
        }
        // Cập nhật lại title toolbar khi quay lại fragment này
        if (getActivity() instanceof TenantHomeActivity) {
            ((TenantHomeActivity) getActivity()).updateToolbarTitle(TAG);
        }
    }

    // Hàm load lại chỉ saved IDs và cập nhật adapter
    private void loadSavedPostIdsAndUpdateAdapter() {
        if (currentUser == null) return;
        db.collection("users").document(currentUser.getUid()).collection("savedPosts")
                .get()
                .addOnCompleteListener(task -> {
                    if (isAdded() && getActivity() != null) {
                        if (task.isSuccessful()) {
                            Set<String> newSavedIds = new HashSet<>();
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                newSavedIds.add(document.getId());
                            }
                            // Chỉ cập nhật adapter nếu có sự thay đổi
                            if (!this.savedPostIds.equals(newSavedIds)) {
                                this.savedPostIds = newSavedIds;
                                adapter.updateSavedPostIds(this.savedPostIds);
                                adapter.notifyDataSetChanged(); // Hoặc tối ưu hơn chỉ cập nhật các item thay đổi
                                Log.d(TAG, "Refreshed saved post IDs on resume.");
                            }
                        } else {
                            Log.w(TAG, "Error refreshing saved posts IDs on resume.", task.getException());
                        }
                    }
                });
    }
}