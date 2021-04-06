package com.huy9515gmail.newambulanceemercall;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;

public class WelcomeScreenFinal extends AppCompatActivity {

    SharedPreferences Prefs;
    final String welcomeScreenShownPref = "welcomeScreenShown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_screen_final);

        Prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = Prefs.edit();
        editor.putBoolean(welcomeScreenShownPref, true);
        editor.commit();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(WelcomeScreenFinal.this, AmbulanceEmerCall.class);
                startActivity(intent);
                finish();
            }}, 3000
        );
    }

}
