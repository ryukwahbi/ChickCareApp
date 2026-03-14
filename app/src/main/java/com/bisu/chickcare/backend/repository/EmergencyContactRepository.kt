package com.bisu.chickcare.backend.repository

import android.content.Context
import androidx.core.content.edit
import com.bisu.chickcare.backend.data.EmergencyContact
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class EmergencyContactRepository(context: Context) {
    private val prefs = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    companion object {
        private const val KEY_CONTACTS = "emergency_contacts_list"
    }
    
    fun getAllContacts(): List<EmergencyContact> {
        val contactsJson = prefs.getString(KEY_CONTACTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<EmergencyContact>>() {}.type
            gson.fromJson(contactsJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    fun getContactsByType(type: EmergencyContact.ContactType): List<EmergencyContact> {
        return getAllContacts().filter { it.type == type }
    }
    
    fun getPrimaryVeterinarian(): EmergencyContact? {
        return getContactsByType(EmergencyContact.ContactType.VETERINARIAN).firstOrNull()
    }
    
    fun addContact(contact: EmergencyContact) {
        val contacts = getAllContacts().toMutableList()
        contacts.add(contact)
        saveContacts(contacts)
    }
    
    fun updateContact(contact: EmergencyContact) {
        val contacts = getAllContacts().toMutableList()
        val index = contacts.indexOfFirst { it.id == contact.id }
        if (index != -1) {
            contacts[index] = contact
            saveContacts(contacts)
        }
    }
    
    fun deleteContact(contactId: String) {
        val contacts = getAllContacts().toMutableList()
        contacts.removeAll { it.id == contactId }
        saveContacts(contacts)
    }
    
    private fun saveContacts(contacts: List<EmergencyContact>) {
        val json = gson.toJson(contacts)
        prefs.edit { putString(KEY_CONTACTS, json) }
    }
}

