package com.ibaevzz.pcr.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ibaevzz.pcr.databinding.DeviceBinding
import com.ibaevzz.pcr.domain.entity.Device

class DeviceListAdapter(private val callback: (address: String) -> Unit)
    : ListAdapter<Device, DeviceListAdapter.DeviceViewHolder>(DeviceItemCallback()){

    class DeviceItemCallback: DiffUtil.ItemCallback<Device>() {
        override fun areItemsTheSame(oldItem: Device, newItem: Device): Boolean = oldItem == newItem
        override fun areContentsTheSame(oldItem: Device, newItem: Device): Boolean = oldItem.name == newItem.name
    }

    class DeviceViewHolder(val binding: DeviceBinding): ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val binding = DeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DeviceViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.binding.name.text = currentList[position].name
        holder.binding.root.setOnClickListener{
            callback(currentList[position].name)
        }
    }
}