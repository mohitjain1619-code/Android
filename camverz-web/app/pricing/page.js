'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useAuth } from '../../lib/auth-context';
import { Check, Zap, Sparkles, Crown, ShieldCheck, Lock, RefreshCw, ArrowRight, Star, Video, EyeOff, MapPin, Globe } from 'lucide-react';
import styles from './page.module.css';

const REGIONAL_COUNTRIES = ['india', 'pakistan', 'bangladesh', 'thailand', 'in', 'pk', 'bd', 'th'];

export default function PricingPage() {
  const { user, userData, setShowLogin, setShowOnboarding } = useAuth();
  const [regionMode, setRegionMode] = useState('international'); // 'regional' | 'international'
  const [detectedLocation, setDetectedLocation] = useState('Detecting location...');

  // Auto-detect user country from userData or IP geolocation
  useEffect(() => {
    let userCountry = '';
    if (userData?.city) {
      userCountry = userData.city.split(',').pop().trim().toLowerCase();
    } else if (userData?.country) {
      userCountry = userData.country.trim().toLowerCase();
    }

    if (userCountry && REGIONAL_COUNTRIES.some(c => userCountry.includes(c))) {
      setRegionMode('regional');
      setDetectedLocation(`Detected: ${userData.city || userCountry.toUpperCase()} (Regional PPP Discount Applied 🏷️)`);
    } else if (userCountry) {
      setRegionMode('international');
      setDetectedLocation(`Detected: ${userData.city || userCountry.toUpperCase()} (International Tier)`);
    } else {
      // Fallback: IP Geolocation lookup
      fetch('https://ipapi.co/json/')
        ? fetch('https://ipapi.co/json/')
            .then(res => res.json())
            .then(data => {
              const countryName = (data.country_name || data.country_code || '').toLowerCase();
              if (REGIONAL_COUNTRIES.some(c => countryName.includes(c))) {
                setRegionMode('regional');
                setDetectedLocation(`Detected: ${data.country_name || 'Regional'} (Regional PPP Discount Applied 🏷️)`);
              } else {
                setRegionMode('international');
                setDetectedLocation(`Detected: ${data.country_name || 'Global'} (International Tier)`);
              }
            })
            .catch(() => {
              setDetectedLocation('Global Pricing');
            })
        : setDetectedLocation('Global Pricing');
    }
  }, [userData]);

  const packages = [
    {
      id: '1-day',
      title: '1 Day Pass',
      icon: Zap,
      badge: 'Quick Trial',
      badgeClass: styles.badgeCyan,
      description: 'Full access for 24 hours. Perfect for quick connections and testing out premium matching.',
      durationLabel: '/ 24 Hours',
      tiers: [
        {
          type: 'With Ads',
          price: regionMode === 'regional' ? '3' : '7',
          tag: 'Standard Access',
          tagClass: styles.tagWithAds,
          isFeatured: false,
          features: [
            'Unlimited Video Calls (24 Hours)',
            'Global Live Matching',
            'Gender Preference Filter',
            'Standard Video Quality',
            'Interstitials & Banner Ads Included',
          ],
          btnText: `Choose 1-Day ($${regionMode === 'regional' ? '3' : '7'})`,
          btnClass: styles.btnStandard,
        },
        {
          type: 'Without Ads (Ad-Free)',
          price: regionMode === 'regional' ? '5' : '10',
          tag: 'Zero Ads + VIP',
          tagClass: styles.tagNoAds,
          isFeatured: true,
          features: [
            'Unlimited HD Video Calls (24 Hours)',
            '100% Ad-Free Experience',
            'Priority Fast Match Queue',
            'Full Gender & Country Filters',
            'VIP Badge on Profile',
          ],
          btnText: `Go Ad-Free ($${regionMode === 'regional' ? '5' : '10'})`,
          btnClass: styles.btnPro,
        },
      ],
    },
    {
      id: '10-days',
      title: '10 Days Package',
      icon: Sparkles,
      badge: 'Most Popular 🔥',
      badgeClass: styles.badgePurple,
      isPopularSection: true,
      description: 'Our most popular choice! 10 full days of uninterrupted video chatting with real people.',
      durationLabel: '/ 10 Days',
      tiers: [
        {
          type: 'With Ads',
          price: regionMode === 'regional' ? '7' : '15',
          tag: 'Standard Access',
          tagClass: styles.tagWithAds,
          isFeatured: false,
          features: [
            'Unlimited Video Calls (10 Days)',
            'Global Live Matching',
            'Gender & Region Filters',
            'Standard Video Quality',
            'Occasional Ads Included',
          ],
          btnText: `Choose 10-Days ($${regionMode === 'regional' ? '7' : '15'})`,
          btnClass: styles.btnStandard,
        },
        {
          type: 'Without Ads (Ad-Free)',
          price: regionMode === 'regional' ? '10' : '19',
          tag: 'Ad-Free + VIP Boost',
          tagClass: styles.tagNoAds,
          isFeatured: true,
          features: [
            'Unlimited Ultra-HD Video Calls (10 Days)',
            '100% Zero Ads Guarantee',
            'Instant 2X Speed Matching Queue',
            'Unlimited Direct Messaging',
            'VIP Gold Badge & Highlighted Avatar',
          ],
          btnText: `Go Ad-Free ($${regionMode === 'regional' ? '10' : '19'})`,
          btnClass: styles.btnPro,
        },
      ],
    },
    {
      id: '1-month',
      title: '1 Month VIP Package',
      icon: Crown,
      badge: 'Best Value 💎',
      badgeClass: styles.badgePink,
      description: '30 Days of ultimate freedom. Maximum savings with unrestricted global video calling.',
      durationLabel: '/ Month',
      tiers: [
        {
          type: 'With Ads',
          price: regionMode === 'regional' ? '12' : '25',
          tag: 'Standard VIP',
          tagClass: styles.tagWithAds,
          isFeatured: false,
          features: [
            'Unlimited Video Calls (30 Days)',
            'Global Matching Across 190+ Countries',
            'Gender & Age Preference Filters',
            'High Quality Video Stream',
            'Standard Ad Stream Included',
          ],
          btnText: `Choose 1-Month ($${regionMode === 'regional' ? '12' : '25'})`,
          btnClass: styles.btnStandard,
        },
        {
          type: 'Without Ads (Ad-Free)',
          price: regionMode === 'regional' ? '18' : '35',
          tag: 'Ultimate Platinum',
          tagClass: styles.tagNoAds,
          isFeatured: true,
          features: [
            'Unlimited 4K HD Video Calls (30 Days)',
            '100% Complete Ad-Free VIP Experience',
            'Top Priority Server & Instant Connect',
            'Unlimited Friends & Direct Messaging',
            'Exclusive Platinum VIP Crown Badge',
          ],
          btnText: `Go Platinum ($${regionMode === 'regional' ? '18' : '35'})`,
          btnClass: styles.btnPro,
        },
      ],
    },
  ];

  const handlePlanClick = (planName, price) => {
    // 1. Auth Gatekeeper: Prompt login if not signed in
    if (!user) {
      setShowLogin(true);
      return;
    }

    // 2. Onboarding Gatekeeper: Prompt onboarding if profile is incomplete
    if (!userData?.gender || !userData?.city || !userData?.name) {
      setShowOnboarding(true);
      return;
    }

    // 3. Authenticated & Onboarded: Proceed to payment process
    alert(`Processing VIP Pass: ${planName} ($${price}). Redirecting to Checkout...`);
  };

  return (
    <div className={styles.page}>
      {/* Background ambient lighting */}
      <div className={styles.ambientGlow1} />
      <div className={styles.ambientGlow2} />

      {/* Page Header */}
      <div className={styles.header}>
        <div className={styles.headerBadge}>
          <Crown size={16} />
          <span>Camverz Premium Passes</span>
        </div>
        <h1 className={styles.title}>
          Choose Your <span className="neon-text">VIP Experience</span>
        </h1>
        <p className={styles.subtitle}>
          Unlock unlimited live video calls, gender filters, fast queueing, and ad-free streaming. Select from our 3 pass durations below!
        </p>

        {/* Region & Location Selector */}
        <div className={styles.regionToggleContainer}>
          <div className={styles.detectedLocationPill}>
            <MapPin size={14} />
            <span>{detectedLocation}</span>
          </div>

          <div className={styles.regionToggleGroup}>
            <button
              className={`${styles.regionBtn} ${regionMode === 'regional' ? styles.regionBtnActive : ''}`}
              onClick={() => setRegionMode('regional')}
            >
              <span>🇮🇳 Regional Tier (IN, PK, BD, TH)</span>
            </button>
            <button
              className={`${styles.regionBtn} ${regionMode === 'international' ? styles.regionBtnActive : ''}`}
              onClick={() => setRegionMode('international')}
            >
              <Globe size={14} />
              <span>Global International Tier</span>
            </button>
          </div>
        </div>
      </div>

      {/* 3 Stacked Package Sections */}
      <div className={styles.sectionsContainer}>
        {packages.map((pkg) => {
          const IconComponent = pkg.icon;
          return (
            <div
              key={pkg.id}
              className={`${styles.packageSection} ${pkg.isPopularSection ? styles.popularSection : ''}`}
            >
              {/* Section Header */}
              <div className={styles.sectionHeader}>
                <div className={styles.sectionTitleGroup}>
                  <div className={styles.sectionIcon}>
                    <IconComponent size={24} />
                  </div>
                  <div>
                    <h2 className={styles.sectionTitle}>{pkg.title}</h2>
                    <p className={styles.sectionDesc}>{pkg.description}</p>
                  </div>
                </div>
                <span className={`${styles.sectionBadge} ${pkg.badgeClass}`}>
                  {pkg.badge}
                </span>
              </div>

              {/* Tiers Grid: With Ads vs Without Ads */}
              <div className={styles.tiersGrid}>
                {pkg.tiers.map((tier, idx) => (
                  <div
                    key={idx}
                    className={`${styles.tierCard} ${tier.isFeatured ? styles.featuredCard : ''}`}
                  >
                    <div className={styles.tierHeader}>
                      <div className={styles.tierName}>
                        {tier.isFeatured ? <Sparkles size={18} style={{ color: 'var(--neon-cyan)' }} /> : <Video size={18} />}
                        {tier.type}
                      </div>
                      <span className={`${styles.adTag} ${tier.tagClass}`}>
                        {tier.tag}
                      </span>
                    </div>

                    <div className={styles.tierPriceContainer}>
                      <span className={styles.priceCurrency}>$</span>
                      <span className={styles.priceValue}>{tier.price}</span>
                      <span className={styles.pricePeriod}>{pkg.durationLabel}</span>
                    </div>

                    <ul className={styles.featureList}>
                      {tier.features.map((feat, fIdx) => (
                        <li key={fIdx} className={styles.featureItem}>
                          {tier.isFeatured ? (
                            <Star size={16} className={styles.starIcon} />
                          ) : (
                            <Check size={16} className={styles.checkIcon} />
                          )}
                          <span>{feat}</span>
                        </li>
                      ))}
                    </ul>

                    <button
                      className={`${styles.buyBtn} ${tier.btnClass}`}
                      onClick={() => handlePlanClick(`${pkg.title} - ${tier.type}`, tier.price)}
                    >
                      <span>{tier.btnText}</span>
                      <ArrowRight size={16} />
                    </button>
                  </div>
                ))}
              </div>
            </div>
          );
        })}
      </div>

      {/* Trust & Guarantee Bar */}
      <div className={styles.guaranteeBar}>
        <div className={styles.guaranteeItem}>
          <div className={styles.guaranteeIcon}>
            <Zap size={22} />
          </div>
          <div className={styles.guaranteeTitle}>Instant Activation</div>
          <div className={styles.guaranteeDesc}>Your VIP pass activates immediately upon checkout.</div>
        </div>
        <div className={styles.guaranteeItem}>
          <div className={styles.guaranteeIcon}>
            <ShieldCheck size={22} />
          </div>
          <div className={styles.guaranteeTitle}>256-Bit SSL Encryption</div>
          <div className={styles.guaranteeDesc}>All payment transactions are 100% encrypted and safe.</div>
        </div>
        <div className={styles.guaranteeItem}>
          <div className={styles.guaranteeIcon}>
            <Lock size={22} />
          </div>
          <div className={styles.guaranteeTitle}>No Automatic Subscriptions</div>
          <div className={styles.guaranteeDesc}>Pay once for your chosen package without hidden recurring charges.</div>
        </div>
      </div>

      {/* Razorpay Compliance Terms & Conditions Notice */}
      <div className={styles.termsNotice}>
        <p>
          By clicking any package above to complete your purchase, you agree to Camverz's{' '}
          <Link href="/legal/terms-and-conditions" className={styles.termsLink}>Terms & Conditions</Link>,{' '}
          <Link href="/legal/privacy-policy" className={styles.termsLink}>Privacy Policy</Link>, and{' '}
          <Link href="/legal/refund-policy" className={styles.termsLink}>Refund Policy (Strictly No Refunds)</Link>.
        </p>
        <p className={styles.subNotice}>
          All digital pass purchases are final, instant, and strictly non-refundable. All payments are processed securely via 256-bit SSL.
        </p>
      </div>
    </div>
  );
}
