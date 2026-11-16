package com.nasbus.app.ui.schedule

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nasbus.app.data.local.entity.StopEntity
import com.nasbus.app.databinding.ItemTripStopTimeBinding
import com.nasbus.app.util.TravelTimeEstimator
import java.util.Locale

class TripStopTimeAdapter : ListAdapter<StopEntity, TripStopTimeAdapter.StopViewHolder>(DiffCallback) {

    private var scheduleDepartureTime: String = "00:00"
    private var scheduleArrivalTime: String = "00:00"
    private var arrivalNextDay: Boolean = false
    private var routeDistanceKm: Double = 1.0
    private var routeEstimatedTravelTime: String = "3 to 4 hours"

    fun setScheduleInfo(
        departureTime: String,
        arrivalTime: String,
        arrivalNextDay: Boolean,
        routeDistanceKm: Double,
        routeEstimatedTravelTime: String
    ) {
        this.scheduleDepartureTime = departureTime
        this.scheduleArrivalTime = arrivalTime
        this.arrivalNextDay = arrivalNextDay
        this.routeDistanceKm = routeDistanceKm
        this.routeEstimatedTravelTime = routeEstimatedTravelTime
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
        val binding = ItemTripStopTimeBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StopViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
        holder.bind(getItem(position), position, itemCount)
    }

    inner class StopViewHolder(
        private val binding: ItemTripStopTimeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stop: StopEntity, position: Int, totalCount: Int) {
            binding.tvStopBadge.text = stop.sequenceOrder.toString()
            binding.tvStopName.text = stop.stopName
            binding.tvStopDistance.text = "${String.format(Locale.US, "%.1f", stop.distanceFromOriginKm)} km from origin"

            // Hide top line for first stop, bottom line for last stop
            binding.viewLineTop.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
            binding.viewLineBottom.visibility = if (position == totalCount - 1) View.INVISIBLE else View.VISIBLE

            val formattedClockTime = when {
                position == 0 -> {
                    val depMins = TravelTimeEstimator.parseClockToMinutes(scheduleDepartureTime)
                    TravelTimeEstimator.formatClock(depMins)
                }
                position == totalCount - 1 -> {
                    val arrMins = TravelTimeEstimator.parseClockToMinutes(scheduleArrivalTime) + if (arrivalNextDay) 1440 else 0
                    TravelTimeEstimator.formatClock(arrMins)
                }
                else -> {
                    val stopMins = TravelTimeEstimator.estimateTimeAtStop(
                        scheduleDepartureTime,
                        stop.distanceFromOriginKm,
                        routeDistanceKm,
                        routeEstimatedTravelTime
                    )
                    TravelTimeEstimator.formatClock(stopMins)
                }
            }

            binding.tvStopTime.text = formattedClockTime
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<StopEntity>() {
        override fun areItemsTheSame(oldItem: StopEntity, newItem: StopEntity): Boolean {
            return oldItem.stopId == newItem.stopId
        }

        override fun areContentsTheSame(oldItem: StopEntity, newItem: StopEntity): Boolean {
            return oldItem == newItem
        }
    }
}
