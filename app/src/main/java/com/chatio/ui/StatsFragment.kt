package com.chatio.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.chatio.adapter.ContactAdapter
import com.chatio.databinding.FragmentStatsBinding
import com.chatio.model.ContactModel
import com.chatio.utils.CallLogReader
import com.chatio.utils.ContactsHelper
import com.chatio.utils.SmsReader
import com.chatio.utils.WhatsAppHelper
import com.chatio.utils.NumberFilter
import com.chatio.model.ContactType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.chatio.R

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ContactAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupRecyclerView()
        loadStats()
    }

    private fun setupRecyclerView() {
        adapter = ContactAdapter { contact ->
            WhatsAppHelper.openWhatsAppChat(requireContext(), contact.phoneNumber)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }

    private fun loadStats() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyTextView.visibility = View.GONE
        
        lifecycleScope.launch {
            try {
                val topContacts = withContext(Dispatchers.IO) {
                    val ctx = context ?: return@withContext emptyList<ContactModel>()
                    
                    val calls = CallLogReader.readCallLog(ctx, distinct = false)
                    val messages = SmsReader.readSmsHistory(ctx, distinct = false)
                    val allInteractions = calls + messages
                    val grouped = allInteractions.groupBy { NumberFilter.cleanPhoneNumber(it.phoneNumber) }
                    
                    val statsList = grouped.map { (cleanNumber, interactionList) ->
                        val bestName = interactionList.find { it.name != null && it.name.isNotBlank() }?.name 
                            ?: ContactsHelper.getContactName(ctx, cleanNumber)
                        
                        val lastInteractionTime = interactionList.maxOf { it.lastContact }
                        
                        // Detaylı Sayım
                        val callsOnly = interactionList.filter { it.type == ContactType.CALL }
                        val incomingCount = callsOnly.count { it.subType == android.provider.CallLog.Calls.INCOMING_TYPE }
                        val outgoingCount = callsOnly.count { it.subType == android.provider.CallLog.Calls.OUTGOING_TYPE }
                        val missedCount = callsOnly.count { it.subType == android.provider.CallLog.Calls.MISSED_TYPE }
                        
                        val smsCount = interactionList.count { it.type == ContactType.SMS }
                        val totalDuration = interactionList.sumOf { it.duration }
                        
                        val totalCallCount = incomingCount + outgoingCount + missedCount
                        
                        // Totals with localization
                        val labelIncoming = getString(R.string.stats_incoming)
                        val labelOutgoing = getString(R.string.stats_outgoing)
                        val labelMissed = getString(R.string.stats_missed)
                        val labelTotal = getString(R.string.stats_total)
                        val labelMsg = getString(R.string.stats_messages)
                        
                        // Örn: "Gelen: xxx Giden: xxx Cevapsız: xxx Toplam: xxx Mesaj: xxx"
                        val desc = "$labelIncoming: $incomingCount  $labelOutgoing: $outgoingCount  $labelMissed: $missedCount  $labelTotal: $totalCallCount  $labelMsg: $smsCount"

                        val totalScore = totalCallCount + smsCount

                        ContactModel(
                            phoneNumber = interactionList.first().phoneNumber,
                            name = bestName,
                            lastContact = lastInteractionTime,
                            type = ContactType.CALL,
                            duration = totalDuration,
                            statsDescription = desc
                        ) to totalScore
                    }
                    
                    statsList.sortedByDescending { it.second }
                        .map { it.first }
                        .take(10) // İlk 5 yerine 10 olsun
                }
                
                if (_binding != null) {
                    binding.progressBar.visibility = View.GONE
                    if (topContacts.isEmpty()) {
                        binding.emptyTextView.visibility = View.VISIBLE
                    } else {
                        adapter.submitList(topContacts)
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
    
    private fun formatDuration(seconds: Long): String {
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        return if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
    }
    
    fun refresh() {
        // ... (no changes needed)
        if (_binding != null) {
            loadStats()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
