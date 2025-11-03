# Firestore Indexes Setup Guide

## ⚠️ Required Indexes

Your app needs **composite indexes** in Firestore for efficient querying. The errors will stop once these indexes are created.

---

## 🔧 How to Create Indexes

### **Method 1: Use the Links from Error Messages** (Easiest)

The error messages in logcat contain direct links to create the indexes. Just click them!

Example link format:
```
https://console.firebase.google.com/v1/r/project/chickcare-ab7bc/firestore/indexes?create_composite=...
```

---

### **Method 2: Manual Setup in Firebase Console**

1. **Open Firebase Console**: https://console.firebase.google.com/
2. **Select your project**: `chickcare-ab7bc`
3. **Go to Firestore Database** → **Indexes** tab
4. **Click "Create Index"**
5. **Add the following indexes:**

#### **Index 1: Detection History Query**
- **Collection ID**: `detections` (under `users/{userId}/detections`)
- **Fields to index**:
  1. `isDeleted` → **Ascending**
  2. `timestamp` → **Descending**
- **Query scope**: Collection
- **Status**: Enable

#### **Index 2: Recently Deleted Query**
- **Collection ID**: `detections` (under `users/{userId}/detections`)
- **Fields to index**:
  1. `isDeleted` → **Ascending**
  2. `deletedTimestamp` → **Descending**
- **Query scope**: Collection
- **Status**: Enable

#### **Index 3: Cleanup Old Deleted Items Query**
- **Collection ID**: `detections` (under `users/{userId}/detections`)
- **Fields to index**:
  1. `isDeleted` → **Ascending**
  2. `deletedTimestamp` → **Ascending**
- **Query scope**: Collection
- **Status**: Enable

---

## ⏱️ Index Creation Time

- **First index**: Usually takes **30 seconds to 5 minutes**
- **Subsequent indexes**: Can take **1-10 minutes** each
- **All indexes must be "Enabled"** before queries will work

---

## ✅ Verification

After creating indexes:
1. Wait for them to show **"Enabled"** status
2. Restart your app
3. Check logcat - the errors should be gone

---

## 📝 Notes

- **The app will still work** with fallback queries (filtering in-memory) while indexes are being created
- **Indexes are free** for most use cases
- **Once created**, queries will be much faster

---

## 🔗 Quick Links

- **Firebase Console**: https://console.firebase.google.com/project/chickcare-ab7bc/firestore/indexes
- **Project Dashboard**: https://console.firebase.google.com/project/chickcare-ab7bc

---

**Status**: ✅ Code updated with fallback queries - app will work even without indexes!

