package io.simplelogin.android.module.settings.view

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RelativeLayout
import android.widget.TextView
import io.simplelogin.android.databinding.LayoutSenderAddressFormatCardViewBinding
import io.simplelogin.android.utils.enums.SenderFormat
import io.simplelogin.android.utils.enums.SenderFormat.A
import io.simplelogin.android.utils.enums.SenderFormat.AT


class SenderAddressFormatCardView : RelativeLayout {
    // Initializer
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    private val binding = LayoutSenderAddressFormatCardViewBinding.inflate(LayoutInflater.from(context), this, true)

    private val senderFormats = listOf(A, AT)

    // Functions
    fun bind(senderFormat: SenderFormat) {
        binding.senderAddressFormatDropdown.setAdapter(
            SenderAddressFormatSpinnerAdapter(context, senderFormats)
        )
        binding.senderAddressFormatDropdown.setText(senderFormat.description, false)
    }

    fun setSenderAddressFormatSpinnerSelectionListener(listener: (SenderFormat) -> Unit) {
        binding.senderAddressFormatDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedSenderFormat = senderFormats[position]
            binding.senderAddressFormatDropdown.setText(selectedSenderFormat.description, false)
            listener(selectedSenderFormat)
        }
    }
}

class SenderAddressFormatSpinnerAdapter(
    context: Context,
    private val senderFormats: List<SenderFormat>,
) : ArrayAdapter<SenderFormat>(context, android.R.layout.simple_spinner_dropdown_item) {
    override fun getCount() = senderFormats.size
    override fun getItem(position: Int) = senderFormats[position]
    override fun getItemId(position: Int) = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as TextView
        view.text = getItem(position).description
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent) as TextView
        view.text = getItem(position).description
        return view
    }
}
