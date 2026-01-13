package com.chatio.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.chatio.databinding.ItemContactBinding
import com.chatio.model.ContactModel
import java.text.SimpleDateFormat
import java.util.*

class ContactAdapter(
    private val onWhatsAppClick: (ContactModel) -> Unit
) : ListAdapter<ContactModel, ContactAdapter.ContactViewHolder>(ContactDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ContactViewHolder(binding, onWhatsAppClick)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ContactViewHolder(
        private val binding: ItemContactBinding,
        private val onWhatsAppClick: (ContactModel) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(contact: ContactModel) {
            // Kişi adı veya numara
            val displayName = contact.name ?: contact.phoneNumber
            binding.nameText.text = displayName
            
            // Telefon numarası
            binding.numberText.text = contact.phoneNumber
            
            // Stats Description varsa tarihi ez
            if (!contact.statsDescription.isNullOrEmpty()) {
                binding.dateText.text = contact.statsDescription
            } else {
                binding.dateText.text = formatDate(contact.lastContact)
            }
            
            // WhatsApp butonu
            binding.whatsappButton.setOnClickListener {
                onWhatsAppClick(contact)
            }
            
            // Rehbere Ekle Butonu
            if (contact.name == null) {
                binding.btnAddContact.visibility = android.view.View.VISIBLE
                binding.btnAddContact.setOnClickListener {
                     val intent = android.content.Intent(android.content.Intent.ACTION_INSERT).apply {
                        type = android.provider.ContactsContract.Contacts.CONTENT_TYPE
                        putExtra(android.provider.ContactsContract.Intents.Insert.PHONE, contact.phoneNumber)
                    }
                    if (intent.resolveActivity(binding.root.context.packageManager) != null) {
                        binding.root.context.startActivity(intent)
                    }
                }
            } else {
                binding.btnAddContact.visibility = android.view.View.GONE
            }
        }

        private fun formatDate(timestamp: Long): String {
            val calendar = Calendar.getInstance()
            val todayStart = calendar.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            
            return when {
                timestamp >= todayStart -> {
                    // Bugün
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    "${binding.root.context.getString(com.chatio.R.string.today)}, ${timeFormat.format(Date(timestamp))}"
                }
                timestamp >= todayStart - 86400000 -> {
                    // Dün
                    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    "${binding.root.context.getString(com.chatio.R.string.yesterday)}, ${timeFormat.format(Date(timestamp))}"
                }
                else -> {
                    // Diğer
                    val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                    dateFormat.format(Date(timestamp))
                }
            }
        }
    }

    class ContactDiffCallback : DiffUtil.ItemCallback<ContactModel>() {
        override fun areItemsTheSame(oldItem: ContactModel, newItem: ContactModel): Boolean {
            return oldItem.phoneNumber == newItem.phoneNumber
        }

        override fun areContentsTheSame(oldItem: ContactModel, newItem: ContactModel): Boolean {
            return oldItem == newItem
        }
    }
}
