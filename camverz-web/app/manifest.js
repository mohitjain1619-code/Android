export default function manifest() {
  return {
    name: 'Camverz Social & Live Video Network',
    short_name: 'Camverz',
    description: 'Instant 1-on-1 video chat, social post sharing, real-life meetups, and inclusive LGBTQ+ connections.',
    start_url: '/',
    display: 'standalone',
    background_color: '#060612',
    theme_color: '#0f0f17',
    icons: [
      {
        src: '/favicon.ico',
        sizes: 'any',
        type: 'image/x-icon',
      },
    ],
  };
}
