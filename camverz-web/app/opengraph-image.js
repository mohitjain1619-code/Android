import { ImageResponse } from 'next/og';

export const runtime = 'edge';

export const alt = 'Camverz — Social Media & Live Video Network';
export const size = {
  width: 1200,
  height: 630,
};

export const contentType = 'image/png';

export default async function Image() {
  return new ImageResponse(
    (
      <div
        style={{
          height: '100%',
          width: '100%',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          backgroundColor: '#060612',
          backgroundImage: 'radial-gradient(circle at 25% 25%, rgba(0, 229, 255, 0.15) 0%, transparent 50%), radial-gradient(circle at 75% 75%, rgba(189, 0, 255, 0.15) 0%, transparent 50%)',
          fontFamily: 'sans-serif',
          color: '#ffffff',
          padding: '60px',
        }}
      >
        {/* Badge */}
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: '8px',
            backgroundColor: 'rgba(0, 229, 255, 0.1)',
            border: '1px solid rgba(0, 229, 255, 0.3)',
            borderRadius: '20px',
            padding: '8px 20px',
            fontSize: 20,
            color: '#00E5FF',
            marginBottom: '30px',
          }}
        >
          ⚡ Live Video & Social Network
        </div>

        {/* Brand Title */}
        <div
          style={{
            fontSize: 72,
            fontWeight: 900,
            letterSpacing: '-0.02em',
            textAlign: 'center',
            marginBottom: '20px',
            background: 'linear-gradient(90deg, #00E5FF, #BD00FF)',
            backgroundClip: 'text',
            WebkitBackgroundClip: 'text',
            color: 'transparent',
          }}
        >
          CAMVERZ
        </div>

        {/* Tagline */}
        <div
          style={{
            fontSize: 32,
            fontWeight: 500,
            textAlign: 'center',
            color: 'rgba(255, 255, 255, 0.85)',
            maxWidth: '900px',
            lineHeight: 1.4,
            marginBottom: '40px',
          }}
        >
          Instant 1-on-1 Video Chat • Real Meet • Inclusive LGBTQ+ Network
        </div>

        {/* Feature Pills */}
        <div
          style={{
            display: 'flex',
            gap: '16px',
          }}
        >
          <div
            style={{
              backgroundColor: 'rgba(255, 255, 255, 0.08)',
              border: '1px solid rgba(255, 255, 255, 0.15)',
              borderRadius: '12px',
              padding: '12px 24px',
              fontSize: 20,
              color: '#ffffff',
            }}
          >
            🔒 Anti-Screenshot Privacy
          </div>
          <div
            style={{
              backgroundColor: 'rgba(255, 255, 255, 0.08)',
              border: '1px solid rgba(255, 255, 255, 0.15)',
              borderRadius: '12px',
              padding: '12px 24px',
              fontSize: 20,
              color: '#ffffff',
            }}
          >
            🛡️ Pose Verified
          </div>
          <div
            style={{
              backgroundColor: 'rgba(255, 255, 255, 0.08)',
              border: '1px solid rgba(255, 255, 255, 0.15)',
              borderRadius: '12px',
              padding: '12px 24px',
              fontSize: 20,
              color: '#ffffff',
            }}
          >
            🚀 100% Free
          </div>
        </div>
      </div>
    ),
    {
      ...size,
    }
  );
}
