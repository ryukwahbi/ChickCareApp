# 🔄 How to Update google-services.json (After Adding SHA-1)

## ⚠️ Problem:
Bisan ug naa na ang SHA-1 sa Firebase Console, ang `google-services.json` file ninyo outdated pa gihapon!

**Current file shows:**
```json
"oauth_client": []
```
**Empty gihapon!** 😱

---

## ✅ Solution: Download NEW google-services.json

### **Step 1: Go to Firebase Console**
1. Open: https://console.firebase.google.com/project/chickcare-ab7bc/settings/general/android:com.bisu.chickcare
2. Or:
   - Firebase Console → Project Settings
   - Scroll down to "Your apps"
   - Click on "ChickCare" Android app

### **Step 2: Download NEW google-services.json**
1. **Click the button "google-services.json"** (naa sa right side)
2. **Save the file** - mag-download siya
3. **Replace** ang old file sa `app/google-services.json`

### **Step 3: Verify the New File**
Open ang bag-ong `google-services.json` ug check kung naa na ang:

```json
"oauth_client": [
  {
    "client_id": "782683773231-xxxxxxxxxxxxx.apps.googleusercontent.com",
    "client_type": 1,
    "android_info": {
      "package_name": "com.bisu.chickcare",
      "certificate_hash": "16:ea:9d:63:fc:ea:c2:25:c7:c4:1f:46:2c:ef:89:68:39:27:c9:61"
    }
  }
]
```

**Kung naa na ang `oauth_client` array naa sulod (dili empty), ✅ tama na!**

---

### **Step 4: Sync and Rebuild**
1. **Sync Gradle:**
   - File → Sync Project with Gradle Files
   - Wait for sync to complete

2. **Clean Project:**
   - Build → Clean Project

3. **Uninstall app from device:**
   - Settings → Apps → ChickCare → Uninstall
   - **Important:** Para ma-clear ang old cached configuration

4. **Rebuild and Install:**
   - Build → Rebuild Project
   - Run the app again

---

## ⏱️ Timing
- **Kung karon lang ninyo gi-add ang SHA-1:** Wait 5-10 minutes before downloading
- **Kung kagahapon pa:** Pwede na dayon mag-download!

---

## ✅ After Updating

**Check logcat - dili na mo-appear ang:**
- ❌ `SecurityException: Unknown calling package name 'com.google.android.gms'`
- ❌ `DEVELOPER_ERROR` for Google Play Services

**Instead, makita ninyo normal Firebase initialization logs.**

---

## 📝 Quick Checklist:
- [x] SHA-1 naa na sa Firebase Console ✅ (confirmed sa screenshot)
- [ ] Download NEW `google-services.json` gikan sa Firebase Console
- [ ] Replace old file sa `app/google-services.json`
- [ ] Verify `oauth_client` array naa sulod (dili empty)
- [ ] Sync Gradle
- [ ] Clean Project
- [ ] **Uninstall app from device**
- [ ] Rebuild and Install

---

**Status:** Ang SHA-1 naa na, pero ang `google-services.json` file outdated pa. Download lang bag-ong file! 🎯

