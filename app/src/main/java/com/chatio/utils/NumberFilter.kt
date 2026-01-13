package com.chatio.utils

object NumberFilter {
    /**
     * Banka, reklam ve bilgilendirme numaralarını filtreler
     * Sadece gerçek telefon numaralarını döndürür
     */
    fun isRealPhoneNumber(number: String): Boolean {
        val cleanNumber = number.replace(Regex("[^0-9]"), "")
        
        // Boş veya çok kısa numaralar
        if (cleanNumber.length < 7) {
            return false
        }
        
        // Kısa kodlar (4-6 haneli)
        if (cleanNumber.length <= 6) {
            return false
        }
        
        // Banka numaraları (444 ile başlayan)
        if (cleanNumber.startsWith("444") && cleanNumber.length <= 10) {
            return false
        }
        
        // Bilgilendirme numaraları
        val infoPatterns = listOf(
            "^0?850",  // 0850
            "^0?888",  // 0888
            "^0?800",  // 0800 (ücretsiz arama)
            "^0?900",  // 0900 (ücretli servisler)
            "^0?500",  // 0500 (özel numaralar)
        )
        
        for (pattern in infoPatterns) {
            if (cleanNumber.matches(Regex(pattern))) {
                return false
            }
        }
        
        // Normal telefon numarası formatı kontrolü
        // Türkiye için: 10-13 haneli (0 veya +90 ile başlayabilir)
        if (cleanNumber.length in 10..13) {
            // 90 ile başlıyorsa Türkiye numarası
            if (cleanNumber.startsWith("90")) {
                return cleanNumber.length in 12..13
            }
            // 0 ile başlıyorsa
            if (cleanNumber.startsWith("0")) {
                return cleanNumber.length in 10..11
            }
            // Diğer durumlarda
            return true
        }
        
        return false
    }
    
    /**
     * Numarayı temizler ve standart formata getirir
     */
    /**
     * Numarayı temizler ve standart formata getirir (Mükerrer kayıtları önlemek için)
     * +90555... -> 555...
     * 0555... -> 555...
     */
    fun cleanPhoneNumber(number: String): String {
        // Sadece rakamları bırak
        var cleaned = number.replace(Regex("[^0-9]"), "")
        
        // Ülke kodu (90) varsa kaldır
        if (cleaned.startsWith("90") && cleaned.length > 10) {
            cleaned = cleaned.substring(2)
        }
        
        // Başta 0 varsa kaldır
        if (cleaned.startsWith("0") && cleaned.length > 10) {
            cleaned = cleaned.substring(1)
        }
        
        return cleaned
    }
}
