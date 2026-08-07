package com.blooddonor.models;

public class DonationHistory {
    private String donationId;
    private String requestId;
    private String requesterName;
    private String requesterPhone;
    private String bloodGroup;
    private String hospitalName;
    private String city;
    private String state;
    private String donationDate;
    private long createdAt;

    public DonationHistory() {} // Required for Firebase

    // Getters
    public String getDonationId()     { return donationId; }
    public String getRequestId()      { return requestId; }
    public String getRequesterName()  { return requesterName; }
    public String getRequesterPhone() { return requesterPhone; }
    public String getBloodGroup()     { return bloodGroup; }
    public String getHospitalName()   { return hospitalName; }
    public String getCity()           { return city; }
    public String getState()          { return state; }
    public String getDonationDate()   { return donationDate; }
    public long   getCreatedAt()      { return createdAt; }

    // Setters
    public void setDonationId(String donationId)         { this.donationId = donationId; }
    public void setRequestId(String requestId)           { this.requestId = requestId; }
    public void setRequesterName(String requesterName)   { this.requesterName = requesterName; }
    public void setRequesterPhone(String requesterPhone) { this.requesterPhone = requesterPhone; }
    public void setBloodGroup(String bloodGroup)         { this.bloodGroup = bloodGroup; }
    public void setHospitalName(String hospitalName)     { this.hospitalName = hospitalName; }
    public void setCity(String city)                     { this.city = city; }
    public void setState(String state)                   { this.state = state; }
    public void setDonationDate(String donationDate)     { this.donationDate = donationDate; }
    public void setCreatedAt(long createdAt)             { this.createdAt = createdAt; }
}