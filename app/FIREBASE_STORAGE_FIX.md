# Firebase Storage Upload Fix Guide 🔧

## 🔴 Problem:
```
StorageException has occurred.
Object does not exist at location.
Code: -13010 HttpResult: 404
```

This error occurs when trying to upload profile pictures or cover photos.

---

## ✅ What Was Fixed:

### 1. **Separated Profile Photo and Cover Photo Uploads**
   - **Before:** Both used `uploadProfileImage()` function
   - **After:** 
     - Profile photos → `uploadProfileImage()` → `profile_pictures/$userId.jpg`
     - Cover photos → `uploadCoverPhoto()` → `cover_photos/$userId.jpg`

### 2. **Added `coverPhotoUrl` Field**
   - Added `coverPhotoUrl` to `UserProfile` data class
   - Cover photos now save to separate field in Firestore

### 3. **Enhanced Error Logging**
   - Added detailed logging for upload operations
   - Logs storage path, upload status, and error details

---

## 🔧 Firebase Storage Security Rules Setup:

**The 404 error usually means Firebase Storage security rules are blocking uploads!**

### Step 1: Go to Firebase Console
1. Open: https://console.firebase.google.com/
2. Select project: **chickcare-ab7bc**
3. Click **Storage** in left menu
4. Click **Rules** tab

### Step 2: Update Security Rules

**Replace your current rules with:**

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    
    // Helper function to check if user is authenticated
    function isAuthenticated() {
      return request.auth != null;
    }
    
    // Helper function to check if user owns the file
    function isOwner(userId) {
      return isAuthenticated() && request.auth.uid == userId;
    }
    
    // Profile pictures - users can upload/read/delete their own
    match /profile_pictures/{userId}.jpg {
      allow read: if isAuthenticated(); // Anyone authenticated can view
      allow write: if isOwner(userId); // Only owner can upload/delete
      allow delete: if isOwner(userId);
    }
    
    // Cover photos - users can upload/read/delete their own
    match /cover_photos/{userId}.jpg {
      allow read: if isAuthenticated(); // Anyone authenticated can view
      allow write: if isOwner(userId); // Only owner can upload/delete
      allow delete: if isOwner(userId);
    }
    
    // Detection images - users can upload/read/delete their own
    match /detections/{userId}/{allPaths=**} {
      allow read, write, delete: if isOwner(userId);
    }
    
    // Default: deny all other access
    match /{allPaths=**} {
      allow read, write: if false;
    }
  }
}
```

### Step 3: Publish Rules
1. Click **Publish** button
2. Wait a few seconds for rules to propagate

---

## 🔍 Verify Firebase Storage is Enabled:

### Check if Storage is Enabled:
1. Firebase Console → **Storage**
2. If you see "Get started" button:
   - Click it
   - Choose **Start in production mode** (or test mode for development)
   - Select a location (choose closest to your users)
   - Click **Enable**

### Check Storage Bucket:
1. Firebase Console → Project Settings → General tab
2. Scroll to **Your apps** → **ChickCare** (Android)
3. Check **Storage bucket** field:
   - Should be: `chickcare-ab7bc.firebasestorage.app`
   - If different, verify in `google-services.json`

---

## 🧪 Testing After Fix:

1. **Clean and Rebuild:**
   ```
   Build → Clean Project
   Build → Rebuild Project
   ```

2. **Run the app:**
   - Try uploading a profile picture
   - Try uploading a cover photo

3. **Check Logcat:**
   - Look for: `AuthViewModel: Starting profile image upload`
   - Look for: `AuthViewModel: Profile image upload completed`
   - Should NOT see `StorageException` anymore

4. **Check Firebase Console:**
   - Storage → Files
   - You should see:
     - `profile_pictures/` folder
     - `cover_photos/` folder
   - Your uploaded images should appear there

---

## ⚠️ Common Issues:

### Issue 1: "Storage bucket not found"
**Solution:** 
- Verify Storage is enabled in Firebase Console
- Check `google-services.json` has correct `storage_bucket` value

### Issue 2: "Permission denied"
**Solution:**
- Update Storage security rules (see above)
- Make sure user is logged in (`auth.currentUser != null`)

### Issue 3: "Object does not exist at location" (404)
**Solution:**
- Storage rules are blocking OR
- Storage bucket not enabled OR
- Wrong bucket name in `google-services.json`

---

## 📋 Quick Checklist:

- [ ] Firebase Storage is enabled
- [ ] Storage security rules are updated (see above)
- [ ] Rules are published
- [ ] `google-services.json` has correct `storage_bucket`
- [ ] Code updated (profile photos use `uploadProfileImage`, cover photos use `uploadCoverPhoto`)
- [ ] `UserProfile` has `coverPhotoUrl` field
- [ ] App rebuilt and tested

---

## 🎯 Next Steps:

1. **Update Firebase Storage security rules** (most important!)
2. **Verify Storage is enabled** in Firebase Console
3. **Rebuild and test** the app
4. **Check Logcat** for detailed upload logs
5. **Verify files appear** in Firebase Console → Storage → Files

---

**The main fix:** Update Firebase Storage security rules to allow authenticated users to upload to `profile_pictures/` and `cover_photos/` folders!

