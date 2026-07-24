'use client';
import { useState, useEffect } from 'react';
import { useAuth } from '../lib/auth-context';
import { updateUser } from '../lib/firestore';
import { X, MapPin, Calendar, User, Check, ChevronRight, ChevronLeft } from 'lucide-react';
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
  const [dobError, setDobError] = useState('');

  const steps = ['Gender', 'Location', 'Birthday', 'Avatar'];

  // Auto-detect location
  const detectLocation = () => {
    if (!navigator.geolocation) return;
    setLocating(true);
    navigator.geolocation.getCurrentPosition(
      async (pos) => {
        try {
          const res = await fetch(`https://nominatim.openstreetmap.org/reverse?lat=${pos.coords.latitude}&lon=${pos.coords.longitude}&format=json&accept-language=en`);
          const data = await res.json();
          setCity(data.address?.city || data.address?.town || data.address?.village || '');
          setCountry(data.address?.country || '');
        } catch (e) { console.error(e); }
        setLocating(false);
      },
      () => setLocating(false),
      { enableHighAccuracy: false, timeout: 10000 }
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

        {/* Step 1: Location */}
        {step === 1 && (
          <div className={styles.stepContent}>
            <p className={styles.stepDesc}>Where are you located?</p>
            <button className="btn-glass" onClick={detectLocation} disabled={locating} style={{ width: '100%', marginBottom: 16 }}>
              <MapPin size={16} />
              {locating ? 'Detecting...' : 'Detect My Location'}
            </button>
            {city && country ? (
              <p style={{ marginTop: '16px', color: 'var(--neon-cyan)', display: 'flex', alignItems: 'center', gap: '8px', justifyContent: 'center' }}>
                <Check size={16} /> Detected: <strong>{city}, {country}</strong>
              </p>
            ) : (
              <p style={{ marginTop: '16px', color: 'rgba(255,255,255,0.5)', textAlign: 'center', fontSize: '0.9rem' }}>
                Click the button to automatically detect your location.
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
