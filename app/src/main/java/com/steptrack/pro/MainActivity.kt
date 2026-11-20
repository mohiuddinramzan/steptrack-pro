package com.steptrack.pro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.steptrack.pro.databinding.ActivityMainBinding
import com.steptrack.pro.ui.GoalsFragment
import com.steptrack.pro.ui.HomeFragment
import com.steptrack.pro.ui.ProfileFragment
import com.steptrack.pro.ui.StatisticsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = fragmentFor(item.itemId)
            if (fragment != null) show(fragment)
            fragment != null
        }

        if (savedInstanceState == null) {
            binding.bottomNav.menu.findItem(R.id.nav_home).isChecked = true
            show(HomeFragment())
        }
    }

    private fun fragmentFor(itemId: Int): Fragment? = when (itemId) {
        R.id.nav_home -> HomeFragment()
        R.id.nav_statistics ->
            StatisticsFragment()
        R.id.nav_goals ->
            GoalsFragment()
        R.id.nav_profile ->
            ProfileFragment()
        else -> null
    }

    private fun show(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }
}
