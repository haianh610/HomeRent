package com.example.homerent.model; // Thay đổi package name cho phù hợp

import com.google.firebase.Timestamp; // Sử dụng Timestamp của Firebase
import java.util.List;

public class Post {
    private String postId;
    private String userId;
    private String title;
    private String description;
    private String address;
    private String ward;
    private String district;
    private String city;
    private double area;
    private long price;
    private int bedrooms;
    private int floors;
    private List<String> imageUrls;
    private Timestamp timestamp; // Thời gian đăng/cập nhật
    private double latitude;
    private double longitude;
    private boolean available;

    // --- Trường mới ---
    private Timestamp startDate; // Ngày bắt đầu hiệu lực/cho thuê
    private Timestamp endDate;   // Ngày kết thúc hiệu lực/cho thuê
    private long viewCount; // <-- Thêm trường này

    // Constructor mặc định (bắt buộc cho Firebase Firestore)
    public Post() {
    }

    // Constructor đầy đủ (đã cập nhật)
    public Post(String postId, String userId, String title, String description, String address, String ward, String district, String city, double area, long price, int bedrooms, int floors, List<String> imageUrls, Timestamp timestamp, double latitude, double longitude, boolean available, Timestamp startDate, Timestamp endDate,long viewCount) {
        this.postId = postId;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.address = address;
        this.ward = ward;
        this.district = district;
        this.city = city;
        this.area = area;
        this.price = price;
        this.bedrooms = bedrooms;
        this.floors = floors;
        this.imageUrls = imageUrls;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.available = available;
        // --- Gán giá trị cho trường mới ---
        this.startDate = startDate;
        this.endDate = endDate;
        this.viewCount = viewCount; // Gán giá trị cho viewCount
    }

    // Getters and Setters cho tất cả các trường
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public double getArea() { return area; }
    public void setArea(double area) { this.area = area; }

    public long getPrice() { return price; }
    public void setPrice(long price) { this.price = price; }

    public int getBedrooms() { return bedrooms; }
    public void setBedrooms(int bedrooms) { this.bedrooms = bedrooms; }

    public int getFloors() { return floors; }
    public void setFloors(int floors) { this.floors = floors; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    // --- Getters and Setters cho trường mới ---
    public Timestamp getStartDate() { return startDate; }
    public void setStartDate(Timestamp startDate) { this.startDate = startDate; }

    public Timestamp getEndDate() { return endDate; }
    public void setEndDate(Timestamp endDate) { this.endDate = endDate; }

    public long getViewCount() { return viewCount; } // Getter cho viewCount
    public void setViewCount(long viewCount) { this.viewCount = viewCount; } // Setter cho viewCount
}