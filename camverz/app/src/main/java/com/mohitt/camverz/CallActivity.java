package com.mohitt.camverz;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraVideoCapturer;
import org.webrtc.DataChannel;
import org.webrtc.DefaultVideoDecoderFactory;
import org.webrtc.DefaultVideoEncoderFactory;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon;
import org.webrtc.RtpReceiver;
import org.webrtc.RtpTransceiver;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.util.ArrayList;
import java.util.List;

import io.socket.client.Socket;

public class CallActivity extends AppCompatActivity {

    private static final String TAG = "CALL_ACTIVITY";
    private static final int PERMISSION_REQ = 101;

    SurfaceViewRenderer localView, remoteView;
    ImageView btnMute, btnSwitch, btnEnd;

    boolean isMuted = false;
    boolean isCameraSwitching = false;

    EglBase eglBase;
    VideoCapturer videoCapturer;
    PeerConnectionFactory factory;
    PeerConnection peerConnection;
    SurfaceTextureHelper surfaceTextureHelper;

    VideoTrack localVideoTrack;
    AudioTrack localAudioTrack;

    String peerId, myUid, roomName;
    Socket socket;
    boolean isInitiator = false;
    boolean callEnded = false;

    Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Runnable timeoutRunnable = () -> {
        if (!callEnded) {
            Toast.makeText(CallActivity.this, "Call Timed Out!", Toast.LENGTH_SHORT).show();
            disconnect();
        }
    };

    private static class SimpleSdp implements SdpObserver {
        private final String eventName;
        public SimpleSdp(String eventName) { this.eventName = eventName; }
        @Override public void onCreateSuccess(SessionDescription s) { Log.d(TAG, eventName + " - onCreateSuccess"); }
        @Override public void onSetSuccess() { Log.d(TAG, eventName + " - onSetSuccess"); }
        @Override public void onCreateFailure(String s) { Log.e(TAG, eventName + " - onCreateFailure: " + s); }
        @Override public void onSetFailure(String s) { Log.e(TAG, eventName + " - onSetFailure: " + s); }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        peerId = getIntent().getStringExtra("peer");
        myUid = FirebaseAuth.getInstance().getUid();
        socket = SocketManager.getInstance();

        roomName = myUid.compareTo(peerId) < 0 ? myUid + "_" + peerId : peerId + "_" + myUid;
        isInitiator = myUid.compareTo(peerId) < 0;

        initUI();
        checkPermissions();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (videoCapturer != null) {
            try {
                videoCapturer.startCapture(720, 1280, 30);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    private void initUI() {
        localView = findViewById(R.id.localView);
        remoteView = findViewById(R.id.remoteView);
        btnMute = findViewById(R.id.btnMute);
        btnSwitch = findViewById(R.id.btnSwitchCamera);
        btnEnd = findViewById(R.id.btnEnd);

        btnMute.setOnClickListener(v -> toggleMute());
        btnSwitch.setOnClickListener(v -> switchCamera());
        btnEnd.setOnClickListener(v -> disconnect());
    }

    private void checkPermissions() {
        String[] perm = {Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO};
        if (ContextCompat.checkSelfPermission(this, perm[0]) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, perm[1]) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, perm, PERMISSION_REQ);
        } else {
            startCall();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQ && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                startCall();
            } else {
                Toast.makeText(this, "Camera & Audio permissions are required.", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void startCall() {
        CallManager.setCallActive(true);
        setupViews();
        initWebRTC();
        setupSocket();
        initPeerConnection();

        try {
            Log.d(TAG, "Emitting join-call-room for room: " + roomName);
            JSONObject roomInfo = new JSONObject();
            roomInfo.put("room", roomName);
            roomInfo.put("uid", myUid);
            socket.emit("join-call-room", roomInfo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        timeoutHandler.postDelayed(timeoutRunnable, 15000);
    }

    private void setupViews() {
        eglBase = EglBase.create();
        localView.init(eglBase.getEglBaseContext(), null);
        remoteView.init(eglBase.getEglBaseContext(), null);
        localView.setZOrderMediaOverlay(true);
        localView.setMirror(true);
        remoteView.setMirror(false);
    }

    private void initWebRTC() {
        PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(this).createInitializationOptions());
        factory = PeerConnectionFactory.builder()
                .setVideoEncoderFactory(new DefaultVideoEncoderFactory(eglBase.getEglBaseContext(), true, true))
                .setVideoDecoderFactory(new DefaultVideoDecoderFactory(eglBase.getEglBaseContext()))
                .createPeerConnectionFactory();

        AudioSource audioSource = factory.createAudioSource(new MediaConstraints());
        localAudioTrack = factory.createAudioTrack("AUDIO", audioSource);

        videoCapturer = createCapturer();
        surfaceTextureHelper = SurfaceTextureHelper.create("capture", eglBase.getEglBaseContext());
        VideoSource videoSource = factory.createVideoSource(videoCapturer.isScreencast());

        videoCapturer.initialize(surfaceTextureHelper, getApplicationContext(), videoSource.getCapturerObserver());
        videoCapturer.startCapture(720, 1280, 30);

        localVideoTrack = factory.createVideoTrack("VIDEO", videoSource);
        localVideoTrack.addSink(localView);
    }

    private VideoCapturer createCapturer() {
        Camera2Enumerator enumerator = new Camera2Enumerator(this);
        for (String deviceName : enumerator.getDeviceNames()) {
            if (enumerator.isFrontFacing(deviceName)) {
                return enumerator.createCapturer(deviceName, null);
            }
        }
        return enumerator.createCapturer(enumerator.getDeviceNames()[0], null); // Fallback
    }

    private void initPeerConnection() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;

        peerConnection = factory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                Log.d(TAG, "onIceCandidate: Sending ICE candidate");
                sendIce(iceCandidate);
            }

            @Override
            public void onTrack(RtpTransceiver transceiver) {
                Log.d(TAG, "onTrack: Remote track received");
                if (transceiver.getReceiver().track() instanceof VideoTrack) {
                    VideoTrack remoteVideoTrack = (VideoTrack) transceiver.getReceiver().track();
                    runOnUiThread(() -> remoteVideoTrack.addSink(remoteView));
                }
            }
            
            @Override public void onConnectionChange(PeerConnection.PeerConnectionState s) {
                Log.d(TAG, "onConnectionChange: " + s);
                if (s == PeerConnection.PeerConnectionState.DISCONNECTED || s == PeerConnection.PeerConnectionState.CLOSED || s == PeerConnection.PeerConnectionState.FAILED) {
                    disconnect();
                }
            }

            // Unused mandatory overrides
            @Override public void onSignalingChange(PeerConnection.SignalingState s) {}
            @Override public void onIceConnectionChange(PeerConnection.IceConnectionState s) {}
            @Override public void onIceConnectionReceivingChange(boolean b) {}
            @Override public void onIceGatheringChange(PeerConnection.IceGatheringState s) {}
            @Override public void onIceCandidatesRemoved(IceCandidate[] i) {}
            @Override public void onAddStream(MediaStream s) {}
            @Override public void onRemoveStream(MediaStream s) {}
            @Override public void onDataChannel(DataChannel d) {}
            @Override public void onRenegotiationNeeded() {}
            @Override public void onAddTrack(RtpReceiver r, MediaStream[] s) {}
        });

        if (localVideoTrack != null) peerConnection.addTrack(localVideoTrack);
        if (localAudioTrack != null) peerConnection.addTrack(localAudioTrack);
    }

    private void setupSocket() {
        socket.off(); // Clean slate

        socket.on("peer-ready", args -> {
            Log.d(TAG, "Socket: peer-ready received");
            timeoutHandler.removeCallbacks(timeoutRunnable);
            if (isInitiator) createOffer();
        });

        socket.on("receive-offer", args -> {
            Log.d(TAG, "Socket: receive-offer received");
            try {
                JSONObject data = (JSONObject) args[0];
                String offerSdp = data.getString("offer");
                SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER, offerSdp);
                if (peerConnection != null) {
                    peerConnection.setRemoteDescription(new SimpleSdp("RemoteOffer"), sdp);
                    createAnswer();
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error processing receive-offer", e);
            }
        });

        socket.on("receive-answer", args -> {
            Log.d(TAG, "Socket: receive-answer received");
            try {
                JSONObject data = (JSONObject) args[0];
                String answerSdp = data.getString("answer");
                SessionDescription sdp = new SessionDescription(SessionDescription.Type.ANSWER, answerSdp);
                if (peerConnection != null) {
                    peerConnection.setRemoteDescription(new SimpleSdp("RemoteAnswer"), sdp);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error processing receive-answer", e);
            }
        });

        socket.on("receive-ice", args -> {
            Log.d(TAG, "Socket: receive-ice received");
            try {
                JSONObject data = (JSONObject) args[0];
                IceCandidate candidate = new IceCandidate(
                        data.getString("sdpMid"),
                        data.getInt("sdpMLineIndex"),
                        data.getString("candidate")
                );
                if (peerConnection != null) {
                    peerConnection.addIceCandidate(candidate);
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error processing receive-ice", e);
            }
        });
    }

    private void createOffer() {
        if (peerConnection == null) return;
        Log.d(TAG, "Creating WebRTC Offer");
        peerConnection.createOffer(new SimpleSdp("CreateOffer") {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                super.onCreateSuccess(sdp);
                if (peerConnection != null) {
                    peerConnection.setLocalDescription(new SimpleSdp("LocalOffer"), sdp);
                    sendOffer(sdp);
                }
            }
        }, new MediaConstraints());
    }

    private void createAnswer() {
        if (peerConnection == null) return;
        Log.d(TAG, "Creating WebRTC Answer");
        peerConnection.createAnswer(new SimpleSdp("CreateAnswer") {
            @Override
            public void onCreateSuccess(SessionDescription sdp) {
                super.onCreateSuccess(sdp);
                if (peerConnection != null) {
                    peerConnection.setLocalDescription(new SimpleSdp("LocalAnswer"), sdp);
                    sendAnswer(sdp);
                }
            }
        }, new MediaConstraints());
    }

    private void sendOffer(SessionDescription sdp) {
        try {
            Log.d(TAG, "Sending offer to peer");
            JSONObject offer = new JSONObject();
            offer.put("to", peerId);
            offer.put("offer", sdp.description);
            offer.put("room", roomName);
            socket.emit("send-offer", offer);
        } catch (JSONException e) {
            Log.e(TAG, "Error sending offer", e);
        }
    }

    private void sendAnswer(SessionDescription sdp) {
        try {
            Log.d(TAG, "Sending answer to peer");
            JSONObject answer = new JSONObject();
            answer.put("to", peerId);
            answer.put("answer", sdp.description);
            answer.put("room", roomName);
            socket.emit("send-answer", answer);
        } catch (JSONException e) {
            Log.e(TAG, "Error sending answer", e);
        }
    }

    private void sendIce(IceCandidate candidate) {
        try {
            JSONObject ice = new JSONObject();
            ice.put("to", peerId);
            ice.put("candidate", candidate.sdp);
            ice.put("sdpMid", candidate.sdpMid);
            ice.put("sdpMLineIndex", candidate.sdpMLineIndex);
            ice.put("room", roomName);
            socket.emit("send-ice", ice);
        } catch (JSONException e) {
            Log.e(TAG, "Error sending ICE candidate", e);
        }
    }

    private void toggleMute() {
        isMuted = !isMuted;
        if (localAudioTrack != null) localAudioTrack.setEnabled(!isMuted);
        btnMute.setImageResource(isMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic_on);
    }

    private void switchCamera() {
        if (videoCapturer instanceof CameraVideoCapturer && !isCameraSwitching) {
            isCameraSwitching = true;
            Log.d(TAG, "Switching camera");
            ((CameraVideoCapturer) videoCapturer).switchCamera(new CameraVideoCapturer.CameraSwitchHandler() {
                @Override
                public void onCameraSwitchDone(boolean isFront) {
                    isCameraSwitching = false;
                }
                @Override
                public void onCameraSwitchError(String s) {
                    isCameraSwitching = false;
                    Log.e(TAG, "Camera switch error: " + s);
                }
            });
        }
    }

    private void disconnect() {
        if (callEnded) {
            return;
        }
        callEnded = true;
        Log.d(TAG, "Disconnecting call and releasing ALL resources.");

        CallManager.setCallActive(false);
        timeoutHandler.removeCallbacks(timeoutRunnable);
        socket.off();

        if (localVideoTrack != null) {
            localVideoTrack.removeSink(localView);
        }

        if (videoCapturer != null) {
            try { 
                videoCapturer.stopCapture(); 
            } catch (InterruptedException e) { 
                e.printStackTrace(); 
            }
            videoCapturer.dispose();
            videoCapturer = null;
        }
        
        if (localVideoTrack != null) {
            localVideoTrack.dispose();
        }
        if (localAudioTrack != null) {
            localAudioTrack.dispose();
        }
        
        if (surfaceTextureHelper != null) {
            surfaceTextureHelper.dispose();
            surfaceTextureHelper = null;
        }

        if (peerConnection != null) {
            peerConnection.close();
            peerConnection.dispose();
            peerConnection = null;
        }
        
        runOnUiThread(() -> {
            if (localView != null) {
                localView.release();
            }
            if (remoteView != null) {
                remoteView.release();
            }
        });

        if (factory != null) {
            factory.dispose();
            factory = null;
        }

        if (eglBase != null) {
            eglBase.release();
            eglBase = null;
        }

        finish();
    }

    @Override
    protected void onDestroy() {
        disconnect();
        super.onDestroy();
    }
}
