import HomePageClient from './HomePageClient';

export const metadata = {
  title: 'Camverz — Social Media, Real-Time Connection & Inclusive Live Video Network',
  description: 'Join Camverz for instant 1-on-1 video connections, social media post sharing, real-time messaging, and inclusive community networking. Safe, 100% verified, and anti-screenshot protected.',
  keywords: 'social media, live video connection, inclusive social network, video discovery, real-time social networking, meet new friends, diverse community, real meet, party host',
  alternates: {
    canonical: 'https://camverz.com',
  },
  openGraph: {
    title: 'Camverz — Social Media, Real-Time Connection & Inclusive Live Video Network',
    description: 'Connect with new friends and inclusive LGBTQ+ communities worldwide through live video connections, story sharing, real-life meetups, and instant social media networking.',
    url: 'https://camverz.com',
    siteName: 'Camverz',
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Camverz — Social Media & Live Video Network',
    description: 'Connect worldwide through live video matching, real-life meetups, and inclusive community networking.',
  },
};

const homeFaqSchema = {
  '@context': 'https://schema.org',
  '@type': 'FAQPage',
  mainEntity: [
    {
      '@type': 'Question',
      name: 'What is Camverz?',
      acceptedAnswer: {
        '@type': 'Answer',
        text: 'Camverz is a real-time social connection and live video discovery platform where users can connect 1-on-1, share social media posts, join inclusive communities, organize local city hangouts (Real Meet), and host community events (Party Host).',
      },
    },
    {
      '@type': 'Question',
      name: 'Is Camverz safe and private?',
      acceptedAnswer: {
        '@type': 'Answer',
        text: 'Yes! Camverz features hardware-level Screenshot & Screen Recording Protection, active pose selfie verification, instant AI moderation, and permanent device hardware bans for bad actors.',
      },
    },
    {
      '@type': 'Question',
      name: 'Is Camverz inclusive of LGBTQ+ communities?',
      acceptedAnswer: {
        '@type': 'Answer',
        text: 'Absolutely. Camverz provides dedicated community orientation tags, inclusive match preferences, and zero-tolerance anti-discrimination moderation.',
      },
    },
    {
      '@type': 'Question',
      name: 'How do Real Meet and Party Host work?',
      acceptedAnswer: {
        '@type': 'Answer',
        text: 'Real Meet allows you to discover and post local city hangouts. Party Host lets hosts publish real-life offline gatherings and venue events with guest lists and announcements.',
      },
    },
  ],
};

const appSchema = {
  '@context': 'https://schema.org',
  '@type': 'SoftwareApplication',
  name: 'Camverz',
  operatingSystem: 'ANDROID, WEB',
  applicationCategory: 'SocialNetworkingApplication',
  offers: {
    '@type': 'Offer',
    price: '0',
    priceCurrency: 'USD',
  },
  description: 'Instant random video matching, real-time social post sharing, inclusive LGBTQ+ communities, and anti-screenshot privacy protection.',
  downloadUrl: 'https://play.google.com/store/apps/details?id=com.mohitt.camverz',
};

export default function Page() {
  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(homeFaqSchema) }}
      />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(appSchema) }}
      />
      <HomePageClient />
    </>
  );
}
