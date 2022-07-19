package com.isoft.clockbt;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    String hour = "";
    String minute = "";
    String year = "";
    String month = "";
    String date = "";
    String dow = "";
    boolean alreadyChanged = false;
    boolean isOncePortClosed = false;

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SharedPreferences btAddress = getSharedPreferences("btAddress", MODE_PRIVATE);


        final String[] selectedDevice = {""};
        ImageButton btBut = findViewById(R.id.btBut);

        if (isAdapterEnabled()) {
            if (btAddress.contains("Address")) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Bluetooth permission denied", Toast.LENGTH_SHORT).show();
                    return;
                }
                BTPairedDevices = mBluetoothAdapter.getBondedDevices();
                if (BTPairedDevices.size() > 0) {
                    for (BluetoothDevice btDev : BTPairedDevices) {
                        if (btDev.getAddress().equals(btAddress.getString("Address", ""))) {
                            btDevice = btDev;
                            System.out.println("Connection started");
                            connectToDevice();

                            }
                }
                }
            }
        }
        btBut.setOnClickListener(v -> {
            if (!isAdapterEnabled()) {
                Toast.makeText(this, "Please enable bluetooth before continuing", Toast.LENGTH_SHORT).show();
            }
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                System.out.println("Permission not granted");
                Toast.makeText(this, "Permission not granted", Toast.LENGTH_LONG).show();
                return;
            }
            BTPairedDevices = mBluetoothAdapter.getBondedDevices();

            if (BTPairedDevices.size() == 0) {
                if (isAdapterEnabled()) {
                    Toast.makeText(this, "Please pair your device first", Toast.LENGTH_SHORT).show();
                }
            } else if (!(btSocket != null && btSocket.isConnected())) {
                ArrayList<String> devices = new ArrayList<>();

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Permission not granted", Toast.LENGTH_SHORT).show());
                    return;
                }

                for (BluetoothDevice btDev : BTPairedDevices) {
                    devices.add(btDev.getName());
                }
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Choose the device to pair");

                builder.setItems(arrayConverter(devices),
                        (dialog, which) -> {
                            defaultSettings = "";

                            selectedDevice[0] = arrayConverter(devices)[which];
                            for (BluetoothDevice btDev : BTPairedDevices) {
                                if (selectedDevice[0].equals(btDev.getName())) {
                                    btDevice = btDev;
                                    SharedPreferences.Editor editBt = btAddress.edit();
                                    editBt.putString("Address", btDevice.getAddress());
                                    editBt.apply();
                                    System.out.println(btDevice);
                                    //Connect using a different thread
                                    connectToDevice();

                                }
                            }
                        });

                AlertDialog dialog = builder.create();
                dialog.show();

            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Are you sure want to disconnect " + selectedDevice[0] + "?");
                builder.setPositiveButton("Ok", (dialog, which) -> {
                    try {
                        btSocket.close();
                        stopService(new Intent(MainActivity.this, WatchNotifications.class));
                        btConnected = false;
                        btBut.setImageResource(R.drawable.bt_not_con);
                        alreadyChanged = false;
                        Toast.makeText(MainActivity.this, "Bluetooth device disconnected", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                builder.setNegativeButton("Cancel", (dialog, which) -> {

                });

                builder.create();
                builder.show();
            }
        });

        TimerTask setLayouts = new TimerTask() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                if (!defaultSettings.equals("")) {
                    String[] splitted = defaultSettings.split(",");
                    System.out.println(splitted[0]);
                    switch (splitted[0]) {
                        case "0":
                            runOnUiThread(() -> {
                                TextView blm = findViewById(R.id.blm);
                                blm.setText("Intelligent");
                            });

                            break;
                        case "1":
                            runOnUiThread(() -> {
                                TextView blm = findViewById(R.id.blm);
                                blm.setText("Off");
                            });
                            break;
                        case "2":
                            runOnUiThread(() -> {
                                TextView blm = findViewById(R.id.blm);
                                blm.setText("On");
                            });
                            break;
                    }
                    runOnUiThread(() -> {
                        SeekBar seekBar = findViewById(R.id.seekBar);
                        try {
                            seekBar.setProgress(Integer.parseInt(splitted[1]));
                            alreadyChanged = true;
                            TextView rl = findViewById(R.id.roomLightStatus);
                            backlightState = Integer.parseInt(splitted[0]);
                            roomLightState = !splitted[2].equals("0");
                            rl.setText(splitted[2].equals("0") ? "Off" : "On");
                        } catch (Exception e) {
                            defaultSettings = "";
                            sendMessage("`e", false);
                        }

                    });


                    defaultSettings = "";
                }

                if (!isOncePortClosed) {
                    if (!isAdapterEnabled()) {
                        try {
                            btSocket.close();
                            isOncePortClosed = true;

                            stopService(new Intent(MainActivity.this, WatchNotifications.class));
                            runOnUiThread(() -> btBut.setImageResource(R.drawable.bt_not_con));
                        } catch (Exception ignored) {

                        }
                    }
                }

                TextView timeShow = findViewById(R.id.time);
                DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
                LocalDateTime now = LocalDateTime.now();
                runOnUiThread(() -> timeShow.setText(dtf.format(now)));


            }
        };

        Timer timer = new Timer("LayoutSet");
        timer.schedule(setLayouts, 0, 100);

        CardView timeSet = findViewById(R.id.timeSet);
        timeSet.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Are you sure want to set the time?");
            builder.setPositiveButton("Ok", (dialog, which) -> {
                String message;
                LocalDateTime now = LocalDateTime.now();
                DateTimeFormatter dtfYear = DateTimeFormatter.ofPattern("y");
                DateTimeFormatter dtfMonth = DateTimeFormatter.ofPattern("M");
                DateTimeFormatter dtfDate = DateTimeFormatter.ofPattern("d");
                DateTimeFormatter dtfDow = DateTimeFormatter.ofPattern("e");
                DateTimeFormatter dtfHour = DateTimeFormatter.ofPattern("H");
                DateTimeFormatter dtfMinute = DateTimeFormatter.ofPattern("m");
                year = dtfYear.format(now).substring(2);
                month = zeroFormatter(dtfMonth.format(now));
                date = zeroFormatter(dtfDate.format(now));
                dow = dtfDow.format(now);
                hour = zeroFormatter(dtfHour.format(now));
                minute = zeroFormatter(dtfMinute.format(now));
                message = "`d" + year + month + date + dow + hour + minute;
                sendMessage(message, true);
                    });
            builder.setNegativeButton("Cancel", (dialog, which) -> {

            });
            builder.create();
            builder.show();

        });


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
            if (backlightState < 2) {
                backlightState++;
            } else {
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
                if (alreadyChanged) {
                    sendMessage("`c" + progress + "\n", false);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                if (!(btSocket != null && btSocket.isConnected())) {
                    Toast.makeText(MainActivity.this, "Please connect to the bluetooth device first", Toast.LENGTH_SHORT).show();
                }
                defaultSettings = "";
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

    private String zeroFormatter(String number) {
        if (Integer.parseInt(number) < 10) {
            return "0" + number;
        } else {
            return number;
        }
    }


    private void connectToDevice(){

        ImageButton btBut = findViewById(R.id.btBut);
        final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
        Thread connectToBT = new Thread() {
            public void run() {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Permission not granted", Toast.LENGTH_SHORT).show());
                    return;
                }


                try {

                    btSocket = btDevice.createRfcommSocketToServiceRecord(myUUID);
                    System.out.println("Connecting to " + btDevice + " with " + myUUID);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Connecting to " + btDevice.getName(), Toast.LENGTH_SHORT).show());
                    btSocket.connect();
                    btConnected = true;
                    btBut.setImageResource(R.drawable.bt_connected);
                    sendMessage("`e\n", true);
                    System.out.println("Connected");
                    startService(new Intent(MainActivity.this, WatchNotifications.class));
                    isOncePortClosed = false;
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Connected", Toast.LENGTH_SHORT).show();

                    });
                    try {
                        byte[] buffer = new byte[1024];
                        int len;
                        do {
                            len = btSocket.getInputStream().read(buffer);
                            byte[] data = Arrays.copyOf(buffer, len);
                            defaultSettings += ASCIIConverter(data);
                            System.out.println(defaultSettings);
                        } while (!defaultSettings.equals(""));
                    } catch (Exception e) {

                        try {
                            btSocket.close();

                            btBut.setImageResource(R.drawable.bt_not_con);
                        } catch (Exception ignored) {
                        }
                    }

                } catch (IOException e) {
                    btConnected = false;

                    btBut.setImageResource(R.drawable.bt_not_con);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to connect", Toast.LENGTH_SHORT).show());
                    e.printStackTrace();
                }
            }

        };

        connectToBT.start();

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