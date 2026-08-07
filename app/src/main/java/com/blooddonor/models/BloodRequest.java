package com.blooddonor.models;

public class BloodRequest {
    private String requestId;
    private String requesterId;
    private String requesterName;
    // Add these two fields to BloodRequest.java
    private String donorId;    // UID of donor who fulfilled it
    private String donorName;  // Name of donor who fulfilled it
    private String requesterPhone;
    private String bloodGroup;
    private String city;
    private String state;
    private String hospitalName;
    private String address;
    private double latitude;
    private double longitude;
    private int unitsNeeded;
    private String urgency;          // "CRITICAL", "URGENT", "NORMAL"
    private String status;           // "OPEN", "FULFILLED", "CLOSED"
    private String patientName;
    private String additionalNotes;
    private long createdAt;
    private long expiresAt;

    public BloodRequest() {} // Required for Firebase

    public BloodRequest(String requesterId, String requesterName, String requesterPhone,
                        String bloodGroup, String city, String state,
                        String hospitalName, int unitsNeeded, String urgency) {
        this.requesterId = requesterId;
        this.requesterName = requesterName;
        this.requesterPhone = requesterPhone;
        this.bloodGroup = bloodGroup;
        this.city = city;
        this.state = state;
        this.hospitalName = hospitalName;
        this.unitsNeeded = unitsNeeded;
        this.urgency = urgency;
        this.status = "OPEN";
        this.createdAt = System.currentTimeMillis();
        this.expiresAt = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000L); // 7 days
    }

    // Getters
    public String getDonorId() { return donorId; }
    public void setDonorId(String donorId) { this.donorId = donorId; }

    public String getDonorName() { return donorName; }
    public void setDonorName(String donorName) { this.donorName = donorName; }
    public String getRequestId() { return requestId; }
    public String getRequesterId() { return requesterId; }
    public String getRequesterName() { return requesterName; }
    public String getRequesterPhone() { return requesterPhone; }
    public String getBloodGroup() { return bloodGroup; }
    public String getCity() { return city; }
    public String getState() { return state; }
    public String getHospitalName() { return hospitalName; }
    public String getAddress() { return address; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public int getUnitsNeeded() { return unitsNeeded; }
    public String getUrgency() { return urgency; }
    public String getStatus() { return status; }
    public String getPatientName() { return patientName; }
    public String getAdditionalNotes() { return additionalNotes; }
    public long getCreatedAt() { return createdAt; }
    public long getExpiresAt() { return expiresAt; }

    // Setters
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public void setRequesterId(String requesterId) { this.requesterId = requesterId; }
    public void setRequesterName(String requesterName) { this.requesterName = requesterName; }
    public void setRequesterPhone(String requesterPhone) { this.requesterPhone = requesterPhone; }
    public void setBloodGroup(String bloodGroup) { this.bloodGroup = bloodGroup; }
    public void setCity(String city) { this.city = city; }
    public void setState(String state) { this.state = state; }
    public void setHospitalName(String hospitalName) { this.hospitalName = hospitalName; }
    public void setAddress(String address) { this.address = address; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    public void setUnitsNeeded(int unitsNeeded) { this.unitsNeeded = unitsNeeded; }
    public void setUrgency(String urgency) { this.urgency = urgency; }
    public void setStatus(String status) { this.status = status; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
}
