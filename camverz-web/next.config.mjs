/** @type {import('next').NextConfig} */
const nextConfig = {
  /* config options here */
  allowedDevOrigins: [
    '192.168.0.104', '192.168.0.104:3000', '192.168.0.104:3001', 
    '192.168.0.102', '192.168.0.102:3000', '192.168.0.102:3001', 
    'localhost:3000'
  ],
};

export default nextConfig;
