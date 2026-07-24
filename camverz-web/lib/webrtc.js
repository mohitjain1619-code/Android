/**
 * WebRTC Manager — Translated from CallActivity.java
 * Handles peer connections, ICE servers, SDP negotiation
 */

const ICE_SERVER_URL = 'https://camverzbackend-production.up.railway.app/webrtc/ice';

export class WebRTCManager {
  constructor({ socket, myUid, peerId, onRemoteStream, onConnectionChange, onDisconnect }) {
    this.socket = socket;
    this.myUid = myUid;
    this.peerId = peerId;
    this.onRemoteStream = onRemoteStream;
    this.onConnectionChange = onConnectionChange;
    this.onDisconnect = onDisconnect;

    this.peerConnection = null;
    this.localStream = null;
    this.remoteStream = null;
    this.roomName = myUid < peerId ? `${myUid}_${peerId}` : `${peerId}_${myUid}`;
    this.isInitiator = myUid < peerId;
    this.callEnded = false;
    this.turnFallbackAttempted = false;
  }

  async startLocalStream(videoEnabled = true) {
    try {
      this.localStream = await navigator.mediaDevices.getUserMedia({
        video: videoEnabled ? { facingMode: 'user', width: { ideal: 720 }, height: { ideal: 1280 } } : false,
        audio: true,
      });
      return this.localStream;
    } catch (err) {
      console.error('Failed to get media:', err);
      throw err;
    }
  }

  async initPeerConnection() {
    const iceServers = [{ urls: 'stun:stun.l.google.com:19302' }, { urls: 'stun:stun1.l.google.com:19302' }];
    const config = { iceServers, sdpSemantics: 'unified-plan' };

    this.peerConnection = new RTCPeerConnection(config);

    if (this.localStream) {
      this.localStream.getTracks().forEach(track => {
        this.peerConnection.addTrack(track, this.localStream);
      });
    }

    this.peerConnection.onicecandidate = (event) => {
      if (event.candidate) {
        this.socket.emit('send-ice', {
          to: this.peerId,
          candidate: event.candidate.candidate,
          sdpMid: event.candidate.sdpMid,
          sdpMLineIndex: event.candidate.sdpMLineIndex,
          room: this.roomName,
        });
      }
    };

    this.peerConnection.ontrack = (event) => {
      if (!this.remoteStream) {
        this.remoteStream = new MediaStream();
        this.onRemoteStream?.(this.remoteStream);
      }
      this.remoteStream.addTrack(event.track);
    };

    this.peerConnection.onconnectionstatechange = () => {
      const state = this.peerConnection?.connectionState;
      this.onConnectionChange?.(state);
      if (state === 'disconnected' || state === 'closed' || state === 'failed') {
        this.disconnect();
      }
    };

    this.peerConnection.oniceconnectionstatechange = () => {
      const state = this.peerConnection?.iceConnectionState;
      if (state === 'failed' && !this.turnFallbackAttempted) {
        this.turnFallbackAttempted = true;
        this.fetchTurnAndReconnect();
      }
    };
  }

  async fetchTurnAndReconnect() {
    try {
      const res = await fetch(`${ICE_SERVER_URL}?useTurn=true`);
      const data = await res.json();
      if (data.iceServers) {
        this.peerConnection.setConfiguration({ iceServers: data.iceServers });
        this.peerConnection.restartIce();
      }
    } catch (e) {
      console.error('TURN fallback failed:', e);
    }
  }

  setupSocketListeners() {
    this.socket.on('peer-ready', () => {
      if (this.isInitiator) this.createOffer();
    });

    this.socket.on('receive-offer', async (data) => {
      if (!this.peerConnection) return;
      const sdp = new RTCSessionDescription({ type: 'offer', sdp: data.offer });
      await this.peerConnection.setRemoteDescription(sdp);
      this.createAnswer();
    });

    this.socket.on('receive-answer', async (data) => {
      if (!this.peerConnection) return;
      const sdp = new RTCSessionDescription({ type: 'answer', sdp: data.answer });
      await this.peerConnection.setRemoteDescription(sdp);
    });

    this.socket.on('receive-ice', async (data) => {
      if (!this.peerConnection) return;
      try {
        await this.peerConnection.addIceCandidate(new RTCIceCandidate({
          candidate: data.candidate,
          sdpMid: data.sdpMid,
          sdpMLineIndex: data.sdpMLineIndex,
        }));
      } catch (e) {
        console.error('Error adding ICE:', e);
      }
    });
  }

  async createOffer() {
    if (!this.peerConnection) return;
    const offer = await this.peerConnection.createOffer();
    await this.peerConnection.setLocalDescription(offer);
    this.socket.emit('send-offer', {
      to: this.peerId,
      offer: offer.sdp,
      room: this.roomName,
    });
  }

  async createAnswer() {
    if (!this.peerConnection) return;
    const answer = await this.peerConnection.createAnswer();
    await this.peerConnection.setLocalDescription(answer);
    this.socket.emit('send-answer', {
      to: this.peerId,
      answer: answer.sdp,
      room: this.roomName,
    });
  }

  joinRoom() {
    this.socket.emit('join-call-room', { room: this.roomName, uid: this.myUid });
  }

  toggleMute() {
    if (this.localStream) {
      const audioTrack = this.localStream.getAudioTracks()[0];
      if (audioTrack) {
        audioTrack.enabled = !audioTrack.enabled;
        return !audioTrack.enabled;
      }
    }
    return false;
  }

  toggleVideo() {
    if (this.localStream) {
      const videoTrack = this.localStream.getVideoTracks()[0];
      if (videoTrack) {
        videoTrack.enabled = !videoTrack.enabled;
        return !videoTrack.enabled;
      }
    }
    return false;
  }

  async switchCamera() {
    if (!this.localStream) return;
    const videoTrack = this.localStream.getVideoTracks()[0];
    if (!videoTrack) return;

    const currentFacing = videoTrack.getSettings().facingMode;
    const newFacing = currentFacing === 'user' ? 'environment' : 'user';

    try {
      const newStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: newFacing, width: { ideal: 720 }, height: { ideal: 1280 } },
      });
      const newVideoTrack = newStream.getVideoTracks()[0];
      const sender = this.peerConnection?.getSenders().find(s => s.track?.kind === 'video');
      if (sender) {
        await sender.replaceTrack(newVideoTrack);
      }
      videoTrack.stop();
      this.localStream.removeTrack(videoTrack);
      this.localStream.addTrack(newVideoTrack);
    } catch (e) {
      console.error('Switch camera failed:', e);
    }
  }

  disconnect() {
    if (this.callEnded) return;
    this.callEnded = true;

    this.socket.emit('leave-call-room', { room: this.roomName, uid: this.myUid });

    this.socket.off('peer-ready');
    this.socket.off('receive-offer');
    this.socket.off('receive-answer');
    this.socket.off('receive-ice');

    if (this.localStream) {
      this.localStream.getTracks().forEach(t => t.stop());
      this.localStream = null;
    }

    if (this.peerConnection) {
      this.peerConnection.close();
      this.peerConnection = null;
    }

    this.onDisconnect?.();
  }
}
