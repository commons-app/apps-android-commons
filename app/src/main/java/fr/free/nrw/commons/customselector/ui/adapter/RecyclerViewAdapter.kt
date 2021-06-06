package fr.free.nrw.commons.customselector.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import androidx.recyclerview.widget.RecyclerView

abstract class RecyclerViewAdapter<T : RecyclerView.ViewHolder?>(val context: Context): RecyclerView.Adapter<T>() {
    val inflater: LayoutInflater = LayoutInflater.from(context)
}