'use client';
import { use } from 'react';
import Link from 'next/link';
import { ArrowLeft, Scale, Shield, Landmark, Cookie, HeartHandshake, AlertTriangle, ShieldCheck } from 'lucide-react';
import styles from './page.module.css';

const legalDocs = {
  'child-safety': {
    title: 'Child Safety & Protection Standards',
    lastUpdated: 'September 18, 2026',
    icon: ShieldCheck,
    themeColor: '#FF0055',
    content: (
      <>
        <p>Camverz is committed to providing a safe, clean, and secure environment for all users. We enforce a strict zero-tolerance policy against any form of Child Sexual Abuse Material (CSAM) and Child Sexual Exploitation and Abuse (CSAE).</p>

        <h2>1. Zero Tolerance Policy for CSAM & CSAE</h2>
        <p>Camverz strictly prohibits the creation, upload, sharing, transmission, or display of any content depicting child sexual abuse or exploitation. Any account or device found attempting to share, display, or engage in CSAM/CSAE will be permanently terminated immediately, with hardware and IP access revoked, and reported to legal authorities.</p>

        <h2>2. Strict 18+ Age Requirement & Minor Protection</h2>
        <p>Camverz is strictly reserved for users aged 18 and older. Minors under the age of 18 are strictly prohibited from creating accounts, accessing the web app, or using our live video matching services. We employ age-verification prompts, automated behavioral filters, and active moderation to detect and remove unauthorized minor accounts instantly.</p>

        <h2>3. Mandatory Reporting to NCMEC & Law Enforcement</h2>
        <p>In accordance with federal and international child protection laws, Camverz reports all detected instances of CSAM/CSAE content, child safety violations, or minor exploitation attempts directly to the National Center for Missing & Exploited Children (NCMEC) and appropriate regional law enforcement agencies.</p>

        <h2>4. In-App Reporting & Contact Information</h2>
        <p>Users can report child safety concerns directly within the video call interface or story feeds using the <strong>Report</strong> button. For urgent child safety inquiries, law enforcement requests, or compliance matters, please reach our designated Child Safety Officer at: <strong>jainmohit.cr007@gmail.com</strong>.</p>
      </>
    )
  },
  'privacy-policy': {
    title: 'Privacy Policy',
    lastUpdated: 'September 18, 2026',
    icon: Shield,
    themeColor: '#00E5FF',
    content: (
      <>
        <p>At Camverz, your privacy and data security are our highest priority. This policy outlines how we collect, process, and protect your information across our Android Mobile Application and Web Application, operated by Mohit Jain.</p>

        <h2>1. Information We Collect</h2>
        <p>We collect only the essential details required to deliver our social matching and messaging services:</p>
        <ul>
          <li><strong>Authentication Details:</strong> Google Account information (email, profile photo, and name) used for secure sign-in.</li>
          <li><strong>Profile Parameters:</strong> Selected username, gender identity, sexual preference (Straight, Gay, Lesbian), profile avatar choice, and city location.</li>
          <li><strong>WebRTC Connection Signals:</strong> Temporary server signaling tokens used solely to establish live peer-to-peer (P2P) connections.</li>
        </ul>

        <h2>2. 100% Zero-Recording Video Call Guarantee</h2>
        <p>All 1-on-1 video calls operate strictly over encrypted Peer-to-Peer (P2P) WebRTC connections. Camverz does NOT record, screenshot, monitor, store, or transmit your video streams or audio conversations on our servers under any circumstances. Your conversations remain entirely private between you and your match.</p>

        <h2>3. Geolocation & Matching Parameters</h2>
        <p>With your explicit browser/device permission, we reverse-geocode your general city and country location to display on your matching card. This helps you connect with nearby or global peers. You can disable location access at any time via your browser settings.</p>

        <h2>4. Permanent Account & Data Deletion</h2>
        <p>All user profiles and active data are stored securely in Google Firebase Firestore. You can request permanent account deletion at any time in your profile settings or via support. Once requested, your profile, posts, stories, and saved preferences are permanently purged from our servers within 48 hours.</p>
      </>
    )
  },
  'terms-and-conditions': {
    title: 'Terms & Conditions',
    lastUpdated: 'September 18, 2026',
    icon: Scale,
    themeColor: '#BD00FF',
    content: (
      <>
        <p>Welcome to Camverz. By accessing our web application, mobile app, or using our live video matching services, you agree to comply with and be bound by these Terms & Conditions. These terms govern the legal agreement between you and the platform operator, Mohit Jain.</p>

        <h2>1. Strict Eligibility Requirements (18+)</h2>
        <p>You must be at least 18 years old to use Camverz. By registering an account or initiating a video match, you represent and warrant that you are of legal adult age. Accounts belonging to minors will be terminated immediately upon discovery without prior notice.</p>

        <h2>2. Strict Anti-Nudity & User Conduct Standards</h2>
        <p>Camverz is a clean, respectful social media and video connection platform. You strictly agree NOT to:</p>
        <ul>
          <li>Display, stream, or post any form of nudity, partial nudity, explicit sexual acts, or adult content.</li>
          <li>Harass, abuse, threaten, bully, or discriminate against other users based on race, gender, or sexual orientation.</li>
          <li>Broadcast unauthorized advertisements, spam links, commercial promotions, or solicit money from other members.</li>
          <li>Use virtual camera software, pre-recorded video loops, or stream-spoofing tools.</li>
        </ul>

        <h2>3. 24/7 Moderation & Instant Hardware Bans</h2>
        <p>Camverz moderators and AI monitoring systems operate 24/7 to enforce community standards. We reserve the absolute right to suspend accounts, revoke access, and issue permanent IP and hardware bans to any user who violates these Terms.</p>

        <h2>4. Platform Purchases & Digital Passes</h2>
        <p>Purchases of digital VIP passes or credit packages on the platform are processed through secure payment gateways. All passes are non-transferable, one-time purchases, and governed by our strict Refund Policy.</p>
      </>
    )
  },
  'refund-policy': {
    title: 'Refund & Cancellation Policy',
    lastUpdated: 'September 18, 2026',
    icon: Landmark,
    themeColor: '#EC4899',
    content: (
      <>
        <p>This policy details the refund and cancellation terms for all digital VIP access passes and feature packages purchased on the Camverz Web Platform.</p>

        <h2>1. Strict 100% No Refunds Policy</h2>
        <p>All sales and purchases of digital VIP access passes (1 Day Pass, 10 Days Package, 1 Month VIP Package) on Camverz are final, instant, and non-refundable. Once a pass is purchased, digital access benefits are granted instantly to your account and cannot be returned, cancelled, or refunded.</p>

        <h2>2. Instant Digital Activation & One-Time Payment</h2>
        <p>All purchases are one-time payments for the chosen duration. There are no recurring subscriptions, hidden fees, or automatic renewals. Because digital benefits are delivered immediately upon payment completion, no partial or pro-rated refunds will be issued.</p>

        <h2>3. Technical Support & Inquiries</h2>
        <p>In the rare event of a technical glitch where payment succeeded but digital pass benefits failed to activate, please contact our support desk at <strong>support@camverz.com</strong> with your transaction ID and registered email for instant manual activation.</p>
      </>
    )
  },
  'cookie-policy': {
    title: 'Cookie Policy',
    lastUpdated: 'September 18, 2026',
    icon: Cookie,
    themeColor: '#00E676',
    content: (
      <>
        <p>We use cookies, LocalStorage, and SessionStorage to optimize your video matching performance, maintain active login sessions, and secure platform features.</p>

        <h2>1. What are Cookies & Local Storage?</h2>
        <p>Cookies and LocalStorage are small text data entries stored in your browser when visiting websites. We use them to keep you logged in safely and store active WebRTC signaling tokens during live video sessions.</p>

        <h2>2. Purpose of Storage</h2>
        <ul>
          <li><strong>Authentication:</strong> Secure session tokens verifying your Firebase account login state.</li>
          <li><strong>User Preferences:</strong> Storing local configurations such as camera selection, microphone mute states, and category preferences.</li>
          <li><strong>Analytics & Performance:</strong> Aggregated indicators to optimize matching speed and monitor platform uptime.</li>
        </ul>

        <h2>3. Managing Browser Preferences</h2>
        <p>You can clear or block cookies at any time through your browser settings. However, disabling essential session storage will prevent you from signing in or launching video calls.</p>
      </>
    )
  },
  'community-guidelines': {
    title: 'Community Guidelines',
    lastUpdated: 'September 18, 2026',
    icon: HeartHandshake,
    themeColor: '#FF006E',
    content: (
      <>
        <p>Camverz is a vibrant, respectful social media and video connection platform. We welcome users of all backgrounds and sexual orientations (Straight, Gay, Lesbian). Follow these guidelines to keep our community safe, clean, and positive.</p>

        <h2>1. Strict Zero-Nudity & Clean Streaming Rule</h2>
        <p>Camverz strictly prohibits any form of nudity, partial nudity, explicit sexual gestures, underwear displays, or suggestive acts on video camera or post feeds. Any user engaging in explicit behavior will be permanently banned immediately.</p>

        <h2>2. Respect & LGBTQ+ Inclusivity</h2>
        <p>Treat every member with respect and kindness. We maintain zero tolerance for homophobic remarks, hate speech, racism, sexism, verbal harassment, or bullying. Any discriminatory behavior towards LGBTQ+ members results in an instant permanent hardware ban.</p>

        <h2>3. Authentic Live Camera Streams Only</h2>
        <p>Always stream live face-to-face video. Using third-party virtual cameras, video loopers, fake camera feeds, or pre-recorded clips is strictly forbidden. Fake streams are flagged and removed automatically.</p>

        <h2>4. Instant In-App Reporting</h2>
        <p>If you encounter a user breaking these rules during a video call or in community posts, press the <strong>Report</strong> button immediately. Our moderation team reviews reports 24/7 to maintain a safe platform.</p>
      </>
    )
  }
};

export default function LegalPage({ params }) {
  const resolvedParams = use(params);
  const slug = resolvedParams.slug;
  
  const docData = legalDocs[slug];

  if (!docData) {
    return (
      <div className={styles.notFoundContainer}>
        <div className={styles.bgGradientPage} />
        <div className={styles.notFoundContent}>
          <AlertTriangle size={48} className={styles.warnIcon} />
          <h2>Document Not Found</h2>
          <p>The legal document you are looking for does not exist.</p>
          <Link href="/" className="btn-neon">
            <ArrowLeft size={16} /> Back to Home
          </Link>
        </div>
      </div>
    );
  }

  const DocIcon = docData.icon;

  return (
    <div className={styles.container}>
      <div className={styles.bgGradientPage} />
      <div className={styles.inner}>
        
        {/* Back Link */}
        <Link href="/" className={styles.backLink}>
          <ArrowLeft size={16} />
          <span>Back to Home</span>
        </Link>

        {/* Legal Document Card */}
        <div className={styles.legalCard} style={{ '--accent-color': docData.themeColor }}>
          <header className={styles.header}>
            <div className={styles.iconCircle}>
              <DocIcon size={28} />
            </div>
            <h1>{docData.title}</h1>
            <span className={styles.dateLabel}>Last Updated: {docData.lastUpdated}</span>
          </header>

          <hr className={styles.divider} />

          <div className={styles.content}>
            {docData.content}
          </div>
        </div>

      </div>
    </div>
  );
}
