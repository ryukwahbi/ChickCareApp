# How to Find Firebase Storage Rules 🔍

## 📍 Problem:
You're looking for Storage Rules, but you can't see the "Rules" tab in the Storage section.

---

## ⚠️ First: Enable Firebase Storage

**The Storage Rules tab only appears AFTER Storage is enabled!**

### Step 1: Enable Storage

1. **Go to Firebase Console:**
   - https://console.firebase.google.com/
   - Select project: **chickcare-ab7bc**

2. **Click "Storage"** in the left sidebar
   - (It's below "Firestore Database")

3. **You'll see one of these:**

   **Option A: "Get started" button**
   - Click **"Get started"**
   - Choose **"Start in production mode"** (or "Start in test mode" for development)
   - Select a **location** (choose closest to your users, e.g., "asia-southeast1")
   - Click **"Enable"**
   - Wait for Storage to initialize (takes a few seconds)

   **Option B: "Upgrade project" button**
   - If you see "To use Storage, upgrade your project's billing plan":
     - Firebase Spark (free) plan supports Storage with limits
     - You can use Storage on the free plan!
   - Click **"Upgrade project"** or look for "Get started" button
   - You might need to add a billing method (but free tier is generous)

4. **Wait for Storage to be enabled**
   - You'll see a success message
   - The page will refresh

---

## 🔍 Step 2: Find the Rules Tab

**After Storage is enabled, you'll see tabs at the top:**

1. **Go to Storage section** (left sidebar)
2. **Look at the top tabs:**
   - **"Files"** (default tab - shows uploaded files)
   - **"Rules"** ← This is what you need!
   - **"Usage"** (shows storage usage statistics)
   - **"Settings"** (bucket configuration)

3. **Click the "Rules" tab**

---

## 📝 Step 3: Update Storage Rules

Once you're in the **Rules** tab, you'll see a code editor. Replace the default rules with:

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

4. **Click "Publish"** button (top right)
5. **Wait for confirmation** that rules are published

---

## 🎯 Quick Visual Guide:

```
Firebase Console
├── Left Sidebar
│   ├── Project Overview
│   ├── Storage ← Click this!
│   └── ...
│
└── Main Area (after clicking Storage)
    ├── Tabs at top:
    │   ├── Files
    │   ├── Rules ← Click this!
    │   ├── Usage
    │   └── Settings
    │
    └── Code editor (shows Storage rules)
        └── Paste rules above
        └── Click "Publish"
```

---

## ⚠️ If You Still Don't See "Rules" Tab:

### Check 1: Storage is enabled?
- Look for "Files" tab at the top
- If no tabs visible → Storage is NOT enabled yet
- Follow Step 1 above to enable it

### Check 2: Billing issue?
- Free Spark plan supports Storage
- You might need to verify your account
- Check Firebase Console → Project Settings → Usage and billing

### Check 3: Wrong section?
- Make sure you're in **Storage**, not **Firestore Database**
- Firestore Database has its own Rules (different from Storage Rules!)

---

## 📋 Checklist:

- [ ] Storage is enabled (no "Get started" button visible)
- [ ] You see "Files" tab at the top
- [ ] You see "Rules" tab next to "Files"
- [ ] Clicked on "Rules" tab
- [ ] Pasted the security rules above
- [ ] Clicked "Publish"
- [ ] Got confirmation that rules are published

---

## 🔗 Direct Link (after Storage is enabled):

After enabling Storage, you can go directly to:
```
https://console.firebase.google.com/project/chickcare-ab7bc/storage/chickcare-ab7bc.firebasestorage.app/rules
```

Replace `chickcare-ab7bc` with your project ID if different.

---

**Summary:** Enable Storage first, then the "Rules" tab will appear at the top! 🎯

