package com.example.ospp;

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

    static final int PERMISSIONS_REQUEST = 0x00000001;
    private static final int REQUEST_IMAGE_CAPTURE = 672;
    private String imageFilePath;
    private Uri photoUri;

    SensorManager sensorManager;
    Sensor stepCountSensor;
    TextView stepCountView;
    TextView textCalories;
    int currentSteps = 0; //발걸음 수
    float[] results = new float[1]; //발걸음
    double distance = 0; //칼로리 계산할 발걸음
    double calories = 0; //칼로리 계산

    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private Location mCurrentLocation;
    private Location location;
    private LocationManager locationManager;
    private LatLng startLatLng = new LatLng(0, 0); //시작점
    private LatLng endLatLng = new LatLng(0, 0); //끝점
    private LatLng currentPosition; // 실시간 이동용
    private LatLng herePosition; // 메모, 카메라 용
    private boolean walkState = false; //걸음 상태
    private Marker currentMarker; // 현위치 마커
    private Marker memoMarker; // 메모 마커
    private Marker cameraMarker; // 카메라 마커
    private MediaScanner mMediaScanner;
    private static final String TAG = "googlemap_example";

    private Button startRecordButton;
    private Button memoButton;
    private Button cameraButton;
    private Button saveButton;

    private List<Polyline> polylines = new ArrayList<>();
    private List<MarkerOptions> memoMarkerOptions = new ArrayList<>(); //메모 마커 옵션들
    private List<MarkerOptions> cameraMarkerOptions = new ArrayList<>(); //카메라 마커 옵션들

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.record_page);

        startRecordButton = (Button) findViewById(R.id.startRecord); // 경로 기록 시작 or 중지
        memoButton = (Button) findViewById(R.id.memo); // 메모
        cameraButton = (Button) findViewById(R.id.camera); // 카메라
        saveButton = (Button) findViewById(R.id.saveEnd); // 저장 후 종료

        stepCountView = findViewById(R.id.movement); //발걸음
        textCalories = findViewById(R.id.calories); //칼로리

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        stepCountSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);

        OnCheckPermission();

        startRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { //걸음 중지 버튼 누를 때도 상태 바뀜
                changeWalkState(); //걸음 상태 변경
            }
        });

        memoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // 메모 버튼 클릭했을 때
                // 창 뜨고 텍스트 입력
                AlertDialog.Builder alert = new AlertDialog.Builder(recordActivity.this);

                alert.setTitle("메모를 입력하세요");

                final EditText input = new EditText(recordActivity.this);
                alert.setView(input);

                alert.setPositiveButton("저장", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String titleValue = input.getText().toString();

                        // 메모 마커 지도에 띄우기
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

                            // 마커 이미지 메모용으로 따로 바꾸기
                            BitmapDrawable bitmapdraw = (BitmapDrawable)getResources().getDrawable(R.drawable.marker, null);
                            Bitmap b = bitmapdraw.getBitmap();
                            Bitmap smallMarker = Bitmap.createScaledBitmap(b, 100, 100, false);
                            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(smallMarker));

                            memoMarker = mMap.addMarker(markerOptions);

                            // 해당 마커 리스트에 저장
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
            public void onClick(View view) { // 카메라 버튼 눌렀을 때
                // 카메라 촬영 시 커스텀 마커 이미지로 저장
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                if(intent.resolveActivity(getPackageManager())!=null) {
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException e) {
                    }

                    if (photoFile != null) {
                        photoUri = FileProvider.getUriForFile(getApplicationContext(), getPackageName(), photoFile);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
                    }
                }
            }
        });

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) { // 저장 후 종료 버튼 클릭했을 때
                /*
                DB에 저장, 기록함과 연동 구현해야 함
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data); //이거 지워?

        if (requestCode==REQUEST_IMAGE_CAPTURE && requestCode == RESULT_OK){ //이거 0에서 바꿈 전자
            Bitmap bitmap = BitmapFactory.decodeFile(imageFilePath);
            ExifInterface exif = null;

            try{
                exif = new ExifInterface(imageFilePath);
            } catch(IOException e){
                e.printStackTrace();
            }

            int exifOrientation;
            int exifDegree;

            if (exif != null){
                exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                exifDegree = exifOrientationToDegrees(exifOrientation);
            } else{
                exifDegree = 0;
            }

            String result = "";
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HHmmss", Locale.getDefault());
            Date curDate = new Date(System.currentTimeMillis());
            String filename = formatter.format(curDate);

            String strFolderName = Environment.getExternalStoragePublicDirectory(DIRECTORY_PICTURES) + File.separator + "OSPP" + File.separator;
            File file = new File(strFolderName);
            if (!file.exists()) {
                file.mkdirs();
            }

            File f = new File(strFolderName + "/" + filename + ".png");
            result = f.getPath();

            FileOutputStream fOut = null;
            try{
                fOut = new FileOutputStream(f);
            } catch (FileNotFoundException e){
                e.printStackTrace();
                result = "Save Error fOut";
            }

            rotate(bitmap, exifDegree).compress(Bitmap.CompressFormat.PNG, 70, fOut);

            try{
                fOut.flush();
            } catch(IOException e){
                e.printStackTrace();
            }

            try{
                fOut.close();
                mMediaScanner.mediaScanning(strFolderName + "/" + filename + ".png");
            } catch(IOException e){
                e.printStackTrace();
                result = "File close Error";
            }

            // 마커 띄우기
            LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            Location loc;

            // 해당 마커 리스트에 저장
            try {
                loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                herePosition = new LatLng(loc.getLatitude(), loc.getLongitude());
                String markerTitle = getCurrentAddress(herePosition);

                MarkerOptions markerOptions = new MarkerOptions();
                markerOptions.position(herePosition);
                markerOptions.title(markerTitle);

                // 마커 이미지 찍은 사진으로 바꾸기
                Bitmap b = null;
                try{
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P){
                        b = ImageDecoder.decodeBitmap(ImageDecoder.createSource(getContentResolver(), photoUri));
                    }
                    else{
                        b = MediaStore.Images.Media.getBitmap(getContentResolver(), photoUri);
                    }
                } catch(IOException e){
                    e.printStackTrace();
                }

                b = Bitmap.createScaledBitmap(b, 200, 200, false);
                //b = rotate(b,exifDegree);
                markerOptions.icon(BitmapDescriptorFactory.fromBitmap(b));

                cameraMarker = mMap.addMarker(markerOptions);

                // 해당 마커 리스트에 저장
                cameraMarkerOptions.add(markerOptions);

            } catch(SecurityException e){
                e.printStackTrace();
            }
        }
    }

    private File createImageFile() throws IOException{
        String timeStamp = new SimpleDateFormat("yyyyMM_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        imageFilePath = image.getAbsolutePath();
        return image;
    }

    public void OnCheckPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    || ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                Toast.makeText(this, "앱 실행을 위해서는 권한 설정이 필요합니다.", Toast.LENGTH_LONG).show();
            }
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA, Manifest.permission.ACTIVITY_RECOGNITION}, PERMISSIONS_REQUEST);
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
                    loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

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

                try{
                    if(stepCountSensor !=null) {
                        // 센서 속도 설정
                        // * 옵션
                        // - SENSOR_DELAY_NORMAL: 20,000 초 딜레이
                        // - SENSOR_DELAY_UI: 6,000 초 딜레이
                        // - SENSOR_DELAY_GAME: 20,000 초 딜레이
                        // - SENSOR_DELAY_FASTEST: 딜레이 없음
                        //
                        sensorManager.registerListener(this,stepCountSensor,SensorManager.SENSOR_DELAY_FASTEST);
                    }
                }catch (SecurityException e){
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
        Location.distanceBetween(startLatLng.latitude, startLatLng.longitude, endLatLng.latitude, endLatLng.longitude, results); //거리 계산. 결과값은 results[0]에 있음.
        distance = distance + results[0];
        calories = (int)(distance * 0.001 * 41); //소모 칼로리 표시(1km당 41kcal)
        textCalories.setText(String.valueOf(calories));
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(stepCountSensor !=null) {
            // 센서 속도 설정
            // * 옵션
            // - SENSOR_DELAY_NORMAL: 20,000 초 딜레이
            // - SENSOR_DELAY_UI: 6,000 초 딜레이
            // - SENSOR_DELAY_GAME: 20,000 초 딜레이
            // - SENSOR_DELAY_FASTEST: 딜레이 없음
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

    private int exifOrientationToDegrees(int exifOrientation){
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90){
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180){
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    private Bitmap rotate(Bitmap bitmap, float degree){
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public String getCurrentAddress(LatLng latlng) { //지오코더, GPS를 주소로 변환
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        // 걸음 센서 이벤트 발생시
        if(walkState==true){
            if(event.sensor.getType() == Sensor.TYPE_STEP_DETECTOR){

                if(event.values[0]==1.0f){
                    // 센서 이벤트가 발생할때 마다 걸음수 증가
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