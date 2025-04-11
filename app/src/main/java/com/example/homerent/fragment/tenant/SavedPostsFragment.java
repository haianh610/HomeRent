package com.example.homerent.fragment.tenant;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homerent.R;
import com.example.homerent.activity.tenant.PostDetailTenantActivity;
import com.example.homerent.activity.tenant.TenantHomeActivity;
import com.example.homerent.adapter.PostViewAdapter;
import com.example.homerent.model.Post;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks; // Import Tasks
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot; // Import QuerySnapshot


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class SavedPostsFragment extends Fragment implements TenantHomeActivity.SearchableFragment, PostViewAdapter.OnPostActionListener {

    public static final String TAG = "SavedPostsFragment";
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private RecyclerView recyclerViewSavedPosts;
    private ProgressBar progressBar;
    private TextView textViewNoPosts;
    private PostViewAdapter adapter;
    private List<Post> allSavedPostList; // Danh sách gốc các tin đã lưu
    private List<Post> displayedSavedPostList; // Danh sách hiển thị (sau lọc)
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;

    private Set<String> savedPostIds = new HashSet<>(); // Luôn chứa ID các tin đang hiển thị

    public SavedPostsFragment() {
        // Required empty public constructor
    }

    public static SavedPostsFragment newInstance() {
        return new SavedPostsFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Khởi tạo db, auth ở đây thay vì onViewCreated để tránh null khi resume sớm
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();
        allSavedPostList = new ArrayList<>();
        displayedSavedPostList = new ArrayList<>();
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_saved_posts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerViewSavedPosts = view.findViewById(R.id.recyclerViewSavedPosts);
        progressBar = view.findViewById(R.id.progressBarSavedPosts);
        textViewNoPosts = view.findViewById(R.id.textViewNoSavedPosts);

        // Adapter dùng displayed list và luôn truyền savedPostIds (vì tất cả đều đã lưu)
        adapter = new PostViewAdapter(requireContext(), displayedSavedPostList, savedPostIds, this);
        recyclerViewSavedPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewSavedPosts.setAdapter(adapter);

        loadSavedPosts();
    }

    private void loadSavedPosts() {
        if (currentUser == null) {
            textViewNoPosts.setText("Vui lòng đăng nhập để xem tin đã lưu.");
            textViewNoPosts.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
            recyclerViewSavedPosts.setVisibility(View.GONE);
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        textViewNoPosts.setVisibility(View.GONE);
        recyclerViewSavedPosts.setVisibility(View.GONE);

        // 1. Lấy danh sách ID đã lưu
        db.collection("users").document(currentUser.getUid()).collection("savedPosts")
                .orderBy("savedAt", Query.Direction.DESCENDING) // Sắp xếp theo thời gian lưu mới nhất
                .get()
                .addOnCompleteListener(task -> {
                    if (!isAdded() || getActivity() == null) return;

                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot savedIdSnapshots = task.getResult();
                        if (savedIdSnapshots.isEmpty()) {
                            // Không có tin nào được lưu
                            handleEmptySavedPosts();
                        } else {
                            // 2. Tạo danh sách các Task để lấy chi tiết từng bài đăng
                            List<Task<DocumentSnapshot>> postDetailTasks = new ArrayList<>();
                            List<String> currentSavedIds = new ArrayList<>(); // List ID tạm thời
                            for (QueryDocumentSnapshot doc : savedIdSnapshots) {
                                String postId = doc.getId();
                                currentSavedIds.add(postId); // Thêm vào list tạm
                                DocumentReference postRef = db.collection("posts").document(postId);
                                postDetailTasks.add(postRef.get());
                            }

                            // Cập nhật Set savedPostIds chính
                            this.savedPostIds = new HashSet<>(currentSavedIds);
                            adapter.updateSavedPostIds(this.savedPostIds); // Cập nhật adapter ngay

                            // 3. Đợi tất cả Task lấy chi tiết hoàn thành
                            Tasks.whenAllSuccess(postDetailTasks).addOnCompleteListener(allTasks -> {
                                if (!isAdded() || getActivity() == null) return;
                                progressBar.setVisibility(View.GONE);

                                if (allTasks.isSuccessful()) {
                                    allSavedPostList.clear();
                                    List<Object> results = allTasks.getResult();
                                    for (Object result : results) {
                                        if (result instanceof DocumentSnapshot) {
                                            DocumentSnapshot postSnapshot = (DocumentSnapshot) result;
                                            if (postSnapshot.exists()) {
                                                try {
                                                    Post post = postSnapshot.toObject(Post.class);
                                                    if (post != null) {
                                                        post.setPostId(postSnapshot.getId());
                                                        // Kiểm tra xem ID này còn trong danh sách savedIds không
                                                        // (phòng trường hợp user bỏ lưu trong lúc đang load)
                                                        if (this.savedPostIds.contains(post.getPostId())) {
                                                            allSavedPostList.add(post);
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    Log.e(TAG, "Error converting saved post: " + postSnapshot.getId(), e);
                                                }
                                            } else {
                                                Log.w(TAG, "Saved post document does not exist anymore: " + postSnapshot.getId());
                                                // Có thể xóa ID này khỏi savedPosts của user ở đây nếu muốn tự động dọn dẹp
                                                this.savedPostIds.remove(postSnapshot.getId()); // Xóa khỏi Set hiện tại
                                                removeNonExistentSavedPost(postSnapshot.getId()); // Xóa khỏi Firestore
                                            }
                                        }
                                    }
                                    Log.d(TAG, "Loaded details for " + allSavedPostList.size() + " saved posts.");
                                    filterAndDisplaySavedPosts(""); // Hiển thị ban đầu
                                } else {
                                    Log.w(TAG, "Error getting some post details.", allTasks.getException());
                                    textViewNoPosts.setText("Lỗi khi tải chi tiết tin đã lưu.");
                                    textViewNoPosts.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        Log.w(TAG, "Error getting saved post IDs.", task.getException());
                        textViewNoPosts.setText("Lỗi tải danh sách tin đã lưu.");
                        textViewNoPosts.setVisibility(View.VISIBLE);
                    }
                });
    }

    // Hàm xóa ID của bài đăng không còn tồn tại khỏi danh sách lưu của user
    private void removeNonExistentSavedPost(String postId) {
        if (currentUser != null && postId != null) {
            db.collection("users").document(currentUser.getUid())
                    .collection("savedPosts").document(postId)
                    .delete()
                    .addOnSuccessListener(aVoid -> Log.i(TAG, "Removed non-existent post ID from saved list: " + postId))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed to remove non-existent post ID: " + postId, e));
        }
    }

    private void handleEmptySavedPosts() {
        progressBar.setVisibility(View.GONE);
        allSavedPostList.clear();
        displayedSavedPostList.clear();
        savedPostIds.clear(); // Xóa hết ID
        adapter.notifyDataSetChanged();
        textViewNoPosts.setText("Bạn chưa lưu tin đăng nào.");
        textViewNoPosts.setVisibility(View.VISIBLE);
        recyclerViewSavedPosts.setVisibility(View.GONE);
    }

    // Lọc và cập nhật RecyclerView cho tin đã lưu
    private void filterAndDisplaySavedPosts(String query) {
        displayedSavedPostList.clear();
        if (TextUtils.isEmpty(query)) {
            displayedSavedPostList.addAll(allSavedPostList);
        } else {
            String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
            List<Post> filteredList = allSavedPostList.stream()
                    .filter(post -> (post.getTitle() != null && post.getTitle().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) ||
                            (post.getDescription() != null && post.getDescription().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) ||
                            (post.getAddress() != null && post.getAddress().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) ||
                            (post.getWard() != null && post.getWard().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) ||
                            (post.getDistrict() != null && post.getDistrict().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) ||
                            (post.getCity() != null && post.getCity().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) )
                    .collect(Collectors.toList());
            displayedSavedPostList.addAll(filteredList);
        }

        if (displayedSavedPostList.isEmpty()) {
            textViewNoPosts.setText(TextUtils.isEmpty(query) ? "Bạn chưa lưu tin đăng nào." : "Không tìm thấy kết quả.");
            textViewNoPosts.setVisibility(View.VISIBLE);
            recyclerViewSavedPosts.setVisibility(View.GONE);
        } else {
            textViewNoPosts.setVisibility(View.GONE);
            recyclerViewSavedPosts.setVisibility(View.VISIBLE);
        }
        // Cập nhật lại Set ID cho adapter (dù không thay đổi nhiều ở đây nhưng để nhất quán)
        adapter.updateSavedPostIds(this.savedPostIds);
        adapter.notifyDataSetChanged();
    }

    // --- Interface Implementations ---

    @Override
    public void onSearchQuery(String query) {
        Log.d(TAG, "Search query received in SavedPosts: " + query);
        filterAndDisplaySavedPosts(query);
    }

    @Override
    public void onPostClick(int position) {
        if (position >= 0 && position < displayedSavedPostList.size()) {
            Post clickedPost = displayedSavedPostList.get(position);
            Intent intent = new Intent(getActivity(), PostDetailTenantActivity.class);
            intent.putExtra("POST_ID", clickedPost.getPostId());
            startActivity(intent);
        }
    }

    @Override
    public void onSaveClick(int position) {
        // Trong màn hình Saved, nút này chỉ có chức năng Bỏ lưu
        if (currentUser == null) return; // Cần user để bỏ lưu
        if (position >= 0 && position < displayedSavedPostList.size()) {
            Post postToUnsave = displayedSavedPostList.get(position);
            String postId = postToUnsave.getPostId();
            if (postId == null) return;

            // Hiển thị dialog xác nhận trước khi bỏ lưu (Optional but recommended)
            new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Bỏ lưu tin")
                    .setMessage("Bạn có chắc muốn bỏ lưu tin này?")
                    .setPositiveButton("Đồng ý", (dialog, which) -> {
                        // Thực hiện bỏ lưu trên Firestore
                        DocumentReference savedPostRef = db.collection("users").document(currentUser.getUid())
                                .collection("savedPosts").document(postId);
                        savedPostRef.delete()
                                .addOnSuccessListener(aVoid -> {
                                    if (isAdded() && getActivity() != null) {
                                        Log.d(TAG, "Post unsaved successfully from SavedPosts: " + postId);
                                        // Xóa khỏi cả 2 danh sách và Set ID
                                        savedPostIds.remove(postId);
                                        allSavedPostList.removeIf(p -> Objects.equals(p.getPostId(), postId));
                                        displayedSavedPostList.remove(position);
                                        // Cập nhật adapter
                                        adapter.notifyItemRemoved(position);
                                        adapter.notifyItemRangeChanged(position, displayedSavedPostList.size()); // Cập nhật các vị trí sau đó
                                        Toast.makeText(requireContext(), "Đã bỏ lưu tin", Toast.LENGTH_SHORT).show();
                                        // Kiểm tra nếu danh sách rỗng sau khi xóa
                                        if(displayedSavedPostList.isEmpty()){
                                            handleEmptySavedPosts(); // Cập nhật lại trạng thái không có tin
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    if (isAdded() && getActivity() != null) {
                                        Toast.makeText(requireContext(), "Lỗi khi bỏ lưu tin", Toast.LENGTH_SHORT).show();
                                        Log.w(TAG, "Error unsaving post from SavedPosts", e);
                                    }
                                });
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Load lại danh sách tin đã lưu khi quay lại fragment
        // để cập nhật nếu có thay đổi từ màn hình chi tiết
        loadSavedPosts();
        // Cập nhật lại title toolbar khi quay lại fragment này
        if (getActivity() instanceof TenantHomeActivity) {
            ((TenantHomeActivity) getActivity()).updateToolbarTitle(TAG);
        }
    }
}