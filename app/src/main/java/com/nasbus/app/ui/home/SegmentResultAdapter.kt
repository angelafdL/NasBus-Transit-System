package com.nasbus.app.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nasbus.app.data.local.model.SegmentResult
import com.nasbus.app.databinding.ItemSegmentResultBinding
import com.nasbus.app.util.TravelTimeEstimator
import java.util.Locale
import kotlin.math.roundToInt

class SegmentResultAdapter(
    private val onItemClick: (SegmentResult) -> Unit
) : ListAdapter<SegmentResult, SegmentResultAdapter.SegmentViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SegmentViewHolder {
        val binding = ItemSegmentResultBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SegmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SegmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SegmentViewHolder(
        private val binding: ItemSegmentResultBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SegmentResult) {
            binding.tvRouteName.text = item.routeName
            binding.tvCompanyName.text = item.companyName
            binding.tvSegmentStops.text = "${item.fromStopName} → ${item.toStopName}"
            binding.tvDistance.text = "Distance: ${String.format(Locale.US, "%.1f", item.segmentDistanceKm)} km"

            val estimatedFarePesos = item.segmentFare.roundToInt()
            binding.tvFare.text = "≈ ₱$estimatedFarePesos"

            val estimatedMins = TravelTimeEstimator.estimateSegmentMinutes(
                segmentDistanceKm = item.segmentDistanceKm,
                routeDistanceKm = item.routeDistanceKm,
                routeEstimatedTravelTime = item.estimatedTravelTime
            )
            val estimatedTimeFormatted = TravelTimeEstimator.formatEstimatedSegmentTime(estimatedMins)
            binding.tvTravelTime.text = "Est. travel time: $estimatedTimeFormatted"

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<SegmentResult>() {
        override fun areItemsTheSame(oldItem: SegmentResult, newItem: SegmentResult): Boolean {
            return oldItem.routeId == newItem.routeId &&
                    oldItem.fromStopName == newItem.fromStopName &&
                    oldItem.toStopName == newItem.toStopName
        }

        override fun areContentsTheSame(oldItem: SegmentResult, newItem: SegmentResult): Boolean {
            return oldItem == newItem
        }
    }
}
