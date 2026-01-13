package com.chatio.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.chatio.model.ContactModel
import com.chatio.model.ContactType

object SmsReader {
    
    /**
     * SMS geçmişini okur
     * @param distinct: True ise numaraları tekilleştirir, False ise ham listeyi döner
     */
    fun readSmsHistory(context: Context, distinct: Boolean = true): List<ContactModel> {
        // İzin kontrolü
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        
        val distinctContacts = mutableMapOf<String, ContactModel>()
        val allContacts = mutableListOf<ContactModel>()
        
        val projection = arrayOf(
            Telephony.Sms.ADDRESS,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE
        )
        
        val sortOrder = "${Telephony.Sms.DATE} DESC"
        
        try {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
                val dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE)
                
                while (cursor.moveToNext()) {
                    val address = cursor.getString(addressIndex) ?: continue
                    
                    // Gerçek numara mı kontrol et
                    if (!NumberFilter.isRealPhoneNumber(address)) {
                        continue
                    }
                    
                    val cleanNumber = NumberFilter.cleanPhoneNumber(address)
                    
                    val contact = ContactModel(
                        phoneNumber = address,
                        name = null,
                        lastContact = cursor.getLong(dateIndex),
                        type = ContactType.SMS
                    )

                    if (distinct) {
                        if (!distinctContacts.containsKey(cleanNumber)) {
                            distinctContacts[cleanNumber] = contact
                        }
                    } else {
                        allContacts.add(contact)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return if (distinct) {
            distinctContacts.values.sortedByDescending { it.lastContact }
        } else {
            allContacts
        }
    }
}
