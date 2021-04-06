package com.huy9515gmail.newambulanceemercall;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ActiveState_Map extends AppCompatActivity implements OnMapReadyCallback {

    public static AppCompatActivity mapActivity;

    public GoogleMap mMap;
    public Location mLastKnownLocation;
    public FusedLocationProviderClient mFusedLocationProviderClient;
    public CameraPosition mCameraPosition;

    public final LatLng mDefaultLocation = new LatLng(16.075505, 108.222208);
    public static final int DEFAULT_ZOOM = 12;
    public static final String TAG = ActiveState_Map.class.getSimpleName();
    public static final String KEY_CAMERA_POSITION = "camera_position";
    public static final String KEY_LOCATION = "location";

    DatabaseReference thisAmbulance, ambulanceLatitudeRef, ambulanceLongitudeRef, destination;
    final DatabaseReference ambulances = FirebaseDatabase.getInstance().getReference("ambulances");
    final DatabaseReference users = FirebaseDatabase.getInstance().getReference("users");

    public double userLatitude, userLongitude, ambulanceLatitude, ambulanceLongitude, hospitalLatitude, hospitalLongitude;
    public String deviceID;

    public Timer t;

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.bottom_navigation_bar)
    BottomNavigationView bottomNavigationView;

    public Marker userMarker, ambulanceMarker, hospitalMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
            getHospitalLocation();
        }

        setContentView(R.layout.active_state_map);
        ButterKnife.bind(this);

        // Invoke activity chain
        mapActivity = this;

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Get device ID
        deviceID = Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize Firebase database
        thisAmbulance = ambulances.child(deviceID);
        ambulanceLatitudeRef = thisAmbulance.child("latitude");
        ambulanceLongitudeRef = thisAmbulance.child("longitude");
        destination = thisAmbulance.child("destination");

        // Set up toolbar
        toolbar.setTitle("[ĐANG CẤP CỨU] Hệ thống dẫn đường");
        toolbar.setTitleTextColor(Color.WHITE);

        // Set up map
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.active_map);
        mapFragment.getMapAsync(this);

        // Declare the timer
        t = new Timer();
        // Set the schedule function and rate
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                getDeviceLocation();

                getUserLocation();

            }
        }, 0, 5000);

        //handling bottom bar event
        bottomNavigationView.getMenu().getItem(1).setChecked(true);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.patient:
                        bottomNavigationView.getMenu().getItem(0).setChecked(false);
                        Intent intent = new Intent(ActiveState_Map.this, ActiveState_Patient.class);
                        startActivity(intent);
                        break;
                    case R.id.map:
                        break;
                    case R.id.check:
                        bottomNavigationView.getMenu().getItem(2).setChecked(false);
                        Intent intent1 = new Intent(ActiveState_Map.this, ActiveState_Check.class);
                        startActivity(intent1);
                        break;
                }
                return true;
            }
        });

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and update its location data
        getDeviceFirstLocation();

        // Get the hospital location and update its location data
        getHospitalLocation();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }

    public void getDeviceLocation() {
        try {
            Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        // Get the user's current location
                        mLastKnownLocation = task.getResult();
                        if (mLastKnownLocation != null) {
                            ambulanceLatitude = mLastKnownLocation.getLatitude();
                            ambulanceLongitude = mLastKnownLocation.getLongitude();
                            // Update Firebase database
                            ambulanceLatitudeRef.setValue(ambulanceLatitude);
                            ambulanceLongitudeRef.setValue(ambulanceLongitude);
                            // Set the map's camera position to the current location of the device and change the marker location
                            LatLng currentAmbulanceLocation = new LatLng(ambulanceLatitude, ambulanceLongitude);
                            if (ambulanceMarker != null)
                                ambulanceMarker.setPosition(currentAmbulanceLocation);
                        }
                    } else {
                        Log.d(TAG, "Current location is null. Using defaults.");
                        Log.e(TAG, "Exception: %s", task.getException());
                        mMap.moveCamera(CameraUpdateFactory
                                .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                        mMap.getUiSettings().setMyLocationButtonEnabled(false);
                    }
                }
            });
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    public void getDeviceFirstLocation() {
        try {
            Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
            locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                    if (task.isSuccessful()) {
                        // Get the user's current location
                        mLastKnownLocation = task.getResult();
                        if (mLastKnownLocation != null) {
                            ambulanceLatitude = mLastKnownLocation.getLatitude();
                            ambulanceLongitude = mLastKnownLocation.getLongitude();
                            // Update Firebase database
                            ambulanceLatitudeRef.setValue(ambulanceLatitude);
                            ambulanceLongitudeRef.setValue(ambulanceLongitude);
                            // Set the map's camera position to the current location of the device and add a marker
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(ambulanceLatitude, ambulanceLongitude), DEFAULT_ZOOM));
                            LatLng currentAmbulanceLocation = new LatLng(ambulanceLatitude, ambulanceLongitude);
                            ambulanceMarker = mMap.addMarker(new MarkerOptions()
                                    .position(currentAmbulanceLocation)
                                    .title("Vị trí hiện tại")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ambulancemarker)));
                        }
                    }
                }
            });
        } catch (SecurityException e) {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    public void getUserLocation() {
        // Access users location Firebase database and update its location, then reposition the marker
        thisAmbulance = ambulances.child(deviceID);
        destination = thisAmbulance.child("destination");

        //setting up user location
        destination.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!(dataSnapshot.getValue(String.class).equals(""))) {
                    //receive target user location
                    final String userID = dataSnapshot.getValue(String.class);
                    final DatabaseReference targetUserRef = users.child(userID);
                    final DatabaseReference targetUserLocationRef = targetUserRef.child("location");
                    final DatabaseReference targetUserLatitudeRef = targetUserLocationRef.child("latitude");
                    final DatabaseReference targetUserLongitudeRef = targetUserLocationRef.child("longitude");
                    targetUserLatitudeRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot1) {
                            userLatitude = dataSnapshot1.getValue(Double.class);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Toast.makeText(ActiveState_Map.this, "Không thể nhận vĩ độ người dùng", Toast.LENGTH_SHORT).show();
                        }
                    });
                    targetUserLongitudeRef.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            userLongitude = dataSnapshot.getValue(Double.class);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Toast.makeText(ActiveState_Map.this, "Không thể nhận kinh độ người dùng", Toast.LENGTH_SHORT).show();
                        }
                    });
                    // Add or reposition the marker
                    if ((userLatitude != 0) && (userLongitude != 0)) {
                        LatLng currentUserLocation = new LatLng(userLatitude, userLongitude);
                        if (userMarker != null) {
                            userMarker.setPosition(currentUserLocation);
                        } else {
                            userMarker = mMap.addMarker(new MarkerOptions()
                                    .position(currentUserLocation)
                                    .title("Vị trí nạn nhân")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.usermarker)));
                        }
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Toast.makeText(ActiveState_Map.this, "Không thể nhận dữ liệu từ máy chủ", Toast.LENGTH_SHORT).show();
            }
        });

    }

    public void getHospitalLocation() {
        // Access the hospital Firebase database, retrieve its location and add a marker

        // Retrieve location
        final DatabaseReference hospitalLocation = FirebaseDatabase.getInstance().getReference("hospitalLocation");
        final DatabaseReference hospitalLatitudeRef = hospitalLocation.child("latitude");
        final DatabaseReference hospitalLongitudeRef = hospitalLocation.child("longitude");

        hospitalLatitudeRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot1) {
                hospitalLatitude = dataSnapshot1.getValue(Double.class);

                hospitalLongitudeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot2) {
                        hospitalLongitude = dataSnapshot2.getValue(Double.class);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        // Add a marker
        if ((hospitalLatitude != 0) && (hospitalLongitude != 0)) {
            LatLng currentHospitalLocation = new LatLng(hospitalLatitude, hospitalLongitude);
            hospitalMarker = mMap.addMarker(new MarkerOptions()
                    .position(currentHospitalLocation)
                    .title("Vị trí bệnh viện")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.hospitalmarker)));
        }
    }


    public void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        t.cancel();
        t.purge();
    }

}
