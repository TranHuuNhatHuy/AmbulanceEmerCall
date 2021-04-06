package com.huy9515gmail.newambulanceemercall;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;

public class ActiveState_Patient extends AppCompatActivity {

    public static AppCompatActivity patientActivity;

    final DatabaseReference ambulances = FirebaseDatabase.getInstance().getReference("ambulances");
    final DatabaseReference users = FirebaseDatabase.getInstance().getReference("users");

    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.bottom_navigation_bar)
    BottomNavigationView bottomNavigationView;

    @BindView(R.id.img_gender)
    ImageView imgGender;
    @BindView(R.id.img_age)
    ImageView imgAge;
    @BindView(R.id.tv_gender)
    TextView tvGender;
    @BindView(R.id.tv_age)
    TextView tvAge;

    @BindView(R.id.lvPatient)
    ListView lvPatient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.active_state_patient);
        ButterKnife.bind(this);

        // Invoke activity chain
        patientActivity = this;

        //setting up toolbar
        toolbar.setTitle("[ĐANG CẤP CỨU] Tình trạng nạn nhân");
        toolbar.setTitleTextColor(Color.WHITE);

        //setting up bottom navigation bar
        bottomNavigationView.getMenu().getItem(0).setChecked(true);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.patient: break;
                    case R.id.map:
                        bottomNavigationView.getMenu().getItem(1).setChecked(true);
                        Intent intent = new Intent(ActiveState_Patient.this, ActiveState_Map.class);
                        startActivity(intent); break;
                    case R.id.check:
                        bottomNavigationView.getMenu().getItem(2).setChecked(true);
                        Intent intent1 = new Intent(ActiveState_Patient.this, ActiveState_Check.class);
                        startActivity(intent1); break;
                }
                return true;
            }
        });

        // Get device ID
        final String deviceID = Settings.Secure.getString(getBaseContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        // Retrieve target user's device ID
        DatabaseReference thisAmbulance = ambulances.child(deviceID);
        DatabaseReference destination = thisAmbulance.child("destination");

        destination.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                final String targetUserID = dataSnapshot.getValue(String.class);

                DatabaseReference targetUserRef = users.child(targetUserID);
                DatabaseReference targetPatientRef = targetUserRef.child("patient");
                DatabaseReference targetPatientAge = targetPatientRef.child("age");
                DatabaseReference targetPatientGender = targetPatientRef.child("gender");
                DatabaseReference targetPatientSymptoms = targetPatientRef.child("symptoms");

                //acquiring patient information

                targetPatientAge.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int age = Integer.parseInt(dataSnapshot.getValue(String.class));
                        if (age <= 2) imgAge.setImageResource(R.drawable.ic_newborn);
                        else if (age <= 9) imgAge.setImageResource(R.drawable.ic_children);
                        else if (age <= 15) imgAge.setImageResource(R.drawable.ic_youngkid);
                        else if (age <= 32) imgAge.setImageResource(R.drawable.ic_youngman);
                        else if (age <= 50) imgAge.setImageResource(R.drawable.ic_adult);
                        else imgAge.setImageResource(R.drawable.ic_oldman); //displaying age icon
                        tvAge.setText(age + " tuổi"); //displaying age
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });

                targetPatientGender.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        String gender = dataSnapshot.getValue(String.class);
                        //displaying patient gender and age
                        switch (gender) {
                            case "Nam": imgGender.setImageResource(R.drawable.ic_male); break;
                            case "Nữ": imgGender.setImageResource(R.drawable.ic_female); break;
                        } //displaying gender icon
                        tvGender.setText(gender); //displaying gender
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });

                targetPatientSymptoms.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        ArrayList<String> symptoms = new ArrayList<>();
                        for (DataSnapshot child: dataSnapshot.getChildren()) {
                            symptoms.add(child.getKey().toString());
                        }
                        ArrayAdapter arrayAdapter = new ArrayAdapter(getBaseContext(), R.layout.row_listview, symptoms);
                        lvPatient.setAdapter(arrayAdapter);
                    }
                    @Override
                    public void onCancelled(DatabaseError databaseError) {}
                });

            }
            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });

    }

}