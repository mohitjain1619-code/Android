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
  title: 'Camverz — Virtual Networking & Global SaaS Community',
  description: 'Connect with professionals, creators, and peers through virtual networking, live video sessions, and interactive social communities.',
  keywords: 'virtual networking, live video community, social saas platform, language exchange, digital networking',
  openGraph: {
    title: 'Camverz — Virtual Networking & Global SaaS Community',
    description: 'Connect with professionals and creators through virtual networking and live video sessions.',
    type: 'website',
  },
  verification: {
    google: 'gDJwrTFx0eAExvQNbZnkCapSCyfkHVT9w30qabdeA1Y',
  },
};

export const viewport = {
  width: 950,
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
