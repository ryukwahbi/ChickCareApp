# Duplicate Class Error Fix

## 🔴 Problem:
**Duplicate class errors** between:
- `com.google.ai.edge.litert:litert-support-api:1.0.1`
- `org.tensorflow:tensorflow-lite-support-api:0.4.4`

Both provide the same classes in `org.tensorflow.lite.support.*` package.

---

## ✅ Solution Applied:

### **1. Fixed Version Alignment** (`gradle/libs.versions.toml`)
**Changed:**
- `tensorflowLiteSupport = "0.5.0"` → `tensorflowLiteSupport = "0.4.4"`

**Why:**
- Version 0.5.0 of `tensorflow-lite-support` pulls in `litert-support-api` as a dependency
- This conflicts with `tensorflow-lite-support-api:0.4.4` used by `tensorflow-lite-task-vision:0.4.4`
- All TFLite support packages must use 0.4.4 for compatibility

---

### **2. Added Global Exclusions** (`build.gradle.kts`)
**Added:**
```kotlin
configurations.all {
    exclude(group = "com.google.ai.edge.litert", module = "litert-support-api")
    resolutionStrategy {
        force("org.tensorflow:tensorflow-lite-support-api:0.4.4")
    }
}
```

**Why:**
- Globally excludes `litert-support-api` from ALL dependencies
- Forces resolution to use `tensorflow-lite-support-api:0.4.4` from `org.tensorflow`

---

### **3. Added Per-Dependency Excludes**
**Added excludes to all TFLite dependencies:**
- `tensorflow-lite`
- `tensorflow-lite-gpu`
- `tensorflow-lite-task-vision`
- `tensorflow-lite-support`
- `tensorflow-lite-gpu-delegate-plugin`

All exclude: `com.google.ai.edge.litert:litert-support-api`

**Why:**
- Extra safety - ensures `litert-support-api` is never included
- Prevents transitive dependency conflicts

---

## 📋 Summary of Changes:

1. ✅ **Version alignment**: `tensorflow-lite-support:0.5.0` → `0.4.4`
2. ✅ **Global exclusion**: Excludes `litert-support-api` from all configurations
3. ✅ **Force resolution**: Forces `tensorflow-lite-support-api:0.4.4`
4. ✅ **Per-dependency excludes**: Added excludes to each TFLite dependency

---

## 🎯 Next Steps:

1. **Sync Gradle:**
   - File → Sync Project with Gradle Files
   - Or: Click "Sync Now" if prompted

2. **Clean and Rebuild:**
   ```powershell
   .\gradlew clean
   .\gradlew assembleDebug
   ```

3. **Verify Build:**
   - Build should succeed without duplicate class errors
   - No more `litert-support-api` conflicts

---

## ✅ Expected Result:

- ✅ No duplicate class errors
- ✅ All TFLite dependencies use compatible versions (0.4.4)
- ✅ Build succeeds
- ✅ Fusion model should load correctly

---

**Status:** ✅ All fixes applied! Ready to rebuild.

