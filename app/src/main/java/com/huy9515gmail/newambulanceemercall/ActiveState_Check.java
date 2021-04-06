package com.huy9515gmail.newambulanceemercall;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ActiveState_Check extends AppCompatActivity {

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.bottom_navigation_bar)
    BottomNavigationView bottomNavigationView;
    @BindView(R.id.active_state_check_btnAffirmative)
    Button btnAffirmative;
    @BindView(R.id.radiogroup)
    RadioGroup caseResult;

    String deviceID;

    DatabaseReference thisAmbulance, destination, thisAmbulanceStatus, statusRef, users, thisUser, patient, patientAge, patientGender, patientSymptoms, userStatus, thisUserStatus, thisStatus, targetAmbulance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.active_state_check);
        ButterKnife.bind(this);

        //initializing Firebase database
        //ambulance Firebase database
        deviceID = Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        thisAmbulance = FirebaseDatabase.getInstance().getReference("ambulances").child(deviceID);
        destination = thisAmbulance.child("destination");
        thisAmbulanceStatus = FirebaseDatabase.getInstance().getReference("ambulanceStatus").child(deviceID);
        statusRef = thisAmbulance.child("status");
        Log.d("first", "okay");

        //setting up toolbar
        toolbar.setTitle("[ĐANG CẤP CỨU] Xác nhận hoàn thành");
        toolbar.setTitleTextColor(Color.WHITE);

        //setting up affirmative button
        btnAffirmative.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //show an analog
                AlertDialog.Builder builder = new AlertDialog.Builder(ActiveState_Check.this);
                builder.setTitle("Xác nhận chuyên chở cấp cứu đã hoàn thành?");
                builder.setMessage("Yêu cầu xác nhận sau khi đã đưa bệnh nhân, người bị nạn đến bệnh viện hoặc họ được chuyên chở đến bệnh viện bởi một nguồn khác, hoặc khi trách nhiệm chuyên chở với ca này kết thúc.");
                builder.setCancelable(false);
                builder.setPositiveButton("Hủy bỏ", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });
                builder.setNegativeButton("Xác nhận", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        destination.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                //acquire user ID and set up user Firebase database
                                final String userID = dataSnapshot.getValue(String.class);
                                Log.d("useridgot", userID);
                                final DatabaseReference users = FirebaseDatabase.getInstance().getReference("users");
                                final DatabaseReference thisUser = users.child(userID);
                                final DatabaseReference patient = thisUser.child("patient");
                                final DatabaseReference patientAge = patient.child("age");
                                final DatabaseReference patientGender = patient.child("gender");
                                final DatabaseReference patientSymptoms = patient.child("symptoms");
                                final DatabaseReference userStatus = FirebaseDatabase.getInstance().getReference("userStatus");
                                final DatabaseReference thisUserStatus = userStatus.child(userID);
                                final DatabaseReference thisStatus = thisUser.child("status"); //inner status
                                final DatabaseReference targetAmbulance = thisUser.child("targetAmbulance");
                                targetAmbulance.setValue("");
                                final DatabaseReference fakeCountRef = FirebaseDatabase.getInstance().getReference("users").child(userID).child("callCount").child("fake");
                                fakeCountRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(DataSnapshot dataSnapshot) {
                                        int fakeCount = dataSnapshot.getValue(Integer.class);
                                        //check if this is a fake call
                                        if (caseResult.getCheckedRadioButtonId() == R.id.rbtnFake) {
                                            fakeCount = fakeCount + 1;
                                            fakeCountRef.setValue(fakeCount);
                                        }
                                    }
                                    @Override
                                    public void onCancelled(DatabaseError databaseError) {}
                                });
                                //reset ambulance Firebase database
                                thisAmbulanceStatus.setValue(0);
                                statusRef.setValue(0);
                                //destroy other activities
                                if (ActiveState_Map.mapActivity != null) ActiveState_Map.mapActivity.finish();
                                if (ActiveState_Patient.patientActivity != null) ActiveState_Patient.patientActivity.finish();
                                //reset user Firebase database
                                thisUserStatus.setValue(0);
                                thisStatus.setValue(0);
                                patientAge.setValue(0);
                                patientGender.setValue("");
                                patientSymptoms.setValue("");
                                targetAmbulance.setValue("");
                                destination.setValue("");
                                //back to main activity
                                Intent intent = new Intent(ActiveState_Check.this, AmbulanceEmerCall.class);
                                startActivity(intent);
                                finish();
                            }
                            @Override
                            public void onCancelled(DatabaseError databaseError) {}
                        });
                    }
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
            }
        });

        //setting up bottom navigation bar
        bottomNavigationView.getMenu().getItem(2).setChecked(true);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.patient:
                        bottomNavigationView.getMenu().getItem(0).setChecked(false);
                        Intent intent = new Intent(ActiveState_Check.this, ActiveState_Patient.class);
                        startActivity(intent); break;
                    case R.id.map:
                        bottomNavigationView.getMenu().getItem(1).setChecked(false);
                        Intent intent1 = new Intent(ActiveState_Check.this, ActiveState_Map.class);
                        startActivity(intent1); break;
                    case R.id.check: break;
                }
                return true;
            }
        });

    }
}
