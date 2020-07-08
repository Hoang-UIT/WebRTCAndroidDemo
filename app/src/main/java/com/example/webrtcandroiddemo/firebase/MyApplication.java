package com.example.webrtcandroiddemo.firebase;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.webrtcadroiddemo.FireBaseService;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

interface LifeCycleDelegate {
    void onAppBackgrounded();
    void onAppForegrounded();
}

public class MyApplication extends Application implements LifeCycleDelegate {

    public static String APP_TOKEN = "";
    private static Boolean loggined = false;

    @Override
    public void onCreate() {
        super.onCreate();
        AppLifecycleHandler handler = new AppLifecycleHandler(MyApplication.this);
        registerLifecycleHandler(handler);
    }

    @Override
    public void onAppBackgrounded() {
        if (!APP_TOKEN.isEmpty() && loggined) {
            createOrUpdateUser("",false);
        }
    }

    @Override
    public void onAppForegrounded() {
        if (!APP_TOKEN.isEmpty() && loggined) {
            createOrUpdateUser("",true);
        }
        getTokenKey();
    }

    private void registerLifecycleHandler(AppLifecycleHandler lifeCycleHandler) {
        registerActivityLifecycleCallbacks(lifeCycleHandler);
        registerComponentCallbacks(lifeCycleHandler);
    }

    private void getTokenKey() {
        FirebaseInstanceId.getInstance().getInstanceId().addOnSuccessListener(new OnSuccessListener<InstanceIdResult>() {
            @Override
            public void onSuccess(InstanceIdResult instanceIdResult) {
                APP_TOKEN = instanceIdResult.getToken();
                MyApplication.this.checkUserExisted();
            }
        });
    }

    private void checkUserExisted() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("root/users/");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Intent intent = new Intent("CheckUserRegistered");
                if (snapshot.hasChild(APP_TOKEN)){
                    intent.putExtra("isRegister", true);
                    loggined = true;
                } else {
                    intent.putExtra("isRegister", false);
                }
                LocalBroadcastManager.getInstance(MyApplication.this).sendBroadcast(intent);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    public static void createOrUpdateUser(String name,Boolean isOnline) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("root/users/" + APP_TOKEN);
        HashMap<String, Object> myDevice = new HashMap<>();
        if (name!= null && !name.isEmpty()) {
            myDevice.put("name", name);
        }
        myDevice.put("token", APP_TOKEN);
        myDevice.put("isOnline", isOnline);
        myDevice.put("sdp", "");
        myDevice.put("candidates", null);
        ref.updateChildren(myDevice);
    }

    public static void deleteUser() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("root/users/" + APP_TOKEN);
        ref.removeValue();
    }

    public static void updateSdpUser(String sdp) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("root/users/" + APP_TOKEN);
        HashMap<String, Object> myDevice = new HashMap<>();
        myDevice.put("token", APP_TOKEN);
        myDevice.put("sdp", sdp);
        ref.updateChildren(myDevice);
    }

    public static void updateCandidatesUser(int index, int label, String id, String candidate) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("root/users/" + APP_TOKEN + "/candidates/" + index);
        HashMap<String, Object> myDevice = new HashMap<>();
        myDevice.put("label", label);
        myDevice.put("id", id);
        myDevice.put("candidate", candidate);
        ref.updateChildren(myDevice);
    }

    public static void sendMessage(String userName, String to, JSONObject data) {
        JSONObject notification = new JSONObject();
        JSONObject body = new JSONObject();
        try {
            notification.put("title",userName);
            notification.put("body","Calling...");

            body.put("to",to);
            body.put("notification",notification);
            if (data != null) {
                body.put("data", data);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://fcm.googleapis.com")

                .addConverterFactory(GsonConverterFactory.create())
                .build();
        FireBaseService service = retrofit.create(FireBaseService.class);
        final Call<ResponseBody> repos = service.PushNotification(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), body.toString()));
        repos.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.code() == 200) {
                    Log.d("Push Notification","Successful");
                } else {
                    Log.d("Push Notification","Fail");
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
    }
}
