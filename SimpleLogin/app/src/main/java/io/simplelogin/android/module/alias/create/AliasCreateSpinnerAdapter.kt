package io.simplelogin.android.module.alias.create

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

class AliasCreateSpinnerAdapter(
    context: Context,
    private val suffixes: List<String>,
) : ArrayAdapter<String>(context, android.R.layout.simple_spinner_dropdown_item, suffixes) {
    override fun getCount() = suffixes.size
    override fun getItem(position: Int) = suffixes[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(
        position: Int,
        convertView: View?,
        parent: ViewGroup,
    ): View {
        val view = super.getView(position, convertView, parent) as TextView
        view.text = getItem(position)
        return view
    }
}
