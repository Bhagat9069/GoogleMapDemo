package aayaamlabs.com.googlemapdemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.Projection;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

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

import aayaamlabs.com.googlemapdemo.models.ClearableAutoCompleteTextView;
import aayaamlabs.com.googlemapdemo.models.DirectionsJSONParser;
import aayaamlabs.com.googlemapdemo.models.PlaceInfo;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        GoogleMap.OnMarkerDragListener,
        GoogleMap.OnMapLongClickListener,
        View.OnClickListener {
    //Our Map
    private GoogleMap mMap;
    private static final String TAG = "MapActivity";
    private PlaceInfo mPlace;
    private PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;
    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 15f;
    private static final int GOOGLE_API_CLIENT_ID = 0;
    private static final int PLACE_PICKER_REQUEST = 1;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40, -168), new LatLng(71, 136));
    //To store longitude and latitude from map
    private double longitude;
    private double latitude;
    public LatLng departureLat;
    private ArrayList<LatLng> mapLatLng;
    int i = 0;
    private Projection projection;
    //Buttons
//    private ImageButton completeRoute;
    private ImageButton buttonView;
    //widgets

    private ImageView buttonCurrent;
    private ImageView completeRoute;
    private ImageView routeOfTwoPlace;
    //Google ApiClient
    private GoogleApiClient googleApiClient;
    Handler handler;
    Runnable runnable;
    AutoCompleteTextView mSearchText;
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        mapLatLng = new ArrayList<>();
        Log.e("Execution", "onCreate");
        //Initializing googleapi client
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .enableAutoManage(this, GOOGLE_API_CLIENT_ID, this)
                .build();
       mSearchText= findViewById(R.id.input_search);
        //Initializing views and adding onclick listeners
        buttonCurrent = (ImageView) findViewById(R.id.ic_gps);
        routeOfTwoPlace = (ImageView) findViewById(R.id.place_picker);
        ImageView search = (ImageView) findViewById(R.id.ic_magnify);
        completeRoute = (ImageView) findViewById(R.id.place_info);
//        mPlacePicker = (ImageView) findViewById(R.id.place_picker);
//        completeRoute = (ImageButton) findViewById(R.id.completeRoute);
//        buttonCurrent = (ImageButton) findViewById(R.id.buttonCurrent);
        mPlaceAutocompleteAdapter = new PlaceAutocompleteAdapter(this, googleApiClient,
                LAT_LNG_BOUNDS, null);

        mSearchText.setAdapter(mPlaceAutocompleteAdapter);
        mSearchText.setOnItemClickListener(mAutocompleteClickListener);
//        mSearchText.setVisibility(View.INVISIBLE);
routeOfTwoPlace.setOnTouchListener(new View.OnTouchListener() {
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        LatLng latLng = new LatLng((getLocation() == null ? latitude : getLocation().getLatitude()), (getLocation() == null ? longitude : getLocation().getLongitude()));
        if (departureLat==null) {
            Toast.makeText(MapsActivity.this, "Please Select a place", Toast.LENGTH_SHORT).show();
            Log.w("Pick a"," Place");
            return false ;
        }
        else
        {
            // Getting URL to the Google Directions API
            String url = getDirectionsUrl(latLng, departureLat);

            DownloadTask downloadTask = new DownloadTask();

            // Start downloading json data from Google Directions API
            downloadTask.execute(url);
        }
        return false;
    }
});
        search.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {

                if (TextUtils.isEmpty(mSearchText.getText().toString())) {
                    Toast.makeText(MapsActivity.this, "Please write some", Toast.LENGTH_SHORT).show();
                } else {
                    geoLocate();
                    //toggleSearch(false);
                }
                return false;
            }
        });
   /*     mSearchText.setOnClearListener(new ClearableAutoCompleteTextView.OnClearListener() {

            @Override
            public void onClear() {
                toggleSearch(true);
            }
        });*/
     /*   mSearchText.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Log.e("On itemclick",mPlaceAutocompleteAdapter.getItem(i).getPlaceId());
                geoLocate();
            }
        });*/
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                Log.w("actionId=" + actionId + "", "keyEvent=" + keyEvent);
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER) {
                    Log.w("mSearchText", " is complete");
                    //execute our method for searching
                    geoLocate();
                }

                return false;
            }
        });
        buttonView = (ImageButton) findViewById(R.id.buttonView);
        completeRoute.setOnClickListener(this);
        buttonCurrent.setOnClickListener(this);
        buttonView.setOnClickListener(this);

    }

    @Override
    protected void onStart() {
        Log.e("Execution", "onStart");
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onResume() {
        //start handler as activity become visible
        Log.e("Execution", "onResume");
        handler = new Handler(Looper.getMainLooper());

        handler.postDelayed(runnable = new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                i++;
                LatLng latLng = new LatLng((getLocation() == null ? latitude : getLocation().getLatitude()), (getLocation() == null ? longitude : getLocation().getLongitude()));

                if (mapLatLng == null) {
                    mapLatLng = new ArrayList<>();
                    mapLatLng.add(latLng);
                    Log.w("mapLatLng", " is null");
                } else if (mapLatLng.size() < 50) {
                    mapLatLng.add(latLng);
                    Log.w("mapLat", latLng.latitude + "," + latLng.longitude);
                    Toast.makeText(MapsActivity.this, latLng.latitude + "," + latLng.longitude, Toast.LENGTH_SHORT).show();
                } else {
                    mapLatLng.clear();
                    Toast.makeText(MapsActivity.this, "Some Error", Toast.LENGTH_SHORT).show();
                    //Log.w("Some","Error");
                }
                handler.postDelayed(this, 2000);

            }
        }, 2000);

        super.onResume();
    }

    @Override
    protected void onPause() {
        Log.e("Execution", "onPause");
        handler.removeCallbacks(runnable); //stop handler when activity not visible
        super.onPause();
    }

    @Override
    public void onClick(View view) {
       /* switch (view.getId())
        {
            case buttonCurrent.getId():

        }*/
        if (view == buttonCurrent) {
            getCurrentLocation();
            if (mapLatLng != null)
                mapLatLng.clear();
            mMap.clear();
            moveMap();
        } else if (view == completeRoute) {

            PolylineOptions polylineOptions = new PolylineOptions();
            for (LatLng locRecorded : mapLatLng) {
                polylineOptions.add(new LatLng(locRecorded.latitude,
                        locRecorded.longitude));
            }
            polylineOptions.color(Color.parseColor("#CC0000FF"));
            polylineOptions.width(10);
            polylineOptions.visible(true);
            Polyline line = mMap.addPolyline(polylineOptions);

        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        getCurrentLocation();
        Log.e("Execution", "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        //Clearing all the markers
        // mMap.clear();
        if (mapLatLng == null) {
            Log.w("MapArrayList", "Is null");
            mapLatLng = new ArrayList<>();
            mapLatLng.add(latLng);
        } else
            mapLatLng.add(latLng);
        //Adding a new marker to the current pressed position we are also making the draggable true
        mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .draggable(true));

    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        Log.e("Execution", "onMarkerDragStart");

    }

    @Override
    public void onMarkerDrag(Marker marker) {
        Log.e("Execution", "onMarkerDrag");

    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        //Getting the coordinates
        latitude = marker.getPosition().latitude;
        longitude = marker.getPosition().longitude;
        // mMap.clear();
        //Moving the map
        moveMap();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        //Initializing our map
        Log.e("Execution", "onMapReady");
        mMap = googleMap;
        //Creating a random coordinate
        LatLng latLng = new LatLng(0, 0);
        //Adding marker to that coordinate
        mMap.addMarker(new MarkerOptions().position(latLng).draggable(true));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        //Setting onMarkerDragListener to track the marker drag
        mMap.setOnMarkerDragListener(this);
        //Adding a long click listener to the map
        mMap.setOnMapLongClickListener(this);

    }

    //Getting current location
    private void getCurrentLocation() {
        //Creating a location object
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (location != null) {
            //Getting longitude and latitude
            longitude = location.getLongitude();
            latitude = location.getLatitude();

            //moving the map to location
            moveMap();
            mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        }
    }

    public Location getLocation() {
        //Creating a location object
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            Log.w("Permission ", "not granted");
            return null;
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (location != null) {
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            return location;
        } else
            return null;
    }

    //Function to move the map
    private void moveMap() {
        //String to display current latitude and longitude
        String msg = latitude + ", " + longitude;

        //Creating a LatLng Object to store Coordinates
        LatLng latLng = new LatLng(latitude, longitude);
    /*    if (mapLatLng==null)
        {
            Log.w("MapArrayList","Is null");
            mapLatLng=new ArrayList<>();
            mapLatLng.add(latLng);
        }
        else
            mapLatLng.add(latLng);*/
        //Adding marker to map
        mMap.addMarker(new MarkerOptions()
                .position(latLng) //setting position
                .draggable(true) //Making the marker draggable
                .title("Current Location")); //Adding a title

        //Moving the camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));

        //Animating the camera
//        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        //Displaying current coordinates in toast
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    /*class MyOverlay extends Overlay{

        public MyOverlay(){

        }

        public void draw(Canvas canvas, MapView mapv, boolean shadow){
            super.draw(canvas, mapv, shadow);

            Paint   mPaint = new Paint();
            mPaint.setDither(true);
            mPaint.setColor(Color.RED);
            mPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            mPaint.setStrokeJoin(Paint.Join.ROUND);
            mPaint.setStrokeCap(Paint.Cap.ROUND);
            mPaint.setStrokeWidth(2);

            GeoPoint gP1 = new GeoPoint(19240000,-99120000);
            GeoPoint gP2 = new GeoPoint(37423157, -122085008);

            Point p1 = new Point();
            Point p2 = new Point();
            Path path = new Path();

            Projection projection=mapv.getProjection();
            projection.toPixels(gP1, p1);
            projection.toPixels(gP2, p2);

            path.moveTo(p2.x, p2.y);
            path.lineTo(p1.x,p1.y);

            canvas.drawPath(path, mPaint);
        }*/
        /*
        --------------------------- google places API autocomplete suggestions -----------------
     */
    private void geoLocate() {
        Log.d(TAG, "geoLocate: geolocating");

        String searchString = mSearchText.getText().toString();

        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage());
        }

        if (list.size() > 0) {
            Address address = list.get(0);

            Log.d(TAG, "geoLocate: found a location: " + address.toString());
            //Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();
            departureLat=new LatLng(address.getLatitude(), address.getLongitude());
            mMap.addMarker(new MarkerOptions().position(departureLat).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).draggable(true));
            mMap.moveCamera(CameraUpdateFactory.newLatLng(departureLat));
            Log.w("geoLocate",departureLat.toString());
          /*  mMap.moveCamera(, DEFAULT_ZOOM,
                    address.getAddressLine(0));*/
        }
    }

    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            hideSoftKeyboard();

            final AutocompletePrediction item = mPlaceAutocompleteAdapter.getItem(i);
            final String placeId = item.getPlaceId();

            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(googleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };

    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.d(TAG, "onResult: Place query did not complete successfully: " + places.getStatus().toString());
                places.release();
                return;
            }
            final Place place = places.get(0);

            try {
                mPlace = new PlaceInfo();
                mPlace.setName(place.getName().toString());
                Log.d(TAG, "onResult: name: " + place.getName());
                mPlace.setAddress(place.getAddress().toString());
                Log.d(TAG, "onResult: address: " + place.getAddress());
//                mPlace.setAttributions(place.getAttributions().toString());
//                Log.d(TAG, "onResult: attributions: " + place.getAttributions());
                mPlace.setId(place.getId());
                Log.d(TAG, "onResult: id:" + place.getId());
                mPlace.setLatlng(place.getLatLng());
                Log.d(TAG, "onResult: latlng: " + place.getLatLng());
                mPlace.setRating(place.getRating());
                Log.d(TAG, "onResult: rating: " + place.getRating());
                mPlace.setPhoneNumber(place.getPhoneNumber().toString());
                Log.d(TAG, "onResult: phone number: " + place.getPhoneNumber());
                mPlace.setWebsiteUri(place.getWebsiteUri());
                Log.d(TAG, "onResult: website uri: " + place.getWebsiteUri());

                Log.d(TAG, "onResult: place: " + mPlace.toString());
            } catch (NullPointerException e) {
                Log.e(TAG, "onResult: NullPointerException: " + e.getMessage());
            }
            departureLat=new LatLng(place.getViewport().getCenter().latitude,
                    place.getViewport().getCenter().longitude);
            mMap.addMarker(new MarkerOptions().position(departureLat).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)).draggable(true));
Log.w("onResult",departureLat.toString());
            mMap.moveCamera(CameraUpdateFactory.newLatLng(departureLat));
          /*  moveCamera(new LatLng(place.getViewport().getCenter().latitude,
                    place.getViewport().getCenter().longitude), DEFAULT_ZOOM, mPlace);*/

            places.release();
        }
    };

    // this toggles between the visibility of the search icon and the search box
// to show search icon - reset = true
// to show search box - reset = false
    protected void toggleSearch(boolean reset) {
        @SuppressLint("WrongViewCast") ClearableAutoCompleteTextView searchBox = (ClearableAutoCompleteTextView) findViewById(R.id.input_search);
        ImageView searchIcon = (ImageView) findViewById(R.id.ic_magnify);
        if (reset) {
            // hide search box and show search icon
            searchBox.setText("");
            searchBox.setVisibility(View.GONE);
            searchIcon.setVisibility(View.VISIBLE);
            // hide the keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(searchBox.getWindowToken(), 0);
        } else {
            // hide search icon and show search box
            searchIcon.setVisibility(View.GONE);
            searchBox.setVisibility(View.VISIBLE);
            searchBox.requestFocus();
            // show the keyboard
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(searchBox, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    ////////////////Find route /////////////
    private class DownloadTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... url) {

            String data = "";

            try {
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();


            parserTask.execute(result);

        }
    }

    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList();
                lineOptions = new PolylineOptions();

                List<HashMap<String, String>> path = result.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap point = path.get(j);

                    double lat = Double.parseDouble(String.valueOf(point.get("lat")));
                    double lng = Double.parseDouble(String.valueOf(point.get("lng")));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                lineOptions.addAll(points);
                lineOptions.width(12);
                lineOptions.color(Color.RED);
                lineOptions.geodesic(true);

            }

        }
    }
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            urlConnection = (HttpURLConnection) url.openConnection();

            urlConnection.connect();

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
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }
    private String getDirectionsUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        // Sensor enabled
        String sensor = "sensor=false";
        String mode = "mode=driving";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor + "&" + mode;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }
}
