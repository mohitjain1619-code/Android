package com.mohitt.camverz;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
// import com.applovin.mediation.ads.MaxInterstitialAd;
// import com.applovin.mediation.MaxAd;
// import com.applovin.mediation.MaxAdListener;
// import com.applovin.mediation.MaxError;
import com.google.gson.JsonObject;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;
import com.mohitt.camverz.api.TokenManager;

import de.hdodenhof.circleimageview.CircleImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import io.socket.emitter.Emitter;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
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

import io.socket.client.Socket;

public class CallActivity extends AppCompatActivity {

    private static final String TAG = "CALL_ACTIVITY";
    private static final int PERMISSION_REQ = 101;

    private SurfaceViewRenderer localView, remoteView;
    private ImageButton btnMute, btnSwitchCamera, btnEnd, btnToggleVideo;
    private RelativeLayout controlsContainer, callContainer;
    private FrameLayout localViewContainer;
    private LinearLayout bottomControls, topUserInfoBar;
    private CircleImageView peerAvatar;
    private TextView connectionStatus, peerName, followButton;
    private View connectionDot;
    private androidx.cardview.widget.CardView followButtonCard;

    private RelativeLayout remoteAvatarOverlay;
    private FrameLayout localAvatarOverlay;
    private ImageView remoteAvatarBlurBg;
    private CircleImageView remoteAvatarLarge, localAvatarSmall;
    private TextView remoteAvatarNameText;

    private boolean isMuted = false, isVideoOff = false;
    private boolean isCameraSwitching = false;
    private boolean areControlsVisible = true;
    private boolean turnFallbackAttempted = false; // Track if TURN fallback was triggered
    private boolean isCaller = false;
    private boolean isPrivateCall = false;
    private boolean isVideoCall = true;
    private boolean connectionAnimationActive = true;

    private EglBase eglBase;
    private VideoCapturer videoCapturer;
    private PeerConnectionFactory factory;
    private PeerConnection peerConnection;
    private SurfaceTextureHelper surfaceTextureHelper;
    private Emitter.Listener receivePrivateOfferListener;
    private Emitter.Listener receivePrivateAnswerListener;
    private Emitter.Listener receivePrivateIceListener;
    private Emitter.Listener privateCallAcceptedListener;
    private Emitter.Listener privateCallRejectedListener;
    private Emitter.Listener privateCallEndedListener;
    private Emitter.Listener callFailedListener;

    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;

    private String peerId, myUid, roomName;
    private String peerNameValue, peerAvatarUrl;
    private Socket socket;
    private Emitter.Listener peerReadyListener;
    private Emitter.Listener receiveOfferListener;
    private Emitter.Listener receiveAnswerListener;
    private Emitter.Listener receiveIceListener;
    private Emitter.Listener peerDisconnectedListener;
    private Emitter.Listener callControlListener;

    private com.google.android.gms.ads.interstitial.InterstitialAd interstitialAd;
    private int retryAttempt;
    private boolean isInitiator = false;

    private final android.content.BroadcastReceiver headsetReceiver = new android.content.BroadcastReceiver() {
        @Override
        public void onReceive(android.content.Context context, android.content.Intent intent) {
            if (android.content.Intent.ACTION_HEADSET_PLUG.equals(intent.getAction())) {
                updateAudioRouting();
            }
        }
    };
    private boolean callEnded = false;
    private boolean isFollowing = false;
    private boolean isUpdatingFollow = false;

    private ApiService api;
    private TokenManager tokenManager;
    private android.media.AudioManager audioManager;

    private final Handler timeoutHandler = new Handler(Looper.getMainLooper());
    private final Handler autoHideHandler = new Handler(Looper.getMainLooper());
    private Runnable autoHideRunnable;
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
        if (isVpnActive()) {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("VPN Detected")
                .setMessage("Please turn off your VPN for video calling to work properly. VPN connections cause calls to fail.\n\nBehtar video call quality ke liye kripya apna VPN/Proxy band kijiye.")
                .setCancelable(false)
                .setPositiveButton("OK", (dialog, which) -> finish())
                .show();
            return;
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);

        // Preload AdMob Interstitial Ad
        com.google.android.gms.ads.AdRequest adRequest = new com.google.android.gms.ads.AdRequest.Builder().build();
        com.google.android.gms.ads.interstitial.InterstitialAd.load(this, getString(R.string.admob_interstitial_ad_unit_id), adRequest,
            new com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback() {
                @Override
                public void onAdLoaded(@NonNull com.google.android.gms.ads.interstitial.InterstitialAd ad) {
                    interstitialAd = ad;
                    retryAttempt = 0;
                    interstitialAd.setFullScreenContentCallback(new com.google.android.gms.ads.FullScreenContentCallback() {
                        @Override
                        public void onAdDismissedFullScreenContent() {
                            interstitialAd = null;
                            finish();
                        }

                        @Override
                        public void onAdFailedToShowFullScreenContent(com.google.android.gms.ads.AdError adError) {
                            interstitialAd = null;
                            finish();
                        }
                    });
                }

                @Override
                public void onAdFailedToLoad(@NonNull com.google.android.gms.ads.LoadAdError loadAdError) {
                    interstitialAd = null;
                    retryAttempt++;
                    int delayMillis = (int) Math.min(Math.pow(2, Math.min(6, retryAttempt)) * 1000, 30000);
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        // Re-try loading
                        com.google.android.gms.ads.AdRequest retryRequest = new com.google.android.gms.ads.AdRequest.Builder().build();
                        com.google.android.gms.ads.interstitial.InterstitialAd.load(CallActivity.this, getString(R.string.admob_interstitial_ad_unit_id), retryRequest, this);
                    }, delayMillis);
                }
            });

        api = ApiClient.getInstance(this).getApi();
        tokenManager = TokenManager.getInstance(this);

        peerId = getIntent().getStringExtra("peer");
        if (peerId == null) {
            peerId = getIntent().getStringExtra("targetUserId");
        }
        if (peerId == null) {
            peerId = getIntent().getStringExtra("callerId");
        }

        isCaller = getIntent().getBooleanExtra("isCaller", false);
        isPrivateCall = getIntent().getBooleanExtra("isPrivateCall", false);
        isVideoCall = getIntent().getBooleanExtra("isVideoCall", true);

        myUid = tokenManager.getUserId();
        socket = SocketManager.getInstance();

        if (myUid == null || peerId == null) {
            finish();
            return;
        }

        roomName = getIntent().getStringExtra("roomName");
        if (roomName == null || roomName.isEmpty()) {
            roomName = myUid.compareTo(peerId) < 0 ? myUid + "_" + peerId : peerId + "_" + myUid;
        }
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
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        btnEnd = findViewById(R.id.btnEnd);
        btnToggleVideo = findViewById(R.id.btnToggleVideo);
        controlsContainer = findViewById(R.id.controls_container);
        callContainer = findViewById(R.id.call_container);
        localViewContainer = findViewById(R.id.local_view_container);
        bottomControls = findViewById(R.id.bottom_controls);
        topUserInfoBar = findViewById(R.id.top_user_info_bar);
        connectionStatus = findViewById(R.id.connection_status);
        connectionDot = findViewById(R.id.connection_dot);
        peerAvatar = findViewById(R.id.peer_avatar);
        peerName = findViewById(R.id.peer_name);
        followButton = findViewById(R.id.follow_button);
        followButtonCard = findViewById(R.id.follow_button_card);

        remoteAvatarOverlay = findViewById(R.id.remote_avatar_overlay);
        localAvatarOverlay = findViewById(R.id.local_avatar_overlay);
        remoteAvatarBlurBg = findViewById(R.id.remote_avatar_blur_bg);
        remoteAvatarLarge = findViewById(R.id.remote_avatar_large);
        localAvatarSmall = findViewById(R.id.local_avatar_small);
        remoteAvatarNameText = findViewById(R.id.remote_avatar_name_text);

        btnMute.setOnClickListener(v -> toggleMute());
        btnSwitchCamera.setOnClickListener(v -> switchCamera());
        btnEnd.setOnClickListener(v -> disconnect());
        btnToggleVideo.setOnClickListener(v -> toggleVideo());
        followButton.setOnClickListener(v -> handleFollowClick());

        String localAvatar = tokenManager.getUserAvatar();
        if (localAvatar != null && !localAvatar.isEmpty()) {
            int avatarResId = getResources().getIdentifier(localAvatar, "drawable", getPackageName());
            if (avatarResId != 0) {
                Glide.with(this).load(avatarResId).placeholder(R.drawable.av1).into(localAvatarSmall);
            }
        }
        
        remoteView.setOnClickListener(v -> toggleControlsVisibility());
        setupDraggableLocalView();
        setupButtonAnimations();
        setupAutoHideControls();
        startConnectionAnimation();
        loadPeerUserInfo();
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
        if (isPrivateCall && isCaller) {
            sendStartPrivateCall();
        }
        if (isPrivateCall && !isCaller) {
            sendAcceptPrivateCall();
        }
        fetchIceServersAndStartCall();

        // Route WebRTC call audio to Speakerphone or Earphones dynamically
        try {
            audioManager = (android.media.AudioManager) getSystemService(android.content.Context.AUDIO_SERVICE);
            if (audioManager != null) {
                audioManager.setMode(android.media.AudioManager.MODE_IN_COMMUNICATION);
                updateAudioRouting();
                
                // Register dynamic listener for earphones plug/unplug events
                android.content.IntentFilter filter = new android.content.IntentFilter(android.content.Intent.ACTION_HEADSET_PLUG);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(headsetReceiver, filter, android.content.Context.RECEIVER_EXPORTED);
                } else {
                    registerReceiver(headsetReceiver, filter);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting up AudioManager: " + e.getMessage());
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
        return enumerator.createCapturer(enumerator.getDeviceNames()[0], null);
    }

    private void initPeerConnection() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun2.l.google.com:19302").createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("stun:stun3.l.google.com:19302").createIceServer());

        // OpenRelay TURN Servers for NAT Traversal on Mobile Cellular Networks
        iceServers.add(PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer());
        iceServers.add(PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp")
                .setUsername("openrelayproject")
                .setPassword("openrelayproject")
                .createIceServer());

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
                    runOnUiThread(() -> {
                        remoteVideoTrack.addSink(remoteView);
                        markConnected();
                    });
                } else if (transceiver.getReceiver().track() instanceof AudioTrack) {
                    AudioTrack remoteAudioTrack = (AudioTrack) transceiver.getReceiver().track();
                    remoteAudioTrack.setEnabled(true);
                    remoteAudioTrack.setVolume(1.0);
                    runOnUiThread(CallActivity.this::markConnected);
                }
            }
            
            @Override public void onConnectionChange(PeerConnection.PeerConnectionState s) {
                Log.d(TAG, "onConnectionChange: " + s);
                if (s == PeerConnection.PeerConnectionState.DISCONNECTED || s == PeerConnection.PeerConnectionState.CLOSED || s == PeerConnection.PeerConnectionState.FAILED) {
                    disconnect();
                }
            }

            @Override public void onSignalingChange(PeerConnection.SignalingState s) {}
            @Override public void onIceConnectionChange(PeerConnection.IceConnectionState s) {
                Log.d(TAG, "onIceConnectionChange: " + s);
                if (s == PeerConnection.IceConnectionState.FAILED && !turnFallbackAttempted) {
                    Log.w(TAG, "STUN failed, attempting TURN fallback...");
                    turnFallbackAttempted = true;
                    fetchTurnServersAndReconnect();
                }
            }
            @Override public void onIceConnectionReceivingChange(boolean b) {}
            @Override public void onIceGatheringChange(PeerConnection.IceGatheringState s) {}
            @Override public void onIceCandidatesRemoved(IceCandidate[] i) {}
            @Override public void onAddStream(MediaStream s) {}
            @Override public void onRemoveStream(MediaStream s) {}
            @Override public void onDataChannel(DataChannel d) {}
            @Override public void onRenegotiationNeeded() {}
            @Override public void onAddTrack(RtpReceiver r, MediaStream[] s) {}
        });

        if (localVideoTrack != null && peerConnection != null) {
            peerConnection.addTrack(localVideoTrack);
        }
        if (localAudioTrack != null && peerConnection != null) {
            peerConnection.addTrack(localAudioTrack);
        }
    }

    private void fetchIceServersAndStartCall() {
        fetchIceServersWithTurn(false);
    }

    private void fetchTurnServersAndReconnect() {
        fetchIceServersWithTurn(true);
    }

    private void fetchIceServersWithTurn(boolean useTurn) {
        api.getIceServers(useTurn).enqueue(new retrofit2.Callback<JsonObject>() {
            @Override
            public void onResponse(retrofit2.Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String body = response.body().toString();
                    List<PeerConnection.IceServer> iceServers = parseIceServers(body);
                    runOnUiThread(() -> startCallWithIceServers(iceServers));
                } else {
                    Log.e(TAG, "Failed to fetch ICE servers: " + response.code());
                    runOnUiThread(() -> {
                        Toast.makeText(CallActivity.this, "Failed to fetch ICE servers", Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            }

            @Override
            public void onFailure(retrofit2.Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Failed to fetch ICE servers: " + t.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(CallActivity.this, "Failed to fetch ICE servers", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });
    }

    private List<PeerConnection.IceServer> parseIceServers(String json) {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        try {
            JSONObject jsonObj = new JSONObject(json);
            if (jsonObj.has("iceServers")) {
                org.json.JSONArray array = jsonObj.getJSONArray("iceServers");
                for (int i = 0; i < array.length(); i++) {
                    JSONObject serverObj = array.getJSONObject(i);
                    if (serverObj.has("urls")) {
                        Object urlsObj = serverObj.get("urls");
                        
                        if (urlsObj instanceof String) {
                            iceServers.add(PeerConnection.IceServer.builder((String) urlsObj).createIceServer());
                        } else if (urlsObj instanceof org.json.JSONArray) {
                            org.json.JSONArray urlArray = (org.json.JSONArray) urlsObj;
                            List<String> urls = new ArrayList<>();
                            for (int j = 0; j < urlArray.length(); j++) {
                                urls.add(urlArray.getString(j));
                            }
                            PeerConnection.IceServer.Builder builder = PeerConnection.IceServer.builder(urls);
                            if (serverObj.has("username") && serverObj.has("credential")) {
                                builder.setUsername(serverObj.getString("username"));
                                builder.setPassword(serverObj.getString("credential"));
                            }
                            iceServers.add(builder.createIceServer());
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing ICE servers: " + e.getMessage());
        }
        
        if (iceServers.isEmpty()) {
            iceServers.add(PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer());
            iceServers.add(PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer());
            iceServers.add(PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80").setUsername("openrelayproject").setPassword("openrelayproject").createIceServer());
            iceServers.add(PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443").setUsername("openrelayproject").setPassword("openrelayproject").createIceServer());
        }
        
        return iceServers;
    }

    private void startCallWithIceServers(List<PeerConnection.IceServer> iceServers) {
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection.dispose();
        }

        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        
        peerConnection = factory.createPeerConnection(rtcConfig, new PeerConnection.Observer() {
            @Override
            public void onIceCandidate(IceCandidate iceCandidate) {
                sendIce(iceCandidate);
            }

            @Override
            public void onTrack(RtpTransceiver transceiver) {
                if (transceiver.getReceiver().track() instanceof VideoTrack) {
                    VideoTrack remoteVideoTrack = (VideoTrack) transceiver.getReceiver().track();
                    runOnUiThread(() -> {
                        remoteVideoTrack.addSink(remoteView);
                        markConnected();
                    });
                }
            }
            
            @Override public void onConnectionChange(PeerConnection.PeerConnectionState s) {
                if (s == PeerConnection.PeerConnectionState.DISCONNECTED || s == PeerConnection.PeerConnectionState.CLOSED || s == PeerConnection.PeerConnectionState.FAILED) {
                    disconnect();
                }
            }

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

        if (localVideoTrack != null && peerConnection != null) {
            peerConnection.addTrack(localVideoTrack);
        }
        if (localAudioTrack != null && peerConnection != null) {
            peerConnection.addTrack(localAudioTrack);
        }

        try {
            JSONObject roomInfo = new JSONObject();
            roomInfo.put("room", roomName);
            roomInfo.put("uid", myUid);
            socket.emit("join-call-room", roomInfo);
        } catch (JSONException e) {
            Log.e(TAG, "Error rejoining call room after ICE refresh", e);
        }
        timeoutHandler.postDelayed(timeoutRunnable, 15000);
    }
    
    private void setupButtonAnimations() {
        View.OnTouchListener buttonAnimator = (v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.85f).scaleY(0.85f).alpha(0.8f).setDuration(150).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).alpha(1.0f).setDuration(150).start();
                    v.performClick();
                    break;
            }
            return true;
        };

        btnMute.setOnTouchListener(buttonAnimator);
        btnSwitchCamera.setOnTouchListener(buttonAnimator);
        btnEnd.setOnTouchListener(buttonAnimator);
        btnToggleVideo.setOnTouchListener(buttonAnimator);
        
        followButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    followButtonCard.animate().scaleX(0.92f).scaleY(0.92f).setDuration(120).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    followButtonCard.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                    v.performClick();
                    break;
            }
            return true;
        });
    }
    
    private void setupAutoHideControls() {
        autoHideRunnable = () -> {
            if (areControlsVisible) {
                toggleControlsVisibility();
            }
        };
        areControlsVisible = true;
        resetAutoHideTimer();
    }
    
    private void resetAutoHideTimer() {
        cancelAutoHideTimer();
        autoHideHandler.postDelayed(autoHideRunnable, 5000);
    }
    
    private void cancelAutoHideTimer() {
        autoHideHandler.removeCallbacks(autoHideRunnable);
    }
    
    private void startConnectionAnimation() {
        if (!connectionAnimationActive) return;
        connectionDot.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .alpha(0.7f)
                .setDuration(800)
                .withEndAction(() -> {
                    if (!connectionAnimationActive) return;
                    connectionDot.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .alpha(1.0f)
                            .setDuration(800)
                            .withEndAction(this::startConnectionAnimation)
                            .start();
                })
                .start();
    }

    private void stopConnectionAnimation() {
        connectionAnimationActive = false;
        if (connectionDot != null) {
            connectionDot.animate().cancel();
            connectionDot.setScaleX(1.0f);
            connectionDot.setScaleY(1.0f);
            connectionDot.setAlpha(1.0f);
        }
    }

    private void markConnected() {
        stopConnectionAnimation();
        runOnUiThread(() -> {
            if (connectionStatus != null) {
                connectionStatus.setText("Connected");
            }
            fadeInRemoteOverlay();
        });
    }

    private void fadeInRemoteOverlay() {
        if (remoteAvatarOverlay == null) return;
        remoteAvatarOverlay.setAlpha(0f);
        remoteAvatarOverlay.setVisibility(View.VISIBLE);
        remoteAvatarOverlay.animate()
                .alpha(1f)
                .setDuration(350)
                .start();
    }

    private void fadeOutRemoteOverlay(Runnable onComplete) {
        if (remoteAvatarOverlay == null || remoteAvatarOverlay.getVisibility() != View.VISIBLE) {
            if (onComplete != null) onComplete.run();
            return;
        }
        remoteAvatarOverlay.animate()
                .alpha(0f)
                .setDuration(250)
                .withEndAction(() -> {
                    remoteAvatarOverlay.setVisibility(View.GONE);
                    remoteAvatarOverlay.setAlpha(1f);
                    if (onComplete != null) onComplete.run();
                })
                .start();
    }

    private void toggleControlsVisibility() {
        areControlsVisible = !areControlsVisible;
        
        if (areControlsVisible) {
            topUserInfoBar.setVisibility(View.VISIBLE);
            bottomControls.setVisibility(View.VISIBLE);
            topUserInfoBar.animate().alpha(1.0f).translationY(0).setDuration(300).start();
            bottomControls.animate().alpha(1.0f).translationY(0).setDuration(300).start();
            resetAutoHideTimer();
        } else {
            topUserInfoBar.animate().alpha(0.0f).translationY(-topUserInfoBar.getHeight()).setDuration(300)
                    .withEndAction(() -> topUserInfoBar.setVisibility(View.GONE)).start();
            bottomControls.animate().alpha(0.0f).translationY(bottomControls.getHeight()).setDuration(300)
                    .withEndAction(() -> bottomControls.setVisibility(View.GONE)).start();
            cancelAutoHideTimer();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupDraggableLocalView() {
        localViewContainer.setOnTouchListener(new View.OnTouchListener() {
            private float dX, dY;

            @Override
            public boolean onTouch(View view, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        dX = view.getX() - event.getRawX();
                        dY = view.getY() - event.getRawY();
                        view.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                        break;

                    case MotionEvent.ACTION_MOVE:
                        view.setY(event.getRawY() + dY);
                        view.setX(event.getRawX() + dX);
                        break;

                    case MotionEvent.ACTION_UP:
                        view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
                        break;

                    default:
                        return false;
                }
                return true;
            }
        });
    }

    private void setupSocket() {
        removeSocketListeners();

        peerReadyListener = args -> {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            if (isInitiator) createOffer();
        };
        socket.on("peer-ready", peerReadyListener);

        receiveOfferListener = args -> {
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
        };
        socket.on("receive-offer", receiveOfferListener);

        receiveAnswerListener = args -> {
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
        };
        socket.on("receive-answer", receiveAnswerListener);

        receiveIceListener = args -> {
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
        };
        socket.on("receive-ice", receiveIceListener);

        if (isPrivateCall) {
            receivePrivateOfferListener = args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String offerSdp = data.getString("offer");
                    SessionDescription sdp = new SessionDescription(SessionDescription.Type.OFFER, offerSdp);
                    if (peerConnection != null) {
                        peerConnection.setRemoteDescription(new SimpleSdp("RemotePrivateOffer"), sdp);
                        createAnswer();
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error processing receive-private-offer", e);
                }
            };
            socket.on("receive-private-offer", receivePrivateOfferListener);

            receivePrivateAnswerListener = args -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String answerSdp = data.getString("answer");
                    SessionDescription sdp = new SessionDescription(SessionDescription.Type.ANSWER, answerSdp);
                    if (peerConnection != null) {
                        peerConnection.setRemoteDescription(new SimpleSdp("RemotePrivateAnswer"), sdp);
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error processing receive-private-answer", e);
                }
            };
            socket.on("receive-private-answer", receivePrivateAnswerListener);

            receivePrivateIceListener = args -> {
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
                    Log.e(TAG, "Error processing receive-private-ice", e);
                }
            };
            socket.on("receive-private-ice", receivePrivateIceListener);

            privateCallAcceptedListener = args -> runOnUiThread(() -> {
                Log.d(TAG, "private-call-accepted received");
                stopConnectionAnimation();
                if (connectionStatus != null) {
                    connectionStatus.setText("Call accepted. Connecting...");
                }
                Toast.makeText(CallActivity.this, "Private call accepted.", Toast.LENGTH_SHORT).show();
            });
            socket.on("private-call-accepted", privateCallAcceptedListener);

            privateCallRejectedListener = args -> runOnUiThread(() -> {
                Toast.makeText(CallActivity.this, "Call was rejected.", Toast.LENGTH_SHORT).show();
                disconnect();
            });
            socket.on("private-call-rejected", privateCallRejectedListener);

            privateCallEndedListener = args -> runOnUiThread(() -> {
                Toast.makeText(CallActivity.this, "Call ended by the other user.", Toast.LENGTH_SHORT).show();
                disconnect();
            });
            socket.on("private-call-ended", privateCallEndedListener);

            callFailedListener = args -> runOnUiThread(() -> {
                try {
                    JSONObject data = (JSONObject) args[0];
                    String reason = data.optString("reason", "Call failed.");
                    Toast.makeText(CallActivity.this, reason, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(CallActivity.this, "Call failed.", Toast.LENGTH_LONG).show();
                }
                disconnect();
            });
            socket.on("call-failed", callFailedListener);
        }

        peerDisconnectedListener = args -> runOnUiThread(this::disconnect);
        socket.on("peer-disconnected", peerDisconnectedListener);

        callControlListener = args -> {
            try {
                JSONObject data = (JSONObject) args[0];
                String type = data.getString("type");
                boolean enabled = data.getBoolean("enabled");
                String senderId = data.getString("senderId");

                if (!myUid.equals(senderId) && "video".equals(type)) {
                    runOnUiThread(() -> remoteAvatarOverlay.setVisibility(enabled ? View.GONE : View.VISIBLE));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error processing call-control event", e);
            }
        };
        socket.on("call-control", callControlListener);
    }

    private void createOffer() {
        if (peerConnection == null) return;
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
            JSONObject offer = new JSONObject();
            if (isPrivateCall) {
                offer.put("targetId", peerId);
                offer.put("offer", sdp.description);
                offer.put("room", roomName);
                socket.emit("send-private-offer", offer);
            } else {
                offer.put("to", peerId);
                offer.put("offer", sdp.description);
                offer.put("room", roomName);
                socket.emit("send-offer", offer);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error sending offer", e);
        }
    }

    private void sendAnswer(SessionDescription sdp) {
        try {
            JSONObject answer = new JSONObject();
            if (isPrivateCall) {
                answer.put("targetId", peerId);
                answer.put("answer", sdp.description);
                answer.put("room", roomName);
                socket.emit("send-private-answer", answer);
            } else {
                answer.put("to", peerId);
                answer.put("answer", sdp.description);
                answer.put("room", roomName);
                socket.emit("send-answer", answer);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error sending answer", e);
        }
    }

    private void sendIce(IceCandidate candidate) {
        try {
            JSONObject ice = new JSONObject();
            if (isPrivateCall) {
                ice.put("targetId", peerId);
                ice.put("room", roomName);
                ice.put("candidate", candidate.sdp);
                ice.put("sdpMid", candidate.sdpMid);
                ice.put("sdpMLineIndex", candidate.sdpMLineIndex);
                socket.emit("send-private-ice", ice);
            } else {
                ice.put("to", peerId);
                ice.put("candidate", candidate.sdp);
                ice.put("sdpMid", candidate.sdpMid);
                ice.put("sdpMLineIndex", candidate.sdpMLineIndex);
                ice.put("room", roomName);
                socket.emit("send-ice", ice);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error sending ICE candidate", e);
        }
    }

    private void sendStartPrivateCall() {
        try {
            JSONObject data = new JSONObject();
            data.put("callerId", myUid);
            data.put("targetId", peerId);
            data.put("isVideo", isVideoCall);
            data.put("room", roomName);
            data.put("callerName", tokenManager.getUserName() != null ? tokenManager.getUserName() : "");
            data.put("callerAvatar", tokenManager.getUserAvatar() != null ? tokenManager.getUserAvatar() : "");
            socket.emit("start-private-call", data);
        } catch (JSONException e) {
            Log.e(TAG, "Error emitting start-private-call", e);
        }
    }

    private void sendAcceptPrivateCall() {
        try {
            JSONObject data = new JSONObject();
            data.put("callerId", peerId);
            data.put("room", roomName);
            socket.emit("accept-private-call", data);
        } catch (JSONException e) {
            Log.e(TAG, "Error emitting accept-private-call", e);
        }
    }

    private void toggleMute() {
        isMuted = !isMuted;
        if (localAudioTrack != null) localAudioTrack.setEnabled(!isMuted);
        
        btnMute.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).withEndAction(() -> {
            btnMute.setImageResource(isMuted ? R.drawable.ic_mic_off : R.drawable.ic_mic_on);
            btnMute.setBackgroundResource(isMuted ? R.drawable.bg_call_btn_red : R.drawable.bg_call_btn_glass);
            btnMute.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
        }).start();
        
        resetAutoHideTimer();
    }

    private void toggleVideo() {
        isVideoOff = !isVideoOff;
        if (localVideoTrack != null) localVideoTrack.setEnabled(!isVideoOff);
        
        btnToggleVideo.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).withEndAction(() -> {
            btnToggleVideo.setImageResource(isVideoOff ? R.drawable.ic_videocam_off : R.drawable.ic_videocam_on);
            btnToggleVideo.setBackgroundResource(isVideoOff ? R.drawable.bg_call_btn_red : R.drawable.bg_call_btn_glass);
            localAvatarOverlay.setVisibility(isVideoOff ? View.VISIBLE : View.GONE);
            btnToggleVideo.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start();
        }).start();

        // Emit call-control event to notify peer
        try {
            JSONObject data = new JSONObject();
            data.put("room", roomName);
            data.put("type", "video");
            data.put("enabled", !isVideoOff);
            data.put("senderId", myUid);
            socket.emit("call-control", data);
        } catch (JSONException e) {
            Log.e(TAG, "Error emitting video call-control event", e);
        }
        
        resetAutoHideTimer();
    }

    private void switchCamera() {
        if (videoCapturer instanceof CameraVideoCapturer && !isCameraSwitching) {
            isCameraSwitching = true;
            ((CameraVideoCapturer) videoCapturer).switchCamera(new CameraVideoCapturer.CameraSwitchHandler() {
                @Override
                public void onCameraSwitchDone(boolean isFront) {
                    isCameraSwitching = false;
                }
                @Override
                public void onCameraSwitchError(String s) {
                    isCameraSwitching = false;
                }
            });
        }
    }

    private void disconnect() {
        if (callEnded) return;
        callEnded = true;

        CallManager.setCallActive(false);
        timeoutHandler.removeCallbacks(timeoutRunnable);
        autoHideHandler.removeCallbacks(autoHideRunnable);

        // Reset AudioManager state when call disconnects
        try {
            if (audioManager != null) {
                audioManager.setMode(android.media.AudioManager.MODE_NORMAL);
                audioManager.setSpeakerphoneOn(false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error resetting AudioManager: " + e.getMessage());
        }
        
        if (isPrivateCall) {
            try {
                JSONObject endData = new JSONObject();
                endData.put("targetId", peerId);
                socket.emit("end-private-call", endData);
            } catch (JSONException e) {
                Log.e(TAG, "Error emitting end-private-call", e);
            }
        } else {
            try {
                JSONObject leaveData = new JSONObject();
                leaveData.put("room", roomName);
                leaveData.put("uid", myUid);
                socket.emit("leave-call-room", leaveData);
            } catch (JSONException e) {
                Log.e(TAG, "Error leaving room", e);
            }
        }

        removeSocketListeners();

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
        
        if (localVideoTrack != null) localVideoTrack.dispose();
        if (localAudioTrack != null) localAudioTrack.dispose();
        
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
            if (localView != null) localView.release();
            if (remoteView != null) remoteView.release();
        });

        fadeOutRemoteOverlay(() -> {
            if (factory != null) {
                factory.dispose();
                factory = null;
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

        android.content.SharedPreferences prefs = getSharedPreferences("app_stats", MODE_PRIVATE);
        int currentCount = prefs.getInt("call_counter", 0);
        currentCount++;
        prefs.edit().putInt("call_counter", currentCount).apply();

        final int finalCount = currentCount;
        runOnUiThread(() -> {
            // Show mediated interstitial ad on every 2nd completed call to ensure optimal user experience
            if (finalCount % 2 == 0 && interstitialAd != null && !isFinishing() && !isDestroyed()) {
                Log.d(TAG, "📺 Showing Interstitial Ad on disconnect (Call #" + finalCount + ")");
                interstitialAd.show(CallActivity.this);
            } else {
                finish();
            }
        });
    }

    private void loadPeerUserInfo() {
        if (peerId == null || peerId.isEmpty()) {
            peerName.setText("Unknown User");
            return;
        }

        api.getUser(peerId).enqueue(new retrofit2.Callback<JsonObject>() {
            @Override
            public void onResponse(retrofit2.Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean() && data.has("user")) {
                        JsonObject user = data.getAsJsonObject("user");
                        peerNameValue = user.has("name") && !user.get("name").isJsonNull() ? user.get("name").getAsString() : "Unknown User";
                        peerAvatarUrl = user.has("avatar") && !user.get("avatar").isJsonNull() ? user.get("avatar").getAsString() : "";
                        isFollowing = user.has("isFollowing") && user.get("isFollowing").getAsBoolean();

                        peerName.setText(peerNameValue);
                        remoteAvatarNameText.setText(peerNameValue);
                        if (peerAvatarUrl != null && !peerAvatarUrl.isEmpty()) {
                            int avatarResId = getResources().getIdentifier(peerAvatarUrl, "drawable", getPackageName());
                            if (avatarResId != 0) {
                                Glide.with(CallActivity.this).load(avatarResId).placeholder(R.drawable.ic_user_placeholder).circleCrop().into(peerAvatar);
                                Glide.with(CallActivity.this).load(avatarResId).placeholder(R.drawable.ic_user_placeholder).into(remoteAvatarLarge);
                                Glide.with(CallActivity.this).load(avatarResId).placeholder(R.drawable.ic_user_placeholder).into(remoteAvatarBlurBg);
                            } else {
                                Glide.with(CallActivity.this).load(peerAvatarUrl).placeholder(R.drawable.ic_user_placeholder).circleCrop().into(peerAvatar);
                                Glide.with(CallActivity.this).load(peerAvatarUrl).placeholder(R.drawable.ic_user_placeholder).into(remoteAvatarLarge);
                                Glide.with(CallActivity.this).load(peerAvatarUrl).placeholder(R.drawable.ic_user_placeholder).into(remoteAvatarBlurBg);
                            }
                        } else {
                            Glide.with(CallActivity.this).load(R.drawable.ic_user_placeholder).circleCrop().into(peerAvatar);
                            Glide.with(CallActivity.this).load(R.drawable.ic_user_placeholder).into(remoteAvatarLarge);
                            Glide.with(CallActivity.this).load(R.drawable.ic_user_placeholder).into(remoteAvatarBlurBg);
                        }
                        updateFollowButtonUI();
                        return;
                    }
                }
                peerName.setText("Unknown User");
            }

            @Override
            public void onFailure(retrofit2.Call<JsonObject> call, Throwable t) {
                peerName.setText("Unknown User");
            }
        });
    }

    private void updateFollowButtonUI() {
        if (isFollowing) {
            followButton.setText("Following");
            followButton.setBackgroundResource(R.drawable.bg_following_button);
        } else {
            followButton.setText("Follow");
            followButton.setBackgroundResource(R.drawable.bg_follow_button);
        }
    }

    private void handleFollowClick() {
        if (isUpdatingFollow || myUid == null || peerId == null) return;
        
        isUpdatingFollow = true;
        followButton.setEnabled(false);

        if (isFollowing) {
            unfollowUser();
        } else {
            followUser();
        }
    }

    private void followUser() {
        api.followUser(peerId).enqueue(new retrofit2.Callback<JsonObject>() {
            @Override
            public void onResponse(retrofit2.Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                isUpdatingFollow = false;
                followButton.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        isFollowing = true;
                        updateFollowButtonUI();
                        Toast.makeText(CallActivity.this, "Following " + peerNameValue, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                Toast.makeText(CallActivity.this, "Failed to follow user", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(retrofit2.Call<JsonObject> call, Throwable t) {
                isUpdatingFollow = false;
                followButton.setEnabled(true);
                Toast.makeText(CallActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void unfollowUser() {
        api.unfollowUser(peerId).enqueue(new retrofit2.Callback<JsonObject>() {
            @Override
            public void onResponse(retrofit2.Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                isUpdatingFollow = false;
                followButton.setEnabled(true);
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean()) {
                        isFollowing = false;
                        updateFollowButtonUI();
                        Toast.makeText(CallActivity.this, "Unfollowed " + peerNameValue, Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                Toast.makeText(CallActivity.this, "Failed to unfollow user", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(retrofit2.Call<JsonObject> call, Throwable t) {
                isUpdatingFollow = false;
                followButton.setEnabled(true);
                Toast.makeText(CallActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isVpnActive() {
        try {
            android.net.ConnectivityManager cm = (android.net.ConnectivityManager) getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
            if (cm != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    android.net.Network activeNetwork = cm.getActiveNetwork();
                    android.net.NetworkCapabilities caps = cm.getNetworkCapabilities(activeNetwork);
                    return caps != null && caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN);
                } else {
                    java.util.List<java.net.NetworkInterface> interfaces = java.util.Collections.list(java.net.NetworkInterface.getNetworkInterfaces());
                    for (java.net.NetworkInterface networkInterface : interfaces) {
                        if (networkInterface.isUp()) {
                            String name = networkInterface.getName().toLowerCase();
                            if (name.contains("tun") || name.contains("ppp") || name.contains("p2p") || name.contains("tap")) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking VPN connection state", e);
        }
        return false;
    }    private void updateAudioRouting() {
        try {
            if (audioManager != null) {
                boolean isWiredHeadset = false;
                try {
                    isWiredHeadset = audioManager.isWiredHeadsetOn();
                } catch (Exception e) {
                    Log.e(TAG, "Error checking wired headset: " + e.getMessage());
                }

                boolean isBluetoothConnected = false;
                // Only check Bluetooth state if we have permissions on Android 12+
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S || 
                    androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    try {
                        isBluetoothConnected = audioManager.isBluetoothScoOn() || audioManager.isBluetoothA2dpOn();
                    } catch (Exception e) {
                        Log.e(TAG, "Error checking bluetooth headset state: " + e.getMessage());
                    }
                }

                boolean isHeadsetConnected = isWiredHeadset || isBluetoothConnected;
                Log.d(TAG, "📺 Audio Routing check: Wired = " + isWiredHeadset + ", BT = " + isBluetoothConnected);
                
                audioManager.setSpeakerphoneOn(!isHeadsetConnected);
            }
        } catch (Exception e) {
            Log.e(TAG, "General error updating audio routing: " + e.getMessage());
            // Fallback: default to speakerphone on to prevent silent call
            try {
                if (audioManager != null) {
                    audioManager.setSpeakerphoneOn(true);
                }
            } catch (Exception ex) {
                Log.e(TAG, "Failed fallback speakerphone: " + ex.getMessage());
            }
        }
    }

    private void removeSocketListeners() {
        if (socket == null) return;
        if (peerReadyListener != null) socket.off("peer-ready", peerReadyListener);
        if (receiveOfferListener != null) socket.off("receive-offer", receiveOfferListener);
        if (receiveAnswerListener != null) socket.off("receive-answer", receiveAnswerListener);
        if (receiveIceListener != null) socket.off("receive-ice", receiveIceListener);
        if (receivePrivateOfferListener != null) socket.off("receive-private-offer", receivePrivateOfferListener);
        if (receivePrivateAnswerListener != null) socket.off("receive-private-answer", receivePrivateAnswerListener);
        if (receivePrivateIceListener != null) socket.off("receive-private-ice", receivePrivateIceListener);
        if (privateCallAcceptedListener != null) socket.off("private-call-accepted", privateCallAcceptedListener);
        if (privateCallRejectedListener != null) socket.off("private-call-rejected", privateCallRejectedListener);
        if (privateCallEndedListener != null) socket.off("private-call-ended", privateCallEndedListener);
        if (callFailedListener != null) socket.off("call-failed", callFailedListener);
        if (peerDisconnectedListener != null) socket.off("peer-disconnected", peerDisconnectedListener);
        if (callControlListener != null) socket.off("call-control", callControlListener);

        peerReadyListener = null;
        receiveOfferListener = null;
        receiveAnswerListener = null;
        receiveIceListener = null;
        peerDisconnectedListener = null;
        callControlListener = null;
    }

    @Override
    protected void onDestroy() {
        disconnect();
        try {
            unregisterReceiver(headsetReceiver);
        } catch (Exception e) {
            // Ignore if not registered
        }
        if (interstitialAd != null) {
            interstitialAd.setFullScreenContentCallback(null);
            interstitialAd = null;
        }
        super.onDestroy();
    }
}
