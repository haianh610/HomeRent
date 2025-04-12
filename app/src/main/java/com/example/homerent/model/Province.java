package com.example.homerent.model;

import com.google.gson.annotations.SerializedName;

public class Province {
    @SerializedName("idProvince")
    private String idProvince;
    @SerializedName("name")
    private String name;

    // Constructor, Getters

    public Province(String idProvince, String name) {
        this.idProvince = idProvince;
        this.name = name;
    }

    public String getIdProvince() {
        return idProvince;
    }

    public String getName() {
        return name;
    }

    // toString() để hiển thị tên trong Spinner
    @Override
    public String toString() {
        return name;
    }
}