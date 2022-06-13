package com.sf3.trash_map;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentManager;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.protobuf.Type;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.LocationTrackingMode;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.UiSettings;;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.overlay.OverlayImage;
import com.naver.maps.map.util.FusedLocationSource;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import kotlin.collections.MapsKt;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    private static ArrayList<Map> mArrayList = new ArrayList<>();
    private static ArrayList<Marker> markerList = new ArrayList<>();
    private static ArrayList<String> markerIDList = new ArrayList<>();

    private static final String TAG = "MainActivity";

    // [START declare_auth]
    private FirebaseAuth mAuth;
    // [END declare_auth]

    private Dialog dialog;

    private FirebaseFirestore mFirestore;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private FusedLocationSource mLocationSource;
    private NaverMap mNaverMap;

    private String markerID_n;

    private Integer likeCountNum;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // [START initialize_auth]
        // Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();
        // [END initialize_auth]

        // 다이얼로그 밖 화면 흐리게
        WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
        layoutParams.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        layoutParams.dimAmount = 0.8f;
        getWindow().setAttributes(layoutParams);

        // 다이얼로그
        dialog = new Dialog(MainActivity.this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.activity_dialog);

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

        FloatingActionButton refreshButton = findViewById(R.id.refreshBtn);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for (int i = 0; i < markerList.size(); i++) {
                    markerList.get(i).setMap(null);
                }
                getMarkerData();
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

                FirebaseUser currentUser = mAuth.getCurrentUser();
                FirebaseFirestore db = mFirestore.getInstance();

                Log.d("firebase", currentUser.getEmail());

                Map<String, Object> docData = new HashMap<>();
                docData.put("user", currentUser.getUid());
                docData.put("email", currentUser.getEmail());
                docData.put("lat", naverMap.getCameraPosition().target.latitude);
                docData.put("lng", naverMap.getCameraPosition().target.longitude);

                db.collection("marker")
                        .add(docData)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d(TAG, "DocumentSnapshot added with ID: " + documentReference.getId());
                                markerID_n = documentReference.getId();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error adding document", e);
                            }
                        });

                Marker marker = new Marker();
                marker.setPosition(new LatLng(naverMap.getCameraPosition().target.latitude, naverMap.getCameraPosition().target.longitude));
                marker.setIcon(OverlayImage.fromResource(R.drawable.ic_baseline_push_pin_24));
                marker.setMap(mNaverMap);
                marker.setOnClickListener(overlay -> {
                    //Toast.makeText(MainActivity.this, currentUser.getUid(), Toast.LENGTH_SHORT).show();
                    showDialog(markerID_n);
                    return true;
                });
                markerList.add(marker);
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
        uiSettings.setZoomControlEnabled(false);
        uiSettings.setCompassEnabled(false);
        uiSettings.setScaleBarEnabled(false);
        uiSettings.setLogoGravity(5);
        uiSettings.setLogoMargin(0, 10, 10, 0);

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
        getMarkerData();
    }
    // [END on_start_check_user]

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

    private void getMarkerData() {
        FirebaseFirestore db = mFirestore.getInstance();
        db.collection("marker")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        // Log.d(TAG, "DocumentSnapshot added with ID: " + task.getResult());
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            Log.d(TAG, document.getId() + " -> " + document.getData());
                            mArrayList.add(document.getData());
                            markerIDList.add(document.getId());
                        }

                        for (int i = 0; i < mArrayList.size(); i++) {
                            Marker marker = new Marker();
                            marker.setPosition(new LatLng((Double) mArrayList.get(i).get("lat"), (Double) mArrayList.get(i).get("lng")));
                            marker.setIcon(OverlayImage.fromResource(R.drawable.ic_baseline_push_pin_24));
                            marker.setMap(mNaverMap);
                            int finalI = i;
                            marker.setOnClickListener(overlay -> {
                                //Toast.makeText(MainActivity.this, mArrayList.get(finalI).get("user").toString(), Toast.LENGTH_SHORT).show();
                                showDialog(markerIDList.get(finalI).toString());
                                return true;
                            });
                            markerList.add(marker);
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Error adding document", e);
                    }
                });

    }

    public void showDialog(String markerID) {
        FirebaseFirestore db = mFirestore.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        dialog.show();

        TextView userName = dialog.findViewById(R.id.userName);
        userName.setText(markerID);

        DocumentReference docRef = db.collection("marker").document(markerID);
        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {
                    DocumentSnapshot document = task.getResult();
                    if (document.exists()) {
                        Log.d(TAG, "DocumentSnapshot data: " + document.getData());
                        userName.setText(document.getData().get("email").toString());
                    } else {
                        Log.d(TAG, "No such document");
                    }
                } else {
                    Log.d(TAG, "get failed with ", task.getException());
                }
            }
        });

        TextView likeCount = dialog.findViewById(R.id.likeCount);
        db.collection("marker")
                .document(markerID)
                .collection("like")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        ArrayList<Map> likeCountList = new ArrayList<>();
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Log.d(TAG, document.getId() + " >> " + document.getData());
                                likeCountList.add(document.getData());
                            }
                            //Log.d(TAG, String.valueOf(likeCountList.size()));
                            likeCount.setText(String.valueOf(likeCountList.size()));
                        } else {
                            Log.d(TAG, "Error getting documents: ", task.getException());
                        }
                    }
                });

        ImageButton likeButton = dialog.findViewById(R.id.likeBtn);
        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Map<String, Object> docData = new HashMap<>();
                docData.put("user", currentUser.getUid());
                db.collection("marker").document(markerID).collection("like")
                        .add(docData)
                        .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                            @Override
                            public void onSuccess(DocumentReference documentReference) {
                                Log.d(TAG, likeCount.getText().toString());
                                likeCountNum = Integer.parseInt(likeCount.getText().toString());
                                likeCountNum = likeCountNum + 1;
                                markerID_n = documentReference.getId();
                                likeCount.setText(likeCountNum.toString());
                                Toast.makeText(MainActivity.this, "좋아요!", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "Error adding document", e);
                            }
                        });
            }
        });

        Button closeButton = dialog.findViewById(R.id.closeBtn);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss();
            }
        });
    }
}