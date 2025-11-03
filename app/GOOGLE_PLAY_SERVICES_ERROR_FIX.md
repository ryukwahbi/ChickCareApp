# Google Play Services Error Fix Guide

## 🔴 Errors Found in Logcat:

1. **SecurityException**: `Unknown calling package name 'com.google.android.gms'`
2. **ProviderInstaller Failure**: Failed to register `com.google.android.gms.providerinstaller`
3. **Phenotype.API Error**: `ConnectionResult(statusCode=DEVELOPER_ERROR)`
4. **ODEX Loading Warning**: `Loading .../base.odex non-executable` (can be ignored)

**Note:** These errors are CONFIGURATION issues, not code bugs. The app will still work, but Firebase features may be limited until SHA-1 is added.

## ⚠️ Common Causes:

1. **Missing SHA-1 Fingerprint** sa Firebase Console (most common)
2. **Debug keystore** wala na-register
3. **Google Play Services outdated** sa device
4. **App signature mismatch**

---

## ✅ Solution Steps:

### **Step 1: Get Your Debug SHA-1 and SHA-256 Fingerprints** ⚠️ REQUIRED

**Option A: Using Terminal in Android Studio (Easiest)**

1. Open **Terminal** tab sa Android Studio (bottom panel)
2. Run this command:
   ```powershell
   keytool -list -v -keystore "$env:USERPROFILE\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
   ```

**Option B: Using Command Prompt/PowerShell**

```powershell
cd C:\Users\PC-1\ChickCare-Thesis\ChickCare
keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android
```

**What to Look For:**
- Find the line that says: `SHA1: XX:XX:XX:XX:XX:XX...`
  - Copy the ENTIRE SHA1 value (example: `16:EA:9D:63:FC:EA:C2:25:C7:C4:1F:46:2C:EF:89:68:39:27:09:61`)
- Find the line that says: `SHA256: XX:XX:XX:XX:XX:XX...`
  - Copy the ENTIRE SHA256 value (example: `38:77:B2:8D:72:34:9F:E5:1E:02:54:B7:00:09:50:4C:FB:32:77:F0:28:97:8C:52:18:BD:87:BC:A3:A7:05:69`)
- Make sure to copy ALL the characters, including colons `:`

---

### **Step 2: Add SHA-1 and SHA-256 to Firebase Console**

1. Open **Firebase Console**: https://console.firebase.google.com/
2. Select your project: **chickcare-ab7bc**
3. Click **Project Settings** (gear icon)
4. Scroll down to **"Your apps"** section
5. Click on your Android app (`com.bisu.chickcare`)
6. Click **"Add fingerprint"**
7. Paste your **SHA-1** fingerprint
8. Click **"Add fingerprint"** again (to add SHA-256)
9. Paste your **SHA-256** fingerprint
10. Click **Save**

---

### **Step 3: Download Updated google-services.json**

1. Sa Firebase Console, click **"Download google-services.json"**
2. Replace ang existing `google-services.json` sa `app` folder
3. Rebuild the app: **Build → Clean Project**, then **Build → Rebuild Project**

---

### **Step 4: Update Google Play Services sa Device** (Optional pero recommended)

1. Open **Play Store** sa imong Samsung device
2. Search **"Google Play Services"**
3. Click **Update** kung naay available update
4. Restart ang device

---

### **Step 5: Rebuild and Test**

```powershell
.\gradlew clean
.\gradlew assembleDebug
```

Or use Android Studio: **Build → Make Project**

---

## 🔍 Alternative: Check if Errors Affect App Functionality

**Important:** Kining errors kasagaran **WARNING lang** ug dili makapa-crash sa app. Check if:
- ✅ Firebase Auth mo-work gihapon?
- ✅ Firestore mo-work gihapon?
- ✅ Location services mo-work gihapon?
- ✅ App mo-run gihapon nga normal?

**Kung tanan mo-work gihapon**, pwede ra nimo i-ignore temporarily. Pero mas maayo if ma-fix gihapon for production.

---

## 📝 Quick Reference:

### Common Error Meanings:

| Error | Meaning | Fix |
|-------|---------|-----|
| `SecurityException: Unknown calling package` | SHA-1 wala registered | Add SHA-1 sa Firebase Console |
| `DEVELOPER_ERROR` | Firebase configuration issue | Update google-services.json |
| `ProviderInstaller failed` | Google Play Services outdated | Update sa Play Store |
| `Phenotype.API not available` | Firebase remote config issue | Usually safe to ignore |

---

## ✅ After Fixing:

After ma-add ang SHA-1 ug ma-rebuild, ang errors ma-disappear or ma-reduce. Check lang gihapon sa logcat.

**Most Important:** Add ang SHA-1 ug SHA-256 fingerprints sa Firebase Console! 🎯

**Note:** Firebase supports both SHA-1 and SHA-256. While SHA-1 is required, adding SHA-256 is recommended for better security and future-proofing.

