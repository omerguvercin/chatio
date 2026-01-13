package com.chatio.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import com.chatio.R

object WhatsAppHelper {
    
    private const val WHATSAPP_PACKAGE = "com.whatsapp"
    private const val WHATSAPP_BUSINESS_PACKAGE = "com.whatsapp.w4b"
    private const val PREFS_NAME = "chatio_prefs"
    private const val KEY_WA_BUSINESS = "use_wa_business"
    
    /**
     * WhatsApp veya Business yüklü mu kontrol eder
     */
    fun isWhatsAppInstalled(context: Context, isBusiness: Boolean): Boolean {
        val packageName = if (isBusiness) WHATSAPP_BUSINESS_PACKAGE else WHATSAPP_PACKAGE
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Seçilen WhatsApp sürümünde sohbet açar
     */
    fun openWhatsAppChat(context: Context, phoneNumber: String, message: String = "") {
        // Tercihi kontrol et
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val useBusiness = prefs.getBoolean(KEY_WA_BUSINESS, false)
        
        // Önce tercih edilen sürüm yüklü mü bak
        if (!isWhatsAppInstalled(context, useBusiness)) {
            val appName = if (useBusiness) "WhatsApp Business" else "WhatsApp"
            Toast.makeText(
                context,
                "$appName yüklü değil!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        val targetPackage = if (useBusiness) WHATSAPP_BUSINESS_PACKAGE else WHATSAPP_PACKAGE
        
        try {
            // Numarayı temizle
            val cleanNumber = NumberFilter.cleanPhoneNumber(phoneNumber)
            
            // + işareti yoksa ekle (uluslararası format için)
            val internationalNumber = if (!cleanNumber.startsWith("+")) {
                if (cleanNumber.startsWith("0")) {
                    "+90${cleanNumber.substring(1)}"
                } else if (cleanNumber.startsWith("90")) {
                    "+$cleanNumber"
                } else {
                    "+90$cleanNumber"
                }
            } else {
                cleanNumber
            }
            
            // WhatsApp intent oluştur
            val url = if (message.isNotEmpty()) {
                "https://api.whatsapp.com/send?phone=$internationalNumber&text=${Uri.encode(message)}"
            } else {
                "https://api.whatsapp.com/send?phone=$internationalNumber"
            }
            
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage(targetPackage)
            }
            
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "WhatsApp açılamadı: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
