package com.example.fred.bluetoothedison;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity extends Activity {

    private static final String TAG = "BluetoothActivity";
    public static byte[] passArray;

    Button On,Connect, Close;
    TextView Version, Count;
    BluetoothSocket Socket;
    BluetoothDevice Device = null;
    BluetoothAdapter Adapter;
    Set<BluetoothDevice> pairedDevices;

    OutputStream outStream = null;
    InputStream inStream = null;
    public static ArrayList<String> List = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        Connect = (Button)findViewById(R.id.button1);
        On = (Button)findViewById(R.id.button2);
        Close = (Button)findViewById(R.id.button3);
        Version = (TextView)findViewById(R.id.text1);
        Count = (TextView)findViewById(R.id.text2);

        Adapter = BluetoothAdapter.getDefaultAdapter();



    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void on(View view) throws IOException {
        String msg = "ON";
        if (outStream == null) {
            Log.d(TAG, "You have to connect");
            return;
        }

        try {
            outStream.write(msg.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d(TAG, "LED ON!");

        String v  = read();

        List.add(v);

        Version.setText("List: " + List);
    }


    public void connect(View view){
        pairedDevices = Adapter.getBondedDevices();

        for(BluetoothDevice bt : pairedDevices) {
            if(bt.getName().equals("edison"))
                Device = bt;
        }
        Log.d(TAG, "Address: " + Device.getAddress());
        Log.d(TAG, "Name: " + Device.getName());

        try {
            BluetoothSocket Socket = Device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));

            if (!Socket.isConnected()){
                Socket.connect();
                Log.d(TAG, "Connected");
            }

            outStream = Socket.getOutputStream();
            inStream = Socket.getInputStream();

            String v  = read();
            Version.setText("Version: "+v);
            Count.setText("Count: 0");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void close(View view) {
        List.clear();
        Count.setText("List: clear");
        if (inStream != null) {
            try {inStream.close();} catch (Exception e) {}
            inStream = null;
        }

        if (outStream != null) {
            try {outStream.close();} catch (Exception e) {}
            outStream = null;
        }

        if (Socket != null) {
            try {Socket.close();} catch (Exception e) {}
            Socket = null;
        }
    }

    /*public Integer readInt() throws IOException {
        byte[] buffer = new byte[64];
        inStream.read(buffer);
        Integer s = new Integer(String.valueOf(buffer));
        return s;
    }*/

    public String read() throws IOException {
        byte[] buffer;
        buffer = new byte[64];
        inStream.read(buffer);
        String s = new String(buffer);
        return s;
    }

    public void setArray(ArrayList<String> setArray){
        this.List = setArray;
    }

    public ArrayList<String> getArray(){
        return(this.List);
    }


    public ArrayList<Byte> testArray(){
        ArrayList<Byte> testArray = new ArrayList<Byte>();

        for (int i = 0; i < 20; i++)
        {
            testArray.add((byte)i);
        }

        return (testArray);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_bluetooth, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /** Called when the user clicks the display graph button */
    public void displayGraph(View view) {
        Intent intent = new Intent(this, displayGraph.class);
        startActivity(intent);
    }

    /** Called when the user clicks the display data button */
    public void displayData(View view){

    }
}