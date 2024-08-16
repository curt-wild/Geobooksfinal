package com.example.geobooks2;

import android.Manifest;
import android.location.Location;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.tasks.OnSuccessListener;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class creates the fragment that contains the map.
 * It is called from the MainActivity class.
 */

public class MapFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap googleMap;
    private ActivityResultLauncher<String[]> locationPermissionRequest;
    private boolean isMapCentered = false;
    private String latitudeColumn = "BirthCityLat";
    private String longitudeColumn = "BirthCityLong";


    @SuppressLint("MissingPermission")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_map, container, false);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Reset isMapCentered to false to center the map on the user's location every time the fragment is created
        isMapCentered = false;


        // initialize location permission request
        locationPermissionRequest = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
            Boolean fineLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false);
            Boolean coarseLocationGranted = result.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false);
            if ((fineLocationGranted != null && fineLocationGranted) || (coarseLocationGranted != null && coarseLocationGranted)) {
                if (googleMap != null) {
                    googleMap.setMyLocationEnabled(true);
                    googleMap.getUiSettings().setMyLocationButtonEnabled(true);
                }
            } else {
                Toast.makeText(getContext(), "Location cannot be obtained due to missing permission.", Toast.LENGTH_LONG).show();
            }
        });

        return view;
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;

        // get permission to access user location
        String[] PERMISSIONS = {android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        locationPermissionRequest.launch(PERMISSIONS);

        googleMap.setMaxZoomPreference(10.0f);
        googleMap.getUiSettings().setMapToolbarEnabled(false);
        googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(getActivity(), R.raw.basemap));

        updateMap(null, R.id.button_birth_place);


        // center the map on the user's location when the permission is granted
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            googleMap.setMyLocationEnabled(true);
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());
            fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(getActivity(), new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(Location location) {
                            // Got last known location. In some rare situations, this can be null.
                            if (location != null && !isMapCentered) {
                                // Logic to handle location object
                                LatLng userLocation = new LatLng(location.getLatitude(), location.getLongitude());
                                googleMap.moveCamera(CameraUpdateFactory.newLatLng(userLocation));
                                googleMap.animateCamera(CameraUpdateFactory.zoomTo(5.0f));
                                isMapCentered = true;
                            }
                        }
                    });
        }
    }

    public void setFilters(String latitudeColumn, String longitudeColumn) {
        this.latitudeColumn = latitudeColumn;
        this.longitudeColumn = longitudeColumn;

        //set buttonId to the currently selected button
        int buttonId;
        if (latitudeColumn.equals("BirthCityLat") && longitudeColumn.equals("BirthCityLong")) {
            buttonId = R.id.button_birth_place;
        } else if (latitudeColumn.equals("PubCityLat") && longitudeColumn.equals("PubCityLong")) {
            buttonId = R.id.button_pub_city;
        } else if (latitudeColumn.equals("ImpCityLat") && longitudeColumn.equals("ImpCityLong")) {
            buttonId = R.id.button_imp_city;
        } else {
            throw new IllegalArgumentException("Invalid latitude or longitude column: " + latitudeColumn + ", " + longitudeColumn);
        }

        updateMap(null, buttonId);
    }


    public void updateMap(String genre, int buttonId) {
        List<LatLng> locations = fetchLocationsFromDatabase(buttonId, genre);
        googleMap.clear();
        HashMap<LatLng, Integer> locationCountMap = getLocationCountMap(locations);
        addMarkersToMap(locationCountMap, buttonId, googleMap);
        setInfoWindowClickListener(genre);
    }

    private void addMarkersToMap(HashMap<LatLng, Integer> locationCountMap, int buttonId, GoogleMap googleMap) {
        int markerDrawableId;
        for (Map.Entry<LatLng, Integer> entry : locationCountMap.entrySet()) {
            markerDrawableId = getMarkerDrawableId(buttonId);

            // Create a BitmapDescriptor for the marker icon
            BitmapDescriptor markerIcon = BitmapDescriptorFactory.fromResource(markerDrawableId);

            setCustomInfoWindow(googleMap);

            //add marker to map
            googleMap.addMarker(new MarkerOptions().position(entry.getKey()).title("Book count: " + entry.getValue()).icon(markerIcon));
        }
    }

    private void setCustomInfoWindow(GoogleMap googleMap) {
        googleMap.setInfoWindowAdapter(new GoogleMap.InfoWindowAdapter() {
            @Override
            public View getInfoWindow(Marker marker) {

                return null; // Use default info window background
            }

            @Override
            public View getInfoContents(Marker marker) {
                // Inflate custom info window layout
                View infoWindow = getLayoutInflater().inflate(R.layout.custom_info_window, null);

                // Find and set the book count text
                TextView bookCountTextView = infoWindow.findViewById(R.id.book_count_text_view);
                bookCountTextView.setText(marker.getTitle());

                return infoWindow;
            }
        });
    }

    private int getMarkerDrawableId(int buttonId) {
        // Determine which marker icon to use
        if (buttonId == R.id.button_birth_place) {
            return R.drawable.birth_city_marker;
        } else if (buttonId == R.id.button_pub_city) {
            return R.drawable.pub_city_marker;
        } else if (buttonId == R.id.button_imp_city) {
            return R.drawable.imp_city_marker;
        } else {
            throw new IllegalArgumentException("Invalid button id: " + buttonId);
        }
    }

    private List<LatLng> fetchLocationsFromDatabase(int buttonId, String genre) {
        List<LatLng> locations = new ArrayList<>();
        DatabaseHelper dbHelper = new DatabaseHelper(getActivity());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Define a projection that specifies which columns from the database are used
        String[] projection;
        if (buttonId == R.id.button_birth_place) {
            projection = new String[] { "BirthCityLat", "BirthCityLong" };
        } else if (buttonId == R.id.button_pub_city) {
            projection = new String[] { "PubCityLat", "PubCityLong" };
        } else if (buttonId == R.id.button_imp_city) {
            projection = new String[] { "ImpCityLat", "ImpCityLong" };
        } else {
            throw new IllegalArgumentException("Invalid button id: " + buttonId);
        }

        String selection;
        String[] selectionArgs;
        if (genre != null) {
            // Database query: WHERE "latitudeColumn" IS NOT NULL AND "longitudeColumn" IS NOT NULL AND "genre" = 'selectedGenre'
            selection = projection[0] + " IS NOT NULL AND " + projection[1] + " IS NOT NULL AND Genre = ?";
            selectionArgs = new String[] {genre};
        } else {
            // Database query: WHERE "latitudeColumn" IS NOT NULL AND "longitudeColumn" IS NOT NULL
            selection = projection[0] + " IS NOT NULL AND " + projection[1] + " IS NOT NULL";
            selectionArgs = null;
        }

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
        );

        while(cursor.moveToNext()) {
            double lat = cursor.getDouble(cursor.getColumnIndexOrThrow(projection[0]));
            double lng = cursor.getDouble(cursor.getColumnIndexOrThrow(projection[1]));
            locations.add(new LatLng(lat, lng));
        }
        cursor.close();
        db.close();
        return locations;
    }


    //Handle InfoWindow click events
    private void setInfoWindowClickListener(final String genre) {
        googleMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                LatLng position = marker.getPosition();
                //Create an intent to start the BookListActivity
                Intent intent = new Intent(getActivity(), BookListActivity.class);
                //Pass the lat, lng and name of the columns of the clicked marker to the BookListActivity
                intent.putExtra("lat", position.latitude);
                intent.putExtra("lng", position.longitude);
                intent.putExtra("latColumn", latitudeColumn);
                intent.putExtra("lngColumn", longitudeColumn);
                intent.putExtra("genre", genre);
                startActivity(intent);
            }
        });
    }

    private HashMap<LatLng, Integer> getLocationCountMap(List<LatLng> locations) {
        // Create a HashMap to store the location and the number of books at that location
        HashMap<LatLng, Integer> locationCountMap = new HashMap<>();
        for (LatLng location : locations) {
            if (locationCountMap.containsKey(location)) {
                locationCountMap.put(location, locationCountMap.get(location) + 1);
            } else {
                locationCountMap.put(location, 1);
            }
        }
        return locationCountMap;
    }


}