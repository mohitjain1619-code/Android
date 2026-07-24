package com.mohitt.camverz;

import android.util.Log;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

public class SocketManager {

    private static final String TAG = "SocketManager";
    private static Socket mSocket;

    // Only your working backend URL here
    private static final String SOCKET_URL = "https://ultra-backend-production-d7f0.up.railway.app";

    private SocketManager() {}

    public static synchronized Socket getInstance() {

        if (mSocket == null) {
            try {
                IO.Options options = new IO.Options();
                options.forceNew = false;
                options.reconnection = true;
                options.reconnectionAttempts = 99999;
                options.reconnectionDelay = 500;
                options.timeout = 5000;

                mSocket = IO.socket(SOCKET_URL, options);
                Log.d(TAG, "Socket initialized: " + SOCKET_URL);

            } catch (URISyntaxException e) {
                Log.e(TAG, "Socket init error: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }

        if (!mSocket.connected()) {
            mSocket.connect();
        }

        return mSocket;
    }
}
