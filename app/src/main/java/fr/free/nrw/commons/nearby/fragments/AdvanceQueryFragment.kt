package fr.free.nrw.commons.nearby.fragments

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.R
import kotlinx.android.synthetic.main.fragment_advance_query.*

class AdvanceQueryFragment : Fragment() {

    lateinit var originalQuery: String
    lateinit var callback: Callback
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
        et_query.setText(originalQuery)
        btn_reset.setOnClickListener {
            btn_reset.post {
                et_query.setText(originalQuery)
                et_query.clearFocus()
                hideKeyBoard()
                callback.reset()
            }
        }

        btn_apply.setOnClickListener {
            btn_apply.post {
                et_query.clearFocus()
                hideKeyBoard()
                callback.apply(et_query.text.toString())
                callback.close()
            }
        }
    }

    fun hideKeyBoard(){
        val inputMethodManager = context?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    interface Callback {
        fun reset()
        fun apply(query: String)
        fun close()
    }
}