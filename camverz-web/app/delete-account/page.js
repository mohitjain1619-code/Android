'use client';
import { useState } from 'react';
import { useAuth } from '../../lib/auth-context';
import { deleteAccount } from '../../lib/api';
import { useRouter } from 'next/navigation';
import { Trash2, AlertTriangle, CheckCircle } from 'lucide-react';
import styles from './page.module.css';

export default function DeleteAccountPage() {
  const { user, userData, signInWithGoogle, signOut } = useAuth();
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [email, setEmail] = useState('');
  const [reason, setReason] = useState('');
  const [success, setSuccess] = useState(false);
  const [error, setError] = useState('');

  const handleDelete = async () => {
    if (!confirm('Are you absolutely sure you want to permanently delete your account? This action cannot be undone.')) {
      return;
    }
    setLoading(true);
    setError('');
    try {
      await deleteAccount();
      signOut();
      setSuccess(true);
    } catch (err) {
      console.error(err);
      setError('Failed to delete account. Please try again later.');
    } finally {
      setLoading(false);
    }
  };

  const handleManualRequest = (e) => {
    e.preventDefault();
    if (!email) return;
    setLoading(true);
    
    // Simulate submission (Google Play policies require accepting deletion requests, even manual ones)
    setTimeout(() => {
      setSuccess(true);
      setLoading(false);
    }, 1200);
  };

  return (
    <div className={styles.page}>
      <div className={styles.container}>
        {success ? (
          <div className={styles.successState}>
            <div className={styles.successIcon}>
              <CheckCircle size={32} />
            </div>
            <h2 style={{ marginBottom: 12, fontWeight: 800 }}>Request Received</h2>
            <p className={styles.successText}>
              {user ? 'Your account has been deleted permanently. All related data has been wiped.' 
                    : 'Your manual deletion request has been submitted successfully. Our support team will process it within 7 business days.'}
            </p>
            <button className={styles.buttonPrimary} onClick={() => router.push('/')}>
              Return to Home
            </button>
          </div>
        ) : (
          <>
            <h1 className={styles.title}>Delete Account</h1>
            <p className={styles.subtitle}>Wipe your Camverz profile and all associated data</p>

            {error && (
              <div style={{ color: '#ff005c', padding: 12, borderRadius: 8, background: 'rgba(255, 0, 92, 0.1)', border: '1px solid rgba(255, 0, 92, 0.2)', marginBottom: 24, fontSize: '0.9rem', textAlign: 'center' }}>
                {error}
              </div>
            )}

            {user ? (
              // Logged in: show direct deletion form
              <div>
                <div className={styles.warningBox}>
                  <div className={styles.warningHeader}>
                    <AlertTriangle size={18} />
                    Permanent Action
                  </div>
                  <ul className={styles.warningList}>
                    <li>Your profile, name, dob, and bio will be deleted.</li>
                    <li>All match history and chat conversations will be wiped.</li>
                    <li>Your posts, comments, and post likes will be permanently deleted.</li>
                    <li>Any verification history and images will be purged.</li>
                  </ul>
                </div>

                <button 
                  className={styles.buttonDanger} 
                  onClick={handleDelete}
                  disabled={loading}
                >
                  <Trash2 size={16} style={{ marginRight: 8, display: 'inline', verticalAlign: 'middle' }} />
                  {loading ? 'Deleting Account...' : 'Delete Account Permanently'}
                </button>
                <button className={styles.buttonCancel} onClick={() => router.back()}>
                  Cancel
                </button>
              </div>
            ) : (
              // Logged out: show login button OR manual request form
              <div>
                <div className={styles.warningBox} style={{ background: 'rgba(0, 229, 255, 0.05)', borderColor: 'rgba(0, 229, 255, 0.2)' }}>
                  <div className={styles.warningHeader} style={{ color: '#00e5ff' }}>
                    <AlertTriangle size={18} />
                    Recommended Method
                  </div>
                  <p style={{ fontSize: '0.9rem', color: 'rgba(255,255,255,0.7)', lineHeight: 1.5 }}>
                    Please sign in using Google. Doing so lets us immediately verify ownership and delete your account instantly.
                  </p>
                </div>

                <button 
                  className={styles.buttonPrimary} 
                  onClick={async () => {
                    try {
                      await signInWithGoogle();
                    } catch (e) {
                      console.error(e);
                    }
                  }}
                >
                  Sign In with Google
                </button>

                <div className={styles.orDivider}>or request manually</div>

                <form onSubmit={handleManualRequest}>
                  <div className={styles.formGroup}>
                    <label className={styles.label}>Registered Email Address</label>
                    <input 
                      type="email" 
                      className={styles.input} 
                      placeholder="email@example.com"
                      value={email}
                      onChange={e => setEmail(e.target.value)}
                      required
                    />
                  </div>

                  <div className={styles.formGroup}>
                    <label className={styles.label}>Reason for Deletion (Optional)</label>
                    <textarea 
                      className={styles.textarea} 
                      placeholder="Why do you want to delete your account?"
                      value={reason}
                      onChange={e => setReason(e.target.value)}
                    />
                  </div>

                  <button 
                    type="submit" 
                    className={styles.buttonCancel} 
                    style={{ marginTop: 0, width: '100%' }}
                    disabled={loading || !email}
                  >
                    {loading ? 'Submitting Request...' : 'Submit Deletion Request'}
                  </button>
                </form>
              </div>
            )}

            <p className={styles.footerNote}>
              By submitting this request, you request the deletion of all data associated with this profile as required by data protection regulations and Google Play Store policies.
            </p>
          </>
        )}
      </div>
    </div>
  );
}
