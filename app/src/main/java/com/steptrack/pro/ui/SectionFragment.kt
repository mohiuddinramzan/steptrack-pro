package com.steptrack.pro.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import com.steptrack.pro.R

/** Temporary shell for tabs whose real content arrives in later phases. */
class SectionFragment : Fragment(R.layout.fragment_section) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val args = requireArguments()
        view.findViewById<TextView>(R.id.sectionTitle).setText(args.getInt(ARG_TITLE))
        view.findViewById<TextView>(R.id.sectionNote).setText(args.getInt(ARG_NOTE))
    }

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_NOTE = "note"

        fun newInstance(titleRes: Int, noteRes: Int) = SectionFragment().apply {
            arguments = bundleOf(ARG_TITLE to titleRes, ARG_NOTE to noteRes)
        }
    }
}
