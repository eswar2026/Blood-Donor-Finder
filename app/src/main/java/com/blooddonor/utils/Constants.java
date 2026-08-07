package com.blooddonor.utils;

public class Constants {

    // Firestore Collections
    public static final String COLLECTION_USERS           = "users";
    public static final String COLLECTION_BLOOD_REQUESTS  = "blood_requests";
    public static final String COLLECTION_NOTIFICATIONS   = "notifications";

    // Blood Groups
    public static final String[] BLOOD_GROUPS = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};

    // Urgency Levels
    public static final String URGENCY_CRITICAL = "CRITICAL";
    public static final String URGENCY_URGENT   = "URGENT";
    public static final String URGENCY_NORMAL   = "NORMAL";

    // Request Status
    public static final String STATUS_OPEN      = "OPEN";
    public static final String STATUS_FULFILLED = "FULFILLED";
    public static final String STATUS_CLOSED    = "CLOSED";

    // Notification Types
    public static final String NOTIF_BLOOD_REQUEST = "BLOOD_REQUEST";
    public static final String NOTIF_DONOR_FOUND   = "DONOR_FOUND";
    public static final String NOTIF_GENERAL       = "GENERAL";

    // Shared Preferences
    public static final String PREF_NAME    = "BloodDonorPrefs";
    public static final String PREF_USER_ID = "userId";

    // Intent Extras
    public static final String EXTRA_USER_ID    = "user_id";
    public static final String EXTRA_REQUEST_ID = "request_id";

    // Compatible blood group mapping
    public static String[] getCompatibleDonors(String bloodGroup) {
        switch (bloodGroup) {
            case "A+":  return new String[]{"A+", "A-", "O+", "O-"};
            case "A-":  return new String[]{"A-", "O-"};
            case "B+":  return new String[]{"B+", "B-", "O+", "O-"};
            case "B-":  return new String[]{"B-", "O-"};
            case "AB+": return new String[]{"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
            case "AB-": return new String[]{"A-", "B-", "AB-", "O-"};
            case "O+":  return new String[]{"O+", "O-"};
            case "O-":  return new String[]{"O-"};
            default:    return new String[]{bloodGroup};
        }
    }
}
