package com.chatio.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CallLog
import androidx.core.content.ContextCompat
import com.chatio.model.ContactModel
import com.chatio.model.ContactType

object CallLogReader {
    
    /**
     * Çağrı geçmişini okur
     * @param distinct: True ise numaraları tekilleştirir (En son görüşme), False ise tüm geçmişi getirir
     */
    fun readCallLog(context: Context, distinct: Boolean = true): List<ContactModel> {
        // İzin kontrolü
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return emptyList()
        }
        
        // distinct true ise unique map, değilse list
        val distinctContacts = mutableMapOf<String, ContactModel>()
        val allContacts = mutableListOf<ContactModel>()
        
        val projection = arrayOf(
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.DATE,
            CallLog.Calls.TYPE,
            CallLog.Calls.DURATION
        )
        
        val sortOrder = "${CallLog.Calls.DATE} DESC"
        
        try {
            context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
            )?.use { cursor ->
                val numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER)
                val nameIndex = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
                val dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE)
                val durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION)
                
                while (cursor.moveToNext()) {
                    val number = cursor.getString(numberIndex) ?: continue
                    
                    // Gerçek numara mı kontrol et
                    if (!NumberFilter.isRealPhoneNumber(number)) {
                        continue
                    }
                    
                    val cleanNumber = NumberFilter.cleanPhoneNumber(number)
                    
                    val contactTypeInt = cursor.getInt(cursor.getColumnIndex(CallLog.Calls.TYPE))
                    
                    val contact = ContactModel(
                        phoneNumber = number,
                        name = cursor.getString(nameIndex),
                        lastContact = cursor.getLong(dateIndex),
                        type = ContactType.CALL,
                        subType = contactTypeInt,
                        duration = cursor.getLong(durationIndex)
                    )

                    if (distinct) {
                        // Aynı numaradan birden fazla kayıt varsa sadece en yenisini al (zaten tarih sıralı geliyor, ilki en yenisi)
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
