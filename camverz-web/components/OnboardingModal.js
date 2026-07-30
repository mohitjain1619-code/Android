'use client';
import { useState } from 'react';
import { useAuth } from '../lib/auth-context';
import { updateUser } from '../lib/firestore';
import { X, MapPin, Calendar, User, Check, ChevronRight, ChevronLeft, AlertCircle } from 'lucide-react';
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
  const [gpsError, setGpsError] = useState('');
  const [dobError, setDobError] = useState('');

  const steps = ['Gender', 'Location', 'Birthday', 'Avatar'];

  // Direct synchronous click handler forces native browser permission popup (iOS & Android)
  const handleDetectLocationClick = () => {
    setLocating(true);
    setGpsError('');

    if (!navigator.geolocation) {
      setGpsError('Geolocation is not supported by your browser.');
      setLocating(false);
      return;
    }

    // Direct call triggers browser native "Allow Location" popup prompt
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
                    setGpsError('Could not resolve city name from GPS. Please try again.');
                  }
                  setLocating(false);
                })
                .catch(() => setLocating(false));
            }
          })
          .catch(() => {
            setGpsError('Reverse geocoding error. Please try again.');
            setLocating(false);
          });
      },
      (err) => {
        console.warn('GPS permission error:', err.code, err.message);
        setLocating(false);
        if (err.code === 1) {
          setGpsError('Location permission denied. Please allow location access when prompted or in browser settings and tap again.');
        } else if (err.code === 3) {
          setGpsError('Location request timed out. Please tap again.');
        } else {
          setGpsError('Could not get GPS location. Please tap again.');
        }
      },
      { enableHighAccuracy: true, timeout: 20000, maximumAge: 0 }
    );
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
      case 1: return city && country;
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

        {/* Step 1: Location (Synchronous User-Gesture Native Permission Popup) */}
        {step === 1 && (
          <div className={styles.stepContent} style={{ textAlign: 'center' }}>
            <p className={styles.stepDesc}>Tap below to prompt native browser location permission</p>
            
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
                <span>GPS Verified: <strong>{city}, {country}</strong></span>
              </div>
            ) : gpsError ? (
              <div style={{
                marginTop: '16px',
                padding: '12px 16px',
                borderRadius: '12px',
                background: 'rgba(239, 68, 68, 0.1)',
                border: '1px solid rgba(239, 68, 68, 0.3)',
                color: '#EF4444',
                display: 'flex',
                alignItems: 'center',
                gap: '8px',
                fontSize: '0.88rem',
                textAlign: 'left'
              }}>
                <AlertCircle size={18} style={{ flexShrink: 0 }} />
                <span>{gpsError}</span>
              </div>
            ) : (
              <p style={{ marginTop: '12px', color: 'rgba(255,255,255,0.5)', fontSize: '0.88rem' }}>
                Tapping the button opens the browser location permission prompt.
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
