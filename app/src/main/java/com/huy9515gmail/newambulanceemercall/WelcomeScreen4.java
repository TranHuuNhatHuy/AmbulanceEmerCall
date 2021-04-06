package com.huy9515gmail.newambulanceemercall;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import butterknife.BindView;
import butterknife.ButterKnife;

public class WelcomeScreen4 extends AppCompatActivity {

    @BindView(R.id.btn_next)
    Button btnNext;
    @BindView(R.id.btnAskPermissions)
    Button btnAskPermissions;
    @BindView(R.id.edtID)
    EditText edtID;

    public static final int PERMISSION_CODE = 1303;

    boolean isPermissionsAccessible = false;

    final DatabaseReference ambulances = FirebaseDatabase.getInstance().getReference("ambulances");
    final DatabaseReference ambulanceStatus = FirebaseDatabase.getInstance().getReference("ambulanceStatus");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen4);
        ButterKnife.bind(this);



        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if ( (isPermissionsAccessible) && (isIDAdded()) ) {

                    //constructing ambulance info
                    String id = new String(edtID.getText().toString());
                    String deviceID = Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                    DatabaseReference thisAmbulanceStatus = ambulanceStatus.child(deviceID); thisAmbulanceStatus.setValue(0);
                    DatabaseReference thisAmbulance = ambulances.child(deviceID);
                    DatabaseReference identifier = thisAmbulance.child("id"); identifier.setValue(id);
                    DatabaseReference lat = thisAmbulance.child("latitude"); lat.setValue(0.0d);
                    DatabaseReference lng = thisAmbulance.child("longitude"); lng.setValue(0.0d);
                    DatabaseReference destination = thisAmbulance.child("destination"); destination.setValue("");
                    DatabaseReference status = thisAmbulance.child("status"); status.setValue(0);
                    DatabaseReference times = thisAmbulance.child("times"); times.setValue(0);

                    //proceed to next activity
                    Intent intent = new Intent(WelcomeScreen4.this, WelcomeScreenFinal.class);
                    startActivity(intent);
                    finish();

                } else {
                    showErrors();
                }

            }
        });

        btnAskPermissions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (!(isPermissionsAccessible)) {

                    if (Build.VERSION.SDK_INT < 23) {
                        Toast.makeText(WelcomeScreen4.this, "Đã cấp quyền truy cập vị trí", Toast.LENGTH_SHORT).show();
                        isPermissionsAccessible = true;
                    } else {
                        if (ActivityCompat.checkSelfPermission(getBaseContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getBaseContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(WelcomeScreen4.this, new String[]{
                                    android.Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
                        }
                    }
                } else {
                    Toast.makeText(WelcomeScreen4.this, "Đã cấp quyền truy cập vị trí", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                isPermissionsAccessible = true;
                Toast.makeText(this, "Đã cấp quyền truy cập vị trí", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cần cấp quyền truy cập vị trí để hệ thống EmerCall hoạt động bình thường", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public boolean isIDAdded() {
        String id = new String(edtID.getText().toString());
        if (!(id.matches(""))) {
            return true;
        } else {
            return false;
        }
    }

    public void showErrors() {
        if ( (!(isPermissionsAccessible)) && (!(isIDAdded())) ) {
            Toast.makeText(this, "Yêu cầu nhập biển số xe cấp cứu và cấp quyền truy cập vị trí thiết bị!", Toast.LENGTH_SHORT).show();
        }

        if ( (!(isPermissionsAccessible)) && (isIDAdded()) ) {
            Toast.makeText(this, "Yêu cầu cấp quyền truy cập vị trí!", Toast.LENGTH_SHORT).show();
        }

        if ( (isPermissionsAccessible) && (!(isIDAdded())) ) {
            Toast.makeText(this, "Yêu cầu nhập biển số xe cấp cứu!", Toast.LENGTH_SHORT).show();
        }
    }

}
