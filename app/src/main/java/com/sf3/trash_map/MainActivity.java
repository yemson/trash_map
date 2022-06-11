package com.sf3.trash_map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MainActivity";

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private FusedLocationSource mLocationSource;
    private NaverMap mNaverMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // [START initialize_auth]
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]

        // 지도 객체 생성
        FragmentManager fm = getSupportFragmentManager();
        MapFragment mapFragment = (MapFragment)fm.findFragmentById(R.id.map_fragment);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            fm.beginTransaction().add(R.id.map_fragment, mapFragment).commit();
        }

        // getMapAsync를 호출하여 비동기로 onMapReady 콜백 메서드 호출
        // onMapReady에서 NaverMap 객체를 받음
        mapFragment.getMapAsync(this);

        // 위치를 반환하는 구현체인 FusedLocationSource 생성
        mLocationSource =
                new FusedLocationSource(this, PERMISSION_REQUEST_CODE);

        FloatingActionButton trackingPosition = findViewById(R.id.trackingPosition);
        trackingPosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            }
        });
    }

    @Override
    public void onMapReady(@NonNull NaverMap naverMap) {
        Log.d( TAG, "onMapReady");

        //지도상에 마커 표시
        Marker marker = new Marker();
        marker.setPosition(new LatLng(naverMap.getCameraPosition().target.latitude, naverMap.getCameraPosition().target.longitude));
        marker.setIcon(OverlayImage.fromResource(R.drawable.ic_baseline_push_pin_24));
        marker.setMap(naverMap);

        TextView makerPosition = (TextView) findViewById(R.id.markerPosition);
        Button addTrashCan = (Button) findViewById(R.id.addTrashCan);

        naverMap.addOnCameraChangeListener(new NaverMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(int i, boolean b) {
                marker.setPosition(new LatLng(naverMap.getCameraPosition().target.latitude, naverMap.getCameraPosition().target.longitude));
                makerPosition.setText("위치 이동 중");
            }
        });

        naverMap.addOnCameraIdleListener(new NaverMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                marker.setPosition(new LatLng(naverMap.getCameraPosition().target.latitude, naverMap.getCameraPosition().target.longitude));
                makerPosition.setText(getAddress(naverMap.getCameraPosition().target.latitude, naverMap.getCameraPosition().target.longitude));
            }
        });

        addTrashCan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("navermap", new LatLng(naverMap.getCameraPosition().target.latitude, naverMap.getCameraPosition().target.longitude).toString());
            }
        });

//        marker.position = LatLng(
//                naverMap.cameraPosition.target.latitude,
//                naverMap.cameraPosition.target.longitude
//        )
//        marker.icon = OverlayImage.fromResource(R.drawable.ic_location_enroll)
//        marker.map = naverMap

        // NaverMap 객체 받아서 NaverMap 객체에 위치 소스 지정
        mNaverMap = naverMap;
        mNaverMap.setLocationSource(mLocationSource);

        Log.d("navermap", mLocationSource.toString());

        UiSettings uiSettings = mNaverMap.getUiSettings();
        uiSettings.setZoomControlEnabled(true);

        // 권한확인. 결과는 onRequestPermissionsResult 콜백 매서드 호출
        ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // request code와 권한획득 여부 확인
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mNaverMap.setLocationTrackingMode(LocationTrackingMode.Follow);
            }
        }
    }

    // [START on_start_check_user]
    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        Log.d("firebase", currentUser.getEmail());
        if(currentUser != null){
            //reload();
        }
    }
    // [END on_start_check_user]

//    private String getAddress(Double lat, Double lng) {
//        Geocoder geoCoder = new Geocoder(getBaseContext(), Locale.KOREA);
//        List<Address> address;
//        String addressResult = "주소를 가져 올 수 없습니다.";
//        try {
//            address = geoCoder.getFromLocation(lat, lng, 1) as
//        }
//    }
    private final String getAddress(double lat, double lng) {
        Geocoder geoCoder = new Geocoder(getBaseContext(), Locale.KOREA);
        ArrayList address = null;
        String addressResult = "주소를 가져 올 수 없습니다.";

        try {
            List var10000 = geoCoder.getFromLocation(lat, lng, 1);
            if (var10000 == null) {
                throw new NullPointerException("null cannot be cast to non-null type kotlin.collections.ArrayList<android.location.Address> /* = java.util.ArrayList<android.location.Address> */");
            }

            address = (ArrayList)var10000;
            if (address.size() > 0) {
                String currentLocationAddress = ((Address)address.get(0)).getAddressLine(0).toString();
                addressResult = currentLocationAddress;
            }
        } catch (IOException var9) {
            var9.printStackTrace();
        }

        return addressResult;
    }
}