# Quick SHA-1 Fix Guide 🚀

## 🔴 Current Error:
```
SecurityException: Unknown calling package name 'com.google.android.gms'
DEVELOPER_ERROR - ConnectionResult{statusCode=DEVELOPER_ERROR}
```

---

## ✅ Quick Fix (3 Steps):

### **Step 1: Get SHA-1 Fingerprint**

**Sa Android Studio Terminal (bottom panel):**

```powershell
keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**OR sa Command Prompt/PowerShell:**

```powershell
cd C:\Users\PC-1\ChickCare-Thesis\ChickCare
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**Copy the SHA1 value** - makita nimo ang line nga `SHA1: XX:XX:XX:XX...`

---

### **Step 2: Add to Firebase Console**

1. Go to: https://console.firebase.google.com/
2. Select project: **chickcare-ab7bc**
3. Click **⚙️ Project Settings** (gear icon sa top left)
4. Scroll down to **"Your apps"** section
5. Click on your Android app: **com.bisu.chickcare**
6. Click **"Add fingerprint"** button
7. Paste your SHA-1 value
8. Click **Save**

---

### **Step 3: Download Updated google-services.json**

1. Sa same page (Project Settings → Your apps → com.bisu.chickcare)
2. Click **"Download google-services.json"**
3. Replace ang existing file sa: `app/google-services.json`
4. **Sync Gradle**: File → Sync Project with Gradle Files
5. **Rebuild**: Build → Clean Project → Rebuild Project

---

## ⚠️ Important Notes:

1. **After adding SHA-1**, mo-take around **5-10 minutes** para ma-propagate sa Google servers
2. **Restart ang app** after rebuilding
3. **Errors mawala** after ma-sync ang SHA-1

---

## 🔍 Alternative: Check if Really Needed

**If these features work, pwede ra temporary i-ignore:**
- ✅ Firebase Auth working?
- ✅ Firestore working?
- ✅ App runs normally?

**Pero mas maayo gihapon if ma-add ang SHA-1** for production! 🎯

---

**Status:** These are CONFIGURATION errors, not code bugs. App works, pero mas maayo if ma-fix! 🙏

