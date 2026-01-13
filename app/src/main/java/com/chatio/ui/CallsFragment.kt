package com.chatio.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chatio.R
import com.chatio.adapter.ContactAdapter
import com.chatio.databinding.FragmentCallsBinding
import com.chatio.model.ContactModel
import com.chatio.utils.CallLogReader
import com.chatio.utils.ContactsHelper
import com.chatio.utils.WhatsAppHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CallsFragment : Fragment() {

    private var _binding: FragmentCallsBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var adapter: ContactAdapter
    private var customMessage: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCallsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadCallHistory()
    }

    private fun setupRecyclerView() {
        adapter = ContactAdapter { contact ->
            onWhatsAppClick(contact)
        }
        binding.recyclerView.adapter = adapter
        setupSwipe()
    }

    private fun setupSwipe() {
        val swipeHandler = object : com.chatio.utils.SwipeCallback(requireContext()) {
            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val contact = adapter.currentList[position]
                
                if (direction == androidx.recyclerview.widget.ItemTouchHelper.RIGHT) {
                    // Sağa Kaydır -> Ara
                    val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                        data = android.net.Uri.parse("tel:${contact.phoneNumber}")
                    }
                    startActivity(intent)
                    adapter.notifyItemChanged(position)
                } else {
                    // Sola Kaydır -> Kopyala
                    val clipboard = requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Phone Number", contact.phoneNumber)
                    clipboard.setPrimaryClip(clip)
                    android.widget.Toast.makeText(context, getString(R.string.action_copy), android.widget.Toast.LENGTH_SHORT).show()
                    adapter.notifyItemChanged(position)
                }
            }
        }
        val itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.recyclerView)
    }

    private var isFiltered: Boolean = true

    // ... loadCallHistory içinde
    
    private fun loadCallHistory() {
        // View destroy olmuşsa işlem yapma
        if (_binding == null) return

        binding.progressBar.visibility = View.VISIBLE
        binding.emptyTextView.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val contacts = withContext(Dispatchers.IO) {
                    // Context kontrolü
                    if (context == null) return@withContext emptyList<ContactModel>()
                    
                    val callContacts = CallLogReader.readCallLog(requireContext())
                    
                    // Kişi isimlerini contacts'tan al ve FİLTRELE
                    callContacts.map { contact ->
                        if (context == null) return@map contact
                        if (contact.name == null) {
                            val name = ContactsHelper.getContactName(requireContext(), contact.phoneNumber)
                            contact.copy(name = name)
                        } else {
                            contact
                        }
                    }.filter { contact ->
                        if (isFiltered) {
                            // Filtre AÇIK (Rehberdekiler): Sadece ismi olanları göster
                            contact.name != null
                        } else {
                            // Filtre KAPALI: Sadece ismi OLMAYANLARI (kayıtsız numaraları) göster
                            contact.name == null
                        }
                    }
                }
                
                // İşlem bittiğinde view hala ayakta mı?
                if (_binding != null) {
                    binding.progressBar.visibility = View.GONE
                    
                    if (contacts.isEmpty()) {
                         // Eğer filtre aktifse ve liste boşsa, uygun mesajı göster
                        binding.emptyTextView.text = if (isFiltered) getString(R.string.no_calls_filtered) else getString(R.string.no_calls)
                        binding.emptyTextView.visibility = View.VISIBLE
                    } else {
                        adapter.submitList(contacts)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                if (_binding != null) {
                   binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    // ... onWhatsAppClick
    
    fun setFilter(filtered: Boolean) {
        if (isFiltered != filtered) {
            isFiltered = filtered
            refresh()
        }
    }


    private fun onWhatsAppClick(contact: ContactModel) {
        if (context != null) {
            WhatsAppHelper.openWhatsAppChat(
                requireContext(),
                contact.phoneNumber,
                customMessage
            )
        }
    }

    fun setCustomMessage(message: String) {
        customMessage = message
    }

    fun refresh() {
        if (_binding != null) {
            loadCallHistory()
        }
        // Eğer binding null ise (view henüz oluşmadıysa), onViewCreated zaten loadCallHistory çağıracak
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
