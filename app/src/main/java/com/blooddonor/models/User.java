package com.blooddonor.models;

public class User {
    private String userId;
    private String name;
    private String email;
    private String phone;
    private String bloodGroup;
    private String city;
    private String state;
    private String address;
    private double latitude;
    private double longitude;
    private boolean isAvailable;
    private boolean isDonor;
    private String profileImageBase64;
    private String lastDonationDate;
    private int totalDonations;
    private String fcmToken;
    private long createdAt;
    private long updatedAt;

    public User() {} // Required for Firebase

    public User(String userId, String name, String email, String phone,
                String bloodGroup, String city, String state) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.bloodGroup = bloodGroup;
        this.city = city;
        this.state = state;
        this.isAvailable = true;
        this.isDonor = true;
        this.totalDonations = 0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters
    public String getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getBloodGroup() { return bloodGroup; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getAddress() { return address; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public boolean isAvailable() { return isAvailable; }
    public boolean isDonor() { return isDonor; }
    public String getProfileImageBase64() { return profileImageBase64; }
    public String getLastDonationDate() { return lastDonationDate; }
    public int getTotalDonations() { return totalDonations; }
    public String getFcmToken() { return fcmToken; }
    public long getCreatedAt() { return createdAt; }
    public long getUpdatedAt() { return updatedAt; }

    // Setters
    public void setUserId(String userId) { this.userId = userId; }
    public void setName(String name) { this.name = name; }
    public void setEmail(String email) { this.email = email; }
    public void setPhone(String phone) { this.phone = phone; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setAddress(String address) { this.address = address; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setAvailable(boolean available) { isAvailable = available; }
    public void setDonor(boolean donor) { isDonor = donor; }
    public void setProfileImageBase64(String profileImageBase64) { this.profileImageBase64 = profileImageBase64; }
    public void setLastDonationDate(String lastDonationDate) { this.lastDonationDate = lastDonationDate; }
    public void setTotalDonations(int totalDonations) { this.totalDonations = totalDonations; }
    public void setFcmToken(String fcmToken) { this.fcmToken = fcmToken; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
