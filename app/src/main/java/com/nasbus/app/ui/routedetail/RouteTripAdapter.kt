package com.nasbus.app.ui.routedetail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nasbus.app.data.local.model.RouteTripItem
import com.nasbus.app.databinding.ItemRouteTripBinding

class RouteTripAdapter : ListAdapter<RouteTripItem, RouteTripAdapter.TripViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val binding = ItemRouteTripBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TripViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TripViewHolder(
        private val binding: ItemRouteTripBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RouteTripItem) {
            binding.chipBusId.text = item.busId
            binding.badgeNextBus.visibility = if (item.isNextBus) View.VISIBLE else View.GONE
            binding.tvDeparts.text = "Departs ${item.fromStopName} ~ ${item.departsFormatted} (${item.countdownText})"
            binding.tvArrives.text = "Arrives ${item.toStopName} ~ ${item.arrivesFormatted}"

            if (item.isOnTheWay) {
                binding.tvLeavesOrigin.text = "On the way"
            } else {
                binding.tvLeavesOrigin.text = "Leaves ${item.originName} at ${item.originDepartureTimeFormatted}"
            }

            binding.cardTrip.alpha = 1.0f
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<RouteTripItem>() {
        override fun areItemsTheSame(oldItem: RouteTripItem, newItem: RouteTripItem): Boolean {
            return oldItem.scheduleId == newItem.scheduleId
        }

        override fun areContentsTheSame(oldItem: RouteTripItem, newItem: RouteTripItem): Boolean {
            return oldItem == newItem
        }
    }
}
