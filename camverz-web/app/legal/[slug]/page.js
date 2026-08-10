'use client';
import { use } from 'react';
import Link from 'next/link';
import { ArrowLeft, Scale, Shield, Landmark, Cookie, HeartHandshake, AlertTriangle, ShieldCheck } from 'lucide-react';
import styles from './page.module.css';

const legalDocs = {
  'child-safety': {
    title: 'Child Safety & Protection Standards',
    lastUpdated: 'July 30, 2026',
    icon: ShieldCheck,
    themeColor: '#FF0055',
    content: (
      <>
        <p>Camverz is committed to providing a safe environment. We enforce a zero-tolerance policy against any form of Child Sexual Abuse Material (CSAM) and Child Sexual Exploitation and Abuse (CSAE).</p>

        <h2>1. Zero Tolerance Policy for CSAM & CSAE</h2>
        <p>Camverz strictly prohibits the creation, upload, sharing, or transmission of any content depicting child sexual abuse or exploitation. Any account found attempting to share or engage in such material will be permanently banned immediately, and reported to relevant legal authorities.</p>

        <h2>2. Age Restrictions & Minor Protection</h2>
        <p>Camverz is strictly for users aged 18 and older. Minors are strictly prohibited from creating accounts or using our video matching services. We employ age-verification prompts and automated moderation filters to detect and remove unauthorized minor accounts.</p>

        <h2>3. Reporting to Law Enforcement & NCMEC</h2>
        <p>In accordance with federal and international child protection laws, Camverz reports all instances of CSAM/CSAE content or child safety violations directly to the National Center for Missing & Exploited Children (NCMEC) and appropriate regional law enforcement agencies.</p>

        <h2>4. In-App Reporting & Contact Information</h2>
        <p>Users can report child safety concerns directly within the app using the in-video report button. For urgent child safety inquiries or compliance matters, please reach our designated Child Safety Officer at: <strong>jainmohit.cr007@gmail.com</strong>.</p>
      </>
    )
  },
  'privacy-policy': {
    title: 'Privacy Policy',
    lastUpdated: 'June 24, 2026',
    icon: Shield,
    themeColor: '#00E5FF',
    content: (
      <>
        <p>At Camverz, your privacy is our highest commitment. This policy describes how we collect, process, and protect your information when using both our Android Mobile Application and our Web Application, owned and operated by Mohit Jain.</p>

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
        <p>Welcome to Camverz. By accessing our website, mobile app, or using our video matching services, you agree to comply with the terms and conditions outlined below. These terms govern the relationship between you and the registered owner, Mohit Jain.</p>

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
    title: 'Refund & Cancellation Policy',
    lastUpdated: 'August 10, 2026',
    icon: Landmark,
    themeColor: '#FF6D00',
    content: (
      <>
        <p>This policy details the strict refund and cancellation terms for all VIP passes and packages purchased on the Camverz Web Platform.</p>

        <h2>1. Strict No Refunds Policy</h2>
        <p>All sales and purchases of digital VIP access passes (1 Day Pass, 10 Days Package, 1 Month VIP Package) on Camverz are final and non-refundable. Once a pass is purchased, digital access is granted instantly to your account and cannot be refunded or cancelled.</p>

        <h2>2. Instant Activation & No Recurring Billing</h2>
        <p>All purchases are one-time payments for the chosen duration. There are no automatic hidden renewals or recurring subscription charges. Because digital benefits are delivered immediately upon successful payment, no partial or pro-rated refunds will be granted.</p>

        <h2>3. Technical Exceptions & Inquiries</h2>
        <p>In the rare event of a technical issue where a payment was processed but digital access failed to activate, please contact support at <strong>support@camverz.com</strong> with your transaction ID and account details for manual activation.</p>
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
