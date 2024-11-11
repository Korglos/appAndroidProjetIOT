package fr.cpe.temperator;

import android.os.Bundle;
import android.os.Looper;
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
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import fr.cpe.temperator.models.DataCapteur;
import fr.cpe.temperator.utils.EncryptionUtil;

import android.os.Handler;

import javax.crypto.SecretKey;

public class TemperatorActivity extends AppCompatActivity {
    private static final String ACTIVITY_NAME = "TemperatorActivity";

    private DatagramSocket socket;
    private Thread receiveThread;
    private Handler handler;
    private Runnable sendUdpRunnable;

    private String ipServerAddress = "192.168.1.76";
    private int udpPortServer = 10000;
    private int screen = 0;

    private TextView currentIpTextView;
    private TextView currentPortTextView;
    private TextView currentScreenTextView;

    private SecretKey secretKey;

    List<DataCapteur> data = Arrays.asList(
            new DataCapteur("0", "Luminosité", null, "lux"),
            new DataCapteur("1", "UV", null, "µW/cm²"),
            new DataCapteur("2", "Infrarouge", null, "µm"),
            new DataCapteur("3", "Pression", null, "hPa"),
            new DataCapteur("4", "Température", null, "°C"),
            new DataCapteur("5", "Humidité", null, "%")
    );

    Set<Integer> screens = new HashSet<>();

    // Initialisation de l'activité
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentIpTextView = findViewById(R.id.current_ip);
        currentPortTextView = findViewById(R.id.current_port);
        currentScreenTextView = findViewById(R.id.current_screen );

        updateCurrentIpPort();

        ImageView settingsIcon = findViewById(R.id.settings_icon);
        settingsIcon.setOnClickListener(v -> showSettingsDialog());

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        DataTextAdaptater adapter = new DataTextAdaptater(data);
        recyclerView.setAdapter(adapter);

        ItemTouchHelper.Callback callback = new DataTextItemTouchHelperCallback(adapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(recyclerView);

        connectSocket();

        startListening();

        getValues();
    }

    // Connexion à la socket
    public void connectSocket() {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        handler = new Handler(Looper.getMainLooper());
        try {
            socket = new DatagramSocket();
            secretKey = EncryptionUtil.getFixedKey();
        } catch (SocketException e) {
            Log.e(ACTIVITY_NAME, "Error creating socket", e);
        }
    }

    // Envoie un message à la socket
    protected void sendUdpMessage(String message, boolean encrypted) {
        try {
            InetAddress serverAddress = InetAddress.getByName(ipServerAddress);

            Log.d("send message", message);

            if (encrypted) {
                message = screen + ";" + message;
                message = EncryptionUtil.encrypt(message, secretKey);
            }

            byte[] sendData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddress, udpPortServer);
            socket.send(sendPacket);
            Log.d("send message encrypted", message);
        } catch (Exception e) {
            Log.e(ACTIVITY_NAME, "Error sending UDP message", e);
        }
    }

    // Écoute les messages de la socket
    private void startListening() {
        receiveThread = new Thread(() -> {
            try {
                byte[] receiveData = new byte[1024];
                while (!Thread.currentThread().isInterrupted()) {
                    DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                    socket.receive(receivePacket);
                    String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    Log.d(ACTIVITY_NAME, "Received message: " + response);
                    runOnUiThread(() -> {
                        Log.d("Received message", response);
                        onNewDataReceived(response);
                    });
                }
            } catch (Exception e) {
                Log.e(ACTIVITY_NAME, "Error receiving UDP message", e);
            }
        });
        receiveThread.start();
    }

    // Envoie le message "getValues()" à la socket toutes les 5 secondes pour récupérer les données des capteurs
    public void getValues() {
        sendUdpRunnable = new Runnable() {
            @Override
            public void run() {
                sendUdpMessage("getValues()", false);
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(sendUdpRunnable);
    }

    // Décrypte les données reçues et les affiche dans le RecyclerView
    public void onNewDataReceived(String newData) {
        try {
            String decryptedData = EncryptionUtil.decrypt(newData, secretKey);
            updateRecyclerView(decryptedData);
        } catch (Exception e) {
            Log.e(ACTIVITY_NAME, "Error decrypting data", e);
        }

    }

    // Met à jour les données du RecyclerView
    public void updateRecyclerView(String message) {
        String[] parts = message.split(";");

        screens.add(Integer.parseInt(parts[0]));

        for (int i = 1; i < parts.length; i++) {
            for (DataCapteur d : data) {
                if (d.getId().equals(String.valueOf(i - 1))) {
                    d.setValeur(parts[i]);
                    break;
                }
            }
        }

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        DataTextAdaptater adapter = (DataTextAdaptater) recyclerView.getAdapter();
        if (Objects.nonNull(adapter)) adapter.notifyDataSetChanged();
    }

    // Met à jour l'adresse IP, le port de la socket et l'écran actuel
    private void updateCurrentIpPort() {
        currentIpTextView.setText(String.format("Adresse IP du serveur : %s", ipServerAddress));
        currentPortTextView.setText(String.format(getString(R.string.port_udp_du_serveur_d), udpPortServer));
        currentScreenTextView.setText(String.format(getString(R.string.eran_d), screen));
    }

    // Affiche la boîte de dialogue des paramètres
    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.settings_layout, (ViewGroup) findViewById(android.R.id.content), false);

        final EditText inputIp = viewInflated.findViewById(R.id.input_ip);
        final EditText inputPort = viewInflated.findViewById(R.id.input_port);
        final Spinner inputScreen = viewInflated.findViewById(R.id.input_screen);

        inputIp.setText(ipServerAddress);
        inputPort.setText(String.valueOf(udpPortServer));

        List<Integer> screensSpinnerList = new ArrayList<>(screens);
        if (screensSpinnerList.isEmpty()) {
            screensSpinnerList.add(0);
        }
        screensSpinnerList.sort(Integer::compareTo);
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, screensSpinnerList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inputScreen.setAdapter(adapter);

        inputScreen.setSelection(adapter.getPosition(screen));

        builder.setView(viewInflated);

        builder.setPositiveButton("OK", (dialog, which) -> {
            ipServerAddress = inputIp.getText().toString();
            udpPortServer = Integer.parseInt(inputPort.getText().toString());

            Integer idScreen = (Integer) inputScreen.getSelectedItem();
            if (Objects.nonNull(idScreen)) {
                screen = idScreen;
            } else {
                screen = 0;
            }
            updateCurrentIpPort();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    // Arrête le Runnable et ferme la socket lorsque l'activité est détruite
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (Objects.nonNull(receiveThread) && !receiveThread.isInterrupted()) {
            receiveThread.interrupt();
        }
        if (Objects.nonNull(socket) && !socket.isClosed()) {
            socket.close();
        }
        handler.removeCallbacks(sendUdpRunnable);
    }
}