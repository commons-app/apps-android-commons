package fr.free.nrw.commons.edit

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import javax.inject.Inject

class EditViewModel () : ViewModel() {

   @Inject
   lateinit var transformImage: TransformImage


    fun rotateImage(degree:String){
          Timber.d("HOOOOOOOOOOOOOOOOLaaaaaaaaaaaaaaaaa")
    }
}