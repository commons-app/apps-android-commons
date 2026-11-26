package fr.free.nrw.commons.media

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import androidx.core.net.toUri
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.ActivityDeletedFileBinding
import fr.free.nrw.commons.theme.BaseActivity
import fr.free.nrw.commons.utils.applyEdgeToEdgeAllInsets
import fr.free.nrw.commons.utils.handleWebUrl

/**
 * Explains that Commons deleted a file, and links to the file page and the deletion discussion.
 */
class DeletedFileActivity : BaseActivity() {
    private var binding: ActivityDeletedFileBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filename = intent.getStringExtra(EXTRA_FILENAME)
        if (filename.isNullOrEmpty()) {
            finish()
            return
        }
        binding = ActivityDeletedFileBinding.inflate(layoutInflater)
        applyEdgeToEdgeAllInsets(binding!!.root)
        setContentView(binding!!.root)
        setSupportActionBar(binding!!.toolbarBinding.toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        title = getString(R.string.deleted_file_activity_title)

        binding!!.viewFilePage.setOnClickListener {
            handleWebUrl(this, wikiPage(filename))
        }
        binding!!.viewDeletionDiscussion.setOnClickListener {
            handleWebUrl(this, wikiPage("Commons:Deletion requests/$filename"))
        }
        binding!!.viewPolicies.setOnClickListener {
            handleWebUrl(this, wikiPage("Commons:Policies and guidelines"))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        binding = null
        super.onDestroy()
    }

    /**
     * MediaWiki page titles use underscores. Characters such as '?' stay encoded in the path.
     */
    private fun wikiPage(title: String): Uri {
        val normalized = title.trim().replace(' ', '_')
        return (BuildConfig.COMMONS_URL + "/wiki/" + Uri.encode(normalized, ":/_")).toUri()
    }

    companion object {
        private const val EXTRA_FILENAME = "filename"

        /**
         * Opens the explanation for a file Commons has deleted.
         *
         * @param filename file title, including the File: prefix
         */
        fun start(
            context: Context,
            filename: String,
        ) {
            context.startActivity(
                Intent(context, DeletedFileActivity::class.java)
                    .putExtra(EXTRA_FILENAME, filename),
            )
        }
    }
}
