'use client';
import { useState, useEffect, useRef, useCallback, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { useAuth } from '../../lib/auth-context';
import { getSocket } from '../../lib/socket';
import { WebRTCManager } from '../../lib/webrtc';
import { getUser } from '../../lib/firestore';
import { Mic, MicOff, Video, VideoOff, PhoneOff, SwitchCamera, SkipForward, ArrowLeft, Loader } from 'lucide-react';
import styles from './page.module.css';

const TESTER_EMAILS = [
  'jainmohit.cr007@gmail.com',
  'mohitj8120@gmail.com',
  'monishkarai206@gmail.com',
  'mohitjain1619@gmail.com',
  'info.meetblis@gmail.com',
  'wetviapp@gmail.com',
  'mohitissuingthis@gmail.com'
];

function CallPageInner() {
  const { user, userData, loading, setShowOnboarding, setShowVerification } = useAuth();
  const searchParams = useSearchParams();
  const router = useRouter();
  const category = searchParams.get('category') || 'straight';

  const [state, setState] = useState('idle'); // idle | connecting | in-call
  const [peerData, setPeerData] = useState(null);
  const [isMuted, setIsMuted] = useState(false);
  const [isVideoOff, setIsVideoOff] = useState(false);
  const [isRemoteVideoOff, setIsRemoteVideoOff] = useState(false);
  const [showControls, setShowControls] = useState(true);
  const [connectionState, setConnectionState] = useState('');
  const [timer, setTimer] = useState(0);

  const localVideoRef = useRef(null);
  const remoteVideoRef = useRef(null);
  const webrtcRef = useRef(null);
  const socketRef = useRef(null);
  const controlTimerRef = useRef(null);
  const callTimerRef = useRef(null);
  const pipRef = useRef(null);
  const draggingRef = useRef(false);
  const dragOffsetRef = useRef({ x: 0, y: 0 });

  useEffect(() => {
    if (loading) return;
    const email = user?.email || userData?.email;
    const isTester = email && TESTER_EMAILS.map(e => e.toLowerCase().trim()).includes(email.toLowerCase().trim());
    if (!isTester) {
      router.push('/?launch=true');
    }
  }, [user, userData, loading, router]);
 
  // Security layout: block right-click, screenshot keyboard keys, and developer tool inspector
  useEffect(() => {
    const preventAction = (e) => e.preventDefault();
    const preventKeys = (e) => {
      // Blocks common inspection/print shortcuts
      if (
        e.key === 'F12' || 
        (e.ctrlKey && e.shiftKey && ['I', 'i', 'J', 'j', 'C', 'c'].includes(e.key)) || 
        (e.ctrlKey && ['U', 'u', 'S', 's', 'P', 'p'].includes(e.key))
      ) {
        e.preventDefault();
      }
    };
    document.addEventListener('contextmenu', preventAction);
    document.addEventListener('keydown', preventKeys);
    return () => {
      document.removeEventListener('contextmenu', preventAction);
      document.removeEventListener('keydown', preventKeys);
    };
  }, []);

  // Call timer
  useEffect(() => {
    if (state === 'in-call') {
      callTimerRef.current = setInterval(() => setTimer(t => t + 1), 1000);
    }
    return () => clearInterval(callTimerRef.current);
  }, [state]);

  // Auto-hide controls
  useEffect(() => {
    if (state === 'in-call') {
      resetControlTimer();
    }
    return () => clearTimeout(controlTimerRef.current);
  }, [state]);

  // Ensure remote video & audio stream is attached & unmuted when video element mounts in DOM
  useEffect(() => {
    if (state === 'in-call' && remoteVideoRef.current && webrtcRef.current?.remoteStream) {
      remoteVideoRef.current.srcObject = webrtcRef.current.remoteStream;
      remoteVideoRef.current.muted = false;
      remoteVideoRef.current.volume = 1.0;
      remoteVideoRef.current.play().catch(e => console.warn('Remote video play error:', e));
    }
  }, [state]);

  const resetControlTimer = () => {
    setShowControls(true);
    clearTimeout(controlTimerRef.current);
    controlTimerRef.current = setTimeout(() => setShowControls(false), 5000);
  };

  const formatTime = (s) => {
    const m = Math.floor(s / 60);
    const sec = s % 60;
    return `${m.toString().padStart(2, '0')}:${sec.toString().padStart(2, '0')}`;
  };

  const startConnecting = async () => {
    if (!userData || !userData.gender) {
      setShowOnboarding(true);
      return;
    }
    
    // If user is female and not verified, block calling and show verification modal
    if (userData.gender.toLowerCase().trim() === 'female' && !userData.verified) {
      setShowVerification(true);
      return;
    }

    // Check for VPN/Proxy before starting stream
    setState('connecting');
    try {
      const ipResponse = await fetch('https://ipapi.co/json/');
      if (ipResponse.ok) {
        const ipData = await ipResponse.json();
        const org = (ipData.org || '').toLowerCase();
        const isVpn = org.includes('vpn') || org.includes('hosting') || org.includes('cloudflare') || org.includes('mullvad') || org.includes('nord') || org.includes('tor ') || org.includes('datacent') || org.includes('digitalocean') || org.includes('ovh') || org.includes('linode') || org.includes('amazon') || org.includes('opera') || org.includes('surfeasy') || org.includes('proxy') || org.includes('vps') || org.includes('server') || org.includes('google') || org.includes('microsoft');
        if (isVpn) {
          alert("VPN/Proxy Detected!\n\nPlease turn off your VPN/Proxy for video calling to connect successfully. VPNs block video stream channels.\n\nBehtar video calling ke liye apna VPN/Proxy band karein.");
          setState('idle');
          return;
        }
      }
    } catch (e) {
      console.warn("VPN check skipped:", e);
    }

    // Clean up previous call & listeners before starting new connection
    cleanupCall();

    setState('connecting');
    try {
      const socket = getSocket();
      socketRef.current = socket;

      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user', width: { ideal: 720 }, height: { ideal: 1280 } },
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });

      if (localVideoRef.current) {
        localVideoRef.current.srcObject = stream;
        localVideoRef.current.play().catch(() => {});
      }

      socket.emit('register-user', {
        uid: user.uid,
        name: userData?.name || '',
        gender: userData?.gender || '',
        avatar: userData?.avatar || 'av1',
      });

      socket.emit('join-queue', {
        uid: user.uid,
        gender: userData?.gender || '',
        category,
      });

      socket.off('match-found');
      socket.off('call-control');
      socket.off('queue-position');

      socket.on('match-found', async (data) => {
        const peerId = data.peerId || data.peer;
        const peerInfo = await getUser(peerId);
        setPeerData(peerInfo || { name: 'User', avatar: 'av1' });

        const webrtc = new WebRTCManager({
          socket,
          myUid: user.uid,
          peerId,
          onRemoteStream: (remoteStream) => {
            setState('in-call');
            setTimer(0);
            setTimeout(() => {
              if (remoteVideoRef.current && remoteStream) {
                remoteVideoRef.current.srcObject = remoteStream;
                remoteVideoRef.current.muted = false;
                remoteVideoRef.current.volume = 1.0;
                remoteVideoRef.current.play().catch(e => console.warn('Remote video play:', e));
              }
            }, 50);
          },
          onConnectionChange: setConnectionState,
          onDisconnect: () => {
            endCall();
          },
        });

        webrtcRef.current = webrtc;
        webrtc.localStream = stream;
        await webrtc.initPeerConnection();
        webrtc.setupSocketListeners();
        webrtc.joinRoom();
      });

      socket.on('call-control', (data) => {
        if (data.senderId !== user.uid && data.type === 'video') {
          setIsRemoteVideoOff(!data.enabled);
        }
      });
    } catch (err) {
      console.error('Error starting call:', err);
      alert('Camera/Microphone access is required for video calling.');
      setState('idle');
    }
  };

  const cleanupCall = () => {
    webrtcRef.current?.disconnect();
    webrtcRef.current = null;
    socketRef.current?.emit('leave-queue', { uid: user?.uid });
    socketRef.current?.off('match-found');
    socketRef.current?.off('queue-position');
    socketRef.current?.off('call-control');
    setIsRemoteVideoOff(false);
    setIsVideoOff(false);
    setIsMuted(false);
    if (localVideoRef.current?.srcObject) {
      localVideoRef.current.srcObject.getTracks().forEach(t => t.stop());
    }
  };

  const endCall = () => {
    cleanupCall();
    setState('idle');
    setPeerData(null);
    setTimer(0);
  };

  const nextCall = () => {
    cleanupCall();
    startConnecting();
  };

  // PIP Drag
  const onPipPointerDown = (e) => {
    if (!pipRef.current) return;
    draggingRef.current = true;
    const rect = pipRef.current.getBoundingClientRect();
    dragOffsetRef.current = { x: e.clientX - rect.left, y: e.clientY - rect.top };
    pipRef.current.style.transition = 'none';
    e.preventDefault();
  };

  useEffect(() => {
    const onPointerMove = (e) => {
      if (!draggingRef.current || !pipRef.current) return;
      const x = e.clientX - dragOffsetRef.current.x;
      const y = e.clientY - dragOffsetRef.current.y;
      const maxX = window.innerWidth - pipRef.current.offsetWidth;
      const maxY = window.innerHeight - pipRef.current.offsetHeight;
      pipRef.current.style.left = `${Math.max(8, Math.min(x, maxX - 8))}px`;
      pipRef.current.style.top = `${Math.max(8, Math.min(y, maxY - 8))}px`;
      pipRef.current.style.right = 'auto';
      pipRef.current.style.bottom = 'auto';
    };
    const onPointerUp = () => {
      draggingRef.current = false;
      if (pipRef.current) pipRef.current.style.transition = '';
    };
    window.addEventListener('pointermove', onPointerMove);
    window.addEventListener('pointerup', onPointerUp);
    return () => {
      window.removeEventListener('pointermove', onPointerMove);
      window.removeEventListener('pointerup', onPointerUp);
    };
  }, []);

  // Idle State
  if (state === 'idle') {
    return (
      <div className={styles.page}>
        <div className={styles.idleContainer}>
          <button className={styles.backBtn} onClick={() => router.push('/')}>
            <ArrowLeft size={20} /> Back
          </button>
          <div className={styles.idleContent}>
            <div className={styles.idleIcon}>
              <Video size={48} />
            </div>
            <h2>Ready to call?</h2>
            <p>Category: <span className="neon-text" style={{ fontWeight: 600, textTransform: 'capitalize' }}>{category}</span></p>
            <button className="btn-neon" onClick={startConnecting} style={{ marginTop: 20, padding: '14px 40px', fontSize: '1.1rem' }}>
              <Video size={20} /> Start Matching
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.page} onClick={resetControlTimer}>
      {/* Remote Video */}
      <video ref={remoteVideoRef} className={styles.remoteVideo} autoPlay playsInline style={{ display: isRemoteVideoOff ? 'none' : 'block' }} />

      {/* Remote Video Off Avatar Overlay */}
      {isRemoteVideoOff && (
        <div className={styles.remoteAvatarOverlay}>
          <div className={styles.remoteAvatarBlurBg} style={{ backgroundImage: `url(/avatars/${peerData?.avatar || 'av1'}.png)` }} />
          <div className={styles.remoteAvatarContainer}>
            <img src={`/avatars/${peerData?.avatar || 'av1'}.png`} alt="" className={styles.remoteAvatar} />
            <div className={styles.remoteAvatarName}>{peerData?.name || 'User'}</div>
            <div className={styles.remoteAvatarSubText}>Camera is turned off</div>
          </div>
        </div>
      )}

      {/* Local PIP */}
      <div ref={pipRef} className={styles.localPip} onPointerDown={onPipPointerDown}>
        <video ref={localVideoRef} className={styles.localVideo} autoPlay playsInline muted style={{ display: isVideoOff ? 'none' : 'block' }} />
        {isVideoOff && (
          <div className={styles.pipAvatarOverlay}>
            <img src={`/avatars/${userData?.avatar || 'av1'}.png`} alt="" className={styles.pipAvatar} />
          </div>
        )}
      </div>

      {/* Connecting Overlay */}
      {state === 'connecting' && (
        <div className={styles.connectingOverlay}>
          <div className={styles.searchAnimation}>
            <div className={styles.ring} />
            <div className={styles.ring2} />
            <div className={styles.ring3} />
            <Video size={32} />
          </div>
          <h3>Finding someone...</h3>
          <p style={{ textTransform: 'capitalize' }}>{category} mode</p>
          <button className="btn-glass" onClick={endCall} style={{ marginTop: 24 }}>
            <PhoneOff size={16} /> Cancel
          </button>
        </div>
      )}

      {/* Top Bar */}
      {state === 'in-call' && (
        <div className={`${styles.topBar} ${showControls ? '' : styles.hidden}`}>
          <div className={styles.peerInfo}>
            {peerData?.avatar && (
              <img src={`/avatars/${peerData.avatar}.png`} alt="" className={styles.peerAvatar} />
            )}
            <div>
              <div className={styles.peerName}>{peerData?.name || 'User'}</div>
              <div className={styles.callTimer}>{formatTime(timer)}</div>
            </div>
          </div>
          <div className={`${styles.connDot} ${connectionState === 'connected' ? styles.dotGreen : ''}`} />
        </div>
      )}

      {/* Bottom Controls */}
      {state === 'in-call' && (
        <div className={`${styles.bottomBar} ${showControls ? '' : styles.hidden}`}>
          <button className={`${styles.controlBtn} ${isMuted ? styles.controlActive : ''}`} onClick={() => { setIsMuted(webrtcRef.current?.toggleMute()); }} title={isMuted ? "Unmute Mic" : "Mute Mic"}>
            {isMuted ? <MicOff size={22} /> : <Mic size={22} />}
          </button>
          <button 
            className={`${styles.controlBtn} ${isVideoOff ? styles.controlActive : ''}`} 
            onClick={() => { 
              const nextVal = !isVideoOff;
              setIsVideoOff(nextVal);
              webrtcRef.current?.toggleVideo();
              socketRef.current?.emit('call-control', {
                room: webrtcRef.current?.roomName,
                type: 'video',
                enabled: !nextVal,
                senderId: user.uid
              });
            }}
            title={isVideoOff ? "Turn Video On" : "Turn Video Off"}
          >
            {isVideoOff ? <VideoOff size={22} /> : <Video size={22} />}
          </button>
          <button className={`${styles.controlBtn} ${styles.endBtn}`} onClick={endCall} title="End Call">
            <PhoneOff size={22} />
          </button>
          <button className={`${styles.controlBtn} ${styles.nextBtn}`} onClick={nextCall} title="Next Partner">
            <SkipForward size={22} />
          </button>
          <button className={styles.controlBtn} onClick={async () => {
            if (webrtcRef.current) {
              const updatedStream = await webrtcRef.current.switchCamera();
              if (updatedStream && localVideoRef.current) {
                localVideoRef.current.srcObject = null;
                localVideoRef.current.srcObject = updatedStream;
                localVideoRef.current.play().catch(e => console.warn('Local preview refresh failed:', e));
              }
            }
          }} title="Switch Camera">
            <SwitchCamera size={22} />
          </button>
        </div>
      )}
    </div>
  );
}

export default function CallPage() {
  return (
    <Suspense fallback={<div style={{ minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><Loader size={32} className="neon-text" /></div>}>
      <CallPageInner />
    </Suspense>
  );
}
