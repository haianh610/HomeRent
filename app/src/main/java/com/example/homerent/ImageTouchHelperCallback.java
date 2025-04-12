package com.example.homerent; // Hoặc package của bạn

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homerent.adapter.SelectedImageAdapter;

import java.util.Collections;
import java.util.List;
// Xóa import android.net.Uri nếu không dùng trực tiếp ở đây nữa
// import android.net.Uri;
import android.graphics.Canvas;
import androidx.recyclerview.widget.ItemTouchHelper.Callback;

// Thay đổi kiểu dữ liệu của List trong constructor và biến thành viên
public class ImageTouchHelperCallback extends ItemTouchHelper.Callback {

    private final SelectedImageAdapter mAdapter;
    private final List<Object> mImageList; // *** THAY ĐỔI KIỂU DỮ LIỆU ***

    public ImageTouchHelperCallback(SelectedImageAdapter adapter, List<Object> imageList) { // *** THAY ĐỔI KIỂU THAM SỐ ***
        mAdapter = adapter;
        mImageList = imageList;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        return false;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN; // Giữ nguyên hướng kéo dọc
        final int swipeFlags = 0;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();

        if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
            return false;
        }

        // --- Cập nhật dữ liệu trong List<Object> ---
        // Collections.swap vẫn hoạt động tốt với List<Object>
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mImageList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mImageList, i, i - 1);
            }
        }

        // --- Thông báo cho Adapter ---
        mAdapter.notifyItemMoved(fromPosition, toPosition);

        return true;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // No action needed
    }

    // Các hàm onSelectedChanged, clearView, onChildDraw giữ nguyên, không cần thay đổi
    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        // ... (Giữ nguyên)
        super.onSelectedChanged(viewHolder, actionState);
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            if (viewHolder != null) {
                viewHolder.itemView.setAlpha(0.7f);
            }
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // ... (Giữ nguyên)
        super.clearView(recyclerView, viewHolder);
        viewHolder.itemView.setAlpha(1.0f);
    }

    /*
    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        // ... (Giữ nguyên)
    }
    */
}