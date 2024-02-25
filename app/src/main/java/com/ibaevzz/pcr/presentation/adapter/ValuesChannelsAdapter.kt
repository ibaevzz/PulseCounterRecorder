package com.ibaevzz.pcr.presentation.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ibaevzz.pcr.databinding.ChannelViewBinding

class ValuesChannelsAdapter(values: Map<Int, Double>?,
                            isChecked: Set<Int>)

    : RecyclerView.Adapter<ValuesChannelsAdapter.ValueViewHolder>() {

    class ValueViewHolder(val binding: ChannelViewBinding): ViewHolder(binding.root)

    private val values = values?.toMutableMap()
    private val isChecked = isChecked.toMutableSet()
    private var find = -1
    private var watch = -1
    private var oldWatch = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ValueViewHolder {
        val binding = ChannelViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ValueViewHolder(binding)
    }

    override fun getItemCount(): Int = values?.size?:10

    override fun onBindViewHolder(holder: ValueViewHolder, position: Int) {
        holder.binding.channel.text = (position + 1).toString()
        holder.binding.weight.setText((values?.get(position) ?:"Ошибка").toString())
        holder.binding.isChecked.isChecked = position in isChecked

        if(position == find){
            holder.binding.root.setBackgroundColor(Color.GREEN)
        }
        if(position == oldWatch){
            oldWatch = -1
            holder.binding.root.setBackgroundColor(Color.TRANSPARENT)
        }
        if(position == watch){
            holder.binding.root.setBackgroundColor(Color.GRAY)
            oldWatch = watch
            watch = -1
        }
    }

    fun setFind(channel: Int){
        isChecked.add(channel)
        find = channel
        oldWatch = -1
        watch = -1
        notifyItemChanged(channel)
    }

    fun getChecked() = isChecked

    fun setWatch(channel: Int){
        watch = channel
        notifyItemChanged(oldWatch)
        notifyItemChanged(channel)
    }

    fun unwatch(){
        watch = -1
        oldWatch = (values?.size?:0) - 1
        notifyItemChanged(oldWatch)
    }

    fun setValue(channel: Int, value: Double){
        values?.set(channel, value)
        notifyItemChanged(channel)
    }

    fun getSize() = values?.size?:0
}