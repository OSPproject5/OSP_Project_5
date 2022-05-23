package com.example.ospp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.widget.Button;
import android.Manifest;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
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

import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import android.location.Location;
import android.location.LocationManager;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.Marker;
import android.view.View;
import android.widget.Toast;
import android.location.Geocoder;
import java.util.Locale;
import java.io.IOException;
import android.location.Address;

import java.util.ArrayList;
import java.util.List;
import android.widget.TextView;

public class recordActivity extends AppCompatActivity implements OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback{
    GoogleMap mMap;
    MarkerOptions markerOptions = new MarkerOptions();

    static final int PERMISSIONS_REQUEST = 0x00000001;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private Location mCurrentLocation;
    private Location location;
    private boolean mPermissionDenied;
    private LocationManager locationManager;
    private LatLng startLatLng = new LatLng(0, 0); //시작점
    private LatLng endLatLng = new LatLng(0, 0); //끝점
    private LatLng currentPosition;
    private boolean walkState = false; //걸음 상태
    private Marker currentMarker; // 현위치 마커
    private static final String TAG = "googlemap_example";

    private Button startRecordButton;
    private Button memoButton;
    private Button cameraButton;

    private List<Polyline> polylines = new ArrayList();
    private TextView textCalories; // 소모 칼로리 텍스트 뷰

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_page);

        startRecordButton = (Button) findViewById(R.id.startRecord); // 경로 기록 시작 or 중지
        memoButton = (Button) findViewById(R.id.memo); // 메모
        cameraButton = (Button) findViewById(R.id.camera); // 카메라

        OnCheckPermission();

        startRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { //걸음 중지 버튼 누를 때도 상태 바뀜
                changeWalkState();        //걸음 상태 변경
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

    public void OnCheckPermission(){
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)){
                Toast.makeText(this, "앱 실행을 위해서는 권한 설정이 필요합니다.", Toast.LENGTH_LONG).show();
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST);
            } else{
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSIONS_REQUEST);
            }
        }
    }

    private void changeWalkState() {
        if (!walkState) {
            try {
                Toast.makeText(getApplicationContext(), "걸음 시작", Toast.LENGTH_SHORT).show();
                walkState = true;

                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                Location loc;
                try {
                    loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                    currentPosition = new LatLng(loc.getLatitude(), loc.getLongitude());
                    startLatLng = currentPosition; //추가, 이제 바다 한가운데 아님
                    String markerTitle = getCurrentAddress(currentPosition);
                    String markerSnippet = "현위치";
                    Log.d(TAG, "onLocationResult : " + markerSnippet);
                    setCurrentLocation(loc, markerTitle, markerSnippet);
                    mCurrentLocation = loc;
                } catch(SecurityException e){
                    e.printStackTrace();
                }

                mMap.moveCamera(CameraUpdateFactory.newLatLng(startLatLng));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

                startRecordButton.setText("기록 중지");
            } catch(SecurityException e){
                e.printStackTrace();
            }

        } else {
            Toast.makeText(getApplicationContext(), "걸음 종료", Toast.LENGTH_SHORT).show();
            walkState = false;
            startRecordButton.setText("기록 시작");
        }
    }

    private void drawPath() {
        PolylineOptions options = new PolylineOptions().add(startLatLng).add(endLatLng).width(15).color(Color.RED).geodesic(true);
        polylines.add(mMap.addPolyline(options));
    }

    @Override
    protected void onStart() {
        super.onStart();
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

                //LatLng currentPosition = new LatLng(location.getLatitude(), location.getLongitude());

                Location loc;
                LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                try {
                    loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                    currentPosition = new LatLng(loc.getLatitude(), loc.getLongitude());
                    String markerTitle = getCurrentAddress(currentPosition);
                    String markerSnippet = "현위치";
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

        LatLng currentLatLng = new LatLng(latitude, longitude);
        markerOptions.position(currentLatLng);
        markerOptions.title(markerTitle);
        markerOptions.snippet(markerSnippet);
        markerOptions.draggable(true);

        currentMarker = mMap.addMarker(markerOptions);

        CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLng(currentLatLng);
        mMap.moveCamera(cameraUpdate);
        if (walkState){
            endLatLng = new LatLng(latitude, longitude);
            drawPath();
            startLatLng = new LatLng(latitude, longitude);
        }
    }

    public String getCurrentAddress(LatLng latlng) { //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(
                    latlng.latitude,
                    latlng.longitude,
                    1);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";
        }

        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

        } else {
            Address address = addresses.get(0);
            return address.getAddressLine(0).toString();
        }
    }
}