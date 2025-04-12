package com.example.homerent.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class AddressData {
    @SerializedName("province")
    private List<Province> provinces;
    @SerializedName("district")
    private List<District> districts;
    @SerializedName("commune")
    private List<Commune> communes;

    // Getters
    public List<Province> getProvinces() { return provinces; }
    public List<District> getDistricts() { return districts; }
    public List<Commune> getCommunes() { return communes; }
}