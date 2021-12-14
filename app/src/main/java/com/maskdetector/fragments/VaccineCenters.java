package com.maskdetector.fragments;

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import com.google.android.material.appbar.MaterialToolbar;
import com.maskdetector.R;
import com.maskdetector.adapter.MainAdapter;
import com.maskdetector.database.models.City;
import com.maskdetector.database.repository.CityRepository;

public class VaccineCenters extends Fragment {
    private MainAdapter adapter;
    private SwipeRefreshLayout refreshLayout;
    private CityRepository cityRepository;

    public VaccineCenters() {
        //
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_vaccine_centers, container, false);

        cityRepository  = new CityRepository(root.getContext());

        MaterialToolbar toolbar = root.findViewById(R.id.top_toolbar);

        MenuItem searchItem = toolbar.getMenu().findItem(R.id.search_cities);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });

        adapter = new MainAdapter(new MainAdapter.OnItemClickListener() {
            @Override
            public void onItemClicked(City city) {
                Log.d("clicked", "You clicked " + city.getCity_name());
                MapsFragment mapFragment = new MapsFragment();
                mapFragment.setCenters(city.getCenters());
                FragmentManager fragmentManager = getFragmentManager();

                fragmentManager.beginTransaction()
                        .replace(R.id.fl_fragment, mapFragment)
                        .addToBackStack("VaccineCenters")
                        .commit();
            }
        });
        RecyclerView recyclerView = root.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(root.getContext()));
        recyclerView.setAdapter(adapter);

        refreshLayout = root.findViewById(R.id.refresh);
        refreshLayout.setOnRefreshListener(this::loadCities);

        loadCities();

        return root;
    }

    private void loadCities() {
        adapter.setData(cityRepository.getAllCities());
        refreshLayout.setRefreshing(false);
    }
}