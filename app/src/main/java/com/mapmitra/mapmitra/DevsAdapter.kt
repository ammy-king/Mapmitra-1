package com.mapmitra.mapmitra

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mapmitra.mapmitra.models.Developers

import kotlinx.android.synthetic.main.item_developer.view.*

class DevsAdapter(val context: Context , val developers: List<Developers>) :
    RecyclerView.Adapter<DevsAdapter.ViewHolder>() {

    override fun getItemCount() = developers.size


    override fun onBindViewHolder(holder: ViewHolder , position: Int) {
        holder.Bind(developers[position])
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun Bind(Developers: Developers) {
            val countryCode = "+91 "

            itemView.dev_name.text = Developers.name
            itemView.dev_mail.text = Developers.Email
            itemView.dev_phone.text = countryCode + Developers.contact_number.toString().toLong()
            itemView.dev_linkedIn.text = Developers.linkedIn
            Glide.with(context).load(Developers.image_url).into(itemView.dev_Image)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup , viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_developer , parent , false)
        return ViewHolder(view)
    }


}