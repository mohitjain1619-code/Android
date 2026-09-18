import './globals.css';
import { AuthProvider } from '../lib/auth-context';
import ClientLayoutWrapper from '../components/ClientLayoutWrapper';

/*
export const metadata = {
  title: 'Camverz — Random Video Calling & Dating',
  description: 'Meet new people through random video calls. Safe, verified, and fun. Connect with strangers worldwide through live video chat.',
  keywords: 'video calling, random video chat, dating, meet strangers, live video, omegle alternative',
  openGraph: {
    title: 'Camverz — Random Video Calling & Dating',
    description: 'Meet new people through random video calls. Safe, verified, and fun.',
    type: 'website',
  },
};
*/

export const metadata = {
  title: 'Camverz — Social Media, Connection & LGBTQ+ Live Video Network',
  description: 'Join Camverz for instant 1-on-1 video connections, social media post sharing, real-time messaging, and inclusive LGBTQ+ social networking. Safe, 100% verified, and private.',
  keywords: 'social media, live video chat, LGBTQ social network, video connection, real-time social discovery, meet new friends, gay chat, lesbian chat, straight chat, social networking app',
  openGraph: {
    title: 'Camverz — Social Media, Connection & LGBTQ+ Live Video Network',
    description: 'Connect with new friends and inclusive LGBTQ+ communities worldwide through live video chat, story sharing, and instant social media connection.',
    type: 'website',
  },
  verification: {
    google: 'gDJwrTFx0eAExvQNbZnkCapSCyfkHVT9w30qabdeA1Y',
  },
};

export const viewport = {
  width: 'device-width',
  initialScale: 1,
  viewportFit: 'cover',
};

export default function RootLayout({ children }) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body suppressHydrationWarning>
        <AuthProvider>
          <ClientLayoutWrapper>{children}</ClientLayoutWrapper>
        </AuthProvider>
      </body>
    </html>
  );
}
