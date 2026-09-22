export const metadata = {
  title: 'Real Meet & Party Host — Local Lesbians, Gay Men, Transgender, Girls & Boys City Meetups | Camverz',
  description: 'In-depth local city meetups and real-world house party platform. Connect with local lesbians, gay men, transgender & queer members, girls, and boys in your city for coffee hangouts, activity dates, house parties, and verified venue events.',
  keywords: [
    // Real Meet - Lesbians
    'meet local lesbians',
    'lesbian city meetups',
    'lesbian coffee hangouts',
    'lesbian friend finder',
    'lesbian local network',
    // Real Meet - Gay Men
    'meet local gay men',
    'gay city meetups',
    'gay local hangouts',
    'gay friend finder',
    'gay offline connection',
    // Real Meet - Transgender & Queer
    'meet local transgender friends',
    'queer city meetups',
    'transgender local network',
    'non binary local hangouts',
    'queer friend finder',
    // Real Meet - Girls & Boys
    'meet local girls',
    'meet local boys',
    'city meetup app',
    'local coffee buddy',
    'workout partner finder',
    'meet singles in city',
    '1 on 1 offline hangouts',
    // Party Host - Lesbians & Queer Women
    'lesbian house party',
    'lesbian party host',
    'queer women party',
    'lesbian venue events',
    // Party Host - Gay Men
    'gay house party',
    'gay party host',
    'queer house party',
    'gay venue events',
    // Party Host - Transgender & LGBTQ+
    'lgbtq party host',
    'transgender inclusive party',
    'pride house party',
    'queer event host',
    // Party Host - Girls, Boys & Verified House Parties
    'host house party',
    'verified female party',
    'house party app',
    'city party host',
    'party guest list app',
    'host real party',
    'local party events',
    'clubbing venue party',
  ],
  alternates: {
    canonical: 'https://camverz.com/realmeet',
  },
  openGraph: {
    title: 'Real Meet & Party Host — Local City Meetups & House Parties for Lesbians, Gay, Trans, Girls & Boys',
    description: 'Discover and post real-world 1-on-1 meetups and house parties in your city for local lesbians, gay men, transgender members, girls, and boys.',
    url: 'https://camverz.com/realmeet',
    siteName: 'Camverz',
    locale: 'en_US',
    type: 'website',
  },
  twitter: {
    card: 'summary_large_image',
    title: 'Real Meet & Party Host | In-Depth City Meetups & House Parties',
    description: 'Connect locally with lesbians, gay men, trans members, girls, and boys for city meetups and real house parties.',
  },
};

// In-Depth Structured JSON-LD FAQ Schemas covering every specific category
const realMeetDetailedFaqSchema = {
  '@context': 'https://schema.org',
  '@type': 'FAQPage',
  mainEntity: [
    // Real Meet - Lesbians
    {
      '@type': 'Question',
      name: 'How does Real Meet work for local lesbians in your city?',
      acceptedAnswer: {
        '@type': 'Answer',
        text: 'Real Meet on Camverz allows local lesbians to post and discover 1-on-1 coffee hangouts, activity dates, and local friend meetups in their city with verified profiles.',
      },
    },
    // Real Meet - Gay Men
    {
      '@type': 'Question',
      name: 'How does Real Meet work for local gay men?',
      acceptedAnswer: {
        '@type': 'Answer',
        text: 'Gay men can use Real Meet to connect with local gay members nearby for workout sessions, city exploration, coffee hangouts, and authentic 1-on-1 real-world meetups.',
      },
    },
    // Real Meet - Transgender & Queer Community
    {
      '@type': 'Question',
      name: 'How does Real Meet support local transgender & queer community members?',
      acceptedAnswer: {
        '@type': 'Answer',
        text: 'Camverz provides inclusive Real Meet tags for transgender, non-binary, and queer members to find supportive local friends, safe offline hangouts, and community meetups in their city.',
      },
    },
    // Real Meet - Girls & Boys
    {
      '@type': 'Question',
      name: 'How does Real Meet work for local girls and boys?',
      acceptedAnswer: {
        '@type': 'Answer',
        text: 'Real Meet enables girls and boys to meet singles in their city, post local hangout invites, find coffee buddies, and build authentic real-life connections.',
      },
    },
    // Party Host - Lesbian & Queer Women Parties
    {
      '@type': 'Question',
      name: 'How does Party Host work for lesbian & queer women house parties?',
      acceptedAnswer: {
        '@type': 'Answer',
        text: 'Hosts can publish lesbian and queer women house parties or venue gatherings on Party Host, set guest limits, review attendee profiles, and send live party announcements.',
      },
    },
    // Party Host - Gay House Parties
    {
      '@type': 'Question',
      name: 'How does Party Host work for gay house parties & social events?',
      acceptedAnswer: {
        '@type': 'Answer',
        text: 'Gay hosts can create real-world party listings, control public or private guest list approvals, coordinate venue locations, and invite local gay members to offline house parties.',
      },
    },
    // Party Host - Transgender & Pride Inclusive Parties
    {
      '@type': 'Question',
      name: 'Does Party Host support transgender & LGBTQ+ pride house parties?',
      acceptedAnswer: {
        '@type': 'Answer',
        text: 'Yes! Party Host includes dedicated LGBTQ+ and transgender inclusive filters, allowing hosts to create safe, welcoming pride house parties and venue celebrations.',
      },
    },
    // Party Host - Verified Female & Social House Parties
    {
      '@type': 'Question',
      name: 'How do verified female & social house parties work on Party Host?',
      acceptedAnswer: {
        '@type': 'Answer',
        text: 'Party Host lets hosts organize house parties with guest filters (such as Verified Females Only or Open Social House Parties), manage join requests, and broadcast venue updates before the party starts.',
      },
    },
  ],
};

const realMeetFeaturesSchema = {
  '@context': 'https://schema.org',
  '@type': 'ItemList',
  name: 'Camverz Real Meet & Party Host Categories',
  itemListElement: [
    {
      '@type': 'ListItem',
      position: 1,
      name: 'Real Meet — Local Lesbians City Hangouts',
    },
    {
      '@type': 'ListItem',
      position: 2,
      name: 'Real Meet — Local Gay Men City Meetups',
    },
    {
      '@type': 'ListItem',
      position: 3,
      name: 'Real Meet — Transgender & Queer Community Connections',
    },
    {
      '@type': 'ListItem',
      position: 4,
      name: 'Real Meet — Local Girls & Boys Singles Meetups',
    },
    {
      '@type': 'ListItem',
      position: 5,
      name: 'Party Host — Lesbian & Queer Women House Parties',
    },
    {
      '@type': 'ListItem',
      position: 6,
      name: 'Party Host — Gay House Parties & Venue Gatherings',
    },
    {
      '@type': 'ListItem',
      position: 7,
      name: 'Party Host — Transgender Inclusive Pride Celebrations',
    },
    {
      '@type': 'ListItem',
      position: 8,
      name: 'Party Host — Verified Female & Open House Parties',
    },
  ],
};

export default function RealMeetLayout({ children }) {
  return (
    <>
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(realMeetDetailedFaqSchema) }}
      />
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(realMeetFeaturesSchema) }}
      />
      {children}
    </>
  );
}
