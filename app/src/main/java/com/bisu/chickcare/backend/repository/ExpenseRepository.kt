package com.bisu.chickcare.backend.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class Expense(
    val id: String = "",
    val category: String = "",
    val amount: Double = 0.0,
    val date: Long = 0,
    val description: String = "",
    val paymentMethod: String = "",
    val createdAt: Long = 0
)

class ExpenseRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun saveExpense(userId: String, expense: Expense) {
        try {
            val expenseData = hashMapOf(
                "category" to expense.category,
                "amount" to expense.amount,
                "date" to expense.date,
                "description" to expense.description,
                "paymentMethod" to expense.paymentMethod,
                "createdAt" to System.currentTimeMillis()
            )
            
            if (expense.id.isEmpty()) {
                usersCollection.document(userId).collection("expenses").add(expenseData).await()
            } else {
                usersCollection.document(userId).collection("expenses")
                    .document(expense.id).set(expenseData).await()
            }
            Log.d("ExpenseRepository", "Expense saved successfully")
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error saving expense: ${e.message}", e)
            throw e
        }
    }

    fun getExpenses(userId: String): Flow<List<Expense>> = callbackFlow {
        val listener = usersCollection.document(userId).collection("expenses")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ExpenseRepository", "Error fetching expenses: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                
                if (snapshot != null) {
                    val expenses = snapshot.documents.map { doc ->
                        Expense(
                            id = doc.id,
                            category = doc.getString("category") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            date = doc.getLong("date") ?: 0L,
                            description = doc.getString("description") ?: "",
                            paymentMethod = doc.getString("paymentMethod") ?: "",
                            createdAt = doc.getLong("createdAt") ?: 0L
                        )
                    }
                    trySend(expenses)
                }
            }
        
        awaitClose { listener.remove() }
    }

    suspend fun deleteExpense(userId: String, expenseId: String) {
        try {
            usersCollection.document(userId).collection("expenses")
                .document(expenseId).delete().await()
            Log.d("ExpenseRepository", "Expense deleted successfully")
        } catch (e: Exception) {
            Log.e("ExpenseRepository", "Error deleting expense: ${e.message}", e)
            throw e
        }
    }
}

