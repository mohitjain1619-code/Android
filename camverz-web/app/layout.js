import './globals.css';
import { AuthProvider } from '../lib/auth-context';
import ClientLayoutWrapper from '../components/ClientLayoutWrapper';

export const metadata = {
  metadataBase: new URL('https://camverz.com'),
  title: {
    default: 'Camverz — Social Media, Connection & Inclusive LGBTQ+ Live Video Network',
    template: '%s | Camverz',
  },
  description: 'Join Camverz for instant 1-on-1 video connections, social media post sharing, Real Meet local city hangouts (meet local lesbians, gay men, transgender members, girls & boys), and Party Host real-world house parties with guest list approvals. Safe, 100% verified, anti-screenshot protected.',
  keywords: [
    'social media',
    'live video call',
    'live video chat',
    'video connection',
    'real-time social discovery',
    'meet new friends',
    'gay chat',
    'lesbian chat',
    'straight chat',
    'gay video chat',
    'lesbian video chat',
    'straight video chat',
    'gay video call',
    'lesbian video call',
    'straight video call',
    'gay social network',
    'lesbian social network',
    'straight social network',
    'video call with girls',
    'video call with boys',
    'talk to girls',
    'talk to boys',
    'meet girls online',
    'meet boys online',
    'meet singles online',
    'video connection app',
    'LGBTQ social network',
    'social networking app',
    'verified video call app',
    'real-time video chat app',
    'chamet alternative',
    'azar alternative',
    'holla alternative',
    'real meet',
    'party host',
    'meet local lesbians',
    'meet local gay men',
    'meet local transgender friends',
    'meet local girls',
    'meet local boys',
    'local offline meetups',
    'city meetup app',
    'host house party',
    'party host app',
    'lgbtq party host',
    'lesbian house party',
    'gay house party',
    'queer house party',
    'verified female party',
    'local party events',
    'guest list party app',
    'video call app',
    'video call platform',
    'video call website',
    'video call online',
    'video call now',
  ],
  authors: [{ name: 'Camverz Team' }],
  creator: 'Camverz',
  publisher: 'Camverz',
  formatDetection: {
    email: false,
    address: false,
    telephone: false,
  },
  alternates: {
    canonical: '/',
  },
  openGraph: {
    title: 'Camverz — Social Media, Connection & Inclusive LGBTQ+ Live Video Network',
    description: 'Connect with new friends and inclusive LGBTQ+ communities worldwide through live video chat, story sharing, real-life meetups, and instant social media connection.',
    url: 'https://camverz.com',
    siteName: 'Camverz',
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Camverz — Social Media & Live Video Network',
    description: 'Connect with verified members worldwide through live video chat, real-world meetups, and inclusive LGBTQ+ social networks.',
    creator: '@camverz',
  },
  robots: {
    index: true,
    follow: true,
    googleBot: {
      index: true,
      follow: true,
      'max-video-preview': -1,
      'max-image-preview': 'large',
      'max-snippet': -1,
    },
  },
  verification: {
    google: '7cDPG_CWZLJes3mfh6UepnJLcblEuQ1JFUuL0Lw9Zz0',
  },
};

export const viewport = {
  width: '1024',
  initialScale: 0.45,
  minimumScale: 0.25,
  maximumScale: 3,
  userScalable: true,
  viewportFit: 'cover',
  themeColor: '#0f0f17',
};

const organizationSchema = {
  '@context': 'https://schema.org',
  '@type': 'Organization',
  name: 'Camverz',
  url: 'https://camverz.com',
  logo: 'https://camverz.com/favicon.ico',
  sameAs: [
    'https://play.google.com/store/apps/details?id=com.mohit.camverz'
  ],
};

const websiteSchema = {
  '@context': 'https://schema.org',
  '@type': 'WebSite',
  name: 'Camverz',
  url: 'https://camverz.com',
  potentialAction: {
    '@type': 'SearchAction',
    target: 'https://camverz.com/blog?q={search_term_string}',
    'query-input': 'required name=search_term_string',
  },
};

export default function RootLayout({ children }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <head>
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(organizationSchema) }}
        />
        <script
          type="application/ld+json"
          dangerouslySetInnerHTML={{ __html: JSON.stringify(websiteSchema) }}
        />
      </head>
      <body suppressHydrationWarning>
        <AuthProvider>
          <ClientLayoutWrapper>{children}</ClientLayoutWrapper>
        </AuthProvider>
      </body>
    </html>
  );
}
