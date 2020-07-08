package com.example.webrtcadroiddemo;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.json.JSONException;
import org.webrtc.MediaStream;



public class MainActivity extends Activity implements WebRtcClient.RtcListener{

    private final static String TAG = MainActivity.class.getCanonicalName();

    private static final String[] RequiredPermissions = new String[]{ Manifest.permission.RECORD_AUDIO};
    protected PermissionChecker permissionChecker = new PermissionChecker();

    private TextView txtUserName;
    private TextView txtGuestName;
    private Button btnStop;

    private WebRtcClient client;
    private AudioManager audioManager;

    public String userName;
    private Caller caller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtUserName = (TextView) findViewById(R.id.txtUserName);
        txtGuestName = (TextView) findViewById(R.id.textView);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnStop.setEnabled(false);
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivity.this.finish();
            }
        });

        audioManager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setSpeakerphoneOn(false);
        checkPermissions();

        Intent intent = getIntent();
        userName = intent.getExtras().getString("userName");
        txtUserName.setText(userName);

        if (getIntent().getSerializableExtra("Caller") != null) {
            caller = (Caller) getIntent().getSerializableExtra("Caller");
            txtGuestName.setText(caller.getName());
            Log.d(TAG, "onCreate: " + caller);
        }

        init();
    }

    private void init() {
        Point displaySize = new Point();

        getWindowManager().getDefaultDisplay().getSize(displaySize);

        PeerConnectionParameters params = new PeerConnectionParameters(
                false, false, displaySize.x, displaySize.y, 30, 1, "VP9", true, 1, "opus", true);

        String socket = "https://hoangtp-stream.herokuapp.com/";
        client = new WebRtcClient(userName, caller, MainActivity.this, socket, params, MainActivity.this);
    }


    @Override
    public void onPause() {
        super.onPause();
        if (client != null) {
            client.onPause();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (client != null) {
            client.onResume();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        if (client != null) {
            client.onDestroy();
        }
        super.onDestroy();
    }

    private void checkPermissions() {
        permissionChecker.verifyPermissions(this, RequiredPermissions, new PermissionChecker.VerifyPermissionsCallback() {

            @Override
            public void onPermissionAllGranted() {

            }

            @Override
            public void onPermissionDeny(String[] permissions) {
                Toast.makeText(MainActivity.this, "Please grant required permissions.", Toast.LENGTH_LONG).show();
            }
        });
    }
    @Override
    public void onCallReady(String callId) {
        if (PermissionChecker.hasPermissions(this, RequiredPermissions)) {
            client.start(userName);
        }
//        if (callId != null) {
//            try {
//                answer(callId);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
//        } else {
//            if (PermissionChecker.hasPermissions(this, RequiredPermissions)) {
//                client.start(userName);
//            }
////            call(callId);
//        }
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
                        MainActivity.this.finish();
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
        if (localStream.videoTracks.size() > 0) {
//            localStream.videoTracks.get(0).addRenderer(new VideoRenderer(localRender));
//            VideoRendererGui.update(localRender,
//                    LOCAL_X_CONNECTING, LOCAL_Y_CONNECTING,
//                    LOCAL_WIDTH_CONNECTING, LOCAL_HEIGHT_CONNECTING,
//                    scalingType, false);
        }
    }

    @Override
    public void onAddRemoteStream(MediaStream remoteStream, int endPoint) {
        if (remoteStream.videoTracks.size() > 0) {
//            remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
//            VideoRendererGui.update(remoteRender,
//                    REMOTE_X, REMOTE_Y,
//                    REMOTE_WIDTH, REMOTE_HEIGHT, scalingType, false);
//            VideoRendererGui.update(localRender,
//                    LOCAL_X_CONNECTED, LOCAL_Y_CONNECTED,
//                    LOCAL_WIDTH_CONNECTED, LOCAL_HEIGHT_CONNECTED,
//                    scalingType, false);
        }
    }

    @Override
    public void onRemoveRemoteStream(int endPoint) {

    }

    @Override
    public void onReceiveGuestName(String name) {
        runOnUiThread(() -> txtGuestName.setText(name));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        permissionChecker.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void answer(String callerId) throws JSONException {
        client.sendMessage(callerId, "init", null);
        startCam();
    }

    public void answerCallingFrom(Caller caller) throws JSONException {
        startCam();
    }

    public void startCam() {
        // Camera settings
        if (PermissionChecker.hasPermissions(this, RequiredPermissions)) {
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
