'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { useAuth } from '../../lib/auth-context';
import { PRICING_MATRIX, getPaymentUrl, getSubscriptionStatus, REGIONAL_COUNTRIES } from '../../lib/subscription';
import { Check, Zap, Sparkles, Crown, ShieldCheck, Lock, ArrowRight, Star, Video, MapPin, Globe } from 'lucide-react';
import styles from './page.module.css';

export default function PricingPage() {
  const { user, userData, setShowLogin, setShowOnboarding } = useAuth();
  const [regionMode, setRegionMode] = useState('international'); // 'india' | 'regional' | 'international'
  const [detectedLocation, setDetectedLocation] = useState('Detecting location...');

  const subStatus = getSubscriptionStatus(userData);

  // Auto-detect user country from userData or IP geolocation
  useEffect(() => {
    let userCountry = '';
    if (userData?.city) {
      userCountry = userData.city.split(',').pop().trim().toLowerCase();
    } else if (userData?.country) {
      userCountry = userData.country.trim().toLowerCase();
    }

    if (userCountry && (userCountry.includes('india') || userCountry === 'in')) {
      setRegionMode('india');
      setDetectedLocation('Location: India (INR ₹ Pricing) 🇮🇳');
    } else if (userCountry && REGIONAL_COUNTRIES.some(c => userCountry.includes(c))) {
      setRegionMode('regional');
      setDetectedLocation(`Location: ${userData.city || userCountry.toUpperCase()} (Regional Tier 🏷️)`);
    } else if (userCountry) {
      setRegionMode('international');
      setDetectedLocation(`Location: ${userData.city || userCountry.toUpperCase()} (International Tier 🌐)`);
    } else {
      // Fallback: IP Geolocation lookup
      fetch('https://ipapi.co/json/')
        .then(res => res.json())
        .then(data => {
          const countryName = (data.country_name || '').toLowerCase();
          const countryCode = (data.country_code || '').toLowerCase();
          if (countryCode === 'in' || countryName.includes('india')) {
            setRegionMode('india');
            setDetectedLocation('Location: India (INR ₹ Pricing) 🇮🇳');
          } else if (REGIONAL_COUNTRIES.some(c => countryName.includes(c) || countryCode.includes(c))) {
            setRegionMode('regional');
            setDetectedLocation(`Location: ${data.country_name || 'Regional'} (Regional Tier 🏷️)`);
          } else {
            setRegionMode('international');
            setDetectedLocation(`Location: ${data.country_name || 'Global'} (International Tier 🌐)`);
          }
        })
        .catch(() => {
          setDetectedLocation('Global Location Applied');
        });
    }
  }, [userData]);

  const currencySymbol = regionMode === 'india' ? '₹' : '$';

  const packages = [
    {
      id: '1-day',
      title: '1 Day Pass',
      icon: Zap,
      badge: 'Quick Trial',
      badgeClass: styles.badgeCyan,
      description: 'Full 24-hour access. Perfect for quick connections and testing premium filters.',
      durationLabel: '/ 24 Hours',
      tiers: [
        {
          type: 'With Ads',
          isAdFree: false,
          tag: 'Standard Access',
          tagClass: styles.tagWithAds,
          isFeatured: false,
          features: [
            'Unlimited Video Calls (24 Hours)',
            'Global Live Matching',
            'Gender Preference Filter',
            'Standard Video Quality',
            'Interstitials & Ads Included',
          ],
          btnClass: styles.btnStandard,
        },
        {
          type: 'Without Ads (Ad-Free)',
          isAdFree: true,
          tag: 'Zero Ads + VIP',
          tagClass: styles.tagNoAds,
          isFeatured: true,
          features: [
            'Unlimited HD Video Calls (24 Hours)',
            '100% Zero Ads Guarantee',
            'Priority Fast Match Queue',
            'Full Gender & Country Filters',
            'VIP Badge on Profile',
          ],
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
          isAdFree: false,
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
          btnClass: styles.btnStandard,
        },
        {
          type: 'Without Ads (Ad-Free)',
          isAdFree: true,
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
          isAdFree: false,
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
          btnClass: styles.btnStandard,
        },
        {
          type: 'Without Ads (Ad-Free)',
          isAdFree: true,
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
          btnClass: styles.btnPro,
        },
      ],
    },
  ];

  const handlePlanClick = (pkgId, isAdFree, planName, price) => {
    // 1. Auth Gatekeeper
    if (!user) {
      setShowLogin(true);
      return;
    }

    // 2. Onboarding Gatekeeper
    if (!userData?.gender || !userData?.city || !userData?.name) {
      setShowOnboarding(true);
      return;
    }

    // 3. Get exact payment URL
    const link = getPaymentUrl(pkgId, isAdFree, regionMode, user?.email || '', userData?.name || '');
    if (link) {
      window.location.href = link;
    } else {
      alert(`Initiating purchase for ${planName} (${currencySymbol}${price})...`);
    }
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
          <span>Camverz VIP Passes</span>
        </div>
        <h1 className={styles.title}>
          Choose Your <span className="neon-text">VIP Experience</span>
        </h1>
        <p className={styles.subtitle}>
          Unlock unlimited live video calls, gender filters, fast queueing, and ad-free streaming. Select your preferred pass duration below!
        </p>

        {/* Active Plan Banner (if user already has an active subscription) */}
        {subStatus.hasActivePlan && (
          <div className={styles.activePlanBanner} style={{ borderColor: subStatus.badgeColor }}>
            <div className={styles.activePlanHeader}>
              <Crown size={20} style={{ color: subStatus.badgeColor }} />
              <span className={styles.activePlanTitle}>Current Active Plan: <strong>{subStatus.planName}</strong></span>
            </div>
            <div className={styles.activePlanMeta}>
              <span className={styles.activeBadge} style={{ background: subStatus.badgeColor }}>
                {subStatus.badgeText}
              </span>
              <span className={styles.activeRemaining}>⏱️ {subStatus.remainingText}</span>
            </div>
          </div>
        )}

        {/* Region & Location Selector */}
        <div className={styles.regionToggleContainer}>
          <div className={styles.detectedLocationPill}>
            <MapPin size={14} />
            <span>{detectedLocation}</span>
          </div>

          {/* REQUIREMENT: Show region switcher ONLY when NOT logged in.
              When logged in, hide region toggle buttons and show locked region pricing. */}
          {!user && (
            <div className={styles.regionToggleGroup}>
              <button
                className={`${styles.regionBtn} ${regionMode === 'india' ? styles.regionBtnActive : ''}`}
                onClick={() => setRegionMode('india')}
              >
                <span>🇮🇳 India (₹ INR)</span>
              </button>
              <button
                className={`${styles.regionBtn} ${regionMode === 'regional' ? styles.regionBtnActive : ''}`}
                onClick={() => setRegionMode('regional')}
              >
                <span>🌏 Regional (USD)</span>
              </button>
              <button
                className={`${styles.regionBtn} ${regionMode === 'international' ? styles.regionBtnActive : ''}`}
                onClick={() => setRegionMode('international')}
              >
                <Globe size={14} />
                <span>International (USD)</span>
              </button>
            </div>
          )}
        </div>
      </div>

      {/* 3 Stacked Package Sections */}
      <div className={styles.sectionsContainer}>
        {packages.map((pkg) => {
          const IconComponent = pkg.icon;
          const pkgData = PRICING_MATRIX[pkg.id];
          const regionMatrix = pkgData[regionMode] || pkgData.international;

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
                {pkg.tiers.map((tier, idx) => {
                  const tierData = tier.isAdFree ? regionMatrix.no_ads : regionMatrix.with_ads;
                  const priceVal = tierData.price;
                  const displayPrice = tierData.displayPrice;

                  return (
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
                        <span className={styles.priceCurrency}>{currencySymbol}</span>
                        <span className={styles.priceValue}>{priceVal}</span>
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
                        onClick={() => handlePlanClick(pkg.id, tier.isAdFree, `${pkg.title} (${tier.type})`, priceVal)}
                      >
                        <span>Choose Plan ({displayPrice})</span>
                        <ArrowRight size={16} />
                      </button>
                    </div>
                  );
                })}
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
