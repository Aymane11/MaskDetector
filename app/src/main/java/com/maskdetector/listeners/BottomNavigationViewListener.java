package com.maskdetector.listeners;

import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.navigation.NavigationBarView;
import com.maskdetector.R;
import com.maskdetector.fragments.MaskDetector;
import com.maskdetector.fragments.VaccineCenters;

public class BottomNavigationViewListener implements NavigationBarView.OnItemSelectedListener {

    private final AppCompatActivity activity;
    private MaskDetector maskDetectorFragment = new MaskDetector();
    private VaccineCenters vaccineCentersFragment = new VaccineCenters();

    public BottomNavigationViewListener(AppCompatActivity activity) {
        this.activity = activity;
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
            case R.id.maskCheckMenu:
                activity.getSupportFragmentManager().beginTransaction().replace(R.id.fl_fragment, maskDetectorFragment).commit();
                return true;
            case R.id.vaccineCentersMenu:
                activity.getSupportFragmentManager().beginTransaction().replace(R.id.fl_fragment, vaccineCentersFragment).commit();
                return true;
            default:
                return false;
        }
    }
}
