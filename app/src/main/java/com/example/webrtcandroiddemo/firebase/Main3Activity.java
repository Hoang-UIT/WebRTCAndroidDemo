package com.example.webrtcandroiddemo.firebase;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.webrtcadroiddemo.Caller;
import com.example.webrtcadroiddemo.Candidate;
import com.example.webrtcadroiddemo.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.webrtc.MediaStream;

public class Main3Activity extends BaseActivity implements WebRtcClient_v3.RtcListener{

    private TextView txtUserName;
    private TextView txtGuestName;
    private Button btnStop;

    private WebRtcClient_v3 client;
    private AudioManager audioManager;

    public String userName;
    private Caller caller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main3);

        txtUserName = findViewById(R.id.txtUserName);
        txtGuestName = findViewById(R.id.textView);
        btnStop = findViewById(R.id.btnStop);

        btnStop.setEnabled(false);
        btnStop.setOnClickListener(v -> Main3Activity.this.finish());

        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(false);

        if (getIntent().getExtras() != null) {
            userName = getIntent().getExtras().getString("userName");
            txtUserName.setText(userName);
        }

        if (getIntent().getSerializableExtra("Caller") != null) {
            caller = (Caller) getIntent().getSerializableExtra("Caller");
            txtGuestName.setText(caller.getName());
        }
        init();
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver, new IntentFilter("ReadyToCall"));
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() != null) {
                String callerId = intent.getExtras().getString("callerId");
                Main3Activity.this.getDataCaller(callerId);
            }
        }
    };

    private void getDataCaller(String callerId) {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference("root/users/" + callerId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                String name = snapshot.child("name").getValue().toString();
                String token = snapshot.child("token").getValue().toString();
                Boolean isOnline = (Boolean) snapshot.child("isOnline").getValue();
                String sdp = snapshot.child("sdp").getValue().toString();

                Caller caller = new Caller();
                caller.setId(token);
                caller.setName(name);
                caller.setOnline(isOnline);
                caller.setSdp(sdp);
                for (DataSnapshot child : snapshot.child("candidates").getChildren()) {
                    String id = child.child("id").getValue().toString();
                    int label = Integer.parseInt(child.child("label").getValue().toString());
                    String candidateStr = child.child("candidate").getValue().toString();
                    Candidate candidate = new Candidate(id, label, candidateStr);
                    caller.addCadidate(candidate);
                }
                Main3Activity.this.client.connectWithCaller(caller);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (client != null) {
            client.onDestroy();
        }
        MyApplication.createOrUpdateUser(userName,true);
        super.onDestroy();
    }

    @Override
    public void onCallReady(String callId) {

    }

    @Override
    public void onStatusChanged(String newStatus) {
        runOnUiThread(() -> {
            Toast.makeText(getApplicationContext(), newStatus, Toast.LENGTH_SHORT).show();
            if (btnStop != null) {

                if (newStatus.contentEquals("CONNECTING") ||
                        newStatus.contentEquals("CONNECTED")) {
                    btnStop.setEnabled(true);
                } else {
                    btnStop.setEnabled(false);
                    if (newStatus.contentEquals("DISCONNECTED") ||
                            newStatus.contentEquals("CLOSED") ||
                            newStatus.contentEquals("FAILED")) {
                        Main3Activity.this.finish();
                    }
                }
            }
        });
    }

    @Override
    public void onLocalStream(MediaStream localStream) {
        if (localStream.audioTracks.size() > 0) {
            /**
             * Audio Track will be played automatically if connection is established properly between peers.
             * And you can control output volume/device with AudioManager.
             * */
            setSpeakerphoneOn(false);
            localStream.audioTracks.get(0);
        }
    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream) {

    }

    @Override
    public void onRemoveRemoteStream(int endPoint) {

    }

    @Override
    public void onReceiveGuestName(String name) {
        runOnUiThread(() -> txtGuestName.setText(name));
    }

    private void init() {
        client = new WebRtcClient_v3(this,this, userName);
        if (caller == null) {
            client.start(userName);
        } else {
            client.setCaller(caller);
            client.start(userName);
        }
    }

    /** Sets the speaker phone mode. */
    private void setSpeakerphoneOn(boolean on) {

        boolean wasOn = audioManager.isSpeakerphoneOn();
        if (wasOn == on) {
            return;
        }
        audioManager.setSpeakerphoneOn(on);
    }

}