package com.maskdetector.database.models;

import java.util.List;

public class City {
    private Integer id;
    private String name;
    private List<Center> centers;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Center> getCenters() {
        return centers;
    }

    public void setCenters(List<Center> centers) {
        this.centers = centers;
    }

    @Override
    public String toString() {
        return name;
    }
}
