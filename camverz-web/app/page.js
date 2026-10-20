import HomePageClient from './HomePageClient';

export const metadata = {
  title: 'Camverz — Social Media, Real-Time Connection & Inclusive LGBTQ+ Live Video Network',
  description: 'Join Camverz for instant 1-on-1 video connections, social media post sharing, real-time messaging, and inclusive LGBTQ+ social networking. Safe, 100% verified, and anti-screenshot protected.',
  keywords: 'social media, live video chat, LGBTQ social network, video connection, real-time social discovery, meet new friends, gay chat, lesbian chat, straight chat, social networking app, real meet, party host',
  alternates: {
    canonical: 'https://camverz.com',
  },
  openGraph: {
    title: 'Camverz — Social Media, Real-Time Connection & Inclusive LGBTQ+ Live Video Network',
    description: 'Connect with new friends and inclusive LGBTQ+ communities worldwide through live video chat, story sharing, real-life meetups, and instant social media connection.',
    url: 'https://camverz.com',
    siteName: 'Camverz',
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Camverz — Social Media & Live Video Network',
    description: 'Connect worldwide through live video chat, real-life meetups, and inclusive LGBTQ+ social networking.',
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
        text: 'Camverz is a real-time social connection and live video chat platform where users can connect 1-on-1, share social media posts, join inclusive LGBTQ+ communities, organize real-life offline meetups (Real Meet), and host real-world house parties (Party Host).',
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
        text: 'Absolutely. Camverz provides dedicated Gay, Lesbian, Bisexual, Transgender, Queer, Non-Binary, and Straight tags, orientation match filters, and zero-tolerance anti-discrimination moderation.',
      },
    },
    {
      '@type': 'Question',
      name: 'How do Real Meet and Party Host work?',
      acceptedAnswer: {
        '@type': 'Answer',
        text: 'Real Meet allows you to post and find real-world 1-on-1 offline hangouts in your city. Party Host lets hosts publish real-life offline house parties and venue events with guest lists and announcements.',
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
  downloadUrl: 'https://play.google.com/store/apps/details?id=com.mohit.camverz',
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
