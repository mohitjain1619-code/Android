'use client';
import { useState, useEffect, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '../lib/auth-context';
import { updateMe, verifyGender } from '../lib/api';
import { Camera, RefreshCw, CheckCircle, AlertCircle, X, Loader, Play } from 'lucide-react';
import styles from './GenderVerificationModal.module.css';

export default function GenderVerificationModal({ onClose }) {
  const { user, userData, refreshUserData } = useAuth();
  const router = useRouter();
  const [stream, setStream] = useState(null);
  const [capturedBlob, setCapturedBlob] = useState(null);
  const [capturedImg, setCapturedImg] = useState(null);
  const [countdown, setCountdown] = useState(null);
  const [status, setStatus] = useState('idle'); // idle | countdown | captured | verifying | success | error
  const [errorMsg, setErrorMsg] = useState('');
  const [permissionError, setPermissionError] = useState(false);

  // Model & Alignment State
  const [isModelLoading, setIsModelLoading] = useState(true);
  const [isFaceAligned, setIsFaceAligned] = useState(false);
  const [showManualCaptureFallback, setShowManualCaptureFallback] = useState(false);

  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const countdownIntervalRef = useRef(null);
  const faceDetectorRef = useRef(null);
  const isDetectingRef = useRef(false);

  // Ref to always track latest status in animations/async callbacks
  const statusRef = useRef(status);
  useEffect(() => {
    statusRef.current = status;
  }, [status]);

  // Helper for direct web auto-verification of male profiles
  const autoVerifyMaleWeb = async () => {
    setStatus('verifying');
    setErrorMsg('');
    try {
      await updateMe({ verified: true });
      await refreshUserData();
      setStatus('success');
    } catch (err) {
      console.error('Error auto-verifying male profile:', err);
      setStatus('error');
      setErrorMsg(err?.message || 'Auto-verification failed.');
    }
  };

  // Helper to redirect to profile settings to change gender
  const handleGoToProfile = () => {
    onClose?.();
    router.push('/profile');
  };

  // Run male auto-verification if gender is male
  useEffect(() => {
    if (user && userData && userData.gender) {
      const gender = userData.gender.toLowerCase().trim();
      if (gender === 'male' && status !== 'success' && status !== 'verifying') {
        autoVerifyMaleWeb();
      }
    }
  }, [user, userData]);

  // Load MediaPipe Face Detector on client mount (only for female/non-male users)
  useEffect(() => {
    const gender = userData?.gender?.toLowerCase()?.trim();
    if (gender === 'male') {
      setIsModelLoading(false);
      return;
    }

    let active = true;
    const loadModel = async () => {
      try {
        setIsModelLoading(true);
        const { FilesetResolver, FaceDetector } = await import('@mediapipe/tasks-vision');
        const vision = await FilesetResolver.forVisionTasks(
          "https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.35/wasm"
        );
        const detector = await FaceDetector.createFromOptions(vision, {
          baseOptions: {
            modelAssetPath: "https://storage.googleapis.com/mediapipe-models/face_detector/blaze_face_short_range/float16/1/blaze_face_short_range.tflite",
            delegate: "GPU"
          },
          runningMode: "VIDEO"
        });
        
        if (active) {
          faceDetectorRef.current = detector;
          setIsModelLoading(false);
          console.log("✅ MediaPipe Face Detector loaded successfully.");
        } else {
          detector.close();
        }
      } catch (err) {
        console.error("Error loading face detector model:", err);
        if (active) {
          setIsModelLoading(false);
        }
      }
    };

    loadModel();

    return () => {
      active = false;
      if (faceDetectorRef.current) {
        faceDetectorRef.current.close();
        faceDetectorRef.current = null;
      }
    };
  }, [userData]);

  // Auto-start camera when modal mounts (only for non-male users)
  useEffect(() => {
    const gender = userData?.gender?.toLowerCase()?.trim();
    if (gender !== 'male') {
      startCamera();
    }
    return () => {
      stopCamera();
    };
  }, [userData]);

  // Trigger manual capture fallback button if face is not aligned after 5 seconds of camera usage
  useEffect(() => {
    let timeoutId;
    if (stream) {
      timeoutId = setTimeout(() => {
        setShowManualCaptureFallback(true);
      }, 5000);
    } else {
      setShowManualCaptureFallback(false);
    }
    return () => clearTimeout(timeoutId);
  }, [stream]);

  const startCamera = async () => {
    try {
      setPermissionError(false);
      setStatus('idle');
      setErrorMsg('');
      setCapturedImg(null);
      setCapturedBlob(null);
      setIsFaceAligned(false);

      // Request front-facing camera
      const mediaStream = await navigator.mediaDevices.getUserMedia({
        video: {
          facingMode: 'user',
          width: { ideal: 640 },
          height: { ideal: 480 },
        },
        audio: false,
      });

      setStream(mediaStream);
      if (videoRef.current) {
        videoRef.current.srcObject = mediaStream;
      }
    } catch (err) {
      console.error('Webcam Access Error:', err);
      setPermissionError(true);
      setStatus('error');
      setErrorMsg('Camera access denied. Please grant camera permission in your browser settings.');
    }
  };

  const stopCamera = () => {
    if (stream) {
      stream.getTracks().forEach((track) => track.stop());
      setStream(null);
    }
    if (countdownIntervalRef.current) {
      clearInterval(countdownIntervalRef.current);
    }
  };

  const startCountdown = () => {
    if (status !== 'idle' || !isFaceAligned) return;
    setStatus('countdown');
    setCountdown(3);

    countdownIntervalRef.current = setInterval(() => {
      setCountdown((prev) => {
        if (prev === 1) {
          clearInterval(countdownIntervalRef.current);
          countdownIntervalRef.current = null;
          captureImage();
          return null;
        }
        return prev - 1;
      });
    }, 1000);
  };

  const cancelCountdown = () => {
    if (countdownIntervalRef.current) {
      clearInterval(countdownIntervalRef.current);
      countdownIntervalRef.current = null;
    }
    setStatus('idle');
    setCountdown(null);
    console.log("⚠️ Countdown cancelled - face moved away");
  };

  // Auto-start countdown when face is aligned (stability check of 800ms)
  useEffect(() => {
    let timeoutId;
    if (status === 'idle' && isFaceAligned) {
      console.log("⏱️ Face aligned. Starting auto-countdown in 800ms...");
      timeoutId = setTimeout(() => {
        if (statusRef.current === 'idle' && isFaceAligned) {
          startCountdown();
        }
      }, 800);
    }
    return () => {
      if (timeoutId) {
        clearTimeout(timeoutId);
      }
    };
  }, [isFaceAligned, status]);

  // Real-time Face Tracking loop when stream is active
  useEffect(() => {
    if (!stream || isModelLoading || !faceDetectorRef.current) {
      setIsFaceAligned(false);
      return;
    }

    const video = videoRef.current;
    if (!video) return;

    isDetectingRef.current = true;
    let animationFrameId;

    const detectFrame = () => {
      if (!isDetectingRef.current || video.paused || video.ended) return;

      if (video.readyState >= 2) { // HAVE_CURRENT_DATA
        try {
          const startTimeMs = performance.now();
          const result = faceDetectorRef.current.detectForVideo(video, startTimeMs);

          if (result && result.detections && result.detections.length > 0) {
            const face = result.detections[0];
            const bbox = face.boundingBox; // { originX, originY, width, height }

            const vWidth = video.videoWidth;
            const vHeight = video.videoHeight;

            if (vWidth > 0 && vHeight > 0) {
              // Calculate alignment inside 180x240 guide centered inside 280x380 wrapper box
              const normCenterX = (bbox.originX + bbox.width / 2) / vWidth;
              const normCenterY = (bbox.originY + bbox.height / 2) / vHeight;

              const wrapperW = 280;
              const wrapperH = 380;
              const videoRatio = vWidth / vHeight;
              const wrapperRatio = wrapperW / wrapperH;

              let scale, offsetX = 0, offsetY = 0;
              if (videoRatio > wrapperRatio) {
                scale = wrapperH / vHeight;
                offsetX = (vWidth * scale - wrapperW) / 2;
              } else {
                scale = wrapperW / vWidth;
                offsetY = (vHeight * scale - wrapperH) / 2;
              }

              // Absolute pixel coordinates relative to the wrapper box
              const faceXInVideo = (bbox.originX + bbox.width / 2) * scale;
              const faceYInVideo = (bbox.originY + bbox.height / 2) * scale;

              const wrapperX = faceXInVideo - offsetX;
              const wrapperY = faceYInVideo - offsetY;

              // Mirror the x-coordinate since the video element is scaled/mirrored visually
              const mirroredWrapperX = wrapperW - wrapperX;

              // Check if face center falls inside the central 60% of the guide ellipse
              // Oval guide center: (140, 190). Radii: (90, 120)
              const dx = (mirroredWrapperX - 140) / 90;
              const dy = (wrapperY - 190) / 120;
              const dist = dx * dx + dy * dy;

              // Bounding box size check to ensure face is not too close or too far
              const faceWidthInWrapper = bbox.width * scale;

              const isInside = dist <= 0.65;
              const isSizeOk = faceWidthInWrapper >= 85 && faceWidthInWrapper <= 220;

              const aligned = isInside && isSizeOk;
              setIsFaceAligned(aligned);

              // Auto-cancel countdown if face alignment is lost during countdown
              if (!aligned && statusRef.current === 'countdown') {
                cancelCountdown();
              }
            } else {
              setIsFaceAligned(false);
            }
          } else {
            setIsFaceAligned(false);
            if (statusRef.current === 'countdown') {
              cancelCountdown();
            }
          }
        } catch (err) {
          console.error("Frame detection execution error:", err);
        }
      }

      animationFrameId = requestAnimationFrame(detectFrame);
    };

    const delayId = setTimeout(() => {
      detectFrame();
    }, 400);

    return () => {
      isDetectingRef.current = false;
      clearTimeout(delayId);
      cancelAnimationFrame(animationFrameId);
    };
  }, [stream, isModelLoading]);

  const captureImage = () => {
    const video = videoRef.current;
    const canvas = canvasRef.current;
    if (!video || !canvas) return;

    const ctx = canvas.getContext('2d');
    const width = video.videoWidth;
    const height = video.videoHeight;

    const maxDim = 400;
    let newWidth, newHeight;

    if (width > height) {
      newWidth = maxDim;
      newHeight = Math.round(height * (maxDim / width));
    } else {
      newHeight = maxDim;
      newWidth = Math.round(width * (maxDim / height));
    }

    canvas.width = newWidth;
    canvas.height = newHeight;

    // Draw video frame mirrored to maintain visual preview consistency
    ctx.translate(newWidth, 0);
    ctx.scale(-1, 1);
    ctx.drawImage(video, 0, 0, newWidth, newHeight);
    ctx.setTransform(1, 0, 0, 1, 0, 0);

    canvas.toBlob(
      (blob) => {
        setCapturedBlob(blob);
        const dataUrl = canvas.toDataURL('image/jpeg', 0.65);
        setCapturedImg(dataUrl);
        setStatus('captured');
        stopCamera();
      },
      'image/jpeg',
      0.65
    );
  };

  const handleRetake = () => {
    startCamera();
  };

  const handleVerify = async () => {
    if (!capturedBlob || !user) return;

    setStatus('verifying');
    setErrorMsg('');

    try {
      const result = await verifyGender(capturedBlob);

      if (!result.ok) {
        setStatus('error');
        setErrorMsg(result.error || 'Verification failed. Please ensure your face is clearly visible.');
        return;
      }

      if (result.verified) {
        await refreshUserData();
        setStatus('success');
      } else {
        setStatus('error');
        setErrorMsg('Gender mismatch or liveness check failed. Please make sure the photo matches your profile gender.');
      }
    } catch (err) {
      console.error('Verification request error:', err);
      
      const errMsg = err?.response?.data?.error || err?.message || (typeof err === 'string' ? err : '');
      const errStatus = err?.response?.status || null;
      
      // Fallback check for network error
      const isNetworkError = 
        errMsg.includes('Failed to fetch') || 
        errMsg.includes('fetch') || 
        errMsg.includes('network') || 
        errMsg.includes('Network Error') ||
        errMsg.includes('Application not found') || 
        errStatus === 404 ||
        err?.name === 'TypeError';

      if (isNetworkError) {
        // Secure offline handling: Since AWS Rekognition cannot run, we must NOT auto-verify females!
        setStatus('error');
        setErrorMsg('Verification servers are temporarily offline. Please try again later.');
        return;
      }
      
      setStatus('error');
      setErrorMsg(errMsg || 'Something went wrong during verification. Please try again.');
    }
  };

  const isOvalActive = status === 'countdown' || (status === 'idle' && isFaceAligned);

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div className={styles.content} onClick={(e) => e.stopPropagation()}>
        <button className={styles.closeBtn} onClick={onClose} aria-label="Close">
          <X size={20} />
        </button>

        <h3 className={styles.title}>Face & Gender Verification</h3>
        <p className={styles.desc}>
          {status === 'success' 
            ? 'Your verification is complete!' 
            : 'Keep your face inside the oval guide and look directly at the front camera.'}
        </p>
        {status !== 'success' && (
          <p style={{ fontSize: '0.8rem', color: 'rgba(255, 255, 255, 0.5)', textAlign: 'center', marginTop: '-8px', marginBottom: '16px', background: 'rgba(0, 229, 255, 0.05)', padding: '6px 12px', borderRadius: '6px', border: '1px dashed rgba(0, 229, 255, 0.2)' }}>
            🔒 <strong>Privacy Guarantee:</strong> This scan is only used for one-time verification. Your photo will <strong>never</strong> be shown to other users or saved on your profile.
          </p>
        )}

        {/* hidden canvas for snapshot processing */}
        <canvas ref={canvasRef} style={{ display: 'none' }} />

        {/* Viewport Box */}
        <div className={styles.cameraWrapper}>
          {status === 'success' ? (
            <div className={styles.video} style={{ background: 'var(--bg-elevated)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <div style={{ padding: 24, textAlign: 'center' }}>
                <CheckCircle size={64} className={styles.successIcon} />
                <div style={{ color: 'var(--neon-green)', fontWeight: 700, marginTop: 8 }}>Verified ✓</div>
              </div>
            </div>
          ) : capturedImg ? (
            <img src={capturedImg} alt="Face Capture" className={styles.previewImg} />
          ) : (
            <>
              <video
                ref={videoRef}
                className={styles.video}
                autoPlay
                playsInline
                muted
              />
              <div className={styles.faceGuideFrame}>
                <div className={`${styles.ovalMask} ${status === 'verifying' ? styles.scanning : (status === 'countdown' || isFaceAligned) ? styles.aligned : styles.notAligned}`}>
                  {status === 'countdown' && (
                    <div className={styles.countdownOverlay}>
                      <span className={styles.countdownNumber}>{countdown}</span>
                    </div>
                  )}
                </div>
              </div>
              {status === 'verifying' && <div className={`${styles.laserLine} ${styles.scanning}`} />}
            </>
          )}
        </div>

        {/* Alignment Status Indicator */}
        {(status === 'idle' || status === 'countdown') && !capturedImg && !isModelLoading && userData?.gender?.toLowerCase()?.trim() !== 'male' && (
          <div className={styles.alignmentStatus}>
            {!isFaceAligned ? (
              <span className={styles.statusRed}>⚠️ Face not aligned. Position your face inside the red oval.</span>
            ) : (
              <span className={styles.statusGreen}>✓ Face aligned! Scanning automatically...</span>
            )}
          </div>
        )}

        {/* Error Notification Banner */}
        {status === 'error' && errorMsg && (
          <div className={styles.statusContainer}>
            <AlertCircle size={24} className={styles.errorIcon} />
            <div className={styles.statusTitle} style={{ color: 'var(--neon-pink)' }}>Verification Error</div>
            <div className={styles.statusDesc}>{errorMsg}</div>
            
            {/* Offer switching gender */}
            <button 
              onClick={handleGoToProfile}
              className="btn-neon"
              style={{ 
                marginTop: '16px', 
                padding: '8px 16px', 
                fontSize: '0.85rem',
                background: 'rgba(0, 229, 255, 0.05)', 
                borderColor: 'rgba(0, 229, 255, 0.4)', 
                color: 'var(--neon-cyan)',
                cursor: 'pointer',
                borderRadius: '6px'
              }}
            >
              ✏️ Change Your Gender
            </button>
          </div>
        )}

        {/* Success Layout info */}
        {status === 'success' && (
          <div className={styles.statusContainer}>
            <div className={styles.statusTitle} style={{ color: 'var(--neon-green)' }}>Profile Verified</div>
            <div className={styles.statusDesc}>
              Congratulations! Your profile has been verified successfully. You now have a verified badge and full matching access.
            </div>
          </div>
        )}

        {/* Actions Controls */}
        <div className={styles.controls}>
          {status === 'idle' && (
            <div style={{ display: 'flex', flexDirection: 'column', width: '100%', gap: '10px' }}>
              {isModelLoading ? (
                <button className="btn-neon" disabled style={{ opacity: 0.7 }}>
                  <Loader size={16} className={styles.spinner} style={{ animation: 'spinSlow 2s linear infinite', marginRight: 8 }} /> Loading Face Guide...
                </button>
              ) : (
                <button 
                  className={isFaceAligned ? "btn-neon" : "btn-glass"} 
                  disabled
                  style={{ 
                    opacity: 1, 
                    cursor: 'default',
                    border: isFaceAligned ? '1px solid var(--neon-green)' : '1px solid rgba(255, 0, 92, 0.4)',
                    color: isFaceAligned ? 'var(--neon-green)' : 'var(--neon-pink)',
                    background: isFaceAligned ? 'rgba(0, 230, 118, 0.05)' : 'rgba(255, 0, 92, 0.02)'
                  }}
                >
                  {isFaceAligned ? "✓ Face Aligned! Hold Still..." : "⚠️ Align Face in Oval to Scan"}
                </button>
              )}
              
              {showManualCaptureFallback && (
                <button 
                  className="btn-neon" 
                  onClick={captureImage}
                  style={{ 
                    width: '100%',
                    background: 'linear-gradient(135deg, var(--neon-pink), var(--neon-purple))',
                    boxShadow: '0 4px 15px rgba(255, 0, 92, 0.3)',
                    border: 'none',
                    cursor: 'pointer'
                  }}
                >
                  📸 Capture Photo Manually
                </button>
              )}
            </div>
          )}

          {status === 'countdown' && (
            <button className="btn-neon" disabled style={{ opacity: 0.7 }}>
              Get Ready...
            </button>
          )}

          {status === 'captured' && (
            <>
              <button className="btn-glass" onClick={handleRetake}>
                <RefreshCw size={16} /> Retake
              </button>
              <button className="btn-neon" onClick={handleVerify}>
                Verify Gender
              </button>
            </>
          )}

          {status === 'verifying' && (
            <button className="btn-neon" disabled style={{ opacity: 0.8 }}>
              <Loader size={16} style={{ animation: 'spinSlow 2s linear infinite' }} /> Submitting Scan...
            </button>
          )}

          {status === 'success' && (
            <button className="btn-neon" onClick={onClose}>
              Done
            </button>
          )}

          {status === 'error' && (
            <>
              {!permissionError && (
                <button className="btn-glass" onClick={handleRetake}>
                  <RefreshCw size={16} /> Try Again
                </button>
              )}
              {permissionError && (
                <button className="btn-neon" onClick={onClose}>
                  Cancel
                </button>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

