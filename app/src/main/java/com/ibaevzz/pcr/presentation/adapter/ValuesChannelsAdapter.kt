package com.ibaevzz.pcr.presentation.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ibaevzz.pcr.R
import com.ibaevzz.pcr.databinding.ChannelViewBinding

class ValuesChannelsAdapter(values: Map<Int, Double>?,
                            isChecked: Set<Int>,
                            var isEqu: Map<Int, Double>? = null)

    : RecyclerView.Adapter<ValuesChannelsAdapter.ValueViewHolder>() {

    class ValueViewHolder(val binding: ChannelViewBinding): ViewHolder(binding.root)

    private val values = values?.toMutableMap()
    private val isChecked = isChecked.toMutableSet()
    private var find = -1
    private var watch = -1
    private var oldWatch = -1
    private var strValues = mutableMapOf<Int, String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ValueViewHolder {
        val binding = ChannelViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ValueViewHolder(binding)
    }

    override fun getItemCount(): Int = values?.size?:10

    @SuppressLint("ResourceType")
    override fun onBindViewHolder(holder: ValueViewHolder, position: Int) {
        holder.binding.channel.text = (position + 1).toString()
        holder.binding.weight.setText((values?.get(position) ?:"Ошибка").toString())
        holder.binding.isChecked.isChecked = position in isChecked

        strValues[position] = values?.get(position).toString()

        holder.binding.isChecked.setOnCheckedChangeListener{_, checked ->
            if(checked && holder.binding.channel.text.toString().toInt() == position + 1){
                isChecked.add(position)
            }else if(holder.binding.channel.text.toString().toInt() == position + 1){
                isChecked.remove(position)
            }
        }
        holder.binding.weight.setTextColor(holder.binding.root.resources.getColor(R.color.black))
        if(isEqu != null){
            if(position + 1 == holder.binding.channel.text.toString().toInt()) {
                if (position in isEqu!!.keys && position in isChecked) {
                    val color =
                        if (isEqu!![position] == values?.get(position)) Color.GREEN else Color.RED
                    holder.binding.weight.setTextColor(color)
                }
            }
        }

        holder.binding.weight.doAfterTextChanged {
            strValues[position] = it.toString()
        }

        if(holder.binding.channel.text.toString().toInt() == position + 1) {
            if (position == find) {
                holder.binding.root.setBackgroundColor(Color.GREEN)
            }
            if (position == oldWatch) {
                oldWatch = -1
                holder.binding.root.setBackgroundColor(Color.TRANSPARENT)
            }
            if (position == watch) {
                holder.binding.root.setBackgroundColor(Color.GRAY)
                oldWatch = watch
                watch = -1
            }
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
        notifyItemChanged(oldWatch)
    }

    fun setValue(channel: Int, value: Double){
        values?.set(channel, value)
        notifyItemChanged(channel)
    }

    fun getValuesMap(): Map<Int, Double>{
        val rMap = mutableMapOf<Int, Double>()
        for(i in isChecked){
            try {
                val str = strValues[i]?:""
                val d = str.toDouble()
                rMap[i] = d
            }catch (_: Exception){}
        }
        return rMap
    }

    fun getAllValues(): Map<Int, Double>{
        val rMap = mutableMapOf<Int, Double>()
        for(i in strValues.keys){
            try {
                val str = strValues[i]?:"-1.0"
                val d = str.toDoubleOrNull()?:-1.0
                rMap[i] = d
            }catch (_: Exception){}
        }
        return rMap
    }

    fun getSize() = values?.size?:0
}