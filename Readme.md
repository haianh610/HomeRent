# HomeRent - Ứng dụng Hỗ trợ Thuê và Cho Thuê Nhà (Android)

![Màn hình HomeRent](readme-images\thumbnail.png)

## Giới thiệu

HomeRent là một ứng dụng di động Android được phát triển nhằm mục đích kết nối người có nhu cầu thuê nhà/phòng trọ với các chủ nhà một cách tiện lợi và trực quan. Dự án này được thực hiện như một bài tập lớn cho môn học Lập trình Di động Nâng cao, tập trung vào việc xây dựng các chức năng cốt lõi và áp dụng các công nghệ hiện đại.

Ứng dụng cho phép người dùng đăng ký tài khoản với vai trò là Người thuê hoặc Chủ nhà, cung cấp các tính năng phù hợp cho từng đối tượng, từ việc tìm kiếm, lọc tin đăng, lưu tin yêu thích cho đến việc đăng tin, quản lý tin và chỉnh sửa thông tin cá nhân.



## Tính năng chính

HomeRent cung cấp một bộ công cụ toàn diện cho cả người tìm thuê và chủ nhà, giúp quá trình thuê và cho thuê trở nên dễ dàng và hiệu quả hơn.

### 🏡 Dành cho Người Tìm Thuê:

*   **Khám phá & Tìm kiếm thông minh:**
    *   Duyệt xem danh sách các tin đăng nhà/phòng trọ được cập nhật liên tục.
    *   Sử dụng thanh tìm kiếm trực quan với gợi ý địa điểm (đường, quận, phường) từ Google Maps.
    *   Lọc tin đăng chính xác theo **Tỉnh/Thành phố** và **Khoảng giá** mong muốn thông qua các lựa chọn trực quan.
*   **Trải nghiệm chi tiết & tương tác:**
    *   Xem chi tiết từng tin đăng với đầy đủ thông tin: hình ảnh, mô tả, giá, diện tích, địa chỉ, và bản đồ vị trí nhỏ.
    *   Dễ dàng mở bản đồ vị trí của tin đăng ra toàn màn hình hoặc xem chỉ đường qua Google Maps.

*   **Lưu trữ & Liên hệ:**
    *   Lưu lại các tin đăng yêu thích để xem lại sau.
    *   Nhanh chóng liên hệ với chủ nhà qua các tùy chọn: Gọi điện, Nhắn tin SMS, hoặc mở Zalo.
*   **Quản lý cá nhân:**
    *   Xem và quản lý danh sách các tin đã lưu.
    *   Cập nhật thông tin tài khoản cá nhân.

### 🔑 Dành cho Chủ Nhà:

*   **Đăng tin dễ dàng & nhanh chóng:**
    *   Tạo tin đăng mới với form nhập liệu chi tiết: địa chỉ (hỗ trợ chọn từ danh sách đơn vị hành chính Việt Nam, lấy vị trí hiện tại, hoặc định vị trên bản đồ), thông tin nhà (diện tích, giá, số phòng, số tầng, số lượng), thời hạn đăng, thời gian liên hệ, tiêu đề và mô tả hấp dẫn.
    *   Tải lên tối đa 5 hình ảnh cho mỗi tin đăng, với khả năng xem trước, xóa và **kéo thả để sắp xếp thứ tự ảnh**.
*   **Quản lý tin đăng hiệu quả:**
    *   Xem danh sách tất cả các tin đã đăng.
    *   **Chỉnh sửa thông tin chi tiết** của tin đăng bất cứ lúc nào.
    *   **Xóa tin đăng** không còn hiệu lực.
    *   Xem số lượt xem của mỗi tin đăng (trong chi tiết tin của chủ nhà).
*   **Quản lý cá nhân & tương tác:**
    *   Cập nhật thông tin tài khoản.
    *   Nhận thông tin liên hệ từ người tìm thuê.


## Công nghệ sử dụng

*   **Ngôn ngữ:** Java
*   **IDE:** Android Studio [Meerkat | 2024.3.1 Patch 2]
*   **Backend:**
    *   Firebase Authentication (Đăng nhập Email/Password)
    *   Cloud Firestore (Cơ sở dữ liệu NoSQL thời gian thực)
    *   Firebase Storage (Lưu trữ hình ảnh)
*   **Giao diện người dùng (UI):**
    *   Material Design 3 Components
    *   XML Layouts
    *   RecyclerView, ViewPager2, CardView
*   **Thư viện bên thứ ba:**
    *   Glide (Tải và hiển thị hình ảnh)
    *   PhotoView (Zoom ảnh)
    *   CircleImageView (Hiển thị avatar tròn)
    *   Google Maps SDK for Android
    *   Google Places SDK for Android (Autocomplete gợi ý địa điểm)
    *   Gson
*   **API:**
    *   Google Maps Geocoding API (thông qua `android.location.Geocoder`)
    *   Google Places API (Autocomplete)
*   **Cảm biến:**
    *   Accelerometer (Gia tốc kế - cho tính năng lắc)
*   **Quản lý phiên bản:** Git, GitHub

## Hướng dẫn cài đặt và Chạy thử

1.  **Clone Repository:**
    ```bash
    git clone https://github.com/haianh610/HomeRent.git
    ```
2.  **Mở bằng Android Studio:** Mở project đã clone bằng Android Studio.
3.  **Kết nối Firebase:**
    *   Tạo một project mới trên [Firebase Console](https://console.firebase.google.com/).
    *   Thêm một ứng dụng Android vào project Firebase với package name là `com.example.homerent`.
    *   Tải file `google-services.json` từ Firebase Console và đặt vào thư mục `app` của project Android.
    *   Bật các dịch vụ Firebase cần thiết: Authentication (Email/Password), Cloud Firestore, Firebase Storage.
4.  **Cấu hình API Key cho Google Maps & Places:**
    *   Truy cập [Google Cloud Console](https://console.cloud.google.com/).
    *   Chọn project đã liên kết với Firebase.
    *   Bật các API: "Maps SDK for Android" và "Places API".
    *   Tạo một API Key mới hoặc sử dụng key hiện có.
    *   **Quan trọng:** Thêm API Key vào file `local.properties` (hoặc `secrets.properties`) trong thư mục gốc của project Android:
        ```properties
        MAPS_API_KEY=AIz...........YOUR_API_KEY_HERE
        ```
5.  **Build và Chạy:**
    *   Sync project với Gradle files.
    *   Build và chạy ứng dụng trên máy ảo hoặc thiết bị thật.


## Thông tin tác giả

*   **Nguyễn Hoàng Hải Anh**
*   **Phan Thị Hồng Hoài**
*   **Đỗ Thành Long**