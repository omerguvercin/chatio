package com.chatio.model

data class ContactModel(
    val phoneNumber: String,
    val name: String?,
    val lastContact: Long,
    val type: ContactType,
    val subType: Int = 0, // CallLog.Calls.INCOMING_TYPE vb.
    val duration: Long = 0L, // Saniye cinsinden
    val statsDescription: String? = null
)

enum class ContactType {
    CALL,
    SMS
}
