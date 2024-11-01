package fr.cpe.temperator;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.cpe.temperator.models.DataCapteur;

import android.os.Handler;

import javax.crypto.SecretKey;

public class MainActivity extends AppCompatActivity {
    private DatagramSocket socket;
    private Thread receiveThread;
    private Handler handler = new Handler();
    private Runnable sendUdpRunnable;

    private String ipServerAddress = "10.56.118.113";
    private int udpPortServer = 10000;
    private int screen = 0;

    private TextView currentIpTextView;
    private TextView currentPortTextView;
    private TextView currentScreenTextView;

    private SecretKey secretKey;
    boolean fritsMessage = true;

    List<DataCapteur> data = Arrays.asList(
            new DataCapteur("0", "Luminosité", null, "lux"),
            new DataCapteur("1", "UV", null, "µW/cm²"),
            new DataCapteur("2", "Infrarouge", null, "µm"),
            new DataCapteur("3", "Pression", null, "hPa"),
            new DataCapteur("4", "Température", null, "°C"),
            new DataCapteur("5", "Humidité", null, "%")
    );

    Set<Integer> screens = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentIpTextView = findViewById(R.id.current_ip);
        currentPortTextView = findViewById(R.id.current_port);
        currentScreenTextView = findViewById(R.id.current_screen );

        updateCurrentIpPort();

        ImageView settingsIcon = findViewById(R.id.settings_icon);
        settingsIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSettingsDialog();
            }
        });

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

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
            secretKey = EncryptionUtil.getFixedKey();
        } catch (Exception e) {
            Log.e("MainActivity", "Error creating socket", e);
        }

        startListening();

        // Initialize the Runnable to send the UDP message every 5 seconds
        sendUdpRunnable = new Runnable() {
            @Override
            public void run() {
                sendUdpMessage("getValues()", false);
                handler.postDelayed(this, 5000); // Re-post the Runnable with a delay of 5 seconds
            }
        };
        handler.post(sendUdpRunnable); // Start the Runnable immediately
    }

    public void onNewDataReceived(String newData) {
        try {
//            String decryptedData = EncryptionUtil.decrypt(newData, secretKey);
            updateRecyclerView(newData);
        } catch (Exception e) {
            Log.e("MainActivity", "Error decrypting data", e);
        }

    }

    public void updateRecyclerView(String message) {
        String[] parts = message.split(";");

        screens.add(Integer.parseInt(parts[0]));

        if (fritsMessage) {
            // Update the screen value with the first message received

        }

        for (int i = 1; i < parts.length && i < data.size(); i++) {
            for (DataCapteur d : data) {
                if (d.getId().equals(String.valueOf(i - 1))) {
                    d.setValeur(parts[i]);
                    break;
                }
            }
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        MyAdapter adapter = (MyAdapter) recyclerView.getAdapter();
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    protected void sendUdpMessage(String message, boolean encrypted) {
        try {
            InetAddress serverAddress = InetAddress.getByName(ipServerAddress);

            Log.d("send message", message);

            if (encrypted) {
                message = screen + ";" + message;
//                message = EncryptionUtil.encrypt(message, secretKey);
            }

            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, udpPortServer);
            socket.send(sendPacket);
            Log.d("send message encrypted", message);
        } catch (Exception e) {
            Log.e("MainActivity", "Error sending UDP message", e);
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
                    Log.d("MainActivity", "Received message: " + response);
                    runOnUiThread(() -> {
                        Log.d("Received message", response);
                        onNewDataReceived(response);
                    });
                }
            } catch (Exception e) {
                Log.e("MainActivity", "Error receiving UDP message", e);
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
        final Spinner inputScreen = viewInflated.findViewById(R.id.input_screen);

        inputIp.setText(ipServerAddress);
        inputPort.setText(String.valueOf(udpPortServer));

        // Populate the Spinner with the values from screens
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>(screens));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inputScreen.setAdapter(adapter);

        // Set the current screen as the selected value
        inputScreen.setSelection(adapter.getPosition(screen));

        builder.setView(viewInflated);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ipServerAddress = inputIp.getText().toString();
                udpPortServer = Integer.parseInt(inputPort.getText().toString());
                screen = (Integer) inputScreen.getSelectedItem();
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
        currentScreenTextView.setText("Ecran : " + screen);
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
        handler.removeCallbacks(sendUdpRunnable); // Stop the Runnable when the activity is destroyed
    }
}