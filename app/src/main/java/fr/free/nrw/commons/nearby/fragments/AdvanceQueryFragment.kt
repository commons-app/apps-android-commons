package fr.free.nrw.commons.nearby.fragments

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.R
import kotlinx.android.synthetic.main.fragment_advance_query.*

class AdvanceQueryFragment : Fragment() {

    lateinit var originalQuery: String
    lateinit var callback: Callback
    lateinit var etQuery: AppCompatEditText
    lateinit var btnApply: AppCompatButton
    lateinit var btnReset: AppCompatButton
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_advance_query, container, false)
        originalQuery = arguments?.getString("query")!!
        setHasOptionsMenu(false)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        etQuery = view.findViewById(R.id.et_query)
        btnApply = view.findViewById(R.id.btn_apply)
        btnReset = view.findViewById(R.id.btn_reset)

        etQuery.setText(originalQuery)
        btnReset.setOnClickListener {
            btnReset.post {
                etQuery.setText(originalQuery)
                etQuery.clearFocus()
                hideKeyBoard()
                callback.reset()
            }
        }

        btnApply.setOnClickListener {
            btnApply.post {
                etQuery.clearFocus()
                hideKeyBoard()
                callback.apply(etQuery.text.toString())
                callback.close()
            }
        }
    }

    fun hideKeyBoard() {
        val inputMethodManager =
            context?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    interface Callback {
        fun reset()
        fun apply(query: String)
        fun close()
    }
}