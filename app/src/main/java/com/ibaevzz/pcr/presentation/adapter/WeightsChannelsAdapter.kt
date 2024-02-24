package com.ibaevzz.pcr.presentation.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ibaevzz.pcr.databinding.WeightChannelViewBinding

class WeightsChannelsAdapter(var weights: List<Double?>,
                             val checkedChannels: MutableSet<Int> = mutableSetOf(),
                             private val selectAll: (Boolean) -> Unit): RecyclerView.Adapter<WeightsChannelsAdapter.ChannelViewHolder>() {

    private var allCheckBox = mutableSetOf<CheckBox>()
    private var weightsMap = mutableMapOf<Int, String>()
    private var isOneUncheck = false

    class ChannelViewHolder(val binding: WeightChannelViewBinding): ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = WeightChannelViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChannelViewHolder(binding)
    }

    override fun getItemCount() = weights.size

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        allCheckBox.add(holder.binding.isChecked)
        if(position in checkedChannels){
            holder.binding.isChecked.isChecked = true
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
            holder.binding.weight.setText((weights[position]?:"Ошибка").toString())
        }
    }

    fun getWeights(): Map<Int, Double>{
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

    fun checkAll(isChecked: Boolean){
        if(isOneUncheck){
            isOneUncheck = false
            return
        }
        if(!isChecked){
            checkedChannels.removeIf{ it > -1 }
        }else{
            for(i in weights.indices){
                checkedChannels.add(i)
            }
        }
        for(i in allCheckBox){
            i.isChecked = isChecked
        }
    }

}