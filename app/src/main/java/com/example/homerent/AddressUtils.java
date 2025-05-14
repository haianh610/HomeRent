package com.example.homerent;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.homerent.model.AddressData;
import com.example.homerent.model.Commune;
import com.example.homerent.model.District;
import com.example.homerent.model.Province;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AddressUtils {

    private static final String TAG = "AddressUtils";
    private static final String JSON_FILE_NAME = "donvihanhchinh.json";

    private static AddressData addressDataCache = null; // Simple cache

    // Load all data (used in Create/Edit)
    public static synchronized AddressData loadAddressData(Context context) {
        if (addressDataCache != null) {
            return addressDataCache;
        }
        try {
            InputStream is = context.getAssets().open(JSON_FILE_NAME);
            Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
            Gson gson = new Gson();
            addressDataCache = gson.fromJson(reader, AddressData.class);
            reader.close();
            Log.d(TAG, "Address data loaded and cached.");
            return addressDataCache;
        } catch (IOException | JsonSyntaxException e) {
            Log.e(TAG, "Error loading/parsing address JSON", e);
            Toast.makeText(context, "Lỗi tải dữ liệu địa chỉ.", Toast.LENGTH_SHORT).show();
            return null; // Return null on error
        }
    }

    // Load only provinces (used in TenantHome)
    public static List<Province> loadProvinces(Context context) {
        AddressData data = loadAddressData(context); // Use the cached or load
        if (data != null && data.getProvinces() != null) {
            // Sort provinces by name
            List<Province> provinces = new ArrayList<>(data.getProvinces());
            Collections.sort(provinces, (p1, p2) -> p1.getName().compareTo(p2.getName()));
            return provinces;
        }
        return new ArrayList<>(); // Return empty list on error
    }

    // Optional: Add methods to get districts/communes if needed elsewhere
    public static List<District> loadDistricts(Context context) {
        AddressData data = loadAddressData(context);
        return (data != null && data.getDistricts() != null) ? data.getDistricts() : new ArrayList<>();
    }

    public static List<Commune> loadCommunes(Context context) {
        AddressData data = loadAddressData(context);
        return (data != null && data.getCommunes() != null) ? data.getCommunes() : new ArrayList<>();
    }
}