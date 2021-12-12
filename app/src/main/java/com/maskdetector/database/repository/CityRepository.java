package com.maskdetector.database.repository;

import android.content.Context;

import com.google.gson.Gson;
import com.maskdetector.database.CitiesData;
import com.maskdetector.database.models.City;
import com.maskdetector.utils.Utils;

import java.util.List;

public class CityRepository {
    public static final String DATABASE_CITIES_JSON = "database/cities.json";

    private final Gson gson = new Gson();
    private final Context context;

    public CityRepository(Context context) {
        this.context = context;
    }

    public List<City> getAllCities() {
        CitiesData citiesData = gson.fromJson(Utils.getJsonFromAssets(context, DATABASE_CITIES_JSON), CitiesData.class);
        return citiesData.getData();
    }
}
