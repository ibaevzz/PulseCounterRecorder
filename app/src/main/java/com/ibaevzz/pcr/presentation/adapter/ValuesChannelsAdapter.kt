package com.ibaevzz.pcr.presentation.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ibaevzz.pcr.R
import com.ibaevzz.pcr.databinding.ChannelButtonViewBinding

class ValuesChannelsAdapter(values: Map<Int, Double>?,
                            private val find: List<Int>? = null,
                            private val toChannel: (Int) -> Unit)

    : RecyclerView.Adapter<ValuesChannelsAdapter.ValueViewHolder>() {

    class ValueViewHolder(val binding: ChannelButtonViewBinding) : ViewHolder(binding.root)

    private val values = values?.toMutableMap()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ValueViewHolder {
        val binding =
            ChannelButtonViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ValueViewHolder(binding)
    }

    override fun getItemCount(): Int = values?.size ?: 10

    @Suppress("DEPRECATION")
    @SuppressLint("ResourceType", "SetTextI18n")
    override fun onBindViewHolder(holder: ValueViewHolder, position: Int) {
        holder.binding.value.text = (values?.get(position) ?: "Ошибка").toString()
        holder.binding.channel.text = "Канал ${position + 1}"

        holder.binding.root.setOnClickListener {
            toChannel(position)
        }

        if(find != null){
            if(position in find){
                holder.binding.back.setBackgroundColor(Color.GREEN)
            }else{
                holder.binding.back.setBackgroundColor(holder.binding.root.resources.getColor(R.color.grey))
            }
        }
    }

}