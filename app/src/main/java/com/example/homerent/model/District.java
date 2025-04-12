package com.example.homerent.model;

import com.google.gson.annotations.SerializedName;

public class District {
    @SerializedName("idProvince")
    private String idProvince;
    @SerializedName("idDistrict")
    private String idDistrict;
    @SerializedName("name")
    private String name;

    // Constructor, Getters

    public District(String idProvince, String idDistrict, String name) {
        this.idProvince = idProvince;
        this.idDistrict = idDistrict;
        this.name = name;
    }

    public String getIdProvince() {
        return idProvince;
    }

    public String getIdDistrict() {
        return idDistrict;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}