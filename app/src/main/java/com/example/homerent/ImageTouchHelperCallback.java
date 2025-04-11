package com.example.homerent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.example.homerent.adapter.SelectedImageAdapter; // Import adapter của bạn

import java.util.Collections;
import java.util.List;
import android.net.Uri;
import android.graphics.Canvas; // Import cho onChildDraw nếu cần hiệu ứng nâng cao
import androidx.recyclerview.widget.ItemTouchHelper.Callback;


public class ImageTouchHelperCallback extends ItemTouchHelper.Callback { // Extend Callback hoặc SimpleCallback

    private final SelectedImageAdapter mAdapter;
    private final List<Uri> mImageList; // Tham chiếu đến list trong Activity/Fragment

    public ImageTouchHelperCallback(SelectedImageAdapter adapter, List<Uri> imageList) {
        mAdapter = adapter;
        mImageList = imageList;
    }

    @Override
    public boolean isLongPressDragEnabled() {
        // Cho phép kéo thả khi nhấn giữ lâu
        return true;
    }

    @Override
    public boolean isItemViewSwipeEnabled() {
        // Không cần chức năng vuốt để xóa
        return false;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        // *** THAY ĐỔI HƯỚNG KÉO ***
        // Cho phép kéo lên (UP) và xuống (DOWN)
        final int dragFlags = ItemTouchHelper.UP | ItemTouchHelper.DOWN;
        // Không cho phép vuốt
        final int swipeFlags = 0;
        return makeMovementFlags(dragFlags, swipeFlags);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        // Được gọi khi một item được kéo di chuyển qua một vị trí khác

        // Lấy vị trí bắt đầu và vị trí đích
        int fromPosition = viewHolder.getAdapterPosition();
        int toPosition = target.getAdapterPosition();

        // Kiểm tra vị trí hợp lệ
        if (fromPosition == RecyclerView.NO_POSITION || toPosition == RecyclerView.NO_POSITION) {
            return false;
        }

        // --- Cập nhật dữ liệu trong List ---
        // Cách 1: Dùng Collections.swap (đơn giản nhất)
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mImageList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mImageList, i, i - 1);
            }
        }
        // Cách 2: Remove và Add (Nếu cần logic phức tạp hơn)
        // Uri movedItem = mImageList.remove(fromPosition);
        // mImageList.add(toPosition, movedItem);

        // --- Thông báo cho Adapter về sự thay đổi vị trí ---
        // Quan trọng: Phải gọi notifyItemMoved để RecyclerView cập nhật giao diện kéo thả
        mAdapter.notifyItemMoved(fromPosition, toPosition);

        return true; // Trả về true để báo rằng việc di chuyển đã được xử lý
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        // Không cần làm gì khi vuốt (vì đã disable swipeFlags)
    }

    // --- Optional: Thêm hiệu ứng hình ảnh khi kéo thả ---

    @Override
    public void onSelectedChanged(@Nullable RecyclerView.ViewHolder viewHolder, int actionState) {
        super.onSelectedChanged(viewHolder, actionState);
        // Được gọi khi một item được chọn để kéo (hoặc vuốt)
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
            if (viewHolder != null) {
                // Làm cho item đang kéo hơi mờ đi hoặc nổi lên
                viewHolder.itemView.setAlpha(0.7f); // Ví dụ: làm mờ
                // viewHolder.itemView.setScaleX(1.1f); // Ví dụ: phóng to nhẹ
                // viewHolder.itemView.setScaleY(1.1f);
                // viewHolder.itemView.setBackgroundColor(Color.LTGRAY); // Ví dụ: đổi màu nền
            }
        }
    }

    @Override
    public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        super.clearView(recyclerView, viewHolder);
        // Được gọi khi item được thả ra hoặc thao tác bị hủy
        // Khôi phục lại giao diện ban đầu của item
        viewHolder.itemView.setAlpha(1.0f);
        // viewHolder.itemView.setScaleX(1.0f);
        // viewHolder.itemView.setScaleY(1.0f);
        // viewHolder.itemView.setBackgroundColor(Color.TRANSPARENT); // Hoặc màu nền gốc
    }

    // --- Optional: Tùy chỉnh cách vẽ nâng cao (ít dùng cho kéo thả cơ bản) ---
    /*
    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_DRAG && isCurrentlyActive) {
            // Có thể tùy chỉnh cách vẽ item khi đang kéo ở đây
        }
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
    */
}