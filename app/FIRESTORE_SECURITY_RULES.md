# Firestore Security Rules for Friend Suggestions

## Problem
The friend suggestions are not showing because Firestore is denying read access with `PERMISSION_DENIED: Missing or insufficient permissions.`

## Solution
You need to update your Firebase Security Rules to allow:
1. Authenticated users to read other users' profiles (for friend suggestions)
2. Users to read/write their own data
3. Users to manage their own friends and friend requests

## Steps to Update Rules:

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Click on **Firestore Database** in the left menu
4. Go to the **Rules** tab
5. Replace the existing rules with the following:

## Firestore Security Rules:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Helper function to check if user is authenticated
    function isAuthenticated() {
      return request.auth != null;
    }
    
    // Helper function to check if user owns the document
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }
    
    // Users collection
    match /users/{userId} {
      // Allow users to read any user's profile (needed for friend suggestions)
      allow read: if isAuthenticated();
      
      // Allow users to write only their own profile
      allow write: if isOwner(userId);
      
      // Friends subcollection
      match /friends/{friendId} {
        allow read, write: if isOwner(userId);
      }
      
      // Friend requests subcollection
      match /friendRequests/{requestId} {
        allow read, write: if isOwner(userId);
      }
      
      // Notifications subcollection
      match /notifications/{notificationId} {
        allow read, write: if isOwner(userId);
      }
      
      // Detections subcollection
      match /detections/{detectionId} {
        allow read, write: if isOwner(userId);
      }
      
      // Timeline posts subcollection
      match /timelinePosts/{postId} {
        allow read: if isAuthenticated();
        allow write: if isOwner(userId);
        allow delete: if isOwner(userId);
      }
    }
  }
}
```

## After Updating Rules:

1. Click **Publish** to save the rules
2. Wait a few seconds for the rules to propagate
3. Try the app again - friend suggestions should now work!

## Important Notes:

- These rules allow **any authenticated user** to **read** other users' profiles (needed for friend suggestions)
- Users can only **write** their own profile and subcollections
- Users can read timeline posts from anyone (for the timeline feature)
- These rules are suitable for a social app where users can see other users' public profiles

## Testing:

After updating the rules, you should see in Logcat:
- `Loaded X friend suggestions from server` (instead of PERMISSION_DENIED)
- Friend suggestions appearing in the app

