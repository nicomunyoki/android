package com.androidapplication.www.kioskmode;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.IntentCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Disco_MapsActivity extends AppCompatActivity implements OnMapReadyCallback,GoogleApiClient.ConnectionCallbacks,GoogleApiClient.OnConnectionFailedListener,LocationListener {

    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;

    double x_lat;
    double y_long;
    String idmeter;
    String  idroute;
    String  acc_no;
    String  acc_name;
    String meternumber;
    int status;
    String disco_debtamount;
    String user_pass = "Nick";
    String user_name = "1234";
    DatabaseHelper mydb;
    LatLng latLng;
    LatLng position;
    ArrayList<LatLng> markerPoints;
    ArrayList<LatLng> points = null;
    PolylineOptions lineOptions = null;
    Polyline polylineFinal = null;
    EditText searchView;
    String str_searchView;
    String searchView_value;
    Button next;
    String accountno;
    String landref_no;



    @Override
    protected void onCreate(Bundle savedInstanceState)    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.disco_activity_maps);
        getSupportActionBar().setTitle("Disconnection Google Map");
        mydb = new DatabaseHelper(this);
        markerPoints = new ArrayList<LatLng>();
        searchView = (EditText) findViewById(R.id.SearchView);
        next = (Button) findViewById(R.id.next);

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        new MarkerTask().execute();
        mapFrag.getMapAsync(this);



        //provide user with choice of changing the google map type
        RadioGroup rgViews = (RadioGroup) findViewById(R.id.rg_views);
        rgViews.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.rb_normal){
                    mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                }else if(checkedId == R.id.rb_satellite){
                    mGoogleMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                }else if(checkedId == R.id.rb_terrain){
                    mGoogleMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                }
            }
        });


        next.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                str_searchView = searchView.getText().toString();
                if (TextUtils.isEmpty(str_searchView)) {
                    searchView.setError("You did not enter a search value");
                    return;
                }else {
                    Intent intent = new Intent(Disco_MapsActivity.this, Disco_MapsActivity_Search.class);
                    intent.putExtra("stsearchView ", str_searchView);
                    startActivity(intent);

                }
            }
        });



        //throw excemption if it occures
        //Thread.setDefaultUncaughtExceptionHandler(new MyExceptionHandler(this));

    }




    @Override
    public void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }

    protected synchronized void buildGoogleApiClient() { mGoogleApiClient = new GoogleApiClient.Builder(this)
            .addConnectionCallbacks(this)
            .addOnConnectionFailedListener(this)
            .addApi(LocationServices.API)
            .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(3000);
        mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {}

    @Override
    public void onLocationChanged(Location location)
    {
        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //Place current location marker
        latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.snippet("").title(String.valueOf(latLng));
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);

        //move map camera
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }
    }




    @Override
    public void onMapReady(GoogleMap googleMap){
        mGoogleMap=googleMap;
        //mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mGoogleMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            mGoogleMap.setMyLocationEnabled(true);
        }



        mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {


            @Override
            public boolean onMarkerClick(Marker marker) {


                if(marker.isInfoWindowShown()) {
                    marker.hideInfoWindow();
                } else {
                    marker.showInfoWindow();
                }


                if(polylineFinal !=null){
                    polylineFinal.remove();
                    markerPoints.clear();
                }else {

                }

                LatLng point_home = latLng;
                if(latLng!=null) {
                    markerPoints.add(point_home);
                }else {

                }

                //Toast.makeText(getApplicationContext(),"Current position"+point_home,Toast.LENGTH_LONG).show();

                LatLng point = marker.getPosition();


                markerPoints.add(point);

                //Toast.makeText(getApplicationContext(),"how many"+point,Toast.LENGTH_LONG).show();

                // this is for decorating the markers thus the name Maker_options

                MarkerOptions options = new MarkerOptions();

                options.position(point);
//                if (markerPoints.size() == 1) {
//                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN));
//
//                } else if (markerPoints.size() == 2) {
//                    options.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
//                }
                // Toast.makeText(getApplicationContext(),"how many "+markerPoints.size()+"\n"+markerPoints.get(0)+"\n"+latLng,Toast.LENGTH_LONG).show();


                // check if the marker point count is greater than 2

                if (markerPoints.size() >= 2) {
                    LatLng origin = markerPoints.get(0);
                    LatLng dest = markerPoints.get(1);
                    String url = getDirectionsUrl(origin, dest);

                    DownloadTask downloadTask = new DownloadTask();
                    downloadTask.execute(url);
                }else {


                }
                return true;
            }


        });


        // Set a listener for info window events.
        mGoogleMap.setOnInfoWindowClickListener(MyOnInfoWindowClickListener);

    }


    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;

        // Output format
        String output = "json";

        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;

        return url;
    }




    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
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
            Log.d("Exception while", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }


    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    public boolean checkLocationPermission(){
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {



                //Prompt the user once explanation has been shown
                //(just doing it here for now, note that with this code, no explanation is shown)
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mGoogleMap.setMyLocationEnabled(true);
                    }
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }




    // Fetches data from url passed
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
            super.onPostExecute(result);

            if (result!=null) {

                ParserTask parserTask = new ParserTask();
                // Invokes the thread for parsing the JSON data
                parserTask.execute(result);

            }else {


            }

        }
    }





    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;


            try {

                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data

                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            MarkerOptions markerOptions = new MarkerOptions();
            if (result != null) {
                // Traversing through all the routes
                for (int i = 0; i < result.size(); i++) {
                    points = new ArrayList<LatLng>();
                    lineOptions = new PolylineOptions();

                    // Fetching i-th route
                    List<HashMap<String, String>> path = result.get(i);

                    // Fetching all the points in i-th route
                    for (int j = 0; j < path.size(); j++) {
                        HashMap<String, String> point = path.get(j);

                        double lat = Double.parseDouble(point.get("lat"));
                        double lng = Double.parseDouble(point.get("lng"));
                        position = new LatLng(lat, lng);

                        points.add(position);
                    }

                    // Adding all the points in the route to LineOptions
                    lineOptions.addAll(points);
                    lineOptions.width(8);
                    lineOptions.color(Color.BLUE);

                }

                // Drawing polyline in the Google Map for the i-th route
                polylineFinal = mGoogleMap.addPolyline(lineOptions);

            } else {


            }

        }
    }



    private class MarkerTask extends AsyncTask<Void, Void, String> {
        private static final String LOG_TAG = "ExampleApp";
        String login_url = "";

        // Invoked by execute() method of this object
        @Override
        protected String doInBackground(Void... args) {
            HttpURLConnection conn = null;
            final StringBuilder json = new StringBuilder();
            try {
                // Connect to the web service
                URL url = new URL(login_url);
                conn = (HttpURLConnection) url.openConnection();
                InputStreamReader in = new InputStreamReader(conn.getInputStream());

                // Read the JSON data into the StringBuilder
                int read;
                char[] buff = new char[1024];
                while ((read = in.read(buff)) != -1) {
                    json.append(buff, 0, read);
                }
            } catch (IOException e) {
                Log.e(LOG_TAG, "Error connecting to service", e);
                //throw new IOException("Error connecting to service", e); //uncaught
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
            return json.toString();
        }

        // Executed after the complete execution of doInBackground() method
        @Override
        protected void onPostExecute(String result) {

            try {

                //get the cordinates of the book
                Cursor cursor = mydb.query_disco_coordinates_to_show();

                if (cursor.moveToFirst()) {

                    do {
                        x_lat = cursor.getDouble(0);
                        y_long = cursor.getDouble(1);
                        idmeter = cursor.getString(2);
                        idroute = cursor.getString(3);
                        acc_no = cursor.getString(4);
                        acc_name = cursor.getString(5);
                        meternumber = cursor.getString(6);
                        status = cursor.getInt(7);
                        disco_debtamount= cursor.getString(8);
                        landref_no=cursor.getString(9);

                        LatLng MAP_PUPI = new LatLng(x_lat, y_long);
                        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(MAP_PUPI, 15));
                        mGoogleMap.animateCamera(CameraUpdateFactory.zoomTo(15), 3000, null);


                        if (status == 0) {



                            Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(x_lat, y_long)).snippet(acc_no).title("Acc No:"+acc_no+";"+"\t"+"LR No:"+landref_no).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));


                        } else if (status == 1) {

                            Marker marker = mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(x_lat, y_long)).snippet("").title("Acc No:"+acc_no+";"+"\t"+"LR No:"+landref_no).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));



                        } else {

                            //do nothing
                        }

                    } while (cursor.moveToNext());
                }

            } catch (SQLiteException se) {

                Log.e(getClass().getSimpleName(), "Open the database");
            }

        }

    }



    GoogleMap.OnInfoWindowClickListener MyOnInfoWindowClickListener
            = new GoogleMap.OnInfoWindowClickListener(){
        @Override
        public void onInfoWindowClick(Marker marker) {

                    accountno = marker.getSnippet();

                    if (accountno.equals("")) {

                        return;

                    } else {

                        Intent intent = new Intent().setClass(getApplicationContext(),
                                Disco_ServiceMeters.class);

                        intent.putExtra("account_no", accountno);
                        startActivity(intent);
                    }
        }
    };



    @Override
    public void onRestart(){
        super.onRestart();
        finish();
        startActivity(getIntent());
    }




    @Override
    public void onBackPressed() {

        //return nothing bro

    }



    public void logout(View view) {
        Intent intent = new Intent(Disco_MapsActivity.this,Device_register.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();

    }

    public void back(View view) {

        Intent intent = new Intent(Disco_MapsActivity.this,Disco_MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | IntentCompat.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

