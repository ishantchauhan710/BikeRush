package com.ishant.bikerush.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.ishant.bikerush.databinding.JourneyBinding
import com.ishant.bikerush.db.Journey
import com.ishant.bikerush.other.TrackingUtility
import java.text.SimpleDateFormat
import java.util.*

class JourneyAdapter: RecyclerView.Adapter<JourneyAdapter.JourneyViewHolder>() {
    inner class JourneyViewHolder(val binding: JourneyBinding): RecyclerView.ViewHolder(binding.root)

    private val differ = AsyncListDiffer(this,object: DiffUtil.ItemCallback<Journey>() {

        override fun areItemsTheSame(oldItem: Journey, newItem: Journey): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Journey, newItem: Journey): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    })

    fun submitList(journeyList: List<Journey>) = differ.submitList(journeyList)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JourneyViewHolder {
        val view = JourneyBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return JourneyViewHolder(view)
    }

    private var onItemClickListener: ((Journey)->Unit) ?= null

    fun setOnItemClickListener(listener: (Journey)->Unit) {
            onItemClickListener = listener
    }

    override fun onBindViewHolder(holder: JourneyViewHolder, position: Int) {
        val journey = differ.currentList[position]
        holder.binding.apply {
            ivMap.setImageBitmap(journey.img)
            tvSpeed.text = "${journey.speed} kmh"
            tvDistance.text = "${journey.distance} km"

            val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
            tvTime.text = dateFormat.format(journey.dateCreated)

            tvDuration.text = TrackingUtility.getFormattedStopwatchTime(journey.duration)


        }

        holder.binding.root.setOnClickListener {
            onItemClickListener?.let {
                it(journey)
            }
        }

    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }



}