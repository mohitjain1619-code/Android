'use client';
import { use } from 'react';
import Link from 'next/link';
import { ArrowLeft, Scale, Shield, Landmark, Cookie, HeartHandshake, AlertTriangle } from 'lucide-react';
import styles from './page.module.css';

const legalDocs = {
  'privacy-policy': {
    title: 'Privacy Policy',
    lastUpdated: 'June 24, 2026',
    icon: Shield,
    themeColor: '#00E5FF',
    content: (
      <>
        <p>At Camverz, your privacy is our highest commitment. This policy describes how we collect, process, and protect your information when using both our Android Mobile Application and our Web Application.</p>

        <h2>1. Information We Collect</h2>
        <p>We collect details necessary to operate our service, including:</p>
        <ul>
          <li><strong>Authentication Details:</strong> Google Account information (email, profile photo, and name) when signing in.</li>
          <li><strong>Profile Data:</strong> Your selected username, gender, date of birth, profile avatar choice, and city location.</li>
          <li><strong>WebRTC Signals:</strong> Temporary server connection states to match users. We do not store or transmit call media.</li>
        </ul>

        <h2>2. Zero-Recording Call Protection</h2>
        <p>All video calls are strictly peer-to-peer (P2P) using WebRTC encryption. We do not monitor, record, screenshot, or store your video or audio signals on our servers under any circumstances. Conversations remain entirely private between you and your match.</p>

        <h2>3. Location Information</h2>
        <p>We request browser/device geolocation to reverse-geocode your general city and country. This location is displayed on your matching card to enhance your conversations. You can opt out of location sharing at any time via your browser settings.</p>

        <h2>4. Data Storage and Deletion</h2>
        <p>All user profiles are stored securely in Google Firebase Firestore. You can request deletion of your account at any time through our contact desk or directly inside your profile page. Once requested, your account and all associated posts/blocked-list subcollections are permanently purged from our database within 48 hours.</p>
      </>
    )
  },
  'terms-and-conditions': {
    title: 'Terms & Conditions',
    lastUpdated: 'June 24, 2026',
    icon: Scale,
    themeColor: '#BD00FF',
    content: (
      <>
        <p>Welcome to Camverz. By accessing our website, mobile app, or using our video matching services, you agree to comply with the terms and conditions outlined below.</p>

        <h2>1. Eligibility Requirements</h2>
        <p>You must be at least 18 years old to use Camverz. By registering an account, you represent and warrant that you are of legal age. If we find accounts belonging to minors, they are terminated immediately without prior notice.</p>

        <h2>2. User Conduct Standards</h2>
        <p>You agree not to use the service to:</p>
        <ul>
          <li>Display or distribute explicit, obscene, or adult content on video streams or community posts.</li>
          <li>Harass, abuse, threaten, or impersonate other users.</li>
          <li>Broadcast advertisements, spam links, commercial content, or solicit money from other users.</li>
        </ul>

        <h2>3. Account Suspension & Hardware Bans</h2>
        <p>Camverz moderators monitor community reports 24/7. We reserve the absolute right to suspend accounts or issue permanent IP and hardware bans to users who violate these Terms of Service.</p>

        <h2>4. Platform Purchases</h2>
        <p>Subscriptons, virtual credits, or packages purchased on the web app are processed through secure payments channels. All credits are non-transferable and subject to our refund guidelines.</p>
      </>
    )
  },
  'refund-policy': {
    title: 'Refund Policy',
    lastUpdated: 'June 24, 2026',
    icon: Landmark,
    themeColor: '#FF6D00',
    content: (
      <>
        <p>This policy details your refund rights for packages, credits, and premium subscriptions purchased on the Camverz Web Platform.</p>

        <h2>1. Premium Web Subscriptions</h2>
        <p>Subscriptions purchased directly on the web app can be cancelled at any time. Once cancelled, your premium benefits remain active until the end of the current billing cycle. No partial refunds are issued for unused portions of billing cycles.</p>

        <h2>2. Virtual Coins and Match Passes</h2>
        <p>Virtual credits or coins are consumed as you use matching preferences. Unused coins are eligible for a full refund within 14 days of purchase, provided none of the coins from the package have been spent. If any portion of the package has been used, the purchase becomes non-refundable.</p>

        <h2>3. Processing Refunds</h2>
        <p>To request a refund, please contact billing support at <strong>billing@camverz.com</strong> with your transaction ID, Google Account email, and custom user ID. Refund transactions take 5–10 business days to clear back to your original payment method.</p>
      </>
    )
  },
  'cookie-policy': {
    title: 'Cookie Policy',
    lastUpdated: 'June 24, 2026',
    icon: Cookie,
    themeColor: '#00E676',
    content: (
      <>
        <p>We use cookies and similar browser storage mechanisms to improve your matching experience, keep you logged in, and analyze web platform performance.</p>

        <h2>1. What are Cookies?</h2>
        <p>Cookies are small text files stored in your browser when you visit a website. We also make use of browser LocalStorage and SessionStorage to manage active WebRTC connections and authentication tokens.</p>

        <h2>2. How We Use Cookies</h2>
        <ul>
          <li><strong>Authentication:</strong> We store secure tokens to verify your login session and Firestore references.</li>
          <li><strong>Preferences:</strong> We store local configurations like your mute states, cameras chosen, and category filters.</li>
          <li><strong>Analytics:</strong> We use aggregated web indicators to count visitors and track load speed.</li>
        </ul>

        <h2>3. Managing Cookie Preferences</h2>
        <p>You can adjust, block, or delete cookies at any time through your browser settings. However, disabling all cookies will prevent you from signing in and launching video calls.</p>
      </>
    )
  },
  'community-guidelines': {
    title: 'Community Guidelines',
    lastUpdated: 'June 24, 2026',
    icon: HeartHandshake,
    themeColor: '#FF006E',
    content: (
      <>
        <p>Camverz is a place to meet new friends, find connections, and have clean conversation. We ask all community members to follow these guidelines to keep our matching pools friendly, safe, and positive.</p>

        <h2>1. Zero Tolerance for Harassment</h2>
        <p>Treat every match with dignity. Verbal abuse, racism, sexism, bullying, hate speech, and unwelcome sexual advances are strictly prohibited. Violators are banned instantly.</p>

        <h2>2. No Nudity or Sexual Behavior</h2>
        <p>Camverz is a clean communication network. Any display of nudity, pornography, or suggestive behaviors on stream or in text posts will result in an immediate permanent ban.</p>

        <h2>3. Be Real: No Spoofing</h2>
        <p>Do not use third-party virtual cameras, loops, pre-recorded videos, or try to spoof your camera stream. We matching users with real, live people only. Fake streams are flagged and banned by automated moderators.</p>

        <h2>4. Report System: Help Your Peers</h2>
        <p>If you match with a user violating these guidelines, press the <strong>Report</strong> button immediately. Reporting helps our moderators review logs and keep the network secure.</p>
      </>
    )
  }
};

export default function LegalPage({ params }) {
  // Resolve params using React.use() wrapper to satisfy React 19 rules in Next.js
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
