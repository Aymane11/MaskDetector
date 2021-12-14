package com.maskdetector.fragments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.maskdetector.R;
import com.maskdetector.database.models.Center;

import java.util.List;

public class MapsFragment extends Fragment {

    private List<Center> centers;

    public void setCenters(List<Center> centers) {
        this.centers = centers;
    }

    public LatLng getMapCenter() {
        double sumLat = 0, sumLon = 0;
        for (Center center : centers) {
            sumLon += Double.parseDouble(center.getLongitude());
            sumLat += Double.parseDouble(center.getLatitude());
        }
        double centerLat = sumLat / centers.size();
        double centerLon = sumLon / centers.size();
        return new LatLng(centerLat, centerLon);
    }

    private OnMapReadyCallback callback = new OnMapReadyCallback() {
        @Override
        public void onMapReady(GoogleMap googleMap) {
            for (Center center : centers) {
                double lat = Double.parseDouble(center.getLatitude());
                double lon = Double.parseDouble(center.getLongitude());
                LatLng centerLatLng = new LatLng(lat, lon);
                googleMap.addMarker(new MarkerOptions().position(centerLatLng).title(center.getName() + "(" + center.getAddress() + ")"));
            }
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(getMapCenter()));
            googleMap.moveCamera(CameraUpdateFactory.zoomTo(10));
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment =
                (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(callback);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
    }
}