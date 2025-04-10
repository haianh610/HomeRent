package com.example.homerent.model;

import com.google.gson.annotations.SerializedName;

public class Commune {
    @SerializedName("idDistrict")
    private String idDistrict;
    @SerializedName("idCommune")
    private String idCommune;
    @SerializedName("name")
    private String name;

    // Constructor, Getters

    public Commune(String idDistrict, String idCommune, String name) {
        this.idDistrict = idDistrict;
        this.idCommune = idCommune;
        this.name = name;
    }

    public String getIdDistrict() {
        return idDistrict;
    }

    public String getIdCommune() {
        return idCommune;
    }

    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}