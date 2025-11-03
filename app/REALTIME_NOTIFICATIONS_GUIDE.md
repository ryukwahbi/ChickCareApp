# Real-Time Notifications Guide

## ✅ Current Status: Real-Time Already Working!

**Your app ALREADY has real-time notifications working!** You're using **Firebase Firestore Snapshot Listeners** which automatically update when new notifications are added to Firestore.

### How It Works Now:
1. `NotificationRepository.getNotifications()` uses `addSnapshotListener()` → Real-time updates
2. `DashboardViewModel.listenToNotifications()` collects the Flow → UI updates automatically
3. When a new notification is added to Firestore, your app receives it **instantly** (while app is open)

---

## 🤔 Do You Need WebSockets?

**Short Answer: NO, you don't need WebSockets.**

### Why WebSockets Are Not Needed:

| Feature | Current Solution | Works? |
|---------|------------------|--------|
| Real-time updates (app open) | ✅ Firestore Snapshot Listeners | ✅ Already implemented |
| Background notifications | ❌ Not implemented (use FCM instead) | ⚠️ Need FCM |
| Custom protocol | ❌ Not needed | ✅ Firestore is enough |

**WebSockets are only needed if:**
- ❌ You want a custom backend (not Firebase)
- ❌ You need a specific protocol that Firestore doesn't support
- ❌ You need real-time communication between devices (like chat)

Since you're using **Firebase**, stick with Firestore listeners + FCM.

---

## 🚀 Recommended: Add Firebase Cloud Messaging (FCM)

**Why add FCM?**
- Firestore listeners work great when app is **open**
- But they stop working when app is **closed** or **backgrounded**
- FCM provides **push notifications** even when app is closed

### Step-by-Step: Adding FCM

#### 1. Add FCM Dependency

Check your `gradle/libs.versions.toml` and add:
```toml
firebase-messaging = "23.4.0"
```

Then in `build.gradle.kts`:
```kotlin
implementation(libs.firebase.messaging)
```

#### 2. Create FirebaseMessagingService

Create: `src/main/java/com/bisu/chickcare/backend/service/ChickCareMessagingService.kt`

```kotlin
package com.bisu.chickcare.backend.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bisu.chickcare.MainActivity
import com.bisu.chickcare.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ChickCareMessagingService : FirebaseMessagingService() {
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Handle notification when app is in foreground
        remoteMessage.notification?.let { notification ->
            sendNotification(
                title = notification.title ?: "ChickCare",
                message = notification.body ?: "",
                data = remoteMessage.data
            )
        }
    }
    
    override fun onNewToken(token: String) {
        // Save FCM token to Firestore for this user
        // This token is needed to send notifications to specific users
        saveTokenToFirestore(token)
    }
    
    private fun sendNotification(title: String, message: String, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            // Add notification data if needed
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val channelId = "chickcare_notifications"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.chicken_icon) // Use your app icon
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "ChickCare Notifications",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
        
        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
    
    private fun saveTokenToFirestore(token: String) {
        // Save FCM token when user logs in
        // Implement in your AuthViewModel or UserRepository
    }
}
```

#### 3. Register Service in AndroidManifest.xml

Add to your `AndroidManifest.xml`:

```xml
<service
    android:name=".backend.service.ChickCareMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

#### 4. Request Notification Permission (Android 13+)

In your `MainActivity` or `DashboardScreen`:

```kotlin
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

@Composable
fun RequestNotificationPermission() {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, FCM can now send notifications
        }
    }
    
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
```

#### 5. Get FCM Token on Login

In your `AuthViewModel` or after login:

```kotlin
import com.google.firebase.messaging.FirebaseMessaging

fun saveFCMToken(userId: String) {
    FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            val token = task.result
            // Save token to Firestore: users/{userId}/fcmToken = token
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .update("fcmToken", token)
        }
    }
}
```

#### 6. Send Push Notification from Backend

When you want to send a notification:

```kotlin
// Option 1: Using Firebase Admin SDK (backend/server)
// Option 2: Using Cloud Functions (recommended)

// Cloud Function example:
exports.sendNotification = functions.firestore
    .document('users/{userId}/notifications/{notificationId}')
    .onCreate(async (snap, context) => {
        const notification = snap.data();
        const userId = context.params.userId;
        
        // Get user's FCM token
        const userDoc = await admin.firestore()
            .collection('users')
            .doc(userId)
            .get();
        const fcmToken = userDoc.data()?.fcmToken;
        
        if (fcmToken) {
            await admin.messaging().send({
                token: fcmToken,
                notification: {
                    title: notification.title,
                    body: notification.message
                },
                data: {
                    type: notification.type,
                    notificationId: context.params.notificationId
                }
            });
        }
    });
```

---

## 📊 Summary: Real-Time Notification Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    NOTIFICATION FLOW                     │
└─────────────────────────────────────────────────────────┘

1. NEW NOTIFICATION CREATED
   ↓
2. Saved to Firestore: users/{userId}/notifications/{id}
   ↓
3. TWO PATHS WORK SIMULTANEOUSLY:

   PATH A (App Open)              PATH B (App Closed/Background)
   ↓                              ↓
   Firestore Snapshot Listener    Cloud Function Triggered
   ↓                              ↓
   Flow updates in Repository     Send FCM Push Notification
   ↓                              ↓
   ViewModel collects Flow        Android System receives FCM
   ↓                              ↓
   UI updates automatically       Notification appears in tray
```

---

## ✅ Best Practices

1. **Use Firestore Listeners** for real-time updates when app is open
2. **Use FCM** for push notifications when app is closed
3. **Store FCM tokens** in Firestore for each user
4. **Use Cloud Functions** to automatically send FCM when notifications are created
5. **Handle notification taps** to navigate to the correct screen

---

## 🎯 Action Items

- [ ] Add FCM dependency to `build.gradle.kts`
- [ ] Create `ChickCareMessagingService.kt`
- [ ] Register service in `AndroidManifest.xml`
- [ ] Request notification permission (Android 13+)
- [ ] Save FCM token on login
- [ ] Create Cloud Function to send FCM (optional but recommended)

---

## ❓ FAQ

**Q: Why not use WebSockets?**
A: Firebase provides all real-time features you need. WebSockets add complexity without benefits for your use case.

**Q: Do I need both Firestore listeners AND FCM?**
A: Yes! Firestore for app-open, FCM for app-closed.

**Q: What if notifications aren't updating in real-time?**
A: Check that `listenToNotifications()` is called in ViewModel init, and the Flow is properly collected in UI.

**Q: How do I test real-time notifications?**
A: 
1. Open app on device A
2. Add notification from Firebase Console or another device
3. Should see it appear instantly on device A

---

## 📚 Resources

- [Firebase Firestore Real-time Updates](https://firebase.google.com/docs/firestore/query-data/listen)
- [Firebase Cloud Messaging](https://firebase.google.com/docs/cloud-messaging)
- [Android Notification Channels](https://developer.android.com/develop/ui/views/notifications/channels)

