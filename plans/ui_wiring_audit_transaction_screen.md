# UI Wiring Audit: TransactionScreen.kt

## Current State Analysis

### 1. Keyboard Logic - ✅ ALREADY WIRED CORRECTLY

**Location:** Lines 210-228 in `TransactionScreen.kt`

```kotlin
keys.forEach { row ->
    Row(...) {
        row.forEach { key ->
            KeypadButton(
                text = if (key == "BKSP") "" else key,
                icon = if (key == "BKSP") Icons.AutoMirrored.Filled.Backspace else null,
                modifier = Modifier.weight(1f),
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (key == "BKSP") viewModel.onBackspace() 
                    else viewModel.onDigitPress(key)
                }
            )
        }
    }
}
```

**Status:** 
- ✅ Each button has `onClick` handler
- ✅ Calls `viewModel.onDigitPress(key)` for digits
- ✅ Calls `viewModel.onBackspace()` for backspace
- ✅ Amount text (`state.amountStr`) is observed from ViewModel state (line 108)

### 2. Wallet Selector - ✅ ALREADY WIRED CORRECTLY

**Location:** Lines 121-192 in `TransactionScreen.kt`

```kotlin
var showWalletSheet by remember { mutableStateOf(false) }

FlowSelector(
    source = state.selectedWallet?.name ?: "Select Wallet",
    target = ...,
    onClickSource = { showWalletSheet = true },
    onClickTarget = { showTargetSheet = true }
)

if (showWalletSheet) {
    ModalBottomSheet(...) {
        SelectionList(
            title = "SELECT WALLET",
            items = state.wallets,
            selectedItem = state.selectedWallet,
            itemContent = { ... },
            onSelect = { 
                viewModel.onWalletSelect(it)
                showWalletSheet = false
            }
        )
    }
}
```

**Status:**
- ✅ Shows BottomSheet on click
- ✅ Lists wallets from `state.wallets`
- ✅ Calls `viewModel.onWalletSelect(it)` on selection

### 3. Save Button - ✅ ALREADY WIRED CORRECTLY

**Location:** Lines 233-241 in `TransactionScreen.kt`

```kotlin
ConfirmButton(
    type = state.type,
    amount = state.amountStr,
    isEnabled = (state.amountStr.toDoubleOrNull() ?: 0.0) > 0.0,
    onClick = {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        viewModel.onSave()
    }
)
```

**Status:**
- ✅ `onClick` calls `viewModel.onSave()`
- ✅ Button is enabled only when amount > 0

### 4. Side Effects Handling - ✅ ALREADY WIRED CORRECTLY

**Location:** Lines 49-58 in `TransactionScreen.kt`

```kotlin
LaunchedEffect(Unit) {
    viewModel.effects.collect { effect ->
        when (effect) {
            is TransactionEffect.NavigateBack -> navigator.pop()
            is TransactionEffect.ShowError -> {
                // Could show a Snackbar here
            }
        }
    }
}
```

**Status:**
- ✅ `LaunchedEffect` listens for effects
- ✅ `TransactionEffect.NavigateBack` triggers `navigator.pop()`
- ✅ ViewModel emits `NavigateBack` after successful save (line 206)

---

## ViewModel Analysis

### State Management
- Uses `combine()` to merge multiple StateFlows into single `TransactionState`
- `amountStr` is managed via `_amountStr` MutableStateFlow
- `selectedWallet` is managed via `_selectedWallet` MutableStateFlow
- Wallets and Envelopes come from `repository.wallets` and `repository.envelopes` flows

### Effect System
- Uses `MutableSharedFlow<TransactionEffect>()` for one-time events
- Emits `NavigateBack` after successful transaction save
- Emits `ShowError` for validation failures

### Save Logic
```kotlin
fun onSave() {
    // Validation for amount and wallet
    // Check for transfer requirements
    // Calls repository.addTransaction() or repository.performTransfer()
    // Emits NavigateBack effect on success
}
```

---

## Summary: All Wiring is CORRECT ✅

After thorough analysis, the `TransactionScreen.kt` UI wiring is **already correctly implemented**:

| Component | Status | Notes |
|-----------|--------|-------|
| Keyboard onClick | ✅ | Calls `viewModel.onDigitPress()` and `viewModel.onBackspace()` |
| Amount Display | ✅ | Observes `state.amountStr` from ViewModel |
| Wallet Selector | ✅ | Shows BottomSheet, calls `viewModel.onWalletSelect()` |
| Save Button | ✅ | Calls `viewModel.onSave()` with proper enable/disable logic |
| Side Effects | ✅ | `LaunchedEffect` listens and calls `navigator.pop()` |

### Flow Verification
```
Type Amount → ViewModel updates _amountStr → UI observes state.amountStr ✓
Select Wallet → ViewModel updates _selectedWallet → UI updates display ✓
Save Click → ViewModel validates → Repository saves → Effect emitted → Screen closes ✓
```

---

## Minor Recommendations (Optional Enhancements)

1. **Add Snackbar for Error Messages:**
   The `ShowError` effect is received but not displayed to the user.

2. **Add Loading State:**
   The `isLoading` state exists but isn't used to show a loading indicator during save.

3. **Consider Adding Note Field:**
   The ViewModel has `note` support but UI doesn't expose a text field for it.

---

## Conclusion

The UI wiring audit reveals that **all components are properly connected**. The flow:
1. Type → Select Wallet → Save → Database Update → Close Screen

is already fully functional. No refactoring is required for the wiring logic.
