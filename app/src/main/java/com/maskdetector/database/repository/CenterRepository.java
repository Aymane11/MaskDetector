package com.maskdetector.database.repository;

import com.google.gson.Gson;
import com.maskdetector.database.models.Center;

import java.util.ArrayList;
import java.util.List;

public class CenterRepository {

    public List<Center> getAllCenters() {
        List<Center> centers = new ArrayList<>();
        Gson gson = new Gson();

        return centers;
    }
}
