package fr.free.nrw.commons.explore.recentsearches

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.FragmentSearchHistoryBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment
import fr.free.nrw.commons.explore.SearchActivity
import javax.inject.Inject

/**
 * Displays the recent searches screen.
 */
class RecentSearchesFragment : CommonsDaggerSupportFragment() {

    @set:Inject
    var recentSearchesDao: RecentSearchesDao? = null
    private var _binding: FragmentSearchHistoryBinding? = null
    private val binding get() = _binding!!
    private var recentSearches: List<String> = emptyList()
    var adapter: ArrayAdapter<String>? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchHistoryBinding.inflate(inflater, container, false)

        val recentSearches = recentSearchesDao?.recentSearches(10) ?: emptyList()

        if (recentSearches.isEmpty()) {
            binding.recentSearchesDeleteButton.visibility = View.GONE
            binding.recentSearchesTextView.setText(R.string.no_recent_searches)
        }

        binding.recentSearchesDeleteButton.setOnClickListener {
            showDeleteRecentAlertDialog(requireContext())
        }

        adapter = ArrayAdapter(
            requireContext(), R.layout.item_recent_searches,
            recentSearches
        )
        binding.recentSearchesList.adapter = adapter
        binding.recentSearchesList.setOnItemClickListener { _, _, position, _ ->
            (context as SearchActivity).updateText(
                recentSearches[position]
            )
        }
        binding.recentSearchesList.setOnItemLongClickListener { _, _, position, _ ->
            showDeleteAlertDialog(requireContext(), position)
            true
        }
        updateRecentSearches()

        return binding.root
    }

    private fun showDeleteRecentAlertDialog(context: Context) {
        AlertDialog.Builder(context)
            .setMessage(getString(R.string.delete_recent_searches_dialog))
            .setPositiveButton(
                R.string.yes
            ) { dialog: DialogInterface?, _ ->
                if (dialog != null) {
                    setDeleteRecentPositiveButton(
                        context,
                        dialog
                    )
                }
            }
            .setNegativeButton(R.string.no, null)
            .create()
            .show()
    }

    private fun setDeleteRecentPositiveButton(
        context: Context,
        dialog: DialogInterface
    ) {
        recentSearchesDao!!.deleteAll()
        binding.recentSearchesDeleteButton.visibility = View.GONE
        binding.recentSearchesTextView.setText(R.string.no_recent_searches)
        Toast.makeText(
            getContext(), getString(R.string.search_history_deleted),
            Toast.LENGTH_SHORT
        ).show()
        recentSearches = recentSearchesDao!!.recentSearches(10)
        adapter = ArrayAdapter(
            context, R.layout.item_recent_searches,
            recentSearches
        )
        binding.recentSearchesList.adapter = adapter
        adapter?.notifyDataSetChanged()
        dialog.dismiss()
    }

    private fun showDeleteAlertDialog(context: Context, position: Int) {
        AlertDialog.Builder(context)
            .setMessage(R.string.delete_search_dialog)
            .setPositiveButton(
                getString(R.string.delete).uppercase(),
                (DialogInterface.OnClickListener { dialog: DialogInterface?, _: Int ->
                    dialog?.let {
                        setDeletePositiveButton(
                            context,
                            it,
                            position
                        )
                    }
                })
            )
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }

    private fun setDeletePositiveButton(
        context: Context,
        dialog: DialogInterface, position: Int
    ) {
        recentSearchesDao?.delete(recentSearchesDao?.find(recentSearches[position]))
        recentSearches = recentSearchesDao!!.recentSearches(10)
        adapter = ArrayAdapter(
            context, R.layout.item_recent_searches,
            recentSearches
        )
        binding.recentSearchesList.adapter = adapter
        adapter?.notifyDataSetChanged()
        dialog.dismiss()
    }

    /**
     * This method is called on back press of activity so we are updating the list from database to
     * refresh the recent searches list.
     */
    override fun onResume() {
        updateRecentSearches()
        super.onResume()
    }

    /**
     * This method is called when search query is null to update Recent Searches
     */
    fun updateRecentSearches() {
        recentSearches = recentSearchesDao!!.recentSearches(10)
        adapter?.notifyDataSetChanged()

        if (recentSearches.isNotEmpty()) {
            binding.recentSearchesDeleteButton.visibility = View.VISIBLE
            binding.recentSearchesTextView.setText(R.string.search_recent_header)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}