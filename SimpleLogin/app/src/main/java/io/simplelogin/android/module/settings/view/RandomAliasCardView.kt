package io.simplelogin.android.module.settings.view

import android.content.Context
import android.text.SpannableStringBuilder
import android.text.style.RelativeSizeSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.text.color
import androidx.core.text.inSpans
import io.simplelogin.android.R
import io.simplelogin.android.databinding.LayoutRandomAliasCardViewBinding
import io.simplelogin.android.databinding.SpinnerRowDomainLiteBinding
import io.simplelogin.android.utils.enums.RandomMode
import io.simplelogin.android.utils.extension.resolveColor
import io.simplelogin.android.utils.model.DomainLite

class RandomAliasCardView : RelativeLayout {
    // Initializer
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    private val binding =
        LayoutRandomAliasCardViewBinding.inflate(LayoutInflater.from(context), this, true)

    // Functions
    fun bind(
        randomMode: RandomMode,
        defaultDomain: String,
        domainLites: List<DomainLite>,
    ) {
        binding.randomModeDropdown.setAdapter(RandomModeSpinnerAdapter())
        binding.randomModeDropdown.setText(randomMode.description, false)
        binding.defaultDomainDropdown.setAdapter(DefaultDomainSpinnerAdapter(domainLites))
        binding.defaultDomainDropdown.setText(defaultDomain, false)
        val defaultDomainLite = domainLites.find { it.name == defaultDomain }
        binding.defaultDomainDropdown.text = if (defaultDomainLite != null) {
            binding.defaultDomainDropdown.minLines = 2
            getDomainDropdownText(defaultDomainLite)
        } else {
            binding.defaultDomainDropdown.minLines = 1
            SpannableStringBuilder(defaultDomain)
        }
    }

    fun setRandomModeSpinnerSelectionListener(listener: (RandomMode) -> Unit) {
        binding.randomModeDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedRandomMode = RandomMode.fromPosition(position)
            binding.randomModeDropdown.setText(selectedRandomMode.description, false)
            listener(selectedRandomMode)
        }
    }

    fun setDefaultDomainSpinnerSelectionListener(listener: (DomainLite) -> Unit) {
        val defaultDomainSpinnerAdapter = binding.defaultDomainDropdown.adapter ?: return
        binding.defaultDomainDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedDomainLite = defaultDomainSpinnerAdapter.getItem(position) as DomainLite
            listener(selectedDomainLite)
            binding.defaultDomainDropdown.text = getDomainDropdownText(selectedDomainLite)
        }
    }

    private fun getDomainDropdownText(domainLite: DomainLite): SpannableStringBuilder {
        val line2 = if (domainLite.isCustom) "Your domain" else "SimpleLogin domain"
        val builder = SpannableStringBuilder()
            .color(context.resolveColor(R.attr.colorPrimary)) {
                append(domainLite.name)
            }
            .append("\n")
            .color(context.resolveColor(R.attr.colorOnSurfaceVariant)) {
                inSpans(RelativeSizeSpan(0.8f)) {
                    append(line2)
                }
            }

        return builder
    }

    inner class RandomModeSpinnerAdapter :
        ArrayAdapter<RandomMode>(context, android.R.layout.simple_spinner_dropdown_item) {
        override fun getCount() = 2
        override fun getItem(position: Int) = RandomMode.fromPosition(position)
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

    inner class DefaultDomainSpinnerAdapter(
        private val domainLites: List<DomainLite>,
    ) : ArrayAdapter<DomainLite>(context, android.R.layout.simple_spinner_dropdown_item) {
        override fun getCount() = domainLites.size
        override fun getItem(position: Int) = domainLites[position]
        override fun getItemId(position: Int) = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: View
            val binding: SpinnerRowDomainLiteBinding
            if (convertView == null) {
                binding = SpinnerRowDomainLiteBinding.inflate(LayoutInflater.from(context), parent, false)
                view = binding.root
                view.tag = binding
            } else {
                view = convertView
                binding = convertView.tag as SpinnerRowDomainLiteBinding
            }
            val domainLite = getItem(position)
            binding.domainNameTextView.text = domainLite.name
            binding.ownerTextView.text = if (domainLite.isCustom) "Your domain" else "SimpleLogin domain"
            return view
        }
    }
}
