package com.example.fmlm

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.fmlm.fragment.login.LoginComponentFragment

import com.example.fmlm.fragment.profile.ProfileComponentFragment
import com.example.fmlm.fragment.routing.RoutingComponentFragment
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawer: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawer = findViewById(R.id.drawer_layout)
        val navigationView: NavigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(this, drawer, toolbar,
            R.string.drawer_open, R.string.drawer_close)
        drawer.addDrawerListener(toggle)
        toggle.syncState()
    }

    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        val transaction = supportFragmentManager.beginTransaction()
        var fragment: Fragment = ProfileComponentFragment()
        when (p0.itemId) {
            R.id.nav_profileComponentFragment -> fragment = ProfileComponentFragment()
            R.id.nav_routingComponentFragment -> fragment = RoutingComponentFragment()
            R.id.nav_loginComponentFragment -> fragment = LoginComponentFragment()
        }
        transaction.replace(R.id.nav_host_fragment, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        if(drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START)
        else
            super.onBackPressed()
    }
}
