package fr.free.nrw.commons.nearby.fragments

import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import fr.free.nrw.commons.databinding.FragmentAdvanceQueryBinding

class AdvanceQueryFragment : Fragment() {

    private var _binding: FragmentAdvanceQueryBinding? = null
    private val binding get() = _binding
    lateinit var originalQuery: String
    lateinit var callback: Callback

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAdvanceQueryBinding.inflate(inflater, container, false)
        originalQuery = arguments?.getString("query")!!
        setHasOptionsMenu(false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        with(requireNotNull(binding)) {
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
    }

    fun hideKeyBoard() {
        val inputMethodManager =
            context?.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager?
        inputMethodManager?.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    interface Callback {
        fun reset()
        fun apply(query: String)
        fun close()
    }
}