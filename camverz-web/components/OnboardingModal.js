'use client';
import { useState } from 'react';
import { useAuth } from '../lib/auth-context';
import { updateUser } from '../lib/firestore';
import { X, MapPin, Calendar, User, Check, ChevronRight, ChevronLeft, AlertCircle, Info } from 'lucide-react';
import styles from './OnboardingModal.module.css';

const AVATARS = Array.from({ length: 15 }, (_, i) => `av${i + 1}`);

export default function OnboardingModal({ onClose, initialStep = 0 }) {
  const { user, refreshUserData, setShowVerification } = useAuth();
  const [step, setStep] = useState(initialStep);
  const [gender, setGender] = useState('');
  const [city, setCity] = useState('');
  const [country, setCountry] = useState('');
  const [dob, setDob] = useState('');
  const [avatar, setAvatar] = useState('av1');
  const [loading, setLoading] = useState(false);
  const [locating, setLocating] = useState(false);
  const [gpsDenied, setGpsDenied] = useState(false);
  const [gpsErrorMsg, setGpsErrorMsg] = useState('');
  const [dobError, setDobError] = useState('');

  const steps = ['Gender', 'Location', 'Birthday', 'Avatar'];

  // Network IP Location Fallback Helper
  const fallbackIpLocation = async () => {
    try {
      const res = await fetch('https://ipapi.co/json/');
      if (res.ok) {
        const data = await res.json();
        if (data.city) setCity(data.city);
        if (data.country_name) setCountry(data.country_name);
        return;
      }
    } catch (err) { }

    try {
      const res2 = await fetch('https://ip-api.com/json/');
      if (res2.ok) {
        const data2 = await res2.json();
        if (data2.city) setCity(data2.city);
        if (data2.country) setCountry(data2.country);
      }
    } catch (e) {
      console.error('IP location error:', e);
    }
  };

  // Rule 3 & 4: Direct synchronous click handler (User gesture mandatory on iOS Safari/Chrome)
  const handleDetectLocationClick = () => {
    setLocating(true);
    setGpsDenied(false);
    setGpsErrorMsg('');

    if (!navigator.geolocation) {
      setGpsErrorMsg('Geolocation is not supported by your browser.');
      fallbackIpLocation().then(() => setLocating(false));
      return;
    }

    // Direct call inside click listener prompts native browser permission popup on iPhone
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        const lat = pos.coords.latitude;
        const lon = pos.coords.longitude;

        fetch(`https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${lat}&longitude=${lon}&localityLanguage=en`)
          .then((res) => res.json())
          .then((bdcData) => {
            const detectedCity = bdcData.city || bdcData.locality || bdcData.principalSubdivision || '';
            const detectedCountry = bdcData.countryName || '';

            if (detectedCity && detectedCountry) {
              setCity(detectedCity);
              setCountry(detectedCountry);
              setLocating(false);
            } else {
              // OpenStreetMap Nominatim fallback
              fetch(`https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lon}&format=json&accept-language=en`)
                .then((r) => r.json())
                .then((nomData) => {
                  const city2 = nomData.address?.city || nomData.address?.town || nomData.address?.village || nomData.address?.state_district || '';
                  const country2 = nomData.address?.country || '';
                  if (city2 && country2) {
                    setCity(city2);
                    setCountry(country2);
                  } else {
                    fallbackIpLocation();
                  }
                  setLocating(false);
                })
                .catch(() => {
                  fallbackIpLocation();
                  setLocating(false);
                });
            }
          })
          .catch(() => {
            fallbackIpLocation();
            setLocating(false);
          });
      },
      (err) => {
        console.warn('GPS error / denied on Safari:', err.code, err.message);
        setLocating(false);
        if (err.code === 1) {
          // Rule 5: Safari / iPhone Permission Denied
          setGpsDenied(true);
        } else if (err.code === 3) {
          setGpsErrorMsg('GPS request timed out. Please make sure Location Services is ON in your iPhone settings and tap again.');
        } else {
          setGpsErrorMsg('Could not retrieve GPS location. Tap again to retry.');
        }
      },
      { enableHighAccuracy: true, timeout: 20000, maximumAge: 0 }
    );
  };

  const handleUseNetworkFallback = async () => {
    setLocating(true);
    await fallbackIpLocation();
    setLocating(false);
    setGpsDenied(false);
  };

  const isAdult = (dateStr) => {
    if (!dateStr) return false;
    const birthDate = new Date(dateStr);
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const m = today.getMonth() - birthDate.getMonth();
    if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) age--;
    return age >= 18;
  };

  const canProceed = () => {
    switch (step) {
      case 0: return !!gender;
      case 1: return !!(city && country);
      case 2: return dob && isAdult(dob);
      case 3: return !!avatar;
      default: return false;
    }
  };

  const handleNext = async () => {
    if (step === 2 && dob && !isAdult(dob)) {
      setDobError('You must be 18 or older to use Camverz.');
      return;
    }
    if (step < steps.length - 1) {
      setStep(step + 1);
    } else {
      // Save all data
      setLoading(true);
      try {
        const parts = dob.split('-');
        const formattedDob = `${parts[2]}/${parts[1]}/${parts[0]}`;
        await updateUser(user.uid, {
          gender,
          city: `${city}, ${country}`,
          dob: formattedDob,
          avatar,
          verified: gender === 'male',
        });
        await refreshUserData();
        onClose?.();
        if (gender === 'female') {
          setShowVerification(true);
        }
      } catch (e) {
        console.error(e);
      }
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal-content" style={{ position: 'relative' }}>
        <button className={styles.close} onClick={onClose}><X size={20} /></button>

        {/* Progress */}
        <div className={styles.progress}>
          {steps.map((s, i) => (
            <div key={s} className={`${styles.dot} ${i <= step ? styles.dotActive : ''}`}>
              {i < step ? <Check size={12} /> : i + 1}
            </div>
          ))}
        </div>

        <h3 className={styles.stepTitle}>{steps[step]}</h3>

        {/* Step 0: Gender */}
        {step === 0 && (
          <div className={styles.stepContent}>
            <p className={styles.stepDesc}>Select your gender</p>
            <div className={styles.genderCards}>
              <button
                className={`${styles.genderCard} ${gender === 'male' ? styles.genderActive : ''}`}
                onClick={() => setGender('male')}
              >
                <span className={styles.genderEmoji}>👨</span>
                <span>Male</span>
              </button>
              <button
                className={`${styles.genderCard} ${gender === 'female' ? styles.genderActive : ''}`}
                onClick={() => setGender('female')}
              >
                <span className={styles.genderEmoji}>👩</span>
                <span>Female</span>
              </button>
            </div>
            {gender === 'female' && (
              <p className={styles.verifyNote}>
                Female users require gender verification to get a verified badge.
              </p>
            )}
          </div>
        )}

        {/* Step 1: Location (Strictly adhering to Safari/iOS rules) */}
        {step === 1 && (
          <div className={styles.stepContent} style={{ textAlign: 'center' }}>
            <p className={styles.stepDesc}>Tap below to allow GPS location permission</p>

            <button
              className="btn-neon"
              onClick={handleDetectLocationClick}
              disabled={locating}
              style={{ width: '100%', padding: '14px 20px', fontSize: '1rem', marginBottom: 16 }}
            >
              <MapPin size={18} />
              {locating ? 'Requesting GPS Location...' : '📍 Detect My Location (GPS)'}
            </button>

            {city && country ? (
              <div style={{
                marginTop: '16px',
                padding: '14px 18px',
                borderRadius: '12px',
                background: 'rgba(0, 229, 255, 0.08)',
                border: '1px solid rgba(0, 229, 255, 0.3)',
                color: 'var(--neon-cyan)',
                display: 'flex',
                alignItems: 'center',
                gap: '10px',
                justifyContent: 'center'
              }}>
                <Check size={18} />
                <span>Detected: <strong>{city}, {country}</strong></span>
              </div>
            ) : gpsDenied ? (
              <div style={{
                marginTop: '12px',
                padding: '14px 16px',
                borderRadius: '12px',
                background: 'rgba(239, 68, 68, 0.08)',
                border: '1px solid rgba(239, 68, 68, 0.25)',
                color: '#FCA5A5',
                textAlign: 'left',
                fontSize: '0.85rem',
                lineHeight: '1.4'
              }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', fontWeight: 'bold', color: '#EF4444', marginBottom: '8px' }}>
                  <AlertCircle size={18} />
                  <span>iPhone Location Permission Denied</span>
                </div>
                <p style={{ margin: '0 0 8px 0' }}>Safari/Chrome location is turned off for camverz.com on your iPhone.</p>
                <div style={{ background: 'rgba(0,0,0,0.3)', padding: '10px 12px', borderRadius: '8px', marginBottom: '12px' }}>
                  <strong>How to enable on iPhone:</strong>
                  <ol style={{ margin: '6px 0 0 16px', padding: 0 }}>
                    <li>Open <strong>Settings</strong> → <strong>Privacy & Security</strong> → <strong>Location Services</strong></li>
                    <li>Select <strong>Safari Websites</strong> (or <strong>Chrome</strong>)</li>
                    <li>Select <strong>"While Using the App"</strong> & turn <strong>"Precise Location" ON</strong></li>
                  </ol>
                </div>
                <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
                  <button className="btn-glass" onClick={handleDetectLocationClick} style={{ flex: 1, padding: '8px 12px', fontSize: '0.8rem' }}>
                    Try GPS Again
                  </button>
                  <button className="btn-glass" onClick={handleUseNetworkFallback} style={{ flex: 1, padding: '8px 12px', fontSize: '0.8rem' }}>
                    Use Network Location
                  </button>
                </div>
              </div>
            ) : gpsErrorMsg ? (
              <p style={{ marginTop: '12px', color: '#EF4444', fontSize: '0.88rem' }}>{gpsErrorMsg}</p>
            ) : (
              <p style={{ marginTop: '12px', color: 'rgba(255,255,255,0.5)', fontSize: '0.88rem' }}>
                Tapping the button opens the native browser location permission prompt.
              </p>
            )}
          </div>
        )}

        {/* Step 2: Birthday */}
        {step === 2 && (
          <div className={styles.stepContent}>
            <p className={styles.stepDesc}>When is your birthday?</p>
            <div className={styles.dobWrapper}>
              <Calendar size={18} />
              <input
                type="date"
                className="input-glass"
                value={dob}
                onChange={e => { setDob(e.target.value); setDobError(''); }}
                max={new Date(new Date().setFullYear(new Date().getFullYear() - 18)).toISOString().split('T')[0]}
                style={{ flex: 1 }}
              />
            </div>
            {dobError && <p className={styles.error}>{dobError}</p>}
            <p className={styles.ageNote}>You must be at least 18 years old</p>
          </div>
        )}

        {/* Step 3: Avatar */}
        {step === 3 && (
          <div className={styles.stepContent}>
            <p className={styles.stepDesc}>Choose your avatar</p>
            <div className={styles.avatarGrid}>
              {AVATARS.map(av => (
                <button
                  key={av}
                  className={`${styles.avatarItem} ${avatar === av ? styles.avatarActive : ''}`}
                  onClick={() => setAvatar(av)}
                >
                  <img src={`/avatars/${av}.png`} alt={av} />
                </button>
              ))}
            </div>
          </div>
        )}

        {/* Navigation */}
        <div className={styles.navBtns}>
          {step > 0 && (
            <button className="btn-glass" onClick={() => setStep(step - 1)}>
              <ChevronLeft size={16} /> Back
            </button>
          )}
          <button
            className="btn-neon"
            onClick={handleNext}
            disabled={!canProceed() || loading}
            style={{ flex: 1 }}
          >
            {loading ? 'Saving...' : step === steps.length - 1 ? 'Complete Setup' : 'Continue'}
            {!loading && step < steps.length - 1 && <ChevronRight size={16} />}
          </button>
        </div>
      </div>
    </div>
  );
}
