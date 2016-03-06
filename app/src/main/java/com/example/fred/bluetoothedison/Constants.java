package com.example.fred.bluetoothedison;

/**
 * From com.example.android.bluetoothchat
 * Created by tdawkins on 3/6/2016.
 */
public interface Constants {
    // Message types sent from the BluetoothChatService Handler
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_DEVICE_NAME = 4;
    int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";


}
