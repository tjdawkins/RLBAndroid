package com.example.fred.bluetoothedison;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelUuid;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import android.bluetooth.BluetoothSocket;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Set;
import java.util.UUID;



public class BluetoothActivity extends Activity {

    private static final String TAG = "BluetoothActivity";
    public final static String DATA_DIR = "com.example.fred.bluetoothedison.DATADIR";

    // UI Elements
    Button On,Connect, Close;
    TextView statusLabel, detailsLabel;
    String status;
    String details;

    // Bluetooth Components
    private static final UUID SVC_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    BluetoothSocket Socket;
    BluetoothDevice Device = null;
    BluetoothAdapter Adapter;
    Set<BluetoothDevice> pairedDevices;

    // Bluetooth IO
    OutputStream outStream = null;
    InputStream inStream = null;

    // Array Lists for Data
    //public static ArrayList<Integer> times = new ArrayList<>();
    public static ArrayList<Date> times = new ArrayList<>();
    public static ArrayList<Float> temp = new ArrayList<>();

    // File IO Components
    private String filename;
    private File dataDir;
    private File dataFile;

    // Date Components
    Date date;
    long polltime = System.currentTimeMillis();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        Connect = (Button)findViewById(R.id.button1);
        On = (Button)findViewById(R.id.button2);
        Close = (Button)findViewById(R.id.button3);
        statusLabel = (TextView)findViewById(R.id.text1);
        detailsLabel = (TextView)findViewById(R.id.text2);

        status = "Status: Not Connected";
        details = "INFO: Press Connect to connect";
        statusLabel.setText(status);
        detailsLabel.setText(details);

        date = new Date();
        Adapter = BluetoothAdapter.getDefaultAdapter();
        DateFormat df = new SimpleDateFormat("MM-dd-yyyy");

        filename = "/data-" + df.format(date) + ".txt";
        dataDir =  getStorageFile("TemperatureData");
        dataFile = new File(dataDir,filename);

    }

    @Override
    protected void onStart() {

        super.onStart();

        statusLabel.setText(status);
        detailsLabel.setText(details);


    }
    public void connect(View view){
        status = "Status: Connecting..." ;
        details = "INFO: Establishing Connection";
        statusLabel.setText(status);
        detailsLabel.setText(details);

        pairedDevices = Adapter.getBondedDevices();

        for(BluetoothDevice bt : pairedDevices) {
            if(bt.getName().toUpperCase().contains("EDISON"))
                Device = bt;
        }
        Log.d(TAG, "Address: " + Device.getAddress());
        Log.d(TAG, "Name: " + Device.getName());
        Log.d(TAG, "Name: " + filename);


        // Check out the UUIDs
        ParcelUuid[] uuids = Device.getUuids();
        for(ParcelUuid uuid: uuids) {

            Log.d(TAG, "UUID\t" + uuid.getUuid().toString());
        }


        try {
            BluetoothSocket Socket = Device.createRfcommSocketToServiceRecord(SVC_UUID);

            if (!Socket.isConnected()){
                Socket.connect();
                Log.d(TAG, "Connected");
            }

            outStream = Socket.getOutputStream();
            inStream = Socket.getInputStream();

            while(inStream.available()<1);

            String v  = read();
            details = "INFO: " + v;

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        status = "Status: Connected" ;
        statusLabel.setText(status);
        detailsLabel.setText(details);
    }


    // COLLECT DATA
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void getData(View view) throws IOException {
        detailsLabel.setText("Getting Data...");
        String msg = "GET_DATA";
        String data = "";
        int dataAvailable = -1;


        if (outStream == null) {
            Log.d(TAG, "ERROR: Bluetooth not connected. Can't read data.");
            details = "ERROR: Device not connected.";
            detailsLabel.setText(details);
            return;
        }
        // Wont respond until edison receives
        outStream.write((new String("CHECK_DATA")).getBytes());

        while(inStream.available()<1); // This is bad. Need to handle in threads it's blocking

        dataAvailable = Integer.parseInt(read());

        if(dataAvailable < 1) {
            details = "ERROR: No Data Available on Edison";
            detailsLabel.setText(details);
            return;

        }

        // Get the available data
        outStream.write(msg.getBytes());
        while(inStream.available()<1); // So bad, but if not it misses data
        data  += read();
        polltime = System.currentTimeMillis();

        try {
            FileWriter fw  = new FileWriter(dataFile, true);
            fw.write(data,0,data.length());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        getDataFromString(data);
        details = "INFO: Data Received";
        detailsLabel.setText(details);

    }

    public void close(View view) {
        details = "INFO: Disconnecting...";
        detailsLabel.setText(details);
        try {
            outStream.write((new String("CLOSE")).getBytes());
        } catch (IOException e ) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        status = "Status: Not Connected";
        details = "INFO: Disconnected...";
        statusLabel.setText(status);
        detailsLabel.setText(details);
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

    // Wrapper for reading input stream
   private String read() throws IOException {
        BufferedInputStream btin = null;
        btin = new BufferedInputStream(inStream);
        String s = "";

       try{
           Thread.sleep(500);
       } catch (InterruptedException e) {

           Log.d(TAG,"Interrupted Exception: ", e);

       }

       while(btin.available()>0) {

           s += (char)btin.read();

       }

       return s;

   }

    // Data processing
    private void getDataFromString(String s) {

        String lines[] = s.split("\n");
        Date time;
        for(String line : lines) {

            String parts[] = line.split(":");


            // Add the time from millis since start
            int seconds = Integer.parseInt(parts[0]);
            time = new java.util.Date((long)seconds);
            DateFormat df = new SimpleDateFormat("MM-dd-yyyy hh:mm:ss");

            Log.d(TAG, "Integer: " + seconds + "Date: " + df.format(time));


            times.add(time);
            temp.add(Float.parseFloat(parts[1]));
        }
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
        intent.putExtra(DATA_DIR,dataDir.toString());

        startActivity(intent);
    }

    /** Called when the user clicks the display data button */
    // TODO: EMAIL FILE NOT WORKING
    public void displayData(View view){


        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.setType("plain/text");
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "Data from" + date);
        Uri fileuri = Uri.parse(dataFile.toString());
        sendIntent.putExtra(Intent.EXTRA_STREAM, fileuri);
        sendIntent.putExtra(Intent.EXTRA_TEXT, "Data attached");
        try {
            startActivity(Intent.createChooser(sendIntent, "Send Mail"));
        } catch (android.content.ActivityNotFoundException ex) {

            Toast.makeText(getApplicationContext(),
                    "There are no email clients installed.",
                    Toast.LENGTH_SHORT).show();

        }



    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }

    public File getStorageFile(String dirName) {
        // Get the directory for the user's public pictures directory.
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS),dirName);
        if (!file.mkdirs()) {
            Log.d(TAG, "Directory not created");
        }
        return file;
    }

}

