package fr.free.nrw.commons.settings

import android.os.Bundle
import android.view.MenuItem
import fr.free.nrw.commons.databinding.ActivitySettingsBinding
import fr.free.nrw.commons.theme.BaseActivity


/**
 * allows the user to change the settings
 */
class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
//    private var settingsDelegate: AppCompatDelegate? = null

    /**
     * to be called when the activity starts
     * @param savedInstanceState the previously saved state
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        setSupportActionBar(binding.toolbarBinding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    // Get an action bar
    /**
     * takes care of actions taken after the creation has happened
     * @param savedInstanceState the saved state
     */
    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
//        if (settingsDelegate == null) {
//            settingsDelegate = AppCompatDelegate.create(this, null)
//        }
//        settingsDelegate?.onPostCreate(savedInstanceState)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    /**
     * Handle action-bar clicks
     * @param item the selected item
     * @return true on success, false on failure
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
