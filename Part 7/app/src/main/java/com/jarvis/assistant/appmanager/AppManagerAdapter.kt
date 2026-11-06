package com.jarvis.assistant.appmanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.jarvis.assistant.databinding.ItemAppManagerBinding

/**
 * App list row: icon, naam, package, cache/data size, checkbox.
 * Locked (system-critical) apps: checkbox disabled + lock reason dikhta hai.
 */
class AppManagerAdapter(
    private val onToggle: (ManagedApp, Boolean) -> Unit
) : RecyclerView.Adapter<AppManagerAdapter.ViewHolder>() {

    private var apps: List<ManagedApp> = emptyList()
    private var selected: Set<String> = emptySet()

    inner class ViewHolder(val binding: ItemAppManagerBinding) : RecyclerView.ViewHolder(binding.root)

    fun submit(newApps: List<ManagedApp>, newSelected: Set<String>) {
        apps = newApps
        selected = newSelected
        notifyDataSetChanged()
    }

    fun updateSelection(newSelected: Set<String>) {
        selected = newSelected
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppManagerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = apps.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        val binding = holder.binding
        binding.tvAppName.text = app.label
        binding.tvAppPackage.text = app.packageName
        val sizeText = if (app.cacheBytes < 0) {
            if (app.isSystem) "System app • size: Usage Access on karo" else "Size: Usage Access on karo"
        } else {
            "Cache: ${AppListRepository.formatBytes(app.cacheBytes)} • Data: ${AppListRepository.formatBytes(app.dataBytes)}"
        }
        binding.tvAppSize.text = sizeText
        // Asli app icon (PackageManager se), na mile to generic icon.
        if (app.icon != null) {
            binding.ivAppIcon.setImageDrawable(app.icon)
        } else {
            binding.ivAppIcon.setImageResource(android.R.drawable.sym_def_app_icon)
        }
        if (app.isLocked) {
            binding.cbSelect.isEnabled = false
            binding.ivLock.visibility = View.VISIBLE
            binding.tvLockReason.visibility = View.VISIBLE
            binding.tvLockReason.text = "🔒 ${app.lockReason ?: "Locked — system app"}"
            binding.root.alpha = 0.6f
        } else {
            binding.cbSelect.isEnabled = true
            binding.ivLock.visibility = View.GONE
            binding.tvLockReason.visibility = View.GONE
            binding.root.alpha = 1f
        }
        // Pehle purana listener hatao (recycled row ka), phir state set karo.
        binding.cbSelect.setOnCheckedChangeListener(null)
        binding.cbSelect.isChecked = if (app.isLocked) false else selected.contains(app.packageName)
        binding.cbSelect.setOnCheckedChangeListener { _, checked ->
            onToggle(app, checked)
        }
        binding.root.setOnClickListener {
            if (!app.isLocked) {
                binding.cbSelect.isChecked = !binding.cbSelect.isChecked
            }
        }
    }
}
