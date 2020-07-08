package com.example.webrtcandroiddemo.demo_2;

import android.content.Context;
import android.util.Log;

import com.example.webrtcadroiddemo.Caller;
import com.example.webrtcadroiddemo.Candidate;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpReceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

public class WebRtcClient_v2 {
    /**
     * Implement this interface to be notified of events.
     */
    public interface RtcListener {
        void onCallReady(String callId);

        void onStatusChanged(String newStatus);

        void onLocalStream(MediaStream localStream);

        void onAddRemoteStream(MediaStream remoteStream);

        void onRemoveRemoteStream(int endPoint);

        void onReceiveGuestName(String name);
    }

    private interface Command {
        void execute(String peerId, JSONObject payload) throws JSONException;
    }

    private final static String TAG = WebRtcClient_v2.class.getCanonicalName();

    private List<PeerConnection.IceServer> iceServers = new ArrayList<>();
    private MediaConstraints pcConstraints = new MediaConstraints();
    private RtcListener mListener;
    private PeerConnectionFactory factory;
    private MediaStream localMS;
    private Socket client;
    private AudioSource audioSource;

    private Peer mPeer;
    private Caller user;
    private Caller caller;

    public WebRtcClient_v2(Context context, RtcListener listener, String host, String userName) {
        this.mListener = listener;
        user = new Caller();
        user.setName(userName);
        PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-IntelVP8/Enabled/")
                .createInitializationOptions();

        PeerConnectionFactory.initialize(initializationOptions);

        EglBase rootEglBase = EglBase.create();
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(rootEglBase.getEglBaseContext(),  true,  true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());

        factory = PeerConnectionFactory.builder()
                .setOptions(options)
                .setVideoEncoderFactory(defaultVideoEncoderFactory)
                .setVideoDecoderFactory(defaultVideoDecoderFactory)
                .createPeerConnectionFactory();

        WebRtcClient_v2.MessageHandler messageHandler = new WebRtcClient_v2.MessageHandler();
        try {
            client = IO.socket(host);
        } catch (URISyntaxException e) {

            e.printStackTrace();
        }

        client.on("id", messageHandler.onId);
        client.on("message", messageHandler.onMessage);
        client.connect();

        PeerConnection.IceServer stunServer = PeerConnection.IceServer
                .builder("stun:stun1.l.google.com:19302")
                .setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK)
                .createIceServer();

        iceServers.add(stunServer);

        PeerConnection.IceServer turnServer =  PeerConnection.IceServer
                .builder("turn:numb.viagenie.ca")
                .setUsername("webrtc@live.com")
                .setPassword("muazkh")
                .setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK)
                .createIceServer();

        iceServers.add(turnServer);

//        List<String> urls = new ArrayList<>();
//        urls.add("turn:ss-turn1.xirsys.com:80?transport=udp");
//        urls.add("turn:ss-turn1.xirsys.com:3478?transport=udp");
//        urls.add("turn:ss-turn1.xirsys.com:80?transport=tcp");
//        urls.add("turn:ss-turn1.xirsys.com:3478?transport=tcp");
//        urls.add("turns:ss-turn1.xirsys.com:443?transport=tcp");
//        urls.add("turns:ss-turn1.xirsys.com:5349?transport=tcp");
//
//        PeerConnection.IceServer turnXirsys =  PeerConnection.IceServer
//                .builder(urls)
//                .setUsername("M6F_8IXrb1cHYekOSMS94rxKxY_gXtdqxPLeQL6rsRHkgvGPQwgkWBjiv0EIMKfhAAAAAF7WINBob2FuZ3Rw")
//                .setPassword("71d8ca58-a4b6-11ea-ac80-0242ac140004")
//                .setTlsCertPolicy(PeerConnection.TlsCertPolicy.TLS_CERT_POLICY_INSECURE_NO_CHECK)
//                .createIceServer();
//
//        iceServers.add(turnXirsys);

        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        pcConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
        pcConstraints.optional.add(new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
    }

    /**
     * Start the client.
     * <p>
     * Set up the local stream and notify the signaling server.
     * Call this method after onCallReady.
     *
     * @param name client name
     */
    public void start(String name) {
        setMediaStream();
        try {
            JSONObject message = new JSONObject();
            message.put("name", name);
            message.put("sdp", "...");
            client.emit("readyToStream", message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void setMediaStream() {
        localMS = factory.createLocalMediaStream("ARDAMS");
        audioSource = factory.createAudioSource(new MediaConstraints());
        AudioTrack audioTrack = factory.createAudioTrack("ARDAMSa0", audioSource);
        audioTrack.setEnabled(true);
        audioTrack.setVolume(1);
        localMS.addTrack(audioTrack);

        mListener.onLocalStream(localMS);
    }

    /**
     * Call this method in Activity.onDestroy()
     */
    public void onDestroy() {
//        if (factory != null) {
//            factory.stopAecDump();
//        }
        if (mPeer != null) {
            if (mPeer.pc != null) {
                if (localMS != null) {
                    mPeer.pc.removeStream(localMS);
                    localMS.dispose();
                    localMS = null;

                }
                mPeer.pc.dispose();
                mPeer.pc = null;
            }
        }

        Log.d(TAG, "Closing audio source.");
        if (audioSource != null) {
            audioSource.dispose();
            audioSource = null;
        }

        Log.d(TAG, "Closing peer connection factory.");
        if (factory != null) {
            factory.dispose();
            factory = null;
        }

        Log.d(TAG, "Closing peer connection done.");
        PeerConnectionFactory.stopInternalTracingCapture();
        PeerConnectionFactory.shutdownInternalTracer();

        client.disconnect();
        client.close();
        client = null;
    }

    public void sendMessage(String to, String type, JSONObject payload) throws JSONException {
        JSONObject message = new JSONObject();
        message.put("to", to);
        message.put("type", type);
        message.put("payload", payload);
        client.emit("message", message);
    }

    private class MessageHandler {
        private HashMap<String, WebRtcClient_v2.Command> commandMap;

        private MessageHandler() {
            this.commandMap = new HashMap<>();
            commandMap.put("init", new WebRtcClient_v2.CreateOfferCommand());
            commandMap.put("offer", new WebRtcClient_v2.CreateAnswerCommand());
            commandMap.put("answer", new WebRtcClient_v2.SetRemoteSDPCommand());
            commandMap.put("candidate", new WebRtcClient_v2.AddIceCandidateCommand());
        }

        private Emitter.Listener onId = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                String id = (String) args[0];
                if (user.getId() == null) {
                    user.setId(id);
                    mListener.onCallReady(id);
                }
            }
        };

        private Emitter.Listener onMessage = new Emitter.Listener() {
            @Override
            public void call(Object... args) {
                JSONObject data = (JSONObject) args[0];
                try {
                    String from = data.getString("from");
                    String type = data.getString("type");
                    JSONObject payload = null;
                    if (!type.equals("init")) {
                        payload = data.getJSONObject("payload");
                    }
                    if (caller == null) {
                        caller = new Caller();
                        if (caller.getId() == null) {
                            caller.setId(from);
                        }
                    }
                    if (mPeer == null) {
                        mPeer = new Peer();
                        commandMap.get(type).execute(from, payload);
                    } else {
                        commandMap.get(type).execute(from, payload);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private class CreateOfferCommand implements WebRtcClient_v2.Command {
        public void execute(String peerId, JSONObject payload) throws JSONException {
            Log.d(TAG, "CreateOfferCommand");
            mPeer.pc.createOffer(mPeer, pcConstraints);
        }
    }

    private class CreateAnswerCommand implements WebRtcClient_v2.Command {
        public void execute(String peerId, JSONObject payload) throws JSONException {
            Log.d(TAG, "CreateAnswerCommand");
            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            mPeer.pc.setRemoteDescription(mPeer, sdp);
            mPeer.pc.createAnswer(mPeer, pcConstraints);
        }
    }

    private class SetRemoteSDPCommand implements WebRtcClient_v2.Command {
        public void execute(String peerId, JSONObject payload) throws JSONException {
            Log.d(TAG, "SetRemoteSDPCommand");
            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            mPeer.pc.setRemoteDescription(mPeer, sdp);
        }
    }

    private class AddIceCandidateCommand implements WebRtcClient_v2.Command {
        public void execute(String peerId, JSONObject payload) throws JSONException {
            Log.d(TAG, "AddIceCandidateCommand");
            String name = payload.getString("name");
            mListener.onReceiveGuestName(name);
            if (mPeer.pc.getRemoteDescription() != null) {
                IceCandidate candidate = new IceCandidate(
                        payload.getString("id"),
                        payload.getInt("label"),
                        payload.getString("candidate")
                );
                mPeer.iceCandidates.add(candidate);
                mPeer.pc.addIceCandidate(candidate);
            }
        }
    }

    private class Peer implements SdpObserver, PeerConnection.Observer {

        private PeerConnection pc;
        private List<IceCandidate> iceCandidates = new ArrayList<>();

        public Peer() {
            PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(iceServers);
            rtcConfiguration.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
            rtcConfiguration.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
            rtcConfiguration.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
            rtcConfiguration.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
            rtcConfiguration.keyType = PeerConnection.KeyType.ECDSA;

            this.pc = factory.createPeerConnection(rtcConfiguration, this);
            this.pc.addStream(localMS);
        }

        /**
         * SdpObserver
         * */
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            Log.d(TAG, "onCreateSuccess: ...");
            user.setSdp(sessionDescription.description);
            // TODO: modify sdp to use pcParams prefered codecs
            try {
                JSONObject payload = new JSONObject();
                payload.put("id", user.getId());
                payload.put("name", user.getName());
                payload.put("type", sessionDescription.type.canonicalForm());
                payload.put("sdp", sessionDescription.description);
                pc.setLocalDescription(Peer.this, sessionDescription);
                sendMessage(caller.getId(), sessionDescription.type.canonicalForm(), payload);
                mListener.onStatusChanged("Ready To Call");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onSetSuccess() {
            Log.d(TAG, "onSetSuccess: ...");
        }

        @Override
        public void onCreateFailure(String s) {
            Log.d(TAG, "onCreateFailure: " + s);
        }

        @Override
        public void onSetFailure(String s) {
            Log.d(TAG, "onSetFailure: " + s);
        }

        /**
         * PeerConnection.Observer
         * */
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {
            Log.d(TAG, "onSignalingChange: " + signalingState.toString());
        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d(TAG, "onIceConnectionChange: " + iceConnectionState);
            mListener.onStatusChanged(iceConnectionState.toString());
            switch (iceConnectionState) {
                case DISCONNECTED:
                    IceCandidate[] iceCandidates = new IceCandidate[mPeer.iceCandidates.size()];
                    for (int index = 0; index < mPeer.iceCandidates.size(); index++) {
                        iceCandidates[index] = mPeer.iceCandidates.get(index);
                    }
                    mPeer.pc.removeIceCandidates(iceCandidates);
                    break;
                case COMPLETED:
                    break;
                case FAILED:
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {
            Log.d(TAG, "onIceConnectionReceivingChange: " + b);
        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {
            Log.d(TAG, "onIceGatheringChange: " + iceGatheringState);
        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            Log.d(TAG, "onIceCandidate: " + iceCandidate);
            user.addCadidate(new Candidate(iceCandidate.sdpMid, iceCandidate.sdpMLineIndex, iceCandidate.sdp));
            JSONObject payload = new JSONObject();
            try {
                payload.put("name", user.getName());
                payload.put("label", iceCandidate.sdpMLineIndex);
                payload.put("id", iceCandidate.sdpMid);
                payload.put("candidate", iceCandidate.sdp);
                sendMessage(caller.getId(), "candidate", payload);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            Log.d(TAG, "onIceCandidatesRemoved: " + iceCandidates);
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d(TAG, "onAddStream: ...");
            mListener.onAddRemoteStream(mediaStream);
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(TAG, "onRemoveStream: ...");
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.d(TAG, "onDataChannel: " + dataChannel);
        }

        @Override
        public void onRenegotiationNeeded() {
            Log.d(TAG, "onRenegotiationNeeded: ...");
        }

        @Override
        public void onAddTrack(RtpReceiver rtpReceiver, MediaStream[] mediaStreams) {
            Log.d(TAG, "onAddTrack: ...");
        }
    }
}
