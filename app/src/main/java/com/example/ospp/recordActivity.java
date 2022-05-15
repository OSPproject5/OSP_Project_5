package com.example.ospp;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

public class recordActivity extends AppCompatActivity implements OnMapReadyCallback {
    GoogleMap mMap;
    MarkerOptions markerOptions = new MarkerOptions();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_page);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map); //this 추가? null err
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        mMap = googleMap;
        //
        LatLng SEOUL = new LatLng(37.56, 126.97);
        markerOptions.position(SEOUL);
        markerOptions.title("서울");
        markerOptions.snippet("한국의 수도");

        mMap.addMarker(markerOptions);
        mMap.moveCamera(CameraUpdateFactory.newLatLng(SEOUL));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));
        //
        googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
    }

    /*Button refreshButton = (Button) findViewById(R.id.refresh); // 새로고침
    Button startRecordButton = (Button) findViewById(R.id.startRecord); // 경로 시작
    Button memoButton = (Button) findViewById(R.id.memo); // 메모
    Button cameraButton = (Button) findViewById(R.id.camera); // 카메라*/

}