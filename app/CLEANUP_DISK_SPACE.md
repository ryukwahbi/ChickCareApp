# Disk Space Cleanup Guide

## ⚠️ Error: "There is not enough space on the disk"

The build failed because your disk is full. Here's how to free up space:

## Quick Solutions:

### 1. **Clean Gradle Build Cache (Frees most space)**
Run these commands in your project root (`ChickCare` directory):

```bash
# Navigate to project root
cd C:\Users\PC-1\ChickCare-Thesis\ChickCare

# Clean build outputs
# NOTE: In PowerShell, use .\gradlew (with .\ prefix)
.\gradlew clean

# Clean Gradle cache (WARNING: This will remove ALL Gradle caches for ALL projects)
.\gradlew cleanBuildCache

# Or manually delete build folders
rmdir /s /q app\build
rmdir /s /q .gradle
rmdir /s /q build
```

### 2. **Clean Android Studio Caches**
In Android Studio:
- **File → Invalidate Caches / Restart → Invalidate and Restart**
- Or manually delete: `C:\Users\PC-1\.android\build-cache`
- Or delete: `C:\Users\PC-1\.gradle\caches` (be careful - this affects all Gradle projects)

### 3. **Delete Old Build Outputs**
```bash
# Delete all build outputs in the app folder
cd C:\Users\PC-1\ChickCare-Thesis\ChickCare\app
rmdir /s /q build
rmdir /s /q .cxx
rmdir /s /q .gradle
```

### 4. **Check Disk Space**
```bash
# Check free space on C: drive
wmic logicaldisk get name,freespace,size
```

### 5. **Temporary Files Cleanup**
- Run Windows Disk Cleanup: `cleanmgr`
- Empty Recycle Bin
- Delete temporary files: `C:\Users\PC-1\AppData\Local\Temp`

### 6. **Android SDK Cleanup** (if needed)
- Delete unused SDK versions in Android Studio: **Tools → SDK Manager → SDK Tools**
- Delete emulator images you don't use: **Tools → AVD Manager**

## Recommended Order:
1. **First**: Run `gradlew clean` in project root
2. **Second**: Clean Android Studio caches
3. **Third**: Check disk space with `wmic` command
4. **Fourth**: If still low, clean Windows temp files

## After Cleanup:
Once you have free space, try building again:
```powershell
cd C:\Users\PC-1\ChickCare-Thesis\ChickCare
.\gradlew assembleDebug
```

## Code Fix Applied:
✅ Fixed the `coerceIn` type mismatch in `AudioSpectrogramConverter.kt` (line 116)

