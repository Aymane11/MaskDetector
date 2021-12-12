package com.maskdetector.database;

import com.maskdetector.database.models.City;

import java.util.ArrayList;
import java.util.List;

public class CitiesData {
    private List<City> data;

    public List<City> getData() {
        return data != null ? data : new ArrayList<>();
    }
}
