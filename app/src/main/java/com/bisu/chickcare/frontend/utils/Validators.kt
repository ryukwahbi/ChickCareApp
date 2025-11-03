package com.bisu.chickcare.frontend.utils

object Validators {
    
    /**
     * Check if email format is valid
     */
    fun isValidEmail(email: String): Boolean {
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
        return email.matches(emailRegex.toRegex())
    }
    
    /**
     * Check if password meets requirements
     * - At least 6 characters
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }
    
    /**
     * Check if password is strong
     * - At least 8 characters
     * - Contains at least one uppercase letter
     * - Contains at least one lowercase letter
     * - Contains at least one digit
     */
    fun isStrongPassword(password: String): Boolean {
        if (password.length < 8) return false
        
        val hasUpperCase = password.any { it.isUpperCase() }
        val hasLowerCase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        
        return hasUpperCase && hasLowerCase && hasDigit
    }
    
    /**
     * Get password strength description
     */
    fun getPasswordStrength(password: String): PasswordStrength {
        return when {
            password.isEmpty() -> PasswordStrength.EMPTY
            password.length < 4 -> PasswordStrength.WEAK
            password.length < 8 -> PasswordStrength.MEDIUM
            isStrongPassword(password) -> PasswordStrength.STRONG
            else -> PasswordStrength.MEDIUM
        }
    }
}

enum class PasswordStrength(val label: String) {
    EMPTY(""),
    WEAK("Weak"),
    MEDIUM("Medium"),
    STRONG("Strong")
}

