package com.example.ospp;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Button;
import android.Manifest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import android.graphics.Color;
import androidx.annotation.NonNull;
import android.util.Log;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.maps.CameraUpdate;
import android.content.Context;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import android.content.Intent;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import android.location.Location;
import android.location.LocationManager;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.Marker;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import android.location.Geocoder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Locale;
import java.io.IOException;
import android.location.Address;
import androidx.core.content.FileProvider;
import android.net.Uri;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import android.os.Build;
import static android.os.Environment.DIRECTORY_PICTURES;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import androidx.core.content.ContextCompat;

public class recordActivity extends AppCompatActivity implements OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback, SensorEventListener{
    GoogleMap mMap;
    ActivityResultLauncher<Intent> activityResultLauncher;
    ImageView iv_photo;

    static final int PERMISSIONS_REQUEST = 0x00000001;
    private static final int REQUEST_IMAGE_CAPTURE = 672;
    private String imageFilePath;
    private Uri photoUri;

    SensorManager sensorManager;
    Sensor stepCountSensor;
    TextView stepCountView;
    TextView textCalories;
    int currentSteps = 0; //????????? ???
    float[] results = new float[1]; //?????????
    double distance = 0; //????????? ????????? ?????????
    double calories = 0; //????????? ??????

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private Location mCurrentLocation;
    private Location location;
    private LocationManager locationManager;
    private LatLng startLatLng = new LatLng(0, 0); //?????????
    private LatLng endLatLng = new LatLng(0, 0); //??????
    private LatLng currentPosition; // ????????? ?????????
    private LatLng herePosition; // ??????, ????????? ???
    private boolean walkState = false; //?????? ??????
    private Marker currentMarker; // ????????? ??????
    private Marker memoMarker; // ?????? ??????
    private Marker cameraMarker; // ????????? ??????
    private MediaScanner mMediaScanner;
    private static final String TAG = "googlemap_example";

    private Button startRecordButton;
    private Button memoButton;
    private Button cameraButton;
    private Button saveButton;

    private List<Polyline> polylines = new ArrayList<>();
    private List<MarkerOptions> memoMarkerOptions = new ArrayList<>(); //?????? ?????? ?????????
    private List<MarkerOptions> cameraMarkerOptions = new ArrayList<>(); //????????? ?????? ?????????

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_page);

        startRecordButton = (Button) findViewById(R.id.startRecord); // ?????? ?????? ?????? or ??????
        memoButton = (Button) findViewById(R.id.memo); // ??????
        cameraButton = (Button) findViewById(R.id.camera); // ?????????
        saveButton = (Button) findViewById(R.id.saveEnd); // ?????? ??? ??????

        stepCountView = findViewById(R.id.movement); //?????????
        textCalories = findViewById(R.id.calories); //?????????

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        OnCheckPermission();

        activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                // ?????? ?????????
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                Location loc;

                /*
                if(result.getResultCode() == RESULT_OK && result.getData() != null){
                    Bundle bundle = result.getData().getExtras();
                    Bitmap bitmap = (Bitmap) bundle.get("data");
                    iv_photo.setImageBitmap(bitmap);
                }
                */

                // ?????? ?????? ???????????? ??????
                try {
                    loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    herePosition = new LatLng(loc.getLatitude(), loc.getLongitude());
                    String markerTitle = getCurrentAddress(herePosition);

                    MarkerOptions markerOptions = new MarkerOptions();
                    markerOptions.position(herePosition);
                    markerOptions.title(markerTitle);


                    // ?????? ????????? ?????? ???????????? ?????????
                    if(result.getResultCode() == RESULT_OK && result.getData() != null){
                        Bundle bundle = result.getData().getExtras();
                        Bitmap bitmap = (Bitmap) bundle.get("data");
                        bitmap = Bitmap.createScaledBitmap(bitmap, 100, 100, false);
                        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(bitmap));
                    }
                    cameraMarker = mMap.addMarker(markerOptions);

                    // ?????? ?????? ???????????? ??????
                    cameraMarkerOptions.add(markerOptions);

                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        });

        startRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { //?????? ?????? ?????? ?????? ?????? ?????? ??????
                changeWalkState(); //?????? ?????? ??????
            }
        });

        memoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // ?????? ?????? ???????????? ???
                // ??? ?????? ????????? ??????
                AlertDialog.Builder alert = new AlertDialog.Builder(recordActivity.this);

                alert.setTitle("????????? ???????????????");

                final EditText input = new EditText(recordActivity.this);
                alert.setView(input);

                alert.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String titleValue = input.getText().toString();

                        // ?????? ?????? ????????? ?????????
                        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                        Location loc;

                        try {
                            loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                            loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                            herePosition = new LatLng(loc.getLatitude(), loc.getLongitude());
                            String markerTitle = getCurrentAddress(herePosition);

                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(herePosition);
                            markerOptions.title(markerTitle);
                            markerOptions.snippet(titleValue);

                            // ?????? ????????? ??????????????? ?????? ?????????
                            BitmapDrawable bitmapdraw = (BitmapDrawable)getResources().getDrawable(R.drawable.marker, null);
                            Bitmap b = bitmapdraw.getBitmap();
                            Bitmap smallMarker = Bitmap.createScaledBitmap(b, 100, 100, false);
                            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(smallMarker));

                            memoMarker = mMap.addMarker(markerOptions);

                            // ?????? ?????? ???????????? ??????
                            memoMarkerOptions.add(markerOptions);

                        } catch(SecurityException e){
                            e.printStackTrace();
                        }
                    }
                });
                alert.show();
            }
        });

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // ????????? ?????? ????????? ???
                // ????????? ?????? ??? ????????? ?????? ???????????? ??????
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                activityResultLauncher.launch(intent);
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // ?????? ??? ?????? ?????? ???????????? ???
                /*
                DB??? ??????, ???????????? ?????? ???????????? ???
                */
                finish();
            }
        });

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        try {
            this.locationRequest = LocationRequest.create().setInterval(1000).setFastestInterval(500).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        } catch(SecurityException e){
            e.printStackTrace();
        }

        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
    }

    public void OnCheckPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                Toast.makeText(this, "??? ????????? ???????????? ?????? ????????? ???????????????.", Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.ACTIVITY_RECOGNITION}, PERMISSIONS_REQUEST);
        }
    }

    private void changeWalkState() {
        if (!walkState) {
            try {
                Toast.makeText(getApplicationContext(), "?????? ??????", Toast.LENGTH_SHORT).show();
                walkState = true;

                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                Location loc;
                try {
                    loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    currentPosition = new LatLng(loc.getLatitude(), loc.getLongitude());
                    startLatLng = currentPosition; //??????, ?????? ?????? ???????????? ??????
                    String markerTitle = getCurrentAddress(currentPosition);
                    String markerSnippet = "?????????";
                    Log.d(TAG, "onLocationResult : " + markerSnippet);
                    setCurrentLocation(loc, markerTitle, markerSnippet);
                    mCurrentLocation = loc;
                } catch(SecurityException e){
                    e.printStackTrace();
                }

                try{
                    if(stepCountSensor !=null) {
                        // ?????? ?????? ??????
                        // * ??????
                        // - SENSOR_DELAY_NORMAL: 20,000 ??? ?????????
                        // - SENSOR_DELAY_UI: 6,000 ??? ?????????
                        // - SENSOR_DELAY_GAME: 20,000 ??? ?????????
                        // - SENSOR_DELAY_FASTEST: ????????? ??????
                        //
                        sensorManager.registerListener(this,stepCountSensor,SensorManager.SENSOR_DELAY_FASTEST);
                    }
                }catch (SecurityException e){
                    e.printStackTrace();
                }

                mMap.moveCamera(CameraUpdateFactory.newLatLng(startLatLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

                startRecordButton.setText("?????? ??????");
            } catch(SecurityException e){
                e.printStackTrace();
            }

        } else {
            Toast.makeText(getApplicationContext(), "?????? ??????", Toast.LENGTH_SHORT).show();
            walkState = false;
            startRecordButton.setText("?????? ??????");
        }
    }

    private void drawPath() {
        PolylineOptions options = new PolylineOptions().add(startLatLng).add(endLatLng).width(15).color(Color.RED).geodesic(true);
        polylines.add(mMap.addPolyline(options));
        Location.distanceBetween(startLatLng.latitude, startLatLng.longitude, endLatLng.latitude, endLatLng.longitude, results); //?????? ??????. ???????????? results[0]??? ??????.
        distance = distance + results[0];
        calories = (int)(distance * 0.001 * 41); //?????? ????????? ??????(1km??? 41kcal)
        textCalories.setText(String.valueOf(calories));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(stepCountSensor !=null) {
            // ?????? ?????? ??????
            // * ??????
            // - SENSOR_DELAY_NORMAL: 20,000 ??? ?????????
            // - SENSOR_DELAY_UI: 6,000 ??? ?????????
            // - SENSOR_DELAY_GAME: 20,000 ??? ?????????
            // - SENSOR_DELAY_FASTEST: ????????? ??????
            //
            sensorManager.registerListener(this,stepCountSensor,SensorManager.SENSOR_DELAY_FASTEST);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } catch(SecurityException e){
            e.printStackTrace();
        }
    }

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);

            List<Location> locationList = locationResult.getLocations();

            if (locationList.size() > 0) {
                location = locationList.get(locationList.size() - 1);

                Location loc;
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                try {
                    loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    currentPosition = new LatLng(loc.getLatitude(), loc.getLongitude());
                    String markerTitle = getCurrentAddress(currentPosition);
                    String markerSnippet = "?????????";
                    Log.d(TAG, "onLocationResult : " + markerSnippet);
                    setCurrentLocation(loc, markerTitle, markerSnippet);
                    mCurrentLocation = loc;
                } catch(SecurityException e){
                    e.printStackTrace();
                }
            }
        }
    };

    public void setCurrentLocation(Location loc, String markerTitle, String markerSnippet) {
        double latitude = loc.getLatitude(), longitude = loc.getLongitude();
        if (currentMarker != null) currentMarker.remove();

        MarkerOptions markerOptions = new MarkerOptions();
        LatLng currentLatLng = new LatLng(latitude, longitude);
        markerOptions.position(currentLatLng);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);

        currentMarker = mMap.addMarker(markerOptions);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng);
        mMap.moveCamera(cameraUpdate);
        if (walkState){
            endLatLng = new LatLng(latitude, longitude);
            drawPath();
            startLatLng = new LatLng(latitude, longitude);
        }
    }

    public String getCurrentAddress(LatLng latlng) { //????????????, GPS??? ????????? ??????
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(
                    latlng.latitude,
                    latlng.longitude,
                    1);
        } catch (IOException ioException) {
            //???????????? ??????
            Toast.makeText(this, "???????????? ????????? ????????????", Toast.LENGTH_LONG).show();
            return "???????????? ????????? ????????????";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "????????? GPS ??????", Toast.LENGTH_LONG).show();
            return "????????? GPS ??????";
        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "?????? ?????????", Toast.LENGTH_LONG).show();
            return "?????? ?????????";

        } else {
            Address address = addresses.get(0);
            return address.getAddressLine(0).toString();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // ?????? ?????? ????????? ?????????
        if(walkState==true){
            if(event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR){

                if(event.values[0]==1.0f){
                    // ?????? ???????????? ???????????? ?????? ????????? ??????
                    currentSteps++;
                    stepCountView.setText(String.valueOf(currentSteps));
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}