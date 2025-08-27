package fr.free.nrw.commons.explore.recentsearches

import android.content.Context
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.AdapterView.OnItemLongClickListener
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
    @JvmField
    @Inject
    var recentSearchesDao: RecentSearchesDao? = null

    private var recentSearches: List<String> = emptyList()
    private lateinit var adapter: ArrayAdapter<String>
    private var binding: FragmentSearchHistoryBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSearchHistoryBinding.inflate(inflater, container, false)

        recentSearches = recentSearchesDao!!.recentSearches(10)

        if (recentSearches.isEmpty()) {
            binding!!.recentSearchesDeleteButton.visibility = View.GONE
            binding!!.recentSearchesTextView.setText(R.string.no_recent_searches)
        }

        binding!!.recentSearchesDeleteButton.setOnClickListener { v: View? ->
            showDeleteRecentAlertDialog(requireContext())
        }

        adapter = ArrayAdapter(requireContext(), R.layout.item_recent_searches, recentSearches)
        binding!!.recentSearchesList.adapter = adapter
        binding!!.recentSearchesList.onItemClickListener =
            OnItemClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                (context as SearchActivity).updateText(recentSearches[position])
            }
        binding!!.recentSearchesList.onItemLongClickListener =
            OnItemLongClickListener { _: AdapterView<*>?, _: View?, position: Int, _: Long ->
                showDeleteAlertDialog(requireContext(), position)
                true
            }
        updateRecentSearches()

        return binding!!.root
    }

    private fun showDeleteRecentAlertDialog(context: Context) {
        AlertDialog.Builder(context)
            .setMessage(getString(R.string.delete_recent_searches_dialog))
            .setPositiveButton(android.R.string.yes) { dialog: DialogInterface, _: Int ->
                setDeleteRecentPositiveButton(context, dialog)
            }
            .setNegativeButton(android.R.string.no, null)
            .setCancelable(false)
            .create()
            .show()
    }

    private fun setDeleteRecentPositiveButton(context: Context, dialog: DialogInterface) {
        recentSearchesDao!!.deleteAll()
        if (binding != null) {
            binding!!.recentSearchesDeleteButton.visibility = View.GONE
            binding!!.recentSearchesTextView.setText(R.string.no_recent_searches)
            Toast.makeText(
                getContext(), getString(R.string.search_history_deleted),
                Toast.LENGTH_SHORT
            ).show()
            recentSearches = recentSearchesDao!!.recentSearches(10)
            adapter = ArrayAdapter(context, R.layout.item_recent_searches, recentSearches)
            binding!!.recentSearchesList.adapter = adapter
            adapter.notifyDataSetChanged()
        }
        dialog.dismiss()
    }

    private fun showDeleteAlertDialog(context: Context, position: Int) {
        AlertDialog.Builder(context)
            .setMessage(R.string.delete_search_dialog)
            .setPositiveButton(
                getString(R.string.delete).uppercase(),
                { dialog: DialogInterface, _: Int ->
                    setDeletePositiveButton(context, dialog, position)
                }
            )
            .setNegativeButton(android.R.string.cancel, null)
            .setCancelable(false)
            .create()
            .show()
    }

    private fun setDeletePositiveButton(context: Context, dialog: DialogInterface, position: Int) {
        recentSearchesDao!!.delete(recentSearchesDao!!.find(recentSearches[position])!!)
        recentSearches = recentSearchesDao!!.recentSearches(10)
        adapter = ArrayAdapter(
            context, R.layout.item_recent_searches,
            recentSearches
        )
        if (binding != null) {
            binding!!.recentSearchesList.adapter = adapter
            adapter.notifyDataSetChanged()
        }
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
        adapter.notifyDataSetChanged()

        if (recentSearches.isNotEmpty()) {
            if (binding != null) {
                binding!!.recentSearchesDeleteButton.visibility = View.VISIBLE
                binding!!.recentSearchesTextView.setText(R.string.search_recent_header)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding = null
    }
}
