# Smart Civic Governance and Leaderboard System

An advanced Android application built using Kotlin and XML layouts following MVVM design patterns. The app empowers citizens to report local civic issues (such as potholes, garbage, or streetlight outages), assigns them to workers, allows workers to submit photographic proof of resolution, and ranks workers on a gamified leaderboard with points and badges.

---

## Technical Stack

*   **Android Development**: Kotlin, Android Studio (targetSdk 34, minSdk 24), ViewBinding, Navigation Component
*   **Firebase Backend**:
    *   **Authentication**: Email/Password Sign-In
    *   **Firestore Database**: Core data models (Complaints, Users, Workers, Notifications)
    *   **Storage**: Photo uploads for reported complaints and worker completion proofs
    *   **Cloud Messaging**: Push notifications for status updates and task assignments
    *   **Client-Side Business Logic**: Point calculations, badges, notifications, and automated ranking triggers are executed client-side (Spark-friendly, no Blaze plan required)
*   **External SDKs**:
    *   **Google Maps SDK**: GPS issue location picker and admin heatmap view
    *   **Glide**: Image loading and caching
    *   **MPAndroidChart**: Analytics dashboards (trends, category resolutions, and status statistics)

---

## Database Collections Schema

### `users/{uid}`
*   `uid`: String (Firebase Auth UID)
*   `name`: String
*   `email`: String
*   `role`: String ("citizen" | "worker" | "admin")
*   `photoUrl`: String
*   `createdAt`: Timestamp
*   `fcmToken`: String
*   `disabled`: Boolean (used to enable/disable user accounts)

### `complaints/{complaintId}`
*   `complaintId`: String
*   `title`: String
*   `description`: String
*   `category`: String ("Pothole" | "Garbage" | "Water Leakage" | "Drainage" | "Streetlight" | "Traffic")
*   `imageUrl`: String
*   `latitude`: Double
*   `longitude`: Double
*   `address`: String
*   `status`: String ("Pending" | "In Progress" | "Resolved" | "Rejected")
*   `citizenId`: String
*   `citizenName`: String
*   `workerId`: String?
*   `workerName`: String?
*   `proofImageUrl`: String?
*   `createdAt`: Timestamp
*   `acceptedAt`: Timestamp?
*   `resolvedAt`: Timestamp?
*   `citizenRating`: Int? (1-5)
*   `citizenFeedback`: String?
*   `priority`: String ("High" | "Medium" | "Low")
*   `isDuplicate`: Boolean (auto-flagged if within 100m filed in last 24h)
*   `verified`: Boolean (admin approval flag)

### `workers/{workerId}`
*   `uid`: String
*   `name`: String
*   `totalPoints`: Int
*   `issuesSolved`: Int
*   `activeTasks`: Int
*   `averageResolutionTimeMinutes`: Double
*   `averageRating`: Double
*   `rank`: Int
*   `badges`: List<String>
*   `joinedAt`: Timestamp

### `notifications/{notifId}`
*   `recipientId`: String
*   `title`: String
*   `body`: String
*   `type`: String ("complaint_accepted" | "complaint_resolved" | "points_earned" | "task_assigned")
*   `complaintId`: String
*   `isRead`: Boolean
*   `createdAt`: Timestamp

---

## Setup and Configurations

### 1. Firebase Configuration

1.  Create a project in the [Firebase Console](https://console.firebase.google.com/).
2.  Enable **Authentication** (Email/Password), **Firestore Database**, and **Cloud Storage**.
3.  Add an Android App to the project with package name `com.example.smartcivicgovernance`.
4.  Download `google-services.json` and place it in the `app/` directory of this project.

### 2. Deploying Firestore & Storage Rules

Make sure you have the [Firebase CLI](https://firebase.google.com/docs/cli) installed. Run:

```bash
# Log in to Firebase CLI
firebase login

# Set your active firebase project
firebase use --add YOUR_PROJECT_ID

# Deploy Firestore Security Rules
firebase deploy --only firestore:rules

# Deploy Storage Security Rules
firebase deploy --only storage:rules
```

### 3. Google Maps API Configuration

1.  Get a Google Maps SDK API key from the [Google Cloud Console](https://console.cloud.google.com/).
2.  Open `app/src/main/AndroidManifest.xml` and replace `YOUR_MAPS_API_KEY_HERE` with your API key:
    ```xml
    <meta-data
        android:name="com.google.android.geo.API_KEY"
        android:value="YOUR_REAL_API_KEY_HERE" />
    ```

---

## Verification Plan

### Build Verification
Verify the project compiles using Gradle:
```powershell
# Set JAVA_HOME to Android Studio's Embedded JBR
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
# Build application
.\gradlew assembleDebug
```

### Manual Verification
1.  Run the application on an emulator or test device.
2.  Register a new Citizen account and file a complaint using the Maps picker to select a location.
3.  Log in as Admin and navigate to **Worker Assignment** to delegate the task to a worker.
4.  Log in as Worker, view active tasks under the **Tasks** tab, click "Accept", and click "Submit Proof" to upload an image and resolve the complaint.
5.  Log in as Admin, navigate to **Verification**, inspect the submitted resolution proof, and click "Approve".
6.  Verify that worker points are successfully added and ranks recalculate on the leaderboard.
