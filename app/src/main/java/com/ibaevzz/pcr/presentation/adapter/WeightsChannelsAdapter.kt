package com.ibaevzz.pcr.presentation.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ibaevzz.pcr.R
import com.ibaevzz.pcr.databinding.ChannelViewBinding

class WeightsChannelsAdapter(var weights: Map<Int, Double?>,
                             val checkedChannels: MutableSet<Int> = mutableSetOf(),
                             private val isEqu: Map<Int, Double?> = emptyMap(),
                             private val selectAll: (Boolean) -> Unit): RecyclerView.Adapter<WeightsChannelsAdapter.ChannelViewHolder>() {

    private var allCheckBox = mutableSetOf<CheckBox>()
    private var weightsMap = mutableMapOf<Int, String>()
    private var isOneUncheck = false

    class ChannelViewHolder(val binding: ChannelViewBinding): ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ChannelViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChannelViewHolder(binding)
    }

    override fun getItemCount() = weights.size

    @Suppress("DEPRECATION")
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        allCheckBox.add(holder.binding.isChecked)
        holder.binding.isChecked.isChecked = position in checkedChannels
        if(position in isEqu.keys && position in checkedChannels){
            val color = if(isEqu[position] == weights[position]) Color.GREEN else Color.RED
            holder.binding.weight.setTextColor(color)
        }else{
            holder.binding.weight.setTextColor(holder.binding.root.context.resources.getColor(R.color.black))
        }
        holder.binding.isChecked.setOnCheckedChangeListener{_, isChecked ->
            if(isChecked){
                checkedChannels.add(position)
                if(checkedChannels.size == weights.size){
                    selectAll(true)
                }
            }else{
                checkedChannels.remove(position)
                if(checkedChannels.size + 1 == weights.size) isOneUncheck = true
                if(checkedChannels.size < weights.size){
                    selectAll(false)
                }
            }
        }
        holder.binding.weight.doAfterTextChanged {
            weightsMap[position] = (it?:"").toString()
        }
        holder.binding.channel.text = (position + 1).toString()
        if(weightsMap.size < weights.size){
            if(weights[position] != 0.0001){
                holder.binding.weight.setText((weights[position]?:"Ошибка").toString())
            }else{
                holder.binding.weight.setText("0.0001")
            }
        }
    }

    fun getWeightsMap(): Map<Int, Double>{
        val w = mutableMapOf<Int, Double>()
        for(i in checkedChannels){
            try {
                val weight = weightsMap[i]?.toDouble()
                if(weight!=null){
                    w[i+1] = weight
                }
            }catch (_: Exception){}
        }
        return w
    }

    fun getAllWeights(): Map<Int, String>{
        return weightsMap
    }

    fun checkAll(isChecked: Boolean){
        if(isOneUncheck){
            isOneUncheck = false
            return
        }
        if(!isChecked){
            checkedChannels.removeIf{ it > -1 }
        }else{
            for(i in weights.keys.indices){
                checkedChannels.add(i)
            }
        }
        for(i in allCheckBox){
            i.isChecked = isChecked
        }
    }

}