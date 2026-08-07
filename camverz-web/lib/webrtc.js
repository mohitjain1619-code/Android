/**
 * WebRTC Manager — Translated from CallActivity.java
 * Handles peer connections, ICE servers, SDP negotiation
 */

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
  }

  async startLocalStream(videoEnabled = true) {
    try {
      this.localStream = await navigator.mediaDevices.getUserMedia({
        video: videoEnabled ? { facingMode: 'user', width: { ideal: 720 }, height: { ideal: 1280 } } : false,
        audio: {
          echoCancellation: true,
          noiseSuppression: true,
          autoGainControl: true,
        },
      });
      return this.localStream;
    } catch (err) {
      console.error('Failed to get media:', err);
      throw err;
    }
  }

  async initPeerConnection() {
    const iceServers = [
      { urls: 'stun:stun.l.google.com:19302' },
      { urls: 'stun:stun1.l.google.com:19302' },
      { urls: 'stun:stun2.l.google.com:19302' },
      { urls: 'stun:stun3.l.google.com:19302' },
      { urls: 'stun:global.stun.twilio.com:3478' },
      {
        urls: [
          'turn:openrelay.metered.ca:80',
          'turn:openrelay.metered.ca:443',
          'turn:openrelay.metered.ca:443?transport=tcp'
        ],
        username: 'openrelayproject',
        credential: 'openrelayproject'
      }
    ];
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
      if (event.streams && event.streams[0]) {
        this.remoteStream = event.streams[0];
      } else {
        if (!this.remoteStream) {
          this.remoteStream = new MediaStream();
        }
        this.remoteStream.addTrack(event.track);
      }
      this.onRemoteStream?.(this.remoteStream);
    };

    this.peerConnection.onconnectionstatechange = () => {
      const state = this.peerConnection?.connectionState;
      this.onConnectionChange?.(state);
      if (state === 'disconnected' || state === 'closed' || state === 'failed') {
        this.disconnect();
      }
    };
  }

  setupSocketListeners() {
    this.socket.off('peer-ready');
    this.socket.off('receive-offer');
    this.socket.off('receive-answer');
    this.socket.off('receive-ice');
    this.socket.off('peer-disconnected');

    this.socket.on('peer-ready', () => {
      if (this.isInitiator) this.createOffer();
    });

    this.socket.on('receive-offer', async (data) => {
      if (!this.peerConnection) return;
      try {
        const sdp = new RTCSessionDescription({ type: 'offer', sdp: data.offer });
        await this.peerConnection.setRemoteDescription(sdp);
        this.createAnswer();
      } catch (e) {
        console.error('Error handling offer:', e);
      }
    });

    this.socket.on('receive-answer', async (data) => {
      if (!this.peerConnection) return;
      try {
        const sdp = new RTCSessionDescription({ type: 'answer', sdp: data.answer });
        await this.peerConnection.setRemoteDescription(sdp);
      } catch (e) {
        console.error('Error handling answer:', e);
      }
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
        console.error('Error adding ICE candidate:', e);
      }
    });

    this.socket.on('peer-disconnected', () => {
      this.disconnect();
    });
  }

  async createOffer() {
    if (!this.peerConnection) return;
    try {
      const offer = await this.peerConnection.createOffer({
        offerToReceiveAudio: true,
        offerToReceiveVideo: true,
      });
      await this.peerConnection.setLocalDescription(offer);
      this.socket.emit('send-offer', {
        to: this.peerId,
        offer: offer.sdp,
        room: this.roomName,
      });
    } catch (e) {
      console.error('Create offer error:', e);
    }
  }

  async createAnswer() {
    if (!this.peerConnection) return;
    try {
      const answer = await this.peerConnection.createAnswer({
        offerToReceiveAudio: true,
        offerToReceiveVideo: true,
      });
      await this.peerConnection.setLocalDescription(answer);
      this.socket.emit('send-answer', {
        to: this.peerId,
        answer: answer.sdp,
        room: this.roomName,
      });
    } catch (e) {
      console.error('Create answer error:', e);
    }
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

    this.currentFacing = this.currentFacing === 'user' ? 'environment' : 'user';

    try {
      const newStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: this.currentFacing, width: { ideal: 720 }, height: { ideal: 1280 } },
      });
      const newVideoTrack = newStream.getVideoTracks()[0];
      const sender = this.peerConnection?.getSenders().find(s => s.track?.kind === 'video');
      if (sender) {
        await sender.replaceTrack(newVideoTrack);
      }
      videoTrack.stop();
      this.localStream.removeTrack(videoTrack);
      this.localStream.addTrack(newVideoTrack);
      return this.localStream;
    } catch (e) {
      console.error('Switch camera failed:', e);
      this.currentFacing = this.currentFacing === 'user' ? 'environment' : 'user';
    }
  }

  disconnect() {
    if (this.callEnded) return;
    this.callEnded = true;

    try {
      this.socket.emit('leave-call-room', { room: this.roomName, uid: this.myUid });

      this.socket.off('peer-ready');
      this.socket.off('receive-offer');
      this.socket.off('receive-answer');
      this.socket.off('receive-ice');
      this.socket.off('peer-disconnected');

      if (this.localStream) {
        this.localStream.getTracks().forEach(t => t.stop());
        this.localStream = null;
      }

      if (this.peerConnection) {
        this.peerConnection.close();
        this.peerConnection = null;
      }
    } catch (e) {
      console.error('Disconnect error:', e);
    }

    this.onDisconnect?.();
  }
}
