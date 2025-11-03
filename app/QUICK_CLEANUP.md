# Quick Disk Space Cleanup (Android Studio Terminal)

## ✅ YES - Run commands in Android Studio Terminal!

### Step 1: Open Terminal in Android Studio
1. Click **View → Tool Windows → Terminal** (or press `Alt + F12`)
2. The terminal will open at the bottom of Android Studio

### Step 2: Run These Commands (One at a time)

```bash
# 1. Navigate to project root (if not already there)
cd C:\Users\PC-1\ChickCare-Thesis\ChickCare

# 2. Clean build outputs (this frees the most space)
# IMPORTANT: Use .\gradlew (not just gradlew) in PowerShell!
.\gradlew clean

# 3. Check if it worked - verify build folder is gone
dir app\build
# (If this says "Directory not found" or empty, it worked!)
```

### Alternative: Clean via Android Studio GUI

**Option A: Build Menu**
1. Click **Build → Clean Project**
2. Then click **Build → Rebuild Project** (only after you have disk space)

**Option B: Invalidate Caches**
1. Click **File → Invalidate Caches / Restart**
2. Select **Invalidate and Restart**
3. This cleans Android Studio caches

### Step 3: Check Disk Space (Optional)

In the terminal, run:
```bash
wmic logicaldisk get name,freespace,size
```

This shows free space on all drives. Look at your C: drive.

### Step 4: Try Building Again

Once you have free space:
```powershell
.\gradlew assembleDebug
```

Or click **Build → Make Project** in Android Studio.

---

## 💡 Tips:
- **Start with `gradlew clean`** - This is the safest and most effective
- You need at least **1-2 GB free** to build successfully
- The terminal in Android Studio works exactly like Command Prompt/PowerShell

## ❌ If Still Getting "Not Enough Space" Error:

Try these additional commands:
```bash
# Delete build folder manually
rmdir /s /q app\build

# Delete .gradle cache (careful - affects all projects)
rmdir /s /q .gradle\caches
```

---

**Ready? Start with Step 2, Command #2 (`gradlew clean`)!** 🚀

