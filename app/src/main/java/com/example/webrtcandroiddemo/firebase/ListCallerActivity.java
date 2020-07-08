package com.example.webrtcandroiddemo.firebase;

import android.Manifest;
import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.webrtcadroiddemo.Caller;
import com.example.webrtcadroiddemo.CallerAdapter;
import com.example.webrtcadroiddemo.Candidate;
import com.example.webrtcadroiddemo.PermissionChecker;
import com.example.webrtcadroiddemo.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ListCallerActivity extends BaseActivity {

    private final static String TAG = ListCallerActivity.class.getCanonicalName();

    private static final String[] RequiredPermissions = new String[]{ Manifest.permission.RECORD_AUDIO};
    protected PermissionChecker permissionChecker = new PermissionChecker();

    private RecyclerView callerRecylerView;
    private RecyclerView.Adapter adapter;
    private Button btnLogout;
    private Button btnRefresh;
    private TextView txtUserName;

    private ArrayList<Caller> callers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list_caller);

        checkPermissions();

        txtUserName = findViewById(R.id.txtUserName);
        callerRecylerView = findViewById(R.id.listCaller);

        btnLogout = findViewById(R.id.btnLogout);
        btnRefresh = findViewById(R.id.btnRefresh);

        txtUserName.setText("");

        adapter = new CallerAdapter(callers);
        ((CallerAdapter) adapter).setMainActivity(this);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        callerRecylerView.setLayoutManager(layoutManager);
        callerRecylerView.setAdapter(adapter);

        btnLogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyApplication.deleteUser();
                Intent intent = new Intent(ListCallerActivity.this, LoginActivity.class);
                intent.putExtra("pressedBack",true);
                startActivity(intent);
                ListCallerActivity.this.finish();
            }
        });

        btnRefresh.setEnabled(false);
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListCallerActivity.this.reloadData();
            }
        });

        if (getIntent().getStringExtra("callerId") != null) {
            String callId = getIntent().getStringExtra("callerId");
            startCall(null);
            JSONObject object = new JSONObject();
            try {
                object.put("type","accepted");
                object.put("callerId",MyApplication.APP_TOKEN);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            MyApplication.sendMessage("",callId,object);
        }
        if (getIntent().getStringExtra("init") != null) {
            Toast.makeText(this,"INIT",Toast.LENGTH_SHORT).show();
        }
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (intent.getExtras() != null) {
                        String callerId = intent.getExtras().getString("callerId");
                        for (Caller caller: callers) {
                            if (caller.getId().contentEquals(callerId)) {
                                startCall(caller);
                                break;
                            }
                        }
                    }
                }
            }, 1000);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        getListCaller();
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("Accepted"));
    }

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionChecker.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void reloadData() {
        btnRefresh.setEnabled(false);
        callers.clear();
        adapter.notifyDataSetChanged();
        getListCaller();
    }

    private void getListCaller() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("root/users/");

        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ArrayList<Caller> callers = new ArrayList<>();
                for (DataSnapshot item : snapshot.getChildren()) {
                    String name = item.child("name").getValue().toString();
                    String token = item.child("token").getValue().toString();
                    Boolean isOnline = (Boolean) item.child("isOnline").getValue();
                    String sdp = item.child("sdp").getValue().toString();

                    Caller caller = new Caller();
                    caller.setId(token);
                    caller.setName(name);
                    caller.setOnline(isOnline);
                    caller.setSdp(sdp);
                    for (DataSnapshot child : item.child("candidates").getChildren()) {
                        String id = child.child("id").getValue().toString();
                        int label = Integer.parseInt(child.child("label").getValue().toString());
                        String candidateStr = child.child("candidate").getValue().toString();
                        Candidate candidate = new Candidate(id, label, candidateStr);
                        caller.addCadidate(candidate);
                    }
                    callers.add(caller);
                };
                ListCallerActivity.this.updateData(callers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void checkPermissions() {
        permissionChecker.verifyPermissions(this, RequiredPermissions, new PermissionChecker.VerifyPermissionsCallback() {

            @Override
            public void onPermissionAllGranted() {

            }

            @Override
            public void onPermissionDeny(String[] permissions) {
                Toast.makeText(ListCallerActivity.this, "Please grant required permissions.", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void updateData(ArrayList<Caller> callers) {
        this.callers.clear();
        adapter.notifyDataSetChanged();

        String token = MyApplication.APP_TOKEN;
        for (Caller caller: callers) {
            if (caller.getId().contentEquals(token)) {
                txtUserName.setText(caller.getName());
                continue;
            }
            this.callers.add(caller);
        }
        adapter.notifyDataSetChanged();
        btnRefresh.setEnabled(true);
    }

    @Override
    public void callUser(Caller caller) {
        super.callUser(caller);

        Toast.makeText(this,"Calling to " + caller.getName(), Toast.LENGTH_LONG).show();
        String name = txtUserName.getText().toString();
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("type","init");
            jsonObject.put("callerId",MyApplication.APP_TOKEN);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        MyApplication.sendMessage(name, caller.getId(),jsonObject);
    }

    private void startCall(Caller caller) {
        String name = txtUserName.getText().toString();
        if (caller != null) {
            Intent intent = new Intent(ListCallerActivity.this, Main3Activity.class);
            intent.putExtra("userName", name);
            intent.putExtra("Caller",caller);
            startActivity(intent);
            return;
        }
        Intent intent = new Intent(ListCallerActivity.this, Main3Activity.class);
        intent.putExtra("userName", name);
        startActivity(intent);
    }
}
