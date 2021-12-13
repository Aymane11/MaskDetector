package com.maskdetector.database.models;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public class City {
    private Integer id;
    private String city_name;
    private List<Center> centers;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCity_name() {
        return city_name;
    }

    public void setCity_name(String city_name) {
        this.city_name = city_name;
    }

    public List<Center> getCenters() {
        return centers;
    }

    public void setCenters(List<Center> centers) {
        this.centers = centers;
    }

    @Override
    public String toString() {
        return city_name;
    }
}
