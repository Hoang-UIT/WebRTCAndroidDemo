package com.example.webrtcadroiddemo;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.webrtcandroiddemo.demo_2.Main2Activity;
import com.example.webrtcandroiddemo.firebase.BaseActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CallerListActivity extends BaseActivity {
    private final static String TAG = CallerListActivity.class.getCanonicalName();

    private static final String[] RequiredPermissions = new String[]{ Manifest.permission.RECORD_AUDIO};
    protected PermissionChecker permissionChecker = new PermissionChecker();

    private RecyclerView callerRecylerView;
    private RecyclerView.Adapter adapter;
    private ArrayList<Caller> callers;


    private Switch swChangeCase;
    private EditText editText;
    private Button btnRefresh;
    private Button btnRegister;

    private Boolean isFirebase;
    private Boolean isChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caller_list);
        Intent intent = getIntent();
        isFirebase = intent.getExtras().getBoolean("isFirebase");

        Toast.makeText(this, "Using Fire Base - " + isFirebase,Toast.LENGTH_SHORT).show();
        checkPermissions();

        callers = new ArrayList<Caller>();

        callerRecylerView = (RecyclerView) findViewById(R.id.listCaller);

        swChangeCase = findViewById(R.id.swChangeCase);
        editText = (EditText) findViewById(R.id.editText);
        btnRefresh = (Button) findViewById(R.id.btnRefresh);
        btnRegister = (Button) findViewById(R.id.btnStartCam);

        swChangeCase.setOnCheckedChangeListener((buttonView, isChecked) -> {
            CallerListActivity.this.isChecked = isChecked;
        });

        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CallerListActivity.this.isFirebase) {
                    CallerListActivity.this.getData();
                } else {
                    CallerListActivity.this.getDataFirebase();
                }
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CallerListActivity.this.startCam();
            }
        });

        adapter = new CallerAdapter(callers);
        ((CallerAdapter) adapter).setMainActivity(this);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayoutManager.VERTICAL, false);
        callerRecylerView.setLayoutManager(layoutManager);
        callerRecylerView.setAdapter(adapter);

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (isFirebase) {
            getDataFirebase();
        } else {
            getData();
        }
    }

    private void checkPermissions() {
        permissionChecker.verifyPermissions(this, RequiredPermissions, new PermissionChecker.VerifyPermissionsCallback() {

            @Override
            public void onPermissionAllGranted() {

            }

            @Override
            public void onPermissionDeny(String[] permissions) {
            }
        });
    }
    private void getData() {
        callers.clear();
        adapter.notifyDataSetChanged();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://hoangtp-stream.herokuapp.com/streams.json/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        StreamService service = retrofit.create(StreamService.class);
        final Call<List<Caller>> repos = service.getStreams();

        repos.enqueue(new Callback<List<Caller>>() {
            @Override
            public void onResponse(Call<List<Caller>> call, Response<List<Caller>> response) {
                if (response.code() == 200) {
                    CallerListActivity.this.updateList((ArrayList<Caller>) response.body());
                }
            }

            @Override
            public void onFailure(Call<List<Caller>> call, Throwable t) {
            }
        });
    }

    private void getDataFirebase() {
        callers.clear();
        adapter.notifyDataSetChanged();
        FirebaseDatabase.getInstance()
        .getReference("root/users/").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                ArrayList<Caller> callers = new ArrayList<>();

                for (DataSnapshot item : snapshot.getChildren()) {
                    String name = item.child("name").getValue().toString();
                    String token = item.child("token").getValue().toString();
                    Log.d(TAG, "name: " + name);
                    Log.d(TAG, "token: " + token);
                    Caller caller = new Caller();
                    caller.setId(token);
                    caller.setName(name);
                    callers.add(caller);
                };
                Log.d(TAG, "onDataChange: " + snapshot.getValue());
                CallerListActivity.this.updateList(callers);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    private void updateList(final ArrayList<Caller> data) {
        callers.addAll(data);
        adapter.notifyDataSetChanged();
    }

    public void startCam() {
        if (isFirebase) {
            createUser();
            return;
        }

        Log.d("MainActivity","Start Camera ....");
        Intent intent;
        if (!isChecked) {
            intent = new Intent(CallerListActivity.this, MainActivity.class);
        } else {
            intent = new Intent(CallerListActivity.this, Main2Activity.class);
        }
        String userName = editText.getText().toString().isEmpty() ? "Anonymous" : editText.getText().toString();
        intent.putExtra("userName", userName);
        startActivity(intent);
    }

    @Override
    public void callUser(Caller caller) {
        super.callUser(caller);
        Log.d("MainActivity","Calling ...." + caller.getName());
        Intent intent;
        if (!isChecked) {
            intent = new Intent(CallerListActivity.this, MainActivity.class);
        } else {
            intent = new Intent(CallerListActivity.this, Main2Activity.class);
        }
        String userName = editText.getText().toString().isEmpty() ? "Anonymous" : editText.getText().toString();
        intent.putExtra("userName", userName);
        intent.putExtra("Caller",caller);
        startActivity(intent);
    }

    private void createUser() {
//        for (Caller caller: callers) {
//            if (caller.getId().contentEquals(token)) {
//                return;
//            }
//        }
//        FirebaseDatabase database = FirebaseDatabase.getInstance();
//        DatabaseReference ref = database.getReference("root/users");
//
//        HashMap<String, String> myDevice = new HashMap<>();
//        myDevice.put("name", editText.getText().toString());
//        myDevice.put("token", token);
//        ref.push().setValue(myDevice);
//        getDataFirebase();
    }
}
