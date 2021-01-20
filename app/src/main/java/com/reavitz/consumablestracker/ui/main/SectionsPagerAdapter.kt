package com.reavitz.consumablestracker.ui.main

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.reavitz.consumablestracker.R

class SectionsPagerAdapter(
    private val context: Context, fm: FragmentManager,
    private val simInfo: List<SimInfo>
) :
    FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {


    override fun getItem(position: Int): Fragment {
        return PlaceholderFragment.newInstance(simInfo[position])
    }

    override fun getPageTitle(position: Int): CharSequence {
        return context.resources.getString(R.string.sim_tab, position + 1)
    }

    override fun getCount(): Int {
        return simInfo.size
    }
}