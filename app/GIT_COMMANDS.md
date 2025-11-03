# Git Commands Guide for ChickCare

## 🚀 PUSH EVERYTHING TO GITHUB

Run these commands in order:

### Step 1: Add ALL files (including untracked files)
```bash
git add .
```

### Step 2: Commit all changes
```bash
git commit -m "Complete project update - all files included"
```

### Step 3: Push to GitHub
```bash
git push origin main
```

**If you get errors during push, use force push (⚠️ Use carefully!):**
```bash
git push origin main --force
```

---

## 📥 SAFE PULL COMMANDS

### Option 1: Standard Safe Pull (Recommended)
```bash
# First, fetch to see what's on remote
git fetch origin

# Check what will change
git log HEAD..origin/main

# Then pull
git pull origin main
```

### Option 2: Pull with Rebase (Cleaner history)
```bash
git pull --rebase origin main
```

### Option 3: If you have local changes, stash first
```bash
# Save your local changes temporarily
git stash

# Pull from remote
git pull origin main

# Restore your changes
git stash pop
```

### Option 4: If conflicts occur during pull
```bash
# Pull and see conflicts
git pull origin main

# After resolving conflicts manually, stage the files
git add .

# Complete the merge
git commit -m "Merge conflicts resolved"
```

---

## 📋 CHECK STATUS BEFORE PUSH/PULL

Always check status first:
```bash
git status
```

---

## 🔧 USEFUL COMMANDS

### See what branch you're on
```bash
git branch
```

### See remote repository URL
```bash
git remote -v
```

### Discard local changes (⚠️ Use carefully!)
```bash
git restore .
```

### See commit history
```bash
git log --oneline
```

