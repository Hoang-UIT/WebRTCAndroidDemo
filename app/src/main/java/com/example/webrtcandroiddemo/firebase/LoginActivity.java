package com.example.webrtcandroiddemo.firebase;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.webrtcadroiddemo.R;

public class LoginActivity extends BaseActivity {

    private ConstraintLayout rootLayout;
    private Button btnLogin;
    private EditText editTextName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("CheckUserRegistered"));

        rootLayout = findViewById(R.id.rootLayout);
        editTextName = findViewById(R.id.editTextName);
        btnLogin = findViewById(R.id.btnLogin);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginActivity.this.handleLogin();
            }
        });

        Intent intent = getIntent();

        if (intent.getBooleanExtra("pressedBack",false) == true){
            rootLayout.setVisibility(View.VISIBLE);
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() != null) {
                Boolean isRegister = intent.getExtras().getBoolean("isRegister");
                if (isRegister) {
                    LoginActivity.this.gotoListCallerActivity();
                } else {
                    rootLayout.setVisibility(View.VISIBLE);
                    LoginActivity.this.btnLogin.setEnabled(true);
                }
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    private void handleLogin() {
        String token = MyApplication.APP_TOKEN;
        if (token.isEmpty()) {
            Toast.makeText(this,"Processing data", Toast.LENGTH_SHORT).show();
            return;
        }

        String name = editTextName.getText().toString();
        if (name.isEmpty()) {
            Toast.makeText(this,"username invalid", Toast.LENGTH_SHORT).show();
            return;
        }
        MyApplication.createOrUpdateUser(name,true);
        gotoListCallerActivity();
    }

    private void gotoListCallerActivity() {
        MyApplication.createOrUpdateUser(null,true);

        Intent intent = new Intent(LoginActivity.this, ListCallerActivity.class);
        if (getIntent().getStringExtra("callerId") != null) {
            intent.putExtra("type","init");
            intent.putExtra("callerId",getIntent().getStringExtra("callerId"));
        }
        startActivity(intent);
        finish();
    }
}
