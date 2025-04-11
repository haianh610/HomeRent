package com.example.homerent.fragment.tenant;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.homerent.R;
import com.example.homerent.activity.tenant.TenantHomeActivity; // Import để cập nhật title

public class TenantAccountFragment extends Fragment {

    public static final String TAG = "TenantAccountFragment";

    public TenantAccountFragment() {
        // Required empty public constructor
    }

    public static TenantAccountFragment newInstance() {
        return new TenantAccountFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // TODO: Tenant Account - Khởi tạo các listener hoặc ViewModel nếu cần ở đây
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tenant_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // TODO: Tenant Account - Ánh xạ view và thiết lập các listener tại đây
        // Ví dụ:
        // Button btnLogout = view.findViewById(R.id.button_logout_tenant);
        // btnLogout.setOnClickListener(v -> { /* Xử lý đăng xuất */ });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cập nhật lại title toolbar khi quay lại fragment này
        if (getActivity() instanceof TenantHomeActivity) {
            ((TenantHomeActivity) getActivity()).updateToolbarTitle(TAG);
        }
        // TODO: Tenant Account - Load lại thông tin người dùng nếu cần khi fragment được hiển thị lại
    }
}