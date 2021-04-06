package com.huy9515gmail.newambulanceemercall;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import butterknife.BindView;
import butterknife.ButterKnife;

public class WelcomeScreen1 extends AppCompatActivity {

    SharedPreferences Prefs;
    final String welcomeScreenShownPref = "welcomeScreenShown";

    @BindView(R.id.btn_next)
    Button btnNext;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Boolean welcomeScreenShown = Prefs.getBoolean(welcomeScreenShownPref, false);

        if (welcomeScreenShown) {
            Intent intent = new Intent(WelcomeScreen1.this, AmbulanceEmerCall.class);
            startActivity(intent);
            finish();
        }

        setContentView(R.layout.activity_welcome_screen1);
        ButterKnife.bind(this);

        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(WelcomeScreen1.this, WelcomeScreen2.class);
                startActivity(intent);
                finish();

            }
        });

    }


}
