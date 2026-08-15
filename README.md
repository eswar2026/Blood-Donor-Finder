
<div align="center">

# 🩸 Blood Donor Finder

**A real-time Android application that connects blood donors with people in medical emergencies**

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](https://android.com)
[![Language](https://img.shields.io/badge/Language-Java-ED8B00?style=flat-square&logo=openjdk&logoColor=white)](https://java.com)
[![Firebase](https://img.shields.io/badge/Backend-Firebase-FFCA28?style=flat-square&logo=firebase&logoColor=black)](https://firebase.google.com)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-API%2024-blue?style=flat-square)](https://developer.android.com)
[![Repo](https://img.shields.io/badge/GitHub-eswar2026-181717?style=flat-square&logo=github)](https://github.com/eswar2026/Blood-Donor-Finder)

</div>

---

## What is this app?

Blood Donor Finder is a free Android app that solves a critical real-world problem — finding blood donors during emergencies. Instead of relying on WhatsApp forwards or phone calls, this app lets users find compatible blood donors instantly, post urgent requests, and connect directly with a single tap.

> India needs **4.5 million+ units** of blood every day with a **38% annual shortage**. This app digitises the donor discovery process and reduces response time from hours to seconds.

---

## Key Features

- **Donor Search** — Filter donors by blood group and city with live Firestore results
- **Blood Requests** — Post requests with urgency levels (Critical / Urgent / Normal)
- **Real-time Notifications** — Matching donors are alerted the moment a request is posted
- **Interactive Map** — View nearby donors pinned on OpenStreetMap
- **Direct Contact** — Call, SMS, or WhatsApp a donor in one tap
- **Donation Tracking** — Records every donation with date, hospital, and recipient details
- **Availability Toggle** — Donors can mark themselves available or unavailable in real time
- **Profile Management** — Photo upload with crop, blood group, city, donation history
- **100% Free** — No paid APIs. Runs entirely on Firebase free tier

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java |
| IDE | Android Studio Panda |
| UI | XML + Material Design Components |
| Authentication | Firebase Auth (Email/Password) |
| Database | Firebase Firestore (NoSQL, real-time) |
| Notifications | Firebase Cloud Messaging (FCM V1) |
| Maps | OpenStreetMap via OSMDroid (free, no API key) |
| Image Storage | Base64 encoding in Firestore (no Firebase Storage needed) |
| Location | Google Play Services Location API |
| HTTP Client | OkHttp (for FCM V1 API calls) |

---

## Architecture

The app follows a clean 5-layer architecture:

```
Presentation   →   Activities, Fragments, Adapters, XML Layouts
Business Logic →   Input validation, blood compatibility, notification handling
Data Models    →   User, BloodRequest, Notification, DonationHistory
Backend        →   Firebase Auth, Firestore, FCM, OSMDroid
Device         →   GPS, Camera, Phone dialer, WhatsApp, Notification tray
```

**Package structure:** 12 Activities · 4 Fragments · 4 Adapters · 4 Models · 5 Utilities

---

## How Notifications Work

```
User posts blood request
       ↓
App finds all donors with matching blood group in Firestore
       ↓
Saves notification to each donor's personal notifications collection
       ↓
Firestore real-time listener fires instantly on donor's device
       ↓
System popup appears + badge updates on bell icon
       ↓
When donor donates → requester receives a fulfillment notification
```

---

## Database Structure

```
users/{userId}
  ├── name, email, phone, bloodGroup, city, state
  ├── available, donor, latitude, longitude
  ├── totalDonations, lastDonationDate
  ├── notifications/{notifId}
  └── donation_history/{donationId}

blood_requests/{requestId}
  ├── bloodGroup, hospitalName, city, urgency
  ├── status → OPEN / FULFILLED / CLOSED
  └── requesterId, donorId, createdAt
```

---

## What Makes This Different

- **Zero paid services** — No Google Maps billing, no Firebase Storage, no server
- **OpenStreetMap** instead of Google Maps — completely free with no API key
- **Base64 image encoding** stores profile photos directly in Firestore
- **Blood compatibility logic** built in — shows only medically compatible donors
- **Offline support** — Firestore local cache works on weak networks
- **90-day donation reminder** — tracks when a donor is next eligible to donate

---




<div align="center">

🩸 *Every donation saves up to 3 lives*

⭐ Star this repo if you found it useful

</div>
