import './globals.css';
import { AuthProvider } from '../lib/auth-context';
import ClientLayoutWrapper from '../components/ClientLayoutWrapper';

export const metadata = {
  metadataBase: new URL('https://camverz.com'),
  title: {
    default: 'Camverz — Social Media & Inclusive Live Video Connection Network',
    template: '%s | Camverz',
  },
  description: 'Join Camverz for instant 1-on-1 video connections, social media post sharing, Real Meet local city hangouts, and verified community events. Safe, 100% verified, and privacy protected.',
  keywords: [
    'social media',
    'live video connection',
    'live video chat',
    'video discovery',
    'real-time social discovery',
    'meet new friends',
    'LGBTQ+ community',
    'inclusive social network',
    'diverse social connections',
    'real-time video matching',
    'social networking app',
    'verified video network',
    'safe video chat app',
    'real meet hangouts',
    'party host events',
    'local offline meetups',
    'city meetup app',
    'community events',
    'authentic friendships',
    'video connection platform',
    'global social network',
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
    title: 'Camverz — Social Media & Inclusive Live Video Connection Network',
    description: 'Connect with new friends and inclusive LGBTQ+ communities worldwide through live video connections, story sharing, real-life meetups, and instant social networking.',
    url: 'https://camverz.com',
    siteName: 'Camverz',
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Camverz — Social Media & Live Video Network',
    description: 'Connect with verified members worldwide through live video matching, real-world meetups, and inclusive community networks.',
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
    'https://play.google.com/store/apps/details?id=com.mohitt.camverz'
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
