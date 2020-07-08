package com.example.webrtcandroiddemo.demo_2;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.webrtcadroiddemo.Caller;
import com.example.webrtcadroiddemo.R;

import org.json.JSONException;
import org.webrtc.MediaStream;

public class Main2Activity extends Activity implements WebRtcClient_v2.RtcListener {
    private TextView txtUserName;
    private TextView txtGuestName;
    private Button btnStop;

    private WebRtcClient_v2 client;
    private AudioManager audioManager;

    public String userName;
    private Caller caller;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        txtUserName = findViewById(R.id.txtUserName);
        txtGuestName = findViewById(R.id.textView);
        btnStop = findViewById(R.id.btnStop);
        btnStop.setEnabled(false);
        btnStop.setOnClickListener(v -> Main2Activity.this.finish());

        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(false);

        Intent intent = getIntent();
        userName = intent.getExtras().getString("userName");
        txtUserName.setText(userName);

        if (getIntent().getSerializableExtra("Caller") != null) {
            caller = (Caller) getIntent().getSerializableExtra("Caller");
            txtGuestName.setText(caller.getName());
        }
        init();
    }

    @Override
    protected void onDestroy() {
        if (client != null) {
            client.onDestroy();
        }
        super.onDestroy();
    }

    @Override
    public void onCallReady(String callId) {
        if (caller != null) {
            try {
                answer(caller.getId());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            startCam();
        }
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
                        Main2Activity.this.finish();
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
        String socket = "https://hoangtp-stream.herokuapp.com/";
        client = new WebRtcClient_v2(this,this, socket, userName);
    }

    /** Sets the speaker phone mode. */
    private void setSpeakerphoneOn(boolean on) {

        boolean wasOn = audioManager.isSpeakerphoneOn();
        if (wasOn == on) {
            return;
        }
        audioManager.setSpeakerphoneOn(on);
    }

    public void startCam() {
        // Camera settings
        client.start(userName);
    }

    public void answer(String callerId) throws JSONException {
        client.sendMessage(callerId, "init", null);
        startCam();
    }
}