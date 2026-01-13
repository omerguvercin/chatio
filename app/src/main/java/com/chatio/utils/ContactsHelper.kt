package com.chatio.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat

object ContactsHelper {
    
    /**
     * Telefon numarasına göre kişi adını bulur
     */
    fun getContactName(context: Context, phoneNumber: String): String? {
        // İzin kontrolü
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }
        
        val cleanNumber = NumberFilter.cleanPhoneNumber(phoneNumber)
        
        try {
            val uri = ContactsContract.PhoneLookup.CONTENT_FILTER_URI
            val numberUri = uri.buildUpon().appendPath(cleanNumber).build()
            
            context.contentResolver.query(
                numberUri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
                    return cursor.getString(nameIndex)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return null
    }
}
