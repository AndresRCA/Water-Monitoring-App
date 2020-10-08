package com.example.watermonitoring;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;

import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    DrawerLayout drawer;
    private SharedPreferences userPrefs;
    String username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            username = savedInstanceState.getString("username");
        }
        else {
            // get username from shared preferences
            userPrefs = getSharedPreferences("user_data", MODE_PRIVATE);
            username = userPrefs.getString("username", null);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // navigation menu
        drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
            navigationView.setCheckedItem(R.id.nav_home);
        }

        // set data in drawer (username)
        NavigationView navView = findViewById(R.id.nav_view);
        View hView = navView.getHeaderView(0);
        TextView navUser = hView.findViewById(R.id.nav_username);
        navUser.setText(username); // set the username for the navigation menu
    }

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        }
        else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.nav_home:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();
                break;
            case R.id.nav_water_info:
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new InfoFragment()).commit();
                break;
            case R.id.settings:
                Intent settingsIntent = new Intent(this, Settings.class);
                drawer.closeDrawer(GravityCompat.START);
                startActivity(settingsIntent); // what should I do? I'll never return true and maybe that's what's making the Settings item highlighted when i press the back button but not the back arrow?
                break;
            case R.id.sign_out:
                SharedPreferences.Editor editor = userPrefs.edit();
                editor.clear(); // clear all user data
                editor.apply();
                Intent signOutIntent = new Intent(this, LoginActivity.class);
                drawer.closeDrawer(GravityCompat.START);
                startActivity(signOutIntent);
                break;
        }

        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /**
     * save values
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("username", username);
    }
}
