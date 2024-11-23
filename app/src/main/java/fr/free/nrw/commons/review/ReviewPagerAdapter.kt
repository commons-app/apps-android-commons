package fr.free.nrw.commons.review

import android.os.Bundle

import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter


class ReviewPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
    private val reviewImageFragments: Array<ReviewImageFragment> = arrayOf(
        ReviewImageFragment(),
        ReviewImageFragment(),
        ReviewImageFragment(),
        ReviewImageFragment()
    )

    override fun getCount(): Int {
        return reviewImageFragments.size
    }

    fun updateFileInformation() {
        for (i in 0 until count) {
            val fragment = reviewImageFragments[i]
            fragment.update(i)
        }
    }

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle().apply {
            putInt("position", position)
        }
        reviewImageFragments[position].arguments = bundle
        return reviewImageFragments[position]
    }
}
