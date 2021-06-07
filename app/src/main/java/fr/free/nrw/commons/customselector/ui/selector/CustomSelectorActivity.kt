package fr.free.nrw.commons.customselector.ui.selector

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import fr.free.nrw.commons.R
import fr.free.nrw.commons.customselector.listeners.FolderClickListener
import fr.free.nrw.commons.customselector.listeners.ImageSelectListener
import fr.free.nrw.commons.customselector.model.Folder
import fr.free.nrw.commons.customselector.model.Image

class CustomSelectorActivity : AppCompatActivity(), FolderClickListener, ImageSelectListener {

    private lateinit var viewModel: CustomSelectorViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(intent.getBooleanExtra("DarkTheme",false)){
            setTheme(R.style.DarkAppTheme)
        }
        else{
            setTheme(R.style.LightAppTheme)
        }
        setContentView(R.layout.activity_custom_selector)

        viewModel = ViewModelProvider(this, CustomSelectorViewModelFactory(this.application)).get(
            CustomSelectorViewModel::class.java
        )
        setupViews()
    }

    private fun setupViews() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, FolderFragment.newInstance())
            .commit()
        viewModel.fetchImages()
        setUpToolbar()
    }

    private fun changeTitle(title:String){
        val titleText =  findViewById<TextView>(R.id.title)
        if(titleText != null)
            titleText.text = title
    }

    private fun setUpToolbar(){
        val back = findViewById<ImageButton>(R.id.back)
        back.setOnClickListener { this.onBackPressed() }
    }

    override fun onFolderClick(folder: Folder) {
        supportFragmentManager.beginTransaction()
            .add(R.id.fragment_container, ImageFragment.newInstance(folder.bucketId))
            .addToBackStack(null)
            .commit()
        changeTitle(folder.name)
    }

    override fun onSelectedImagesChanged(selectedImages: ArrayList<Image>){
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val fragment= supportFragmentManager.findFragmentById(R.id.fragment_container)
        if(fragment!=null && fragment is FolderFragment){
            changeTitle("Custom Selector")
        }
    }


}