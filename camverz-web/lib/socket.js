import { io } from 'socket.io-client';

// Configurable URL: local Docker → http://localhost:3000
// Production → https://api.camverz.com
const SOCKET_URL = process.env.NEXT_PUBLIC_SOCKET_URL || 'http://localhost:3000';

let socket = null;

export function getSocket() {
  if (!socket) {
    socket = io(SOCKET_URL, {
      reconnection: true,
      reconnectionAttempts: 99999,
      reconnectionDelay: 500,
      timeout: 5000,
    });
  }
  if (!socket.connected) {
    socket.connect();
  }
  return socket;
}

export function disconnectSocket() {
  if (socket) {
    socket.disconnect();
    socket = null;
  }
}
