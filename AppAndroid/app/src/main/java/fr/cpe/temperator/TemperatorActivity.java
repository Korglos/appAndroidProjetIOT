package fr.cpe.temperator;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.cpe.temperator.models.DataCapteur;
import fr.cpe.temperator.services.SocketService;

public class TemperatorActivity extends AppCompatActivity {
    private SocketService socketService;
    private boolean isBound = false;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            SocketService.LocalBinder binder = (SocketService.LocalBinder) service;
            socketService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };

    private String ipServerAddress = "10.56.118.113";
    private int udpPortServer = 10000;
    private int screen = 0;

    private TextView currentIpTextView;
    private TextView currentPortTextView;
    private TextView currentScreenTextView;

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
        currentScreenTextView = findViewById(R.id.current_screen);

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

        startSocketService();
    }

    private void startSocketService() {
        Intent intent = new Intent(this, SocketService.class);
        intent.putExtra("ipServerAddress", ipServerAddress);
        intent.putExtra("udpPortServer", udpPortServer);
        intent.putExtra("screen", screen);
        startService(intent);
    }

    private void updateCurrentIpPort() {
        currentIpTextView.setText(String.format("Adresse IP du serveur : %s", ipServerAddress));
        currentPortTextView.setText(String.format(getString(R.string.port_udp_du_serveur_d), udpPortServer));
        currentScreenTextView.setText(String.format(getString(R.string.eran_d), screen));
    }

    private void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Settings");

        View viewInflated = LayoutInflater.from(this).inflate(R.layout.settings_layout, (ViewGroup) findViewById(android.R.id.content), false);

        final EditText inputIp = viewInflated.findViewById(R.id.input_ip);
        final EditText inputPort = viewInflated.findViewById(R.id.input_port);
        final Spinner inputScreen = viewInflated.findViewById(R.id.input_screen);

        inputIp.setText(ipServerAddress);
        inputPort.setText(String.valueOf(udpPortServer));

        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>(screens));
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inputScreen.setAdapter(adapter);

        inputScreen.setSelection(adapter.getPosition(screen));

        builder.setView(viewInflated);

        builder.setPositiveButton("OK", (dialog, which) -> {
            ipServerAddress = inputIp.getText().toString();
            udpPortServer = Integer.parseInt(inputPort.getText().toString());
            screen = (Integer) inputScreen.getSelectedItem();
            updateCurrentIpPort();
            startSocketService(); // Restart service with new settings
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, SocketService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(this, SocketService.class));
    }

    public void sendUdpMessage(String message, boolean encrypted) {
        if (isBound) {
            socketService.sendUdpMessage(message, encrypted);
        }
    }
}