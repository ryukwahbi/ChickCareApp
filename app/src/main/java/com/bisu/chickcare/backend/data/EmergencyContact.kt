package com.bisu.chickcare.backend.data

data class EmergencyContact(
    val id: String,
    val name: String,
    val phoneNumber: String,
    val type: ContactType, // Veterinarian, Farm Help, Emergency, etc.
    val notes: String? = null
) {
    enum class ContactType {
        VETERINARIAN,
        FARM_HELP,
        EMERGENCY,
        OTHER
    }
    
    fun getTypeLabel(): String {
        return when (type) {
            ContactType.VETERINARIAN -> "Veterinarian"
            ContactType.FARM_HELP -> "Farm Help"
            ContactType.EMERGENCY -> "Emergency"
            ContactType.OTHER -> "Other"
        }
    }
}

