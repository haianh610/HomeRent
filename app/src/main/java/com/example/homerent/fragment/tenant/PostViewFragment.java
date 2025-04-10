package com.example.homerent.fragment.tenant;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.homerent.R;
import com.example.homerent.adapter.PostViewAdapter;
import com.example.homerent.model.Post;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PostViewFragment extends Fragment {

    private RecyclerView recyclerViewPostView;
    private ProgressBar progressBar;
    private TextView textViewNoPosts;
    private PostViewAdapter adapter;
    private List<Post> postList;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_post_view, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();

        recyclerViewPostView = view.findViewById(R.id.recyclerViewPostView);
        progressBar = view.findViewById(R.id.progressBarPostView);
        textViewNoPosts = view.findViewById(R.id.textViewNoPostsView);

        postList = new ArrayList<>();
        adapter = new PostViewAdapter(requireContext(), postList, position -> {
            // Xử lý khi click vào bài đăng (mở chi tiết)
        }, position -> {
            // Xử lý lưu tin (placeholder, bạn sẽ thêm sau)
        });
        recyclerViewPostView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewPostView.setAdapter(adapter);

        loadAllPosts();
    }

    private void loadAllPosts() {
        progressBar.setVisibility(View.VISIBLE);
        textViewNoPosts.setVisibility(View.GONE);
        recyclerViewPostView.setVisibility(View.GONE);

        db.collection("posts")
                .whereEqualTo("available", true)
                .get()
                .addOnCompleteListener(task -> {
                    progressBar.setVisibility(View.GONE);
                    if (task.isSuccessful()) {
                        postList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Post post = document.toObject(Post.class);
                            post.setPostId(document.getId());
                            postList.add(post);
                        }
                        adapter.notifyDataSetChanged();
                        if (postList.isEmpty()) {
                            textViewNoPosts.setVisibility(View.VISIBLE);
                        } else {
                            recyclerViewPostView.setVisibility(View.VISIBLE);
                        }
                    } else {
                        textViewNoPosts.setVisibility(View.VISIBLE);
                        textViewNoPosts.setText("Lỗi tải tin đăng");
                    }
                });
    }
}