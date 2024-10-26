package fr.free.nrw.commons.fileusages

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import fr.free.nrw.commons.databinding.FragmentFileUsagesBinding
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment

class FileUsagesFragment: CommonsDaggerSupportFragment() {

    private var _binding: FragmentFileUsagesBinding? = null
    private val binding get() = _binding!!


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFileUsagesBinding.inflate(layoutInflater)
        return binding.root
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}