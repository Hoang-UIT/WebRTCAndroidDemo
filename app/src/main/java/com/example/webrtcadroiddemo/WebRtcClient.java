package com.example.webrtcadroiddemo;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;

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
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;


public class WebRtcClient {

    /**
     * Implement this interface to be notified of events.
     */
    public interface RtcListener {
        void onCallReady(String callId);

        void onStatusChanged(String newStatus);

        void onLocalStream(MediaStream localStream);

        void onAddRemoteStream(MediaStream remoteStream, int endPoint);

        void onRemoveRemoteStream(int endPoint);

        void onReceiveGuestName(String name);
    }

    private final static String TAG = WebRtcClient.class.getCanonicalName();
    private final static int MAX_PEER = 2;
    private boolean[] endPoints = new boolean[MAX_PEER];
    private PeerConnectionFactory factory;
    private HashMap<String, Peer> peers = new HashMap<>();
    private ArrayList<PeerConnection.IceServer> iceServers = new ArrayList<>();
    private PeerConnectionParameters pcParams;
    private MediaConstraints pcConstraints = new MediaConstraints();
    private MediaStream localMS;
    private VideoSource videoSource;
    private RtcListener mListener;
    private Socket client;
    private AudioSource audioSource;

    private Peer mPeer;
    private Caller user;
    private Caller caller;

    public WebRtcClient(String userName, Caller caller, RtcListener listener, String host, PeerConnectionParameters params, Context context) {
        if (caller != null) {
            this.caller = caller;
        }
        user = new Caller();
        user.setName(userName);
        mListener = listener;
        pcParams = params;

        PeerConnectionFactory.InitializationOptions initializationOptions = PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .setFieldTrials("WebRTC-IntelVP8/Enabled/")
                .createInitializationOptions();

        PeerConnectionFactory.initialize(initializationOptions);

        EglBase rootEglBase = EglBase.create();
        PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
        options.networkIgnoreMask = 4;
//        options.disableEncryption = false;
//        options.disableNetworkMonitor = true;
        DefaultVideoEncoderFactory defaultVideoEncoderFactory = new DefaultVideoEncoderFactory(rootEglBase.getEglBaseContext(),  true,  true);
        DefaultVideoDecoderFactory defaultVideoDecoderFactory = new DefaultVideoDecoderFactory(rootEglBase.getEglBaseContext());

        factory = PeerConnectionFactory.builder()
        .setOptions(options)
        .setVideoEncoderFactory(defaultVideoEncoderFactory)
        .setVideoDecoderFactory(defaultVideoDecoderFactory)
        .createPeerConnectionFactory();

        MessageHandler messageHandler = new MessageHandler();
        try {
            client = IO.socket(host);
        } catch (URISyntaxException e) {

            e.printStackTrace();
        }

        client.on("id", messageHandler.onId);
        client.on("message", messageHandler.onMessage);
        client.on("ListOnline", messageHandler.onListOnline);
        client.on("StartCall", messageHandler.onStartCall);
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

    public void sendMessage(String to, String type, JSONObject payload) throws JSONException {
//        JSONObject message = new JSONObject();
//        message.put("to", to);
//        message.put("type", type);
//        message.put("payload", payload);
//        client.emit("message", message);
    }
    /**
     * Call this method in Activity.onPause()
     */
    public void onPause() {
    }

    /**
     * Call this method in Activity.onResume()
     */
    public void onResume() {

    }


    /**
     * Call this method in Activity.onDestroy()
     */
    public void onDestroy() {
        if (peers.size() > 0) {
            for (Peer peer : peers.values()) {
                peer.pc.dispose();
                peer.pc = null;
            }
        }
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

    private int findEndPoint() {
        for (int i = 0; i < MAX_PEER; i++) if (!endPoints[i]) return i;
        return MAX_PEER;
    }

    private Peer addPeer(String id, int endPoint) {
        Peer peer = new Peer(id, endPoint);
        peers.put(id, peer);

        endPoints[endPoint] = true;
        return peer;
    }

    private void removePeer(String id) {
        if (id != null && !id.isEmpty()) {
            Peer peer = peers.get(id);
            peers.remove(peer.id);
            mListener.onRemoveRemoteStream(peer.endPoint);
            endPoints[peer.endPoint] = false;
            peer.pc.close();
            peer.pc = null;
        }
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
        mPeer = new Peer();
        if (caller != null) {
            SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER, caller.getSdp());

            int count = caller.getCandidates().size();
            mPeer.iceCandidates = new IceCandidate[count];
            for (int index = 0; index < count; index++) {
                Candidate item = caller.getCandidates().get(index);
                IceCandidate candidate = new IceCandidate(
                        item.getId(),
                        item.getLabel(),
                        item.getCandidate()
                );
                mPeer.iceCandidates[index] = candidate;
                mPeer.pc.addIceCandidate(candidate);
            }
            mPeer.pc.setRemoteDescription(mPeer, sdp);
            mPeer.pc.createAnswer(mPeer, pcConstraints);
        } else {
            mPeer.pc.createOffer(mPeer, pcConstraints);
        }
//        try {
//            JSONObject message = new JSONObject();
//            message.put("name", name);
//            client.emit("readyToStream", message);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }

    private void setMediaStream() {
        localMS = factory.createLocalMediaStream("ARDAMS");
        if (pcParams.videoCallEnabled) {
            MediaConstraints videoConstraints = new MediaConstraints();
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxHeight", Integer.toString(pcParams.videoHeight)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxWidth", Integer.toString(pcParams.videoWidth)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("maxFrameRate", Integer.toString(pcParams.videoFps)));
            videoConstraints.mandatory.add(new MediaConstraints.KeyValuePair("minFrameRate", Integer.toString(pcParams.videoFps)));

//            videoSource = factory.createVideoSource(videoCapturer, videoConstraints);
            localMS.addTrack(factory.createVideoTrack("ARDAMSv0", videoSource));
        }

        audioSource = factory.createAudioSource(new MediaConstraints());
        AudioTrack audioTrack = factory.createAudioTrack("ARDAMSa0", audioSource);
        audioTrack.setEnabled(true);
        audioTrack.setVolume(1);
        localMS.addTrack(audioTrack);

        mListener.onLocalStream(localMS);
    }

    private VideoCapturer getVideoCapturer() {

//        String frontCameraDeviceName = VideoCapturerAndroid.getNameOfFrontFacingDevice();
//        return VideoCapturerAndroid.create(frontCameraDeviceName);
        return null;
    }

    private class Peer implements SdpObserver, PeerConnection.Observer{
        private PeerConnection pc;
        private String id;
        private int endPoint;
        private IceCandidate[] iceCandidates;

        public Peer(String id, int endPoint) {
            this.id = id;
            this.endPoint = endPoint;

            PeerConnection.RTCConfiguration rtcConfiguration = new PeerConnection.RTCConfiguration(iceServers);
            rtcConfiguration.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
            rtcConfiguration.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
            rtcConfiguration.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
            rtcConfiguration.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
            rtcConfiguration.keyType = PeerConnection.KeyType.ECDSA;

            rtcConfiguration.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

            this.pc = factory.createPeerConnection(rtcConfiguration, this);
            this.pc.addStream(localMS);
            mListener.onStatusChanged("CONNECTING");
        }

        public Peer() {
            // TCP candidates are only useful when connecting to a server that supports ICE-TCP
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

            user.setSdp(sessionDescription.description);

            // TODO: modify sdp to use pcParams prefered codecs
            try {
                JSONObject payload = new JSONObject();
                payload.put("id", user.getId());
                payload.put("name", user.getName());
                payload.put("type", sessionDescription.type.canonicalForm());
                payload.put("sdp", sessionDescription.description);
//                sendMessage(id, sessionDescription.type.canonicalForm(), payload);
                pc.setLocalDescription(Peer.this, sessionDescription);
                client.emit("ReadyToCall", payload);
                mListener.onStatusChanged("ReadyToCall");
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

        }

        @Override
        public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {
            Log.d(TAG, "onIceConnectionChange: " + iceConnectionState);
            mListener.onStatusChanged(iceConnectionState.toString());
            switch (iceConnectionState) {
                case DISCONNECTED:
                    mPeer.pc.removeIceCandidates(mPeer.iceCandidates);
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

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }

        @Override
        public void onIceCandidate(IceCandidate iceCandidate) {
            user.addCadidate(new Candidate(iceCandidate.sdpMid, iceCandidate.sdpMLineIndex, iceCandidate.sdp));
            JSONObject candidate = new JSONObject();
            try {
                candidate.put("name", user.getName());
                candidate.put("label", iceCandidate.sdpMLineIndex);
                candidate.put("id", iceCandidate.sdpMid);
                candidate.put("candidate", iceCandidate.sdp);
                client.emit("updateCandidate", candidate);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            if (caller != null && user.getCandidates().size() == 5) {
                client.emit("StartCall", caller.getId());
            }

//            JSONObject payload = new JSONObject();
//            try {
//                payload.put("name", userName);
//                payload.put("label", iceCandidate.sdpMLineIndex);
//                payload.put("id", iceCandidate.sdpMid);
//                payload.put("candidate", iceCandidate.sdp);
//                sendMessage(id, "candidate", payload);
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
        }

        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {
            Log.d(TAG, "onIceCandidatesRemoved: ...");
        }

        @Override
        public void onAddStream(MediaStream mediaStream) {
            Log.d(TAG, "onAddStream " + mediaStream.toString());
            // remote streams are displayed from 1 to MAX_PEER (0 is localStream)
            mListener.onAddRemoteStream(mediaStream, endPoint + 1);
        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {
            Log.d(TAG, "onRemoveStream " + mediaStream.toString());
            removePeer(id);
        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {
            Log.d(TAG, "onDataChannel: ...");
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

    /**
     * Send a message through the signaling server
     *
     * @param to      id of recipient
     * @param type    type of message
     * @param payload payload of message
     * @throws JSONException
     */

    private class MessageHandler {
        private HashMap<String, Command> commandMap;

        private MessageHandler() {
            this.commandMap = new HashMap<>();
            commandMap.put("init", new CreateOfferCommand());
            commandMap.put("offer", new CreateAnswerCommand());
            commandMap.put("answer", new SetRemoteSDPCommand());
            commandMap.put("candidate", new AddIceCandidateCommand());
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
                    // if peer is unknown, try to add him
                    if (!peers.containsKey(from)) {
                        // if MAX_PEER is reach, ignore the call
                        int endPoint = findEndPoint();
                        if (endPoint != MAX_PEER) {
                            Peer peer = addPeer(from, endPoint);
                            peer.pc.addStream(localMS);
                            commandMap.get(type).execute(from, payload);
                        }
                    } else {
                        commandMap.get(type).execute(from, payload);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
        }
        };

        private Emitter.Listener onListOnline = args -> {
            JSONObject data = (JSONObject) args[0];
            Log.d(TAG, "call: " + data.toString());
        };

        private Emitter.Listener onStartCall = args -> {
            JSONObject data = (JSONObject) args[0];
            Log.d(TAG, "Start call: " + data.toString());
            Gson gson = new Gson();
            caller = gson.fromJson(data.toString(), Caller.class);
            mListener.onReceiveGuestName(caller.getName());
            SessionDescription sdp = new SessionDescription(SessionDescription.Type.ANSWER, caller.getSdp());
            mPeer.pc.setRemoteDescription(mPeer, sdp);
            int count = caller.getCandidates().size();
            mPeer.iceCandidates = new IceCandidate[count];
            for (int index = 0; index < count; index++) {
                Candidate item = caller.getCandidates().get(index);
                IceCandidate candidate = new IceCandidate(
                        item.getId(),
                        item.getLabel(),
                        item.getCandidate()
                );
                mPeer.iceCandidates[index] = candidate;
                mPeer.pc.addIceCandidate(candidate);
            }
        };
    }

    private interface Command {
        void execute(String peerId, JSONObject payload) throws JSONException;
    }

    private class CreateOfferCommand implements Command {
        public void execute(String peerId, JSONObject payload) throws JSONException {
            Log.d(TAG, "CreateOfferCommand");
            Peer peer = peers.get(peerId);
            Log.d(TAG, "PeerId:" + peerId);
            peer.pc.createOffer(peer, pcConstraints);
        }
    }

    private class CreateAnswerCommand implements Command {
        public void execute(String peerId, JSONObject payload) throws JSONException {
            Log.d(TAG, "CreateAnswerCommand");
            Peer peer = peers.get(peerId);
            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            peer.pc.setRemoteDescription(peer, sdp);
            peer.pc.createAnswer(peer, pcConstraints);
        }
    }

    private class SetRemoteSDPCommand implements Command {
        public void execute(String peerId, JSONObject payload) throws JSONException {
            Log.d(TAG, "SetRemoteSDPCommand");
            Peer peer = peers.get(peerId);
            SessionDescription sdp = new SessionDescription(
                    SessionDescription.Type.fromCanonicalForm(payload.getString("type")),
                    payload.getString("sdp")
            );
            peer.pc.setRemoteDescription(peer, sdp);
        }
    }

    private class AddIceCandidateCommand implements Command {
        public void execute(String peerId, JSONObject payload) throws JSONException {
            Log.d(TAG, "AddIceCandidateCommand");
            String name = payload.getString("name");
            mListener.onReceiveGuestName(name);
            PeerConnection pc = peers.get(peerId).pc;
            if (pc.getRemoteDescription() != null) {
                IceCandidate candidate = new IceCandidate(
                        payload.getString("id"),
                        payload.getInt("label"),
                        payload.getString("candidate")
                );
                pc.addIceCandidate(candidate);
            }
        }
    }
}


