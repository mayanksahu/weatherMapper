package com.example.masah.weathermapper;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

import static com.google.maps.android.PolyUtil.decode;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private static final int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 644;
    private EditText sourceBar;
    private EditText destinationBar;
    private String source = "My Location";
    private String destination;
    private String sourceTitle = "My Location";
    private String destinationTitle;
    private Button searchButton;
    private int padding = 150;
//    private int currentStretchColour;
    private Weather currentWeather;

    private static final String DIRECTION_API_KEY = "AIzaSyCCTo0wejIJxBM-tONImmp7hoL6X0iTyTA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (ContextCompat.checkSelfPermission(getApplicationContext(),
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_FINE_LOCATION);

            }
        }


        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        final Context context = getApplicationContext();

        legendButton = (Button) findViewById(R.id.legend);
        legendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment legendDialog = new LegendDialogFragment();
                legendDialog.show(getSupportFragmentManager(), "Legend");
            }
        });

        searchButton = (Button) findViewById(R.id.search);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!source.isEmpty() && !destination.isEmpty()) {
                    if(source.equals("My Location")) {
                        LocationManager lm = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
                        try {
                            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                            source = Double.toString(location.getLatitude())+","+Double.toString(location.getLongitude());
                        } catch (SecurityException s) {
                            Log.d("Error", "Location needed");
                            System.exit(0);
                        }
                    } else {
                        source = source.replace(' ', '+');
                    }
                    destination = destination.replace(' ','+');
                    String directionsUrl = getDirectionsUrl(source, destination);

                    DownloadTask downloadTask = new DownloadTask();

                    // Start downloading json data from Google Directions API
                    downloadTask.execute(directionsUrl);

                    // Directions and weather code
                }
            }
        });

        final PlaceAutocompleteFragment sourceCompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.source);
        sourceCompleteFragment.setText("My Location");

        sourceCompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i("INFO", "Place: " + place.getName());
                source = place.getAddress().toString();
                sourceTitle = place.getName().toString();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i("INFO", "An error occurred: " + status);
            }
        });

        final PlaceAutocompleteFragment destinationCompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.destination);
        destinationCompleteFragment.setHint("Destination");

        destinationCompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                // TODO: Get info about the selected place.
                Log.i("INFO", "Place: " + place.getName());
                destination = place.getAddress().toString();
                destinationTitle = place.getName().toString();
            }

            @Override
            public void onError(Status status) {
                // TODO: Handle the error.
                Log.i("INFO", "An error occurred: " + status);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.

                    // Obtain the SupportMapFragment and get notified when the map is ready to be used.
                    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                            .findFragmentById(R.id.map);
                    mapFragment.getMapAsync(this);


                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    System.exit(0);
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
//        LatLng sydney = new LatLng(-34, 151);
//        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
//        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        try {
            mMap.setMyLocationEnabled(true);

            LocationManager lm = (LocationManager) getApplicationContext().getSystemService(LOCATION_SERVICE);
            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 16.0f));
        } catch(SecurityException s) {
            Toast.makeText(this, "Needs location permission", Toast.LENGTH_LONG).show();
            System.out.println(s.getMessage());
            System.exit(0);
        }


    }

    public void updateMapView(List<LatLng> positions){
    LatLngBounds.Builder builder = new LatLngBounds.Builder();
    for (LatLng position : positions) {
    builder.include(position);
    }

    LatLngBounds bounds = builder.build();

    CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);

    mMap.animateCamera(cu);
    }

    private String getDirectionsUrl(String source, String destination) {

        // Origin of route
        String str_origin = "origin=" + source;

        // Destination of route
        String str_dest = "destination=" + destination;

        // The key for the direction API.
        String str_key = "key=" + DIRECTION_API_KEY;

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + str_key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        return url;
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";

        HttpURLConnection urlConnection = null;

        InputStream iStream = new InputStream() {
            @Override
            public int read() throws IOException {
                return 0;
            }
        };

        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url.
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url.
            urlConnection.connect();

            // Reading data from url.
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception url", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private String getWeatherURL(LatLng location) {
        final String BASE_FORECAST_URL = "https://api.darksky.net/forecast/";
        final String DARK_SKY_API_KEY = "6edb5cb76cf8ac5be321a139d82076ed";
        String url;
        url = BASE_FORECAST_URL + DARK_SKY_API_KEY;
        url += "/" + location.latitude + "," + location.longitude;
        return url;
    }

    // Fetches data from url passed.
    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            mMap.clear();
            super.onPostExecute(result);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Path path = gson.fromJson(result, Path.class);

            // Invokes the thread for parsing the JSON data
            if(path.routes.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Source and destination need to be connected by road", Toast.LENGTH_LONG).show();
                return;
            }
            List<Step> steps = path.routes.get(0).legs.get(0).steps;
            int timeCovered = 0;
            LatLng startLocation = new LatLng(steps.get(0).start_location.lat, steps.get(0).start_location.lng);
            LatLng endLocation = new LatLng(steps.get(steps.size()-1).end_location.lat, steps.get(steps.size()-1).end_location.lng);
            long totalTimeCovered = System.currentTimeMillis() / 1000;
            List<LatLng> toBeAdded = new ArrayList<LatLng>();
            List<LatLng> tmp = decode(steps.get(0).polyline.points);
            List<LatLng> positions = new ArrayList<LatLng>();
            toBeAdded.add(tmp.get(0));
            int hourCount = 0;
            for(int i = 0; i < steps.size(); i++) {
                List<LatLng> polyLineLatLng = decode(steps.get(i).polyline.points);
                double totalDistance = 0;
                positions.addAll(polyLineLatLng);
                int totalTime = steps.get(i).duration.value;
                for(int j = 1; j < polyLineLatLng.size(); j++) {
                   totalDistance += latLongDistance(polyLineLatLng.get(j - 1), polyLineLatLng.get(j));
                }
                for(int j = 1; j < polyLineLatLng.size(); j++) {
                    toBeAdded.add(polyLineLatLng.get(j));
                    double currDist = latLongDistance(polyLineLatLng.get(j - 1), polyLineLatLng.get(j));
                    double currTime = currDist / totalDistance * totalTime;
                    timeCovered += currTime;
                    totalTimeCovered += currTime;
                    if(timeCovered >= 3600 || (j == polyLineLatLng.size() - 1 && i == steps.size() - 1)) {
                        timeCovered = 0;
                        LatLng start = toBeAdded.get(0);
                        LatLng end = toBeAdded.get(toBeAdded.size() - 1);
                        double midLat = (start.latitude + end.latitude) / 2.0;
                        double midLng = (start.longitude + end.longitude) / 2.0;

                        String weatherURL = getWeatherURL(new LatLng(midLat, midLng));
                        WeatherDownloadTask weatherDownload = new WeatherDownloadTask();
                        weatherDownload.hourCount = hourCount;
                        weatherDownload.toBeAdded = new ArrayList<LatLng>();
                        for(int k = 0 ; k < toBeAdded.size() ; k++){
                            weatherDownload.toBeAdded.add(toBeAdded.get(k));
                        }
//                        getWeatherResponse(weatherURL);
                        weatherDownload.execute(weatherURL);
                        hourCount++;
                        toBeAdded.clear();
                        toBeAdded.add(polyLineLatLng.get(j));

                    }
                }
            }
            updateMapView(positions);
            mMap.addMarker(new MarkerOptions().position(startLocation).title(sourceTitle).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            mMap.addMarker(new MarkerOptions().position(endLocation).title(destinationTitle).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        }
    }

    private void getWeatherResponse(String url) {

        // For storing data from web service
        String data = "";

        try {
            // Fetching the data from web service
            data = downloadUrl(url);

        } catch (Exception e) {
            Log.d("Background Task", e.toString());
        }
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        currentWeather = gson.fromJson(data, Weather.class);

    }


    private class WeatherDownloadTask extends AsyncTask<String, Void, String> {
        private List<LatLng> toBeAdded;
        private int hourCount;
        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);

            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }

            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            currentWeather = gson.fromJson(result, Weather.class);
            String colourItem = currentWeather.hourly.data.get(hourCount).icon;
            System.out.println(hourCount);
            System.out.println(colourItem);


                        int colour=0;

            switch(colourItem) {
                case "clear-day":
                    colour = Color.parseColor("#FFFF33");
                    break;
                case "clear-night":
                    colour = Color.BLACK;
                    break;
                case "rain":
                    colour = Color.BLUE;
                    break;
                case "snow":
                    colour = Color.parseColor("#66FFFF");
                    break;
                case "sleet":
                    colour = Color.RED;
                    break;
                case "wind":
                    colour = Color.parseColor("#330000");
                    break;
                case "fog":
                    colour = Color.parseColor("#660000");
                    break;
                case "cloudy":
                    colour = Color.GRAY;
                    break;
                case "partly-cloudy-day":
                    colour = Color.LTGRAY;
                    break;
                case "partly-cloudy-night":
                    colour = Color.DKGRAY;
                    break;
                default:
                    colour = Color.GREEN;
                    break;
            }


            com.google.android.gms.maps.model.Polyline addPart = mMap.addPolyline(new PolylineOptions().addAll(toBeAdded).color(colour));
        }
    }

    private double latLongDistance(LatLng source, LatLng destination) {
        double sourceLat = (source.latitude) / 180.0 * Math.PI;
        double sourceLng = (source.longitude) / 180.0 * Math.PI;
        double destinationLat = (destination.latitude) / 180.0 * Math.PI;
        double destinationLng = (destination.longitude) / 180.0 * Math.PI;
        double diffLat = (destinationLat - sourceLat) / 2.0;
        double diffLng = (destinationLng - sourceLng) / 2.0;
        double a = Math.sin(diffLat) * Math.sin(diffLat) + Math.cos(sourceLat) * Math.cos(destinationLat) * Math.sin(diffLng) * Math.sin(diffLng);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return c;

    }


}