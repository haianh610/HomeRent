package com.example.homerent.fragment.tenant;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils; // Thêm import này
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast; // Thêm import này

import android.app.Dialog; // Import Dialog
import android.content.DialogInterface;
import android.view.LayoutInflater; // Import LayoutInflater for dialogs
import android.widget.ArrayAdapter;
import android.widget.Button; // Import Button
import android.widget.ListView; // Import ListView
import androidx.appcompat.app.AlertDialog; // Import AlertDialog

import com.example.homerent.adapter.AutocompleteAdapter;
import com.example.homerent.model.Province;
import com.example.homerent.AddressUtils; // Use utility
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.AutocompletePrediction;
import com.google.android.libraries.places.api.model.AutocompleteSessionToken;
import com.google.android.libraries.places.api.model.TypeFilter;
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest;
import com.google.android.libraries.places.api.net.PlacesClient;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.chip.Chip; // Import Chip
import com.google.android.material.chip.ChipGroup; // Import ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.search.SearchBar; // Import M3 SearchBar
import com.google.android.material.search.SearchView; // Import M3 SearchView
import com.google.android.material.slider.RangeSlider; // Import RangeSlider

import java.text.NumberFormat; // For price formatting
import java.util.List; // Keep List
import java.util.Locale; // Keep Locale
import java.util.Objects; // Keep Objects
import java.util.stream.Collectors; // Keep Collectors

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
import com.example.homerent.model.Province;
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
public class PostViewFragment extends Fragment implements PostViewAdapter.OnPostActionListener, AutocompleteAdapter.OnItemClickListener {

    public static final String TAG = "PostViewFragment";
    private static final float SLIDER_VALUE_FROM = 0.0f;
    private static final float SLIDER_VALUE_TO = 50.0f;

    private RecyclerView recyclerViewPostView;
    private ProgressBar progressBar;
    private TextView textViewNoPosts;
    private PostViewAdapter adapter;
    private List<Post> allPostList;
    private List<Post> displayedPostList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private Set<String> savedPostIds;

    // --- New UI Elements ---
    private SearchBar searchBar;
    private SearchView searchView;
    private Chip chipProvince;
    private Chip chipPriceRange;
    private ChipGroup chipGroupFilters;

    // --- Filter Data ---
    private List<Province> allProvinces = new ArrayList<>();
    private Province selectedProvince = null; // Initially null (meaning "All")
    private float minPriceSelected = 0f;     // Min price in Millions
    private float maxPriceSelected = 50f;    // Max price in Millions (match slider)
    private String currentQuery = "";       // Current text query

    private NumberFormat currencyFormatter = NumberFormat.getInstance(new Locale("vi", "VN"));

    // --- Autocomplete Variables ---
    private RecyclerView rvAutocompleteSuggestions;
    private AutocompleteAdapter autocompleteAdapter;
    private PlacesClient placesClient;
    private AutocompleteSessionToken sessionToken;
    private Handler autocompleteHandler = new Handler(Looper.getMainLooper());
    private Runnable autocompleteRunnable;
    private static final long AUTOCOMPLETE_DELAY_MS = 300; // Delay for debouncing

    // Reference to BottomNavigationView
    private BottomNavigationView bottomNavView;


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

        // --- Bind Views ---
        recyclerViewPostView = view.findViewById(R.id.recyclerViewPostView);
        progressBar = view.findViewById(R.id.progressBarPostView);
        textViewNoPosts = view.findViewById(R.id.textViewNoPostsView);
        searchBar = view.findViewById(R.id.searchBarPostView);
        searchView = view.findViewById(R.id.searchViewPostView);
        chipProvince = view.findViewById(R.id.chipProvince);
        chipPriceRange = view.findViewById(R.id.chipPriceRange);
        chipGroupFilters = view.findViewById(R.id.chipGroupFilters); // Needed? Maybe not directly
        updateChipStates(); // Set initial state after binding

        if (savedInstanceState != null) {
            currentQuery = savedInstanceState.getString("CURRENT_QUERY", "");
            // Restore selectedProvince, min/max price if saved
        }

        // --- Initialize Places Client ---
        if (!Places.isInitialized()) {
            Log.e(TAG, "Places SDK not initialized! Make sure it's called in Application or Activity.");
            // Handle error - maybe disable search features
        } else {
            placesClient = Places.createClient(requireContext());
            Log.d(TAG, "PlacesClient created successfully.");
        }


        // --- Get BottomNavigationView from Activity ---
        if (getActivity() instanceof TenantHomeActivity) {
            bottomNavView = ((TenantHomeActivity) getActivity()).getBottomNavigationView();
        } else {
            Log.e(TAG, "Fragment not attached to TenantHomeActivity, cannot access BottomNav.");
        }


        // --- Setup Autocomplete RecyclerView ---
        rvAutocompleteSuggestions = view.findViewById(R.id.rvAutocompleteSuggestions); // Find RV inside SearchView
        autocompleteAdapter = new AutocompleteAdapter(requireContext(), this); // Pass listener
        rvAutocompleteSuggestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvAutocompleteSuggestions.setAdapter(autocompleteAdapter);
        // Optional: Add divider
        // rvAutocompleteSuggestions.addItemDecoration(new DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL));

        // --- Initialize Lists & Adapter ---
        allPostList = new ArrayList<>();
        displayedPostList = new ArrayList<>();
        savedPostIds = new HashSet<>();
        adapter = new PostViewAdapter(requireContext(), displayedPostList, savedPostIds, this);
        recyclerViewPostView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerViewPostView.setAdapter(adapter);

        // --- Load Data ---
        loadProvinceData(); // Load provinces for filter dialog
        loadInitialData(); // Load saved IDs and then posts

        // --- Setup UI Interactions ---
        setupSearchViewAndChips();
    }

    private void loadProvinceData() {
        allProvinces = AddressUtils.loadProvinces(requireContext());
        if (allProvinces.isEmpty()) {
            chipProvince.setEnabled(false); // Disable chip if no provinces loaded
        } else {
            // Add "All" option manually if needed for dialog logic
            // This list is just for the dialog
        }
    }

    private void setupSearchViewAndChips() {
        searchView.setupWithSearchBar(searchBar);



        searchView.getEditText().addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {} // Not needed now

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString();
                Log.d(TAG, "onTextChanged: Query = " + currentQuery);
                if (autocompleteRunnable != null) { autocompleteHandler.removeCallbacks(autocompleteRunnable); }
                autocompleteRunnable = () -> {
                    Log.d(TAG, "Runnable executing for query: " + currentQuery); // Xem runnable có chạy không
                    if (!TextUtils.isEmpty(currentQuery)) { fetchAutocompleteSuggestions(currentQuery); }
                    // ...
                };
                autocompleteHandler.postDelayed(autocompleteRunnable, AUTOCOMPLETE_DELAY_MS);
            }
        });

        // --- Filter on submit ---
        searchView.getEditText().setOnEditorActionListener((v, actionId, event) -> {
            // currentQuery is already up-to-date thanks to TextWatcher
            searchView.hide(); // Hide keyboard/searchview
            autocompleteAdapter.setPredictions(new ArrayList<>()); // Xóa gợi ý
            return true; // Indicate handled
        });

        // --- Restore query when SearchView is shown ---
        // --- Handle SearchView State Changes (Show/Hide BottomNav) ---
        searchView.addTransitionListener((sv, previousState, newState) -> {
            Log.d(TAG, "SearchView Transition: " + previousState + " -> " + newState);
            if (bottomNavView != null) {
                // --- Logic ẩn/hiện BottomNav ---
                if (newState == SearchView.TransitionState.SHOWING || newState == SearchView.TransitionState.SHOWN) {
                    bottomNavView.setVisibility(View.GONE);
                    if (newState == SearchView.TransitionState.SHOWING && sessionToken == null) {
                        sessionToken = AutocompleteSessionToken.newInstance();
                        Log.d(TAG, "New Session Token created.");
                    }
                } else if (newState == SearchView.TransitionState.HIDING || newState == SearchView.TransitionState.HIDDEN) {
                    bottomNavView.setVisibility(View.VISIBLE);
                    autocompleteAdapter.setPredictions(new ArrayList<>());
                    if (newState == SearchView.TransitionState.HIDDEN) {
                        sessionToken = null;
                        Log.d(TAG, "SearchView HIDDEN, triggering filterAndDisplayPosts() with query: '" + currentQuery + "'");
                        // *** GỌI FILTER KHI ĐÃ ẨN HOÀN TOÀN ***
                        filterAndDisplayPosts();
                    }
                }
            }
        });
        // --- Chip Click Listeners ---
        chipProvince.setOnClickListener(v -> showProvinceSelectionDialog());
        chipPriceRange.setOnClickListener(v -> showPriceRangeSelectionDialog());

        // --- Set initial chip state (unchecked) ---
        updateChipStates(); // Call this initially
    }

    private void fetchAutocompleteSuggestions(String query) {
        if (placesClient == null) { Log.e(TAG, "fetchAutocompleteSuggestions: placesClient is NULL"); return; }
        if (sessionToken == null) { Log.w(TAG, "fetchAutocompleteSuggestions: sessionToken is NULL"); sessionToken = AutocompleteSessionToken.newInstance();}

        // --- Xây dựng query cuối cùng bao gồm tỉnh/thành phố đã chọn ---
        String finalQuery;
        if (selectedProvince != null) { // Chỉ thêm tỉnh nếu đã chọn (không phải "Tất cả")
            finalQuery = query + ", " + selectedProvince.getName(); // Nối tên tỉnh vào query
        } else {
            finalQuery = query;
        }
        Log.d(TAG, "fetchAutocompleteSuggestions: Fetching for finalQuery='" + finalQuery + "' with token=" + sessionToken);
        // -------------------------------------------------------------

        FindAutocompletePredictionsRequest.Builder requestBuilder =
                FindAutocompletePredictionsRequest.builder()
                        // *** Sử dụng finalQuery ***
                        .setQuery(finalQuery)
                        .setCountries("VN")
                        // .setTypeFilter(TypeFilter.ADDRESS) // Cân nhắc bỏ hoặc dùng GEOCODE/REGIONS nếu ADDRESS quá hẹp
                        .setTypeFilter(TypeFilter.GEOCODE) // Thử dùng GEOCODE cho kết quả rộng hơn
                        .setSessionToken(sessionToken);

        placesClient.findAutocompletePredictions(requestBuilder.build())
                .addOnSuccessListener((response) -> {
                    if (isAdded() && getActivity() != null) {
                        List<AutocompletePrediction> predictions = response.getAutocompletePredictions();
                        Log.d(TAG, "Autocomplete SUCCESS: Received " + predictions.size() + " predictions for query '" + finalQuery + "'.");

                        // --- (Tùy chọn) Lọc thêm phía client nếu API vẫn trả về kết quả ngoài tỉnh ---
                        // Mặc dù nối tên tỉnh thường hiệu quả, đôi khi API vẫn có thể trả về gợi ý hơi lệch.
                        // Bạn có thể lọc thêm ở đây nếu cần độ chính xác tuyệt đối.
                        List<AutocompletePrediction> clientFilteredPredictions;
                        if (selectedProvince != null) {
                            String provinceNameLower = selectedProvince.getName().toLowerCase(Locale.getDefault());
                            clientFilteredPredictions = predictions.stream()
                                    .filter(p -> {
                                        // Kiểm tra xem tên tỉnh có trong secondary text hoặc full text không
                                        String secondaryLower = p.getSecondaryText(null) != null ? p.getSecondaryText(null).toString().toLowerCase(Locale.getDefault()) : "";
                                        String fullLower = p.getFullText(null) != null ? p.getFullText(null).toString().toLowerCase(Locale.getDefault()) : "";
                                        // Có thể cần logic so khớp tên tỉnh linh hoạt hơn (vd: bỏ chữ "Tỉnh", "Thành phố")
                                        return secondaryLower.contains(provinceNameLower) || fullLower.contains(provinceNameLower);
                                    })
                                    .collect(Collectors.toList());
                            Log.d(TAG, "Client-side filtered predictions: " + clientFilteredPredictions.size());
                        } else {
                            // Không cần lọc phía client nếu chọn "Tất cả"
                            clientFilteredPredictions = predictions;
                        }
                        autocompleteAdapter.setPredictions(clientFilteredPredictions);
                        // autocompleteAdapter.setPredictions(predictions); // Bỏ lọc phía client nếu thấy không cần thiết

                    }
                })
                .addOnFailureListener((exception) -> {
                    if (isAdded() && getActivity() != null) {
                        // *** LOG LỖI QUAN TRỌNG ***
                        Log.e(TAG, "Autocomplete FAILED", exception);
                        autocompleteAdapter.setPredictions(new ArrayList<>());
                        // Hiển thị lỗi cho người dùng nếu cần
                        // Toast.makeText(requireContext(), "Lỗi tìm gợi ý: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // --- Implement Autocomplete Item Click Listener ---
    @Override
    public void onPredictionClick(AutocompletePrediction prediction) {
        String selectedText = prediction.getPrimaryText(null).toString();
        Log.d(TAG, "Autocomplete prediction selected: " + selectedText);

        searchView.setText(selectedText); // Set text in SearchView
        currentQuery = selectedText; // Update current query
        searchView.hide(); // Hide the search view
        filterAndDisplayPosts(); // Trigger filtering with the selected text

        // Clear autocomplete suggestions list after selection
        autocompleteAdapter.setPredictions(new ArrayList<>());
    }

    // --- onDestroyView to clean up handler ---
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (autocompleteHandler != null && autocompleteRunnable != null) {
            autocompleteHandler.removeCallbacks(autocompleteRunnable); // Prevent memory leaks
        }
        // Maybe set adapters to null? recyclerViewPostView.setAdapter(null); etc.
    }

    private void showProvinceSelectionDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_select_province, null);
        builder.setView(dialogView);

        ListView listViewProvinces = dialogView.findViewById(R.id.listViewProvinces);

        // Add "All" option for the dialog display
        List<Province> dialogProvinceList = new ArrayList<>();
        dialogProvinceList.add(new Province("0", "Tất cả Tỉnh/Thành")); // "All" option
        dialogProvinceList.addAll(allProvinces); // Add the rest

        ArrayAdapter<Province> dialogAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1, // Simple list item layout
                dialogProvinceList
        );
        listViewProvinces.setAdapter(dialogAdapter);

        final AlertDialog dialog = builder.create();

        listViewProvinces.setOnItemClickListener((parent, view, position, id) -> {
            Province clickedProvince = (Province) parent.getItemAtPosition(position);
            boolean changed = false; // Flag to check if filter actually changed

            if ("0".equals(clickedProvince.getIdProvince())) { // "All" selected
                if (selectedProvince != null) { // Check if it was previously filtered
                    selectedProvince = null;
                    changed = true;
                }
            } else { // Specific province selected
                if (selectedProvince == null || !selectedProvince.getIdProvince().equals(clickedProvince.getIdProvince())) {
                    selectedProvince = clickedProvince;
                    changed = true;
                }
            }

            updateChipStates(); // Update chip appearance
            if (changed) {
                filterAndDisplayPosts(); // Re-filter only if changed
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showPriceRangeSelectionDialog() {
        // Use MaterialAlertDialogBuilder for M3 dialog style
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireContext()); // *** USE M3 BUILDER ***
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_select_price_range, null);
        builder.setView(dialogView);
        // builder.setTitle("Chọn khoảng giá (Triệu VNĐ)"); // Title can be set in layout

        // --- Find views inside dialogView ---
        RangeSlider localRangeSlider = dialogView.findViewById(R.id.rangeSliderPrice);
        TextView tvRange = dialogView.findViewById(R.id.tvSelectedPriceRange);
        Button btnReset = dialogView.findViewById(R.id.btnResetPrice);
        Button btnApply = dialogView.findViewById(R.id.btnApplyPrice);

        // Kiểm tra null cho localRangeSlider nếu cẩn thận
        if (localRangeSlider == null) {
            Log.e(TAG, "RangeSlider not found in dialog layout!");
            Toast.makeText(requireContext(), "Lỗi giao diện chọn giá", Toast.LENGTH_SHORT).show();
            return;
        }

        // Store temporary values for the dialog
        final float[] tempMinPrice = {minPriceSelected};
        final float[] tempMaxPrice = {maxPriceSelected};

        localRangeSlider.setValues(tempMinPrice[0], tempMaxPrice[0]);
        updatePriceRangeText(tvRange, tempMinPrice[0], tempMaxPrice[0]);

        localRangeSlider.addOnChangeListener((slider, value, fromUser) -> {
            List<Float> values = slider.getValues();
            tempMinPrice[0] = values.get(0);
            tempMaxPrice[0] = values.get(1);
            updatePriceRangeText(tvRange, tempMinPrice[0], tempMaxPrice[0]);
        });

        final AlertDialog dialog = builder.create(); // Create the dialog

        btnReset.setOnClickListener(v -> {
            boolean changed = !(minPriceSelected == 0f && maxPriceSelected == 50f); // Check if filter was active
            minPriceSelected = 0f;
            maxPriceSelected = 50f;
            updateChipStates(); // Update chip appearance
            if (changed) {
                filterAndDisplayPosts(); // Re-filter only if changed
            }
            dialog.dismiss();
        });

        btnApply.setOnClickListener(v -> {
            boolean changed = !(minPriceSelected == tempMinPrice[0] && maxPriceSelected == tempMaxPrice[0]); // Check if values changed
            minPriceSelected = tempMinPrice[0];
            maxPriceSelected = tempMaxPrice[0];
            updateChipStates(); // Update chip appearance
            if (changed) {
                filterAndDisplayPosts(); // Re-filter only if changed
            }
            dialog.dismiss();
        });

        dialog.show();
    }

    // --- New method to update chip text and checked state ---
    private void updateChipStates() {
        // Province Chip
        if (selectedProvince == null) { // "All" is selected
            chipProvince.setText("Tỉnh/TP: Tất cả");
            chipProvince.setChecked(false); // Not checked when "All"
            chipProvince.setChipIconVisible(true); // Keep icon visible
        } else {
            chipProvince.setText("Tỉnh/TP: " + selectedProvince.getName());
            chipProvince.setChecked(true); // Checked when a specific province is selected
            chipProvince.setChipIconVisible(true);
        }

        // Price Range Chip
        float defaultMin = 0f;
        float defaultMax = 50f; // Assuming this is the slider's valueTo

        boolean isPriceFiltered = !(minPriceSelected <= defaultMin && maxPriceSelected >= defaultMax);

        updatePriceChipText(minPriceSelected, maxPriceSelected); // Update text first
        chipPriceRange.setChecked(isPriceFiltered); // Check only if filtered
        chipPriceRange.setChipIconVisible(true);
    }

    // Helper to format price text for display (Millions)
    private String formatPriceMillions(float price) {
        if (price <= 0) return "0";
        // Use integer part for display if possible
        if (price == (int) price) return String.format(Locale.US,"%d", (int) price);
        return String.format(Locale.US,"%.1f", price); // Show one decimal if needed
    }

    private void updatePriceRangeText(TextView textView, float min, float max) {
        String minStr = formatPriceMillions(min);
        String maxStr = formatPriceMillions(max);

        // *** SỬ DỤNG HẰNG SỐ THAY VÌ BIẾN rangeSlider Ở ĐÂY ***
        if (min <= SLIDER_VALUE_FROM && max >= SLIDER_VALUE_TO) {
            textView.setText("Bất kỳ");
        } else if (max >= SLIDER_VALUE_TO) { // Kiểm tra với giá trị max của slider
            textView.setText(String.format("Từ %s triệu", minStr));
        } else if (min <= SLIDER_VALUE_FROM) { // Kiểm tra với giá trị min của slider
            textView.setText(String.format("Đến %s triệu", maxStr));
        } else {
            textView.setText(String.format("%s - %s triệu", minStr, maxStr));
        }
    }

    // updatePriceChipText - Keep this logic for setting the text
    private void updatePriceChipText(float min, float max) {
        String minStr = formatPriceMillions(min);
        String maxStr = formatPriceMillions(max);

        // *** SỬ DỤNG HẰNG SỐ ***
        if (min <= SLIDER_VALUE_FROM && max >= SLIDER_VALUE_TO) {
            chipPriceRange.setText("Giá: Bất kỳ");
        } else if (max >= SLIDER_VALUE_TO) {
            chipPriceRange.setText(String.format("Giá: Từ %s tr", minStr));
        } else if (min <= SLIDER_VALUE_FROM) {
            chipPriceRange.setText(String.format("Giá: Đến %s tr", maxStr));
        } else {
            chipPriceRange.setText(String.format("Giá: %s-%s tr", minStr, maxStr));
        }
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

    // loadInitialData, loadSavedPostIdsAndThenPosts, loadAllPosts remain similar
    // BUT loadAllPosts should call filterAndDisplayPosts() without arguments initially
    private void loadAllPosts(@Nullable Set<String> currentSavedIds) {
        // ... (Show progress bar)
        db.collection("posts")
                .whereEqualTo("available", true)
                .orderBy("timestamp", Query.Direction.DESCENDING)
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
                            }                            Log.d(TAG, "Loaded all posts: " + allPostList.size());
                            filterAndDisplayPosts(); // Call filter without args initially

                        } else {
                            Log.w(TAG, "Error getting posts.", task.getException());
                            textViewNoPosts.setVisibility(View.VISIBLE);
                            textViewNoPosts.setText("Lỗi tải tin đăng");
                        }
                    }
                });
    }


    // --- Central Filtering Logic ---
    private void filterAndDisplayPosts() {
        // Get current filter values
        Province province = selectedProvince; // Biến thành viên
        String query = currentQuery; // Biến thành viên
        float minPrice = minPriceSelected * 1_000_000; // Biến thành viên
        float maxPrice = maxPriceSelected * 1_000_000; // Biến thành viên

        Log.d(TAG, "Filtering with Province: " + (province != null ? province.getName() : "All") +
                ", Price: " + minPriceSelected + "-" + maxPriceSelected + "M" +
                ", Query: '" + query + "'");

        displayedPostList.clear();
        List<Post> tempList = new ArrayList<>(allPostList);

        // 1. Filter by Province
        if (province != null) { // No need to check ID "0" as null means "All"
            String provinceName = province.getName();
            tempList = tempList.stream()
                    .filter(post -> post.getCity() != null && post.getCity().equals(provinceName))
                    .collect(Collectors.toList());
        }

        // 2. Filter by Price Range (Only if max is not the default max or min is not default min)
        boolean isPriceDefault = (minPriceSelected <= 0f && maxPriceSelected >= 50f);
        if (!isPriceDefault) {
            tempList = tempList.stream()
                    .filter(post -> post.getPrice() >= minPrice && post.getPrice() <= maxPrice)
                    .collect(Collectors.toList());
        }

        // 3. Filter by Query Text
        if (!TextUtils.isEmpty(query)) {
            String lowerCaseQuery = query.toLowerCase(Locale.getDefault());
            tempList = tempList.stream()
                    .filter(post -> (post.getDistrict() != null && post.getDistrict().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) ||
                                    (post.getWard() != null && post.getWard().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) ||
                                    (post.getAddress() != null && post.getAddress().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery)) ||
                                    (post.getTitle() != null && post.getTitle().toLowerCase(Locale.getDefault()).contains(lowerCaseQuery))
                            // Don't search description here? Might be too broad with location filters
                    )
                    .collect(Collectors.toList());
        }

        // 4. Update displayed list and adapter
        displayedPostList.addAll(tempList);
        updateNoPostsView(displayedPostList.isEmpty()); // Helper method
        adapter.updateSavedPostIds(this.savedPostIds);
        adapter.notifyDataSetChanged();
    }

    private void updateNoPostsView(boolean isEmpty) {
        if (isEmpty) {
            textViewNoPosts.setVisibility(View.VISIBLE);
            // More descriptive message based on active filters
            if (selectedProvince == null && (minPriceSelected <= 0f && maxPriceSelected >= 50f) && TextUtils.isEmpty(currentQuery)) {
                textViewNoPosts.setText("Không có tin đăng nào.");
            } else {
                textViewNoPosts.setText("Không tìm thấy kết quả phù hợp với bộ lọc.");
            }
            recyclerViewPostView.setVisibility(View.GONE);
        } else {
            textViewNoPosts.setVisibility(View.GONE);
            recyclerViewPostView.setVisibility(View.VISIBLE);
        }
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
        Log.d(TAG, "onResume called. Refreshing filters and saved posts.");
        // 1. Load lại trạng thái các tin đã lưu
        if (currentUser != null) {
            loadSavedPostIdsAndUpdateAdapter(); // Hàm này nên chỉ cập nhật set và adapter nếu cần
        }
        // 2. Quan trọng: Áp dụng lại bộ lọc hiện tại vào danh sách bài đăng
        // Điều này đảm bảo RecyclerView được cập nhật đúng trạng thái filter
        // ngay cả khi không có gì thay đổi trong saved IDs.
        filterAndDisplayPosts(); // Gọi lại filter khi quay lại fragment
    }

    // Save and restore currentQuery (optional but good practice)
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("CURRENT_QUERY", currentQuery);
        // You might also want to save selectedProvince ID, min/max price here
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