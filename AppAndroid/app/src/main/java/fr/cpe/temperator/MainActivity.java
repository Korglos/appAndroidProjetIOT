package fr.cpe.temperator;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

import fr.cpe.temperator.models.DataCapteur;

public class MainActivity extends AppCompatActivity {
    private DatagramSocket socket;
    private Thread receiveThread;

    private String ipServerAddress = "192.168.1.1";
    private int udpPortServer = 10000;

    private TextView currentIpTextView;
    private TextView currentPortTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentIpTextView = findViewById(R.id.current_ip);
        currentPortTextView = findViewById(R.id.current_port);

        updateCurrentIpPort();

        ImageView settingsIcon = findViewById(R.id.settings_icon);
        settingsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });

        Button sendButton = findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendUdpMessage("Hello, Kiki!");
            }
        });

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        List<DataCapteur> data = Arrays.asList(
                new DataCapteur("1", "Infrarouge", "value1"),
                new DataCapteur("2", "Humidité", "value2"),
                new DataCapteur("3", "UV", "value3"),
                new DataCapteur("4", "Pression", "value4"),
                new DataCapteur("5", "Luminosité", "value5"),
                new DataCapteur("6", "Température", "value6")
        );
        MyAdapter adapter = new MyAdapter(data);
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new MyItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        // Allow network operations on the main thread for simplicity (not recommended for production)
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        try {
            socket = new DatagramSocket();
        } catch (Exception e) {
            Log.e("NetworkActivity", "Error creating socket", e);
        }

        startListening();
    }

    private void sendUdpMessage(String message) {
        try {
            InetAddress serverAddress = InetAddress.getByName(ipServerAddress);

            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, udpPortServer);
            socket.send(sendPacket);
        } catch (Exception e) {
            Log.e("NetworkActivity", "Error sending UDP message", e);
        }
    }

    private void startListening() {
        receiveThread = new Thread(() -> {
            try {
                byte[] receiveData = new byte[1024];
                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);
                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    Log.d("NetworkActivity", "Received message: " + response);
                    runOnUiThread(() -> {
                        // action received message
                    });
                }
            } catch (Exception e) {
                Log.e("NetworkActivity", "Error receiving UDP message", e);
            }
        });
        receiveThread.start();
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");

        // Inflate the dialog_settings layout
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.settings_layout, (ViewGroup) findViewById(android.R.id.content), false);

        // Set up the input fields
        final EditText inputIp = viewInflated.findViewById(R.id.input_ip);
        final EditText inputPort = viewInflated.findViewById(R.id.input_port);

        inputIp.setText(ipServerAddress);
        inputPort.setText(String.valueOf(udpPortServer));

        builder.setView(viewInflated);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ipServerAddress = inputIp.getText().toString();
                udpPortServer = Integer.parseInt(inputPort.getText().toString());
                updateCurrentIpPort();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void updateCurrentIpPort() {
        currentIpTextView.setText("Adresse IP du serveur : " + ipServerAddress);
        currentPortTextView.setText("Port UDP du serveur : " + udpPortServer);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (receiveThread != null && !receiveThread.isInterrupted()) {
            receiveThread.interrupt();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}