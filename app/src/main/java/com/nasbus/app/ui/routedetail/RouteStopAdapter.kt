package com.nasbus.app.ui.routedetail

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.nasbus.app.R
import com.nasbus.app.data.local.entity.StopEntity
import com.nasbus.app.databinding.ItemStopTimelineBinding
import java.util.Locale

class RouteStopAdapter : ListAdapter<StopEntity, RouteStopAdapter.StopViewHolder>(DiffCallback) {

    private var fromStopName: String? = null
    private var toStopName: String? = null

    fun setHighlightSegment(from: String?, to: String?) {
        fromStopName = from
        toStopName = to
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
        val binding = ItemStopTimelineBinding.inflate(
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
        private val binding: ItemStopTimelineBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(stop: StopEntity, position: Int, totalCount: Int) {
            val context = binding.root.context
            binding.tvStopBadge.text = stop.sequenceOrder.toString()
            binding.tvStopName.text = stop.stopName
            binding.tvStopDistance.text = "${String.format(Locale.US, "%.1f", stop.distanceFromOriginKm)} km from origin"

            // Hide top line for first item, bottom line for last item
            binding.viewLineTop.visibility = if (position == 0) View.INVISIBLE else View.VISIBLE
            binding.viewLineBottom.visibility = if (position == totalCount - 1) View.INVISIBLE else View.VISIBLE

            val stopsList = currentList
            val fromIndex = stopsList.indexOfFirst { it.stopName.equals(fromStopName, ignoreCase = true) }
            val toIndex = stopsList.indexOfFirst { it.stopName.equals(toStopName, ignoreCase = true) }

            val isSegmentActive = (fromIndex != -1 && toIndex != -1 && fromIndex < toIndex)
            val isHighlighted = if (isSegmentActive) {
                position in fromIndex..toIndex
            } else {
                true
            }

            val primaryColor = ContextCompat.getColor(context, R.color.colorPrimary)

            if (isHighlighted) {
                binding.root.alpha = 1.0f
                binding.tvStopName.setTypeface(null, Typeface.BOLD)

                val shapeDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(primaryColor)
                }
                binding.tvStopBadge.background = shapeDrawable
                binding.viewLineTop.setBackgroundColor(primaryColor)
                binding.viewLineBottom.setBackgroundColor(primaryColor)
            } else {
                binding.root.alpha = 0.45f
                binding.tvStopName.setTypeface(null, Typeface.NORMAL)

                val dimmedBadgeColor = Color.parseColor("#90A4AE")
                val dimmedLineColor = Color.parseColor("#CFD8DC")

                val shapeDrawable = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(dimmedBadgeColor)
                }
                binding.tvStopBadge.background = shapeDrawable
                binding.viewLineTop.setBackgroundColor(dimmedLineColor)
                binding.viewLineBottom.setBackgroundColor(dimmedLineColor)
            }
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
