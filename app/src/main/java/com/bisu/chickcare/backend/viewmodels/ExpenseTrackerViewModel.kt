package com.bisu.chickcare.backend.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bisu.chickcare.backend.repository.Expense
import com.bisu.chickcare.backend.repository.ExpenseRepository
import com.bisu.chickcare.backend.utils.OfflineAuthHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ExpenseTrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ExpenseRepository()
    private val auth = FirebaseAuth.getInstance()
    
    private val _expenses = MutableStateFlow<List<Expense>>(emptyList())
    val expenses: StateFlow<List<Expense>> = _expenses.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private fun getCurrentUserId(): String? {
        val firebaseUid = auth.currentUser?.uid
        if (firebaseUid != null) return firebaseUid
        return OfflineAuthHelper.getCurrentLocalUserId(getApplication())
    }
    
    init {
        loadExpenses()
    }

    private fun loadExpenses() {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.getExpenses(userId).collect { expenses ->
                    _expenses.value = expenses
                }
            } catch (_: Exception) {
                // Error handling
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveExpense(expense: Expense) {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.saveExpense(userId, expense)
            } catch (_: Exception) {
                // Error handling
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteExpense(expenseId: String) {
        val userId = getCurrentUserId() ?: return
        viewModelScope.launch {
            try {
                _isLoading.value = true
                repository.deleteExpense(userId, expenseId)
            } catch (_: Exception) {
                // Error handling
            } finally {
                _isLoading.value = false
            }
        }
    }

}

