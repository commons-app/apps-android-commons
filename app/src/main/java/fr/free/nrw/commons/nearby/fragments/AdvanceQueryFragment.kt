package fr.free.nrw.commons.nearby.fragments

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.databinding.FragmentAdvanceQueryBinding

class AdvanceQueryFragment : Fragment() {

    private var _binding: FragmentAdvanceQueryBinding? = null
    private val binding get() = _binding

    lateinit var callback: Callback

    /**
     * View Elements
     */
    private var etQuery: EditText? = null
    private var btnApply: Button? = null
    private var btnReset: Button? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAdvanceQueryBinding.inflate(inflater, container, false)
        etQuery = binding?.etQuery
        btnApply = binding?.btnApply
        btnReset = binding?.btnReset
        setHasOptionsMenu(false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setUi()

        setClickListeners()
    }

    private fun setUi() {
        etQuery?.setText(arguments?.getString("query")!!)
    }

    private fun setClickListeners() {
        btnReset?.setOnClickListener {
            etQuery?.setText(arguments?.getString("query")!!)
            etQuery?.clearFocus()
            hideKeyBoard()
            callback.reset()
        }

        btnApply?.setOnClickListener {
            etQuery?.clearFocus()
            hideKeyBoard()
            callback.apply(etQuery?.text.toString())
            callback.close()
        }
    }

    fun hideKeyBoard() {
        val inputMethodManager =
            context?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
        inputMethodManager?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    interface Callback {
        fun reset()
        fun apply(query: String)
        fun close()
    }
}