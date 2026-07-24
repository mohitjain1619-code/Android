'use client';
import { useState, useEffect, useRef, useCallback, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { useAuth } from '../../lib/auth-context';
import { getSocket } from '../../lib/socket';
import { WebRTCManager } from '../../lib/webrtc';
import { getUser } from '../../lib/firestore';
import { Mic, MicOff, Video, VideoOff, PhoneOff, SwitchCamera, UserPlus, ArrowLeft, Loader } from 'lucide-react';
import styles from './page.module.css';

function CallPageInner() {
  const { user, userData, setShowOnboarding, setShowVerification } = useAuth();
  const searchParams = useSearchParams();
  const router = useRouter();
  const category = searchParams.get('category') || 'straight';

  const [state, setState] = useState('idle'); // idle | connecting | in-call
  const [peerData, setPeerData] = useState(null);
  const [isMuted, setIsMuted] = useState(false);
  const [isVideoOff, setIsVideoOff] = useState(false);
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
    if (!user) { router.push('/?login=true'); }
    return () => cleanupCall();
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

    setState('connecting');
    try {
      const socket = getSocket();
      socketRef.current = socket;

      const stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'user', width: { ideal: 720 }, height: { ideal: 1280 } },
        audio: true,
      });

      if (localVideoRef.current) {
        localVideoRef.current.srcObject = stream;
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

      socket.on('match-found', async (data) => {
        const peerId = data.peerId || data.peer;
        const peerInfo = await getUser(peerId);
        setPeerData(peerInfo || { name: 'User', avatar: 'av1' });

        const webrtc = new WebRTCManager({
          socket,
          myUid: user.uid,
          peerId,
          onRemoteStream: (remoteStream) => {
            if (remoteVideoRef.current) {
              remoteVideoRef.current.srcObject = remoteStream;
            }
            setState('in-call');
            setTimer(0);
          },
          onConnectionChange: setConnectionState,
          onDisconnect: () => {
            setState('idle');
            setPeerData(null);
            stream.getTracks().forEach(t => t.stop());
          },
        });

        webrtcRef.current = webrtc;
        webrtc.localStream = stream;
        await webrtc.initPeerConnection();
        webrtc.setupSocketListeners();
        webrtc.joinRoom();
      });

      socket.on('queue-position', (data) => {
        // Optional: show queue position
      });
    } catch (err) {
      console.error('Error starting call:', err);
      alert('Camera/Microphone access is required for video calling.');
      setState('idle');
    }
  };

  const cleanupCall = () => {
    webrtcRef.current?.disconnect();
    socketRef.current?.emit('leave-queue', { uid: user?.uid });
    socketRef.current?.off('match-found');
    socketRef.current?.off('queue-position');
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
    webrtcRef.current?.disconnect();
    setState('connecting');
    socketRef.current?.emit('join-queue', {
      uid: user.uid,
      gender: userData?.gender || '',
      category,
    });
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
      <video ref={remoteVideoRef} className={styles.remoteVideo} autoPlay playsInline />

      {/* Local PIP */}
      <div ref={pipRef} className={styles.localPip} onPointerDown={onPipPointerDown}>
        <video ref={localVideoRef} className={styles.localVideo} autoPlay playsInline muted />
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
          <button className={`${styles.controlBtn} ${isMuted ? styles.controlActive : ''}`} onClick={() => { setIsMuted(webrtcRef.current?.toggleMute()); }}>
            {isMuted ? <MicOff size={22} /> : <Mic size={22} />}
          </button>
          <button className={`${styles.controlBtn} ${isVideoOff ? styles.controlActive : ''}`} onClick={() => { setIsVideoOff(webrtcRef.current?.toggleVideo()); }}>
            {isVideoOff ? <VideoOff size={22} /> : <Video size={22} />}
          </button>
          <button className={`${styles.controlBtn} ${styles.endBtn}`} onClick={endCall}>
            <PhoneOff size={22} />
          </button>
          <button className={styles.controlBtn} onClick={() => webrtcRef.current?.switchCamera()}>
            <SwitchCamera size={22} />
          </button>
          <button className={`${styles.controlBtn} ${styles.nextBtn}`} onClick={nextCall}>
            Next
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
