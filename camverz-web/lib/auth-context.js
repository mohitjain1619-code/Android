'use client';
import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { authWithGoogle, getMe, getToken, removeToken, updateMe } from './api';

const AuthContext = createContext({});

// Google Sign-In without Firebase SDK
// Uses Google's GSI (Sign In With Google) library
async function getGoogleIdToken(clientId) {
  return new Promise((resolve, reject) => {
    if (typeof window === 'undefined') return reject('Not in browser');
    
    // Load Google Identity Services library if not loaded
    if (!window.google?.accounts?.id) {
      const script = document.createElement('script');
      script.src = 'https://accounts.google.com/gsi/client';
      script.async = true;
      script.onload = () => {
        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: (response) => {
            resolve(response.credential); // This is the ID token
          },
        });
        window.google.accounts.id.prompt((notification) => {
          if (notification.isNotDisplayed() || notification.isSkippedMoment()) {
            // Fallback to button-triggered popup
            reject('Google prompt not shown');
          }
        });
      };
      document.head.appendChild(script);
    } else {
      window.google.accounts.id.initialize({
        client_id: clientId,
        callback: (response) => {
          resolve(response.credential);
        },
      });
      window.google.accounts.id.prompt();
    }
  });
}

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [userData, setUserData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [showLogin, setShowLogin] = useState(false);
  const [showVerification, setShowVerification] = useState(false);
  const [showOnboarding, setShowOnboarding] = useState(false);
  const [showAppRedirect, setShowAppRedirect] = useState(null);

  // Check existing JWT token on mount
  useEffect(() => {
    const token = getToken();
    if (token) {
      loadUser();
    } else {
      setLoading(false);
    }
  }, []);

  // Listen for auth:expired event
  useEffect(() => {
    const handleExpired = () => {
      setUser(null);
      setUserData(null);
      setShowLogin(true);
    };
    window.addEventListener('auth:expired', handleExpired);
    return () => window.removeEventListener('auth:expired', handleExpired);
  }, []);

  const loadUser = useCallback(async () => {
    try {
      const response = await getMe();
      if (response.ok) {
        setUser({ uid: response.user.id, ...response.user });
        setUserData(response.user);
      }
    } catch (err) {
      console.error('Failed to load user:', err);
      removeToken();
    } finally {
      setLoading(false);
    }
  }, []);

  const signInWithGoogle = async () => {
    try {
      // Use Google OAuth popup flow (without Firebase)
      const GOOGLE_CLIENT_ID = process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || '';
      
      // Open Google OAuth popup
      const width = 500;
      const height = 600;
      const left = window.screenX + (window.outerWidth - width) / 2;
      const top = window.screenY + (window.outerHeight - height) / 2;
      
      const redirectUri = `${window.location.origin}/auth/callback`;
      const scope = 'openid email profile';
      const authUrl = `https://accounts.google.com/o/oauth2/v2/auth?` +
        `client_id=${GOOGLE_CLIENT_ID}` +
        `&redirect_uri=${encodeURIComponent(redirectUri)}` +
        `&response_type=id_token` +
        `&scope=${encodeURIComponent(scope)}` +
        `&nonce=${Math.random().toString(36).substring(2)}`;
      
      // For now, use a simpler approach: the Google One Tap / popup
      // The Android app sends the ID token directly to /auth/google
      // For web, we'll use Google's sign-in button approach
      
      return new Promise((resolve, reject) => {
        const popup = window.open(authUrl, 'google-signin', 
          `width=${width},height=${height},left=${left},top=${top}`
        );
        
        // Listen for the callback message
        const handleMessage = async (event) => {
          if (event.origin !== window.location.origin) return;
          if (event.data?.type === 'google-auth' && event.data?.idToken) {
            window.removeEventListener('message', handleMessage);
            popup?.close();
            
            try {
              const result = await authWithGoogle(event.data.idToken);
              if (result.ok) {
                setUser({ uid: result.user.id, ...result.user });
                setUserData(result.user);
                
                if ((result.isNewUser || !result.user.gender) && window.location.pathname !== '/affiliate') {
                  setShowOnboarding(true);
                }
                resolve(result);
              }
            } catch (err) {
              reject(err);
            }
          }
        };
        
        window.addEventListener('message', handleMessage);
        
        // Cleanup on popup close
        const checkClosed = setInterval(() => {
          if (popup?.closed) {
            clearInterval(checkClosed);
            window.removeEventListener('message', handleMessage);
          }
        }, 1000);
      });
    } catch (error) {
      console.error('Google Sign-In Error:', error);
      throw error;
    }
  };

  const signOut = async () => {
    removeToken();
    setUser(null);
    setUserData(null);
  };

  const refreshUserData = async () => {
    if (getToken()) {
      await loadUser();
    }
  };

  return (
    <AuthContext.Provider value={{ 
      user, userData, loading, 
      signInWithGoogle, signOut, refreshUserData, 
      showLogin, setShowLogin, 
      showVerification, setShowVerification, 
      showOnboarding, setShowOnboarding, 
      showAppRedirect, setShowAppRedirect 
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);
