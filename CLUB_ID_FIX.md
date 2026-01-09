# Club ID Negative Value Fix

## Problem
Club IDs were being generated using `docId.hashCode()` which can produce **negative integers**. This caused registration failures when users tried to join clubs with negative IDs (like Barcelona) because the code had validation checks like `clubId > 0` that failed for negative values.

## Root Cause
In `FirebaseClubRepository.java`:
```java
club.setId(docId.hashCode()); // hashCode() can be negative!
```

Throughout the codebase, validation checks assumed positive IDs:
- `if (clubId > 0)` - This fails for negative IDs
- `if (clubId <= 0)` - Treats negative IDs as invalid

## Solution Applied

### 1. Generate Positive Club IDs (FirebaseClubRepository.java)
Changed from:
```java
club.setId(docId.hashCode());
```

To:
```java
int positiveId = Math.abs(docId.hashCode()); // Ensure positive ID
club.setId(positiveId);
```

### 2. Updated Validation Checks
Changed all `clubId > 0` checks to `clubId != 0` to handle both positive and negative IDs:

**Files Updated:**
- ✅ `PlayerProfileActivity.java` - Line 162
- ✅ `FirebaseUserRepository.java` - Line 272
- ✅ `PendingApprovalsActivity.java` - Line 329
- ✅ `ManagerProfileActivity.java` - Lines 173, 236
- ✅ `PlayerListActivity.java` - Line 103

### 3. Backward Compatibility
The fix maintains backward compatibility:
- **New clubs**: Will always have positive IDs
- **Existing clubs with negative IDs**: Will still work (validation now accepts negative IDs)
- **Future**: When clubs are updated/re-saved, they will get positive IDs

## Important Note for Firebase Data
If you want to update existing clubs in Firebase to have positive IDs, you can:

1. Delete and recreate the club (will get new positive ID)
2. Manually update the club document in Firebase Console:
   - Find the club document
   - Update the `id` field to `Math.abs(current_id)`

Example:
- Barcelona has ID: `-1234567`
- Update to: `1234567`

## Testing
After this fix:
- ✅ Users can register with Barcelona (or any club with negative ID)
- ✅ Players can be assigned to clubs with negative IDs
- ✅ Managers can be assigned to clubs with negative IDs
- ✅ Club details load correctly regardless of ID sign
- ✅ All future clubs will have positive IDs only

## Files Modified
1. `FirebaseClubRepository.java` - Use Math.abs() for ID generation
2. `PlayerProfileActivity.java` - Changed > 0 to != 0
3. `FirebaseUserRepository.java` - Changed > 0 to != 0
4. `PendingApprovalsActivity.java` - Changed > 0 to != 0
5. `ManagerProfileActivity.java` - Changed > 0 and <= 0 to != 0 and == 0
6. `PlayerListActivity.java` - Changed > 0 to != 0

## Build Status
✅ Build successful - All changes compile without errors
