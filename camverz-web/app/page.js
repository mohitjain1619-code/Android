import HomePageClient from './HomePageClient';

export const metadata = {
  title: 'Camverz — 18+ Random Video Chat, Dating, RealMeet & Party Host',
  description: 'Join Camverz for instant 1-on-1 live random video chat, in-person RealMeet dating, wild house party hosting, and open-minded fantasy meetups. 100% verified & anti-screenshot protected.',
  keywords: 'random video call, live video chat, adult dating app, real meet, party host, fantasy dating, stranger video call, omegle alternative, ometv alternative, video call with girls, meet singles online, house party app',
  alternates: {
    canonical: 'https://camverz.com',
  },
  openGraph: {
    title: 'Camverz — 18+ Random Video Chat, Dating, RealMeet & Party Host',
    description: 'Connect with verified girls & boys worldwide through live 1-on-1 random video calls, real-world dating meetups, and wild house parties.',
    url: 'https://camverz.com',
    siteName: 'Camverz',
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Camverz — 18+ Random Video Chat & RealMeet Party Host',
    description: 'Connect worldwide through live random video chat, real-life dating meetups, and wild party hosting.',
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
        text: 'Camverz is an 18+ live random video chat and real-world dating platform where users connect 1-on-1 via live video, organize real-life offline meetups (RealMeet), host private house parties (Party Host), and explore open fantasies.',
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
        text: 'Yes. Camverz provides dedicated Gay, Lesbian, Bisexual, and Straight matching filters with 100% verified authentic profiles.',
      },
    },
    {
      '@type': 'Question',
      name: 'How do Real Meet and Party Host work?',
      acceptedAnswer: {
        '@type': 'Answer',
        text: 'Real Meet allows you to discover and post 1-on-1 real-world dates and hangouts in your city. Party Host lets hosts publish real-life offline house parties and nightlife events with guest list approvals. Fantasy lets members connect discreetly for open desires.',
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
