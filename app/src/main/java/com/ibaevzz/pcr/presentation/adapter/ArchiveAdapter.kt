package com.ibaevzz.pcr.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ibaevzz.pcr.databinding.ArchiveViewBinding
import java.text.SimpleDateFormat
import java.util.*

class ArchiveAdapter(val map: List<Pair<Date, Double?>>): RecyclerView.Adapter<ArchiveAdapter.ArchiveViewHolder>() {
    class ArchiveViewHolder(val binding: ArchiveViewBinding): ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ArchiveViewHolder {
        val binding = ArchiveViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ArchiveViewHolder(binding)
    }

    override fun getItemCount() = map.size

    override fun onBindViewHolder(holder: ArchiveViewHolder, position: Int) {
        holder.binding.num.text = (position + 1).toString()
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        holder.binding.date.text = format.format(map[position].first)
        holder.binding.value.text = map[position].second.toString()
    }


}