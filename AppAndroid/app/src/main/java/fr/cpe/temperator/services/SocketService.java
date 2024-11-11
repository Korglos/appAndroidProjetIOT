package fr.cpe.temperator.services;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import javax.crypto.SecretKey;

import fr.cpe.temperator.utils.EncryptionUtil;

public class SocketService extends Service {
    private static final String SERVICE_NAME = "SocketService";
    private DatagramSocket socket;
    private Thread receiveThread;
    private Handler handler;
    private Runnable sendUdpRunnable;
    private SecretKey secretKey;
    private String ipServerAddress;
    private int udpPortServer;
    private int screen;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public SocketService getService() {
            return SocketService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        try {
            socket = new DatagramSocket();
            secretKey = EncryptionUtil.getFixedKey();
        } catch (SocketException e) {
            Log.e(SERVICE_NAME, "Error creating socket", e);
        }
        startListening();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ipServerAddress = intent.getStringExtra("ipServerAddress");
        udpPortServer = intent.getIntExtra("udpPortServer", 10000);
        screen = intent.getIntExtra("screen", 0);
        getValues();
        return START_STICKY;
    }

    private void startListening() {
        receiveThread = new Thread(() -> {
            try {
                byte[] receiveData = new byte[1024];
                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);
                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    Log.d(SERVICE_NAME, "Received message: " + response);
//                    runOnUiThread(() -> {
//                        Log.d("Received message", response);
//                        onNewDataReceived(response);
//                    });
                }
            } catch (Exception e) {
                Log.e(SERVICE_NAME, "Error receiving UDP message", e);
            }
        });
        receiveThread.start();
    }

    private void getValues() {
        sendUdpRunnable = new Runnable() {
            @Override
            public void run() {
                sendUdpMessage("getValues()", false);
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(sendUdpRunnable);
    }

    public void sendUdpMessage(String message, boolean encrypted) {
        try {
            InetAddress serverAddress = InetAddress.getByName(ipServerAddress);
            if (encrypted) {
                message = screen + ";" + message;
                message = EncryptionUtil.encrypt(message, secretKey);
            }
            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, udpPortServer);
            socket.send(sendPacket);
        } catch (Exception e) {
            Log.e(SERVICE_NAME, "Error sending UDP message", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiveThread != null && !receiveThread.isInterrupted()) {
            receiveThread.interrupt();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        handler.removeCallbacks(sendUdpRunnable);
    }
}