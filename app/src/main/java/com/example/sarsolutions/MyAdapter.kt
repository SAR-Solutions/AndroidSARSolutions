package com.example.sarsolutions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class MyAdapter() : RecyclerView.Adapter<MyAdapter.MyViewHolder>() {
    private val dataSet = ArrayList<String>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_view_item, parent, false)
        return MyViewHolder(view)

    }
    override fun getItemCount() = dataSet.size

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        return
    }

//    fun addItem()

    class MyViewHolder(private val itemView: View) : RecyclerView.ViewHolder(itemView)
}