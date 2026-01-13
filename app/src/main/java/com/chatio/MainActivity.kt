package com.chatio

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import com.chatio.databinding.ActivityMainBinding
import com.chatio.ui.CallsFragment
import com.chatio.ui.SmsFragment
import com.chatio.ui.StatsFragment
import com.chatio.ui.ViewPagerAdapter
import com.google.android.material.tabs.TabLayoutMediator
import com.chatio.model.ContactType

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter
    private lateinit var sharedPreferences: SharedPreferences
    
    private val permissions = arrayOf(
        Manifest.permission.READ_CALL_LOG,
        Manifest.permission.READ_SMS,
        Manifest.permission.READ_CONTACTS
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            loadData()
        } else {
            showPermissionDeniedDialog()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        
        // setupToolbar() kaldırıldı çünkü yeni tasarımda custom header var
        setupViewPager()
        setupMessageInput()
        setupFilterSwitch()
        setupFab()
        setupSettingsButton()
        setupInfoButton()
        
        checkPermissions()
    }

    // setupToolbar fonksiyonu kaldırıldı


    private fun setupViewPager() {
        viewPagerAdapter = ViewPagerAdapter(this)
        binding.viewPager.adapter = viewPagerAdapter
        
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> getString(R.string.tab_calls)
                1 -> getString(R.string.tab_messages)
                2 -> getString(R.string.tab_stats)
                else -> ""
            }
        }.attach()
    }

    private fun setupMessageInput() {
        // Kaydedilmiş mesajı yükle
        // Kaydedilmiş mesajı yükle. Eğer boşsa varsayılan "Merhaba" mesajını getir.
        var savedMessage = sharedPreferences.getString(KEY_MESSAGE, "")
        if (savedMessage.isNullOrEmpty()) {
            savedMessage = getString(R.string.default_message)
        }
        binding.messageInput.setText(savedMessage)
        
        // Mesaj değiştiğinde fragments'a bildir ve kaydet
        binding.messageInput.doAfterTextChanged { text ->
            val message = text?.toString() ?: getString(R.string.default_message)
            
            // Fragment'lara mesajı ilet
            updateFragmentsMessage(message)
            
            // SharedPreferences'a kaydet
            sharedPreferences.edit().putString(KEY_MESSAGE, message).apply()
        }
        
        // İlk yüklemede fragment'lara mesajı gönder
        updateFragmentsMessage(savedMessage)
    }

    private fun updateFragmentsMessage(message: String) {
        // CallsFragment'a mesajı ilet
        (viewPagerAdapter.getFragment(0) as? CallsFragment)?.setCustomMessage(message)
        
        // SmsFragment'a mesajı ilet
        (viewPagerAdapter.getFragment(1) as? SmsFragment)?.setCustomMessage(message)
    }

    private fun checkPermissions() {
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (missingPermissions.isEmpty()) {
            loadData()
        } else {
            requestPermissions()
        }
    }

    private fun requestPermissions() {
        // İzin açıklaması göster
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_required))
            .setMessage(getString(R.string.permission_dialog_message))
            .setPositiveButton(getString(R.string.grant_permission)) { _, _ ->
                permissionLauncher.launch(permissions)
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
                showPermissionDeniedDialog()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.permission_denied))
            .setMessage(getString(R.string.permission_denied_message))
            .setPositiveButton(getString(R.string.retry)) { _, _ ->
                requestPermissions()
            }
            .setNegativeButton(getString(R.string.exit)) { _, _ ->
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun loadData() {
        // Fragment'ları yenile
        (viewPagerAdapter.getFragment(0) as? CallsFragment)?.refresh()
        (viewPagerAdapter.getFragment(1) as? SmsFragment)?.refresh()
        (viewPagerAdapter.getFragment(2) as? StatsFragment)?.refresh()
    }

    companion object {
        private const val PREFS_NAME = "chatio_prefs"
        private const val KEY_MESSAGE = "custom_message"
        private const val KEY_FILTER = "filter_contacts"
        private const val KEY_WA_BUSINESS = "use_wa_business"
        private const val KEY_LANG = "use_lang"
        private const val KEY_HISTORY = "manual_input_history"
    }
    
    // ... setupMessageInput() sonrası çağrılacak
    private fun setupFilterSwitch() {
        // Kaydedilmiş filtre durumunu yükle (Default: true)
        val isFiltered = sharedPreferences.getBoolean(KEY_FILTER, true)
        binding.switchFilterContacts.isChecked = isFiltered
        
        // İlk açılışta fragment'lara bildir
        updateFragmentsFilter(isFiltered)
        
        // Switch değiştiğinde
        binding.switchFilterContacts.setOnCheckedChangeListener { _, isChecked ->
            // Kaydet
            sharedPreferences.edit().putBoolean(KEY_FILTER, isChecked).apply()
            
            // Fragment'ları güncelle
            updateFragmentsFilter(isChecked)
        }
    }
    
    // ... setupFilterSwitch sonrası
    private fun setupFab() {
        binding.fabManualInput.setOnClickListener {
            showNumberInputDialog()
        }
    }

    private fun showNumberInputDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_number_input, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // Arka planı transparent yap (Card radius görünsün diye)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val input = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.numberInput)
        val btnOpen = dialogView.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnOpenChat)
        
        // History UI
        val tvHistoryTitle = dialogView.findViewById<android.widget.TextView>(R.id.tvHistoryTitle)
        val scrollHistory = dialogView.findViewById<android.widget.HorizontalScrollView>(R.id.scrollHistory)
        val chipGroupHistory = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupHistory)

        // Geçmişi Yükle
        val historyList = loadHistory()
        if (historyList.isNotEmpty()) {
            tvHistoryTitle.visibility = View.VISIBLE
            scrollHistory.visibility = View.VISIBLE
            
            for (number in historyList) {
                val chip = com.google.android.material.chip.Chip(this)
                chip.text = number
                chip.isClickable = true
                chip.isCheckable = false
                chip.setChipBackgroundColorResource(R.color.md_theme_light_surface)
                chip.setTextColor(android.graphics.Color.BLACK) // Garanti siyah
                
                chip.setOnClickListener {
                    input.setText(number)
                    input.setSelection(number.length)
                }
                chipGroupHistory.addView(chip)
            }
        }

        btnOpen.setOnClickListener {
            val number = input.text.toString().trim()
            if (number.isNotEmpty()) {
                val currentMessage = binding.messageInput.text.toString() // Güncel mesajı al
                
                // History'e kaydet
                saveToHistory(number)
                
                // ContactModel oluştur
                val contact = com.chatio.model.ContactModel(
                    phoneNumber = number,
                    name = null,
                    lastContact = System.currentTimeMillis(),
                    type = ContactType.CALL
                )
                
                // WhatsAppHelper ile aç
                 com.chatio.utils.WhatsAppHelper.openWhatsAppChat(
                    this,
                    contact.phoneNumber,
                    currentMessage
                )
                dialog.dismiss()
            } else {
                Toast.makeText(this, getString(R.string.dialog_input_hint), Toast.LENGTH_SHORT).show()
            }
        }
        
        dialog.show()
    }

    private fun saveToHistory(number: String) {
        val history = loadHistory().toMutableList()
        // Varsa çıkar (en başa eklemek için)
        history.remove(number)
        // En başa ekle
        history.add(0, number)
        // İlk 10 taneyi tut
        if (history.size > 10) {
            history.removeAt(history.lastIndex)
        }
        
        // Kaydet (Basitçe virgülle birleştirerek)
        val historyString = history.joinToString(",")
        sharedPreferences.edit().putString(KEY_HISTORY, historyString).apply()
    }
    
    private fun loadHistory(): List<String> {
        val historyString = sharedPreferences.getString(KEY_HISTORY, "")
        if (historyString.isNullOrEmpty()) return emptyList()
        
        return historyString.split(",").filter { it.isNotEmpty() }
    }
    
    // ... setupFab sonrası


    // ... setupFab sonrası
    private fun setupSettingsButton() {
        binding.btnSettings.setOnClickListener {
            showSettingsDialog()
        }
    }



    private fun setupInfoButton() {
        binding.btnInfo.setOnClickListener {
            showAppInfoDialog()
        }
        
        // Settings Button Setup (Header içine eklenecek, şimdilik Info yanında)
        // Not: Layout'a settings butonu eklemeliyiz, şimdilik sadece fonksiyonu yazıyorum
        // XML güncellemesi bir sonraki adımda yapılacak.
    }
    
    // Header'da settings butonu olmadığı için Info dialogunun içine ekliyorum:
    // Bu yüzden showAppInfoDialog fonksiyonunu güncelleyip oradan Settings açtırabilirim.
    // VEYA XML'e Settings butonu ekleyip yeni bir setup ekleyebilirim.
    // Planımızda Header'a buton eklemek vardı.

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
            
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        val switchWaBusiness = dialogView.findViewById<com.google.android.material.materialswitch.MaterialSwitch>(R.id.switchWaBusiness)
        val chipGroupLanguage = dialogView.findViewById<com.google.android.material.chip.ChipGroup>(R.id.chipGroupLanguage)
        val btnClose = dialogView.findViewById<View>(R.id.btnCloseSettings)
        
        // Mevcut ayarı yükle (Business)
        val isBusiness = sharedPreferences.getBoolean(KEY_WA_BUSINESS, false)
        switchWaBusiness.isChecked = isBusiness
        
        // Mevcut dili yükle ve chip seç
        val currentLang = sharedPreferences.getString(KEY_LANG, "auto")
        when (currentLang) {
            "en" -> chipGroupLanguage.check(R.id.chipEn)
            "tr" -> chipGroupLanguage.check(R.id.chipTr)
            "ar" -> chipGroupLanguage.check(R.id.chipAr)
            "ru" -> chipGroupLanguage.check(R.id.chipRu)
        }
        
        // İşletme ayarını dinle
        switchWaBusiness.setOnCheckedChangeListener { _, isChecked ->
            sharedPreferences.edit().putBoolean(KEY_WA_BUSINESS, isChecked).apply()
        }
        
        // Dil seçimini dinle
        chipGroupLanguage.setOnCheckedStateChangeListener { _, checkedIds ->
            val selectedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val newLang = when (selectedId) {
                R.id.chipEn -> "en"
                R.id.chipTr -> "tr"
                R.id.chipAr -> "ar"
                R.id.chipRu -> "ru"
                else -> "en"
            }
            
            if (newLang != sharedPreferences.getString(KEY_LANG, "auto")) {
                // Eğer mevcut mesaj varsayılan ise, yeni dile geçince o dilin varsayılanı gelsin diye siliyoruz
                val currentMessage = binding.messageInput.text.toString()
                if (currentMessage == getString(R.string.default_message)) {
                    sharedPreferences.edit().remove(KEY_MESSAGE).commit()
                }
                setLocale(newLang)
            }
        }
        
        btnClose.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    private fun setLocale(lang: String) {
        // commit() kullanarak diske yazıldığından emin oluyoruz
        sharedPreferences.edit().putString(KEY_LANG, lang).commit()
        
        // Activity'i yeniden başlat (Ayarın hemen uygulanması için)
        val intent = intent
        finish()
        startActivity(intent)
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(android.app.Activity.OVERRIDE_TRANSITION_OPEN, 0, 0)
            overrideActivityTransition(android.app.Activity.OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString(KEY_LANG, "auto") ?: "auto"
        
        if (lang != "auto") {
            val locale = java.util.Locale(lang)
            java.util.Locale.setDefault(locale)
            
            val config = android.content.res.Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                config.setLayoutDirection(locale)
            }
            
            val context = newBase.createConfigurationContext(config)
            super.attachBaseContext(context)
        } else {
            super.attachBaseContext(newBase)
        }
    }

    private fun showAppInfoDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_app_info, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
            
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        
        // Buttons
        dialogView.findViewById<View>(R.id.btnCloseInfo).setOnClickListener {
            dialog.dismiss()
        }
        
        dialogView.findViewById<View>(R.id.btnTelegram).setOnClickListener {
            openUrl("https://t.me/iomerg")
        }
        
        dialogView.findViewById<View>(R.id.btnYoutube).setOnClickListener {
            openUrl("https://youtube.com/@omer.guvercin")
        }
        
        dialogView.findViewById<View>(R.id.btnInstagram).setOnClickListener {
            openUrl("https://instagram.com/omerguvercins")
        }
        
        dialog.show()
    }
    
    private fun openUrl(url: String) {
        try {
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Link açılamadı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateFragmentsFilter(isFiltered: Boolean) {
        (viewPagerAdapter.getFragment(0) as? CallsFragment)?.setFilter(isFiltered)
        (viewPagerAdapter.getFragment(1) as? SmsFragment)?.setFilter(isFiltered)
    }
}
