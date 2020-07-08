package com.example.webrtcadroiddemo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class HomeActivity extends AppCompatActivity {

    private Button btnSignalingServer;
    private Button btnFirebase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnSignalingServer = findViewById(R.id.btnSignalingServer);
        btnFirebase = findViewById(R.id.btnFireBase);

        btnSignalingServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HomeActivity.this.goToListCallerActivity(false);
            }
        });

        btnFirebase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                HomeActivity.this.goToListCallerActivity(true);
            }
        });
    }


    private void goToListCallerActivity(Boolean isFirebase) {
        Intent intent = new Intent(HomeActivity.this, CallerListActivity.class);
        intent.putExtra("isFirebase", isFirebase);
        startActivity(intent);
    }
}
