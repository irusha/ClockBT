package com.isoft.clockbt;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    String defaultSettings = "";
    Set<BluetoothDevice> BTPairedDevices = null;
    BluetoothDevice btDevice = null;
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    BluetoothSocket btSocket = null;
    boolean btConnected = false;
    Boolean roomLightState = true;
    int backlightState = 0;
    boolean alreadyChanged = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        final String[] selectedDevice = {""};
        ImageButton btBut = findViewById(R.id.btBut);
        btBut.setOnClickListener(v -> {
            if (!isAdapterEnabled()) {
                Toast.makeText(this, "Please enable bluetooth before continuing", Toast.LENGTH_SHORT).show();
            }
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                System.out.println("Permission not granted");
                return;
            }
            BTPairedDevices = mBluetoothAdapter.getBondedDevices();


            if (BTPairedDevices.size() == 0) {
                Toast.makeText(this, "Please pair your device first", Toast.LENGTH_SHORT).show();
            } else if (!btConnected) {
                ArrayList<String> devices = new ArrayList<>();

                for (BluetoothDevice btDev : BTPairedDevices) {
                    devices.add(btDev.getName());
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Choose the device");

                builder.setItems(arrayConverter(devices),
                        (dialog, which) -> {
                            defaultSettings = "";

                            selectedDevice[0] = arrayConverter(devices)[which];
                            for (BluetoothDevice btDev : BTPairedDevices) {
                                if (selectedDevice[0].equals(btDev.getName())) {
                                    btDevice = btDev;
                                    System.out.println(btDevice);
                                    //Connect using a different thread
                                    Thread connectToBT = new Thread() {
                                        public void run() {
                                            try {
                                                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                                                    System.out.println("Permission not granted");
                                                    return;
                                                }
                                                btSocket = btDevice.createRfcommSocketToServiceRecord(myUUID);
                                                System.out.println("Connecting to " + btDevice + " with " + myUUID);
                                                btSocket.connect();
                                                btConnected = true;
                                                sendMessage("`e\n", true);
                                                System.out.println("Connected");
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {

                                                        Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                                try {
                                                    byte[] buffer = new byte[1024];
                                                    int len;
                                                    while (true) {
                                                        len = btSocket.getInputStream().read(buffer);
                                                        byte[] data = Arrays.copyOf(buffer, len);
                                                        defaultSettings += ASCIIConverter(data);
                                                        System.out.println(defaultSettings);
                                                        if (defaultSettings.equals("")) {
                                                            break;
                                                        }
                                                    }
                                                } catch (Exception e) {

                                                    try {
                                                        btSocket.close();
                                                    } catch (Exception ignored) {
                                                    }
                                                }


                                            } catch (IOException e) {
                                                btConnected = false;
                                                runOnUiThread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        Toast.makeText(MainActivity.this, "Failed to connect", Toast.LENGTH_SHORT).show();

                                                    }
                                                });
                                                e.printStackTrace();
                                            }
                                        }

                                    };

                                    connectToBT.start();

                                }
                            }
                        });

                AlertDialog dialog = builder.create();
                dialog.show();

            } else {
                try {
                    btSocket.close();
                    btConnected = false;
                    Toast.makeText(this, "Bluetooth device disconnected", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        TimerTask setLayouts = new TimerTask() {
            @Override
            public void run() {
                if (!defaultSettings.equals("")) {
                    String[] splitted = defaultSettings.split(",");
                    System.out.println(splitted[0]);
                    switch (splitted[0]) {
                        case "0":
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView blm = findViewById(R.id.blm);
                                    blm.setText("Intelligent");
                                }
                            });

                            break;
                        case "1":
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView blm = findViewById(R.id.blm);
                                    blm.setText("Off");
                                }
                            });
                            break;
                        case "2":
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    TextView blm = findViewById(R.id.blm);
                                    blm.setText("On");
                                }
                            });
                            break;
                    }
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SeekBar seekBar = findViewById(R.id.seekBar);
                            seekBar.setProgress(Integer.parseInt(splitted[1]));
                            alreadyChanged = true;
                            TextView rl = findViewById(R.id.roomLightStatus);
                            backlightState = Integer.parseInt(splitted[0]);
                            roomLightState = !splitted[2].equals("0");
                            rl.setText(splitted[2].equals("0") ? "Off" : "On");
                        }
                    });

                    defaultSettings = "";
                }
            }
        };

        Timer timer = new Timer("aaaa");
        timer.schedule(setLayouts, 0, 100);


        CardView light = findViewById(R.id.roomLight);
        light.setOnClickListener(v -> {
            sendMessage("`a\n", true);
            TextView rl = findViewById(R.id.roomLightStatus);
            roomLightState = !roomLightState;
            rl.setText(!roomLightState ? "Off" : "On");
        });

        CardView backlight = findViewById(R.id.backlight);
        backlight.setOnClickListener(v -> {
            sendMessage("`b\n", true);
            if (backlightState < 2){
                backlightState++;
            }
            else {
                backlightState = 0;
            }
            TextView blm = findViewById(R.id.blm);

            switch (backlightState) {

                case 0:
                    blm.setText("Intelligent");
                    break;
                case 1:
                    blm.setText("Off");
                    break;
                case 2:
                    blm.setText("On");
                    break;
            }

        });

        SeekBar seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(alreadyChanged) {
                    sendMessage("`c" + progress + "\n", false);
                }
                defaultSettings = "";
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (!(btSocket != null && btConnected)) {
                    Toast.makeText(MainActivity.this, "Please connect to the bluetooth device first", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }


    public void sendMessage(String message, boolean toast) {
        if (btSocket != null && btSocket.isConnected()) {

            try {
                btSocket.getOutputStream().write(message.getBytes());
            } catch (IOException e) {
                e.printStackTrace();

            }
        } else {
            if (toast) {
                Toast.makeText(this, "Please connect to the bluetooth device first", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private static String[] arrayConverter(ArrayList<String> array) {
        String[] newArr = new String[array.size()];
        for (int i = 0; i < array.size(); i++) {
            newArr[i] = array.get(i);
        }
        return newArr;
    }

    private boolean isAdapterEnabled() {

        return mBluetoothAdapter.isEnabled();
    }


    private String ASCIIConverter(byte[] array) {
        String retString = "";
        for (int currentNum : array) {
            retString += Character.toString((char) currentNum);
        }
        return retString;
    }
}