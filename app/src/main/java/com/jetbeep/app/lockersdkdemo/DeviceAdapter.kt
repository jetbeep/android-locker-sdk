package com.jetbeep.app.lockersdkdemo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.jetbeep.lockersdk.locker.LockerDevice


class DeviceAdapter(private val onItemClick: (LockerDevice) -> Unit) :
    ListAdapter<LockerDevice, DeviceAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = ViewHolder(
        LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list_device, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        val context = holder.root.context
        holder.label.text =
            context.getString(R.string.device_item_label, item.deviceId)
        holder.connect.setOnClickListener { onItemClick(item) }
    }


    class DiffCallback : DiffUtil.ItemCallback<LockerDevice>() {
        override fun areItemsTheSame(oldItem: LockerDevice, newItem: LockerDevice) =
            oldItem == newItem

        override fun areContentsTheSame(oldItem: LockerDevice, newItem: LockerDevice) =
            oldItem == newItem
    }

    class ViewHolder(val root: View) : RecyclerView.ViewHolder(root) {
        val label: TextView = root.findViewById(R.id.text)
        val connect: Button = root.findViewById(R.id.connect)
    }
}