package com.huy9515gmail.newambulanceemercall;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
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

public class AmbulanceEmerCall extends AppCompatActivity implements OnMapReadyCallback {

    public GoogleMap mMap;
    public Location mLastKnownLocation;
    public FusedLocationProviderClient mFusedLocationProviderClient;

    public Marker ambulanceMarker;

    public final LatLng mDefaultLocation = new LatLng(-33.8523341, 151.2106085);
    public static final int DEFAULT_ZOOM = 12;
    public static final String TAG = ActiveState_Map.class.getSimpleName();

    double ambulanceLatitude, ambulanceLongitude;
    String deviceID;

    DatabaseReference thisAmbulance, thisAmbulanceStatus, ambulanceLatitudeRef, ambulanceLongitudeRef;
    final DatabaseReference ambulances = FirebaseDatabase.getInstance().getReference("ambulances");
    final DatabaseReference ambulanceStatus = FirebaseDatabase.getInstance().getReference("ambulanceStatus");

    public Timer t;

    @BindView(R.id.toolbar)
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ambulance_emer_call);
        ButterKnife.bind(this);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Get device ID
        deviceID = Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize Firebase database
        thisAmbulance = ambulances.child(deviceID);
        ambulanceLatitudeRef = thisAmbulance.child("latitude");
        ambulanceLongitudeRef = thisAmbulance.child("longitude");
        thisAmbulanceStatus = ambulanceStatus.child(deviceID);

        toolbar.setTitle("Bản đồ (chưa kích hoạt)");
        toolbar.setTitleTextColor(Color.WHITE);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        // Declare the timer
        Timer t = new Timer();
        // Set the schedule function and rate
        t.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {

                getDeviceLocation();

                checkActiveState();

            }
        }, 0, 5000);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and update its location data
        getDeviceFirstLocation();

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
                            if (ambulanceMarker != null) ambulanceMarker.setPosition(currentAmbulanceLocation);
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
        } catch (SecurityException e)  {
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
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                            LatLng currentAmbulanceLocation = new LatLng(ambulanceLatitude, ambulanceLongitude);
                            ambulanceMarker = mMap.addMarker(new MarkerOptions()
                                    .position(currentAmbulanceLocation)
                                    .title("Vị trí hiện tại")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.ambulancemarker)));
                        }
                    }
                }
            });
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    public void checkActiveState() {
        thisAmbulanceStatus.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                int status = dataSnapshot.getValue(Integer.class);
                if (status == 1) {
                    Intent intent = new Intent(AmbulanceEmerCall.this, ActiveState_Map.class);
                    startActivity(intent);
                    finish();
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });
    }

    public void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        mMap.setMyLocationEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
    }

    @Override
    public void onPause() {
        super.onPause();
        t.cancel(); t.purge();
    }

}
