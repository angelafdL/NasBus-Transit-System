package com.nasbus.app.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nasbus.app.data.local.model.ScheduleWithDetails
import com.nasbus.app.databinding.ItemScheduleListBinding
import com.nasbus.app.util.FareCalculator
import com.nasbus.app.util.TravelTimeEstimator
import kotlin.math.roundToInt

class ScheduleListAdapter(
    private val onItemClick: (ScheduleWithDetails) -> Unit
) : ListAdapter<ScheduleWithDetails, ScheduleListAdapter.ScheduleViewHolder>(DiffCallback) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).scheduleId.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val binding = ItemScheduleListBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ScheduleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ScheduleViewHolder(
        private val binding: ItemScheduleListBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: ScheduleWithDetails) {
            val depMins = TravelTimeEstimator.parseClockToMinutes(item.departureTime)
            val arrMins = TravelTimeEstimator.parseClockToMinutes(item.arrivalTime) + if (item.arrivalNextDay) 1440 else 0

            val depFormatted = TravelTimeEstimator.formatClock(depMins)
            val arrFormatted = TravelTimeEstimator.formatClock(arrMins)

            binding.tvTimes.text = "$depFormatted → $arrFormatted"
            binding.tvRouteName.text = item.routeName
            binding.tvCompanyName.text = item.companyName
            binding.chipBusId.text = item.busId

            val regFare = if (item.distanceKm > 0.0 && item.farePerKm > 0.0) {
                FareCalculator.regularFare(item.distanceKm, item.farePerKm)
            } else {
                item.estimatedFare.roundToInt()
            }
            val discFare = FareCalculator.discountedFare(regFare)

            binding.tvRegularFare.text = "Estimated fare ${FareCalculator.formatPeso(regFare)}"
            binding.tvDiscountFare.text = "${FareCalculator.DISCOUNT_LABEL} ${FareCalculator.formatPeso(discFare)}"

            binding.root.setOnClickListener {
                onItemClick(item)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<ScheduleWithDetails>() {
        override fun areItemsTheSame(oldItem: ScheduleWithDetails, newItem: ScheduleWithDetails): Boolean {
            return oldItem.scheduleId == newItem.scheduleId
        }

        override fun areContentsTheSame(oldItem: ScheduleWithDetails, newItem: ScheduleWithDetails): Boolean {
            return oldItem == newItem
        }
    }
}
