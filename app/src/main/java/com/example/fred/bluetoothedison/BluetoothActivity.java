package com.example.fred.bluetoothedison;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
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

    // UI Components
    Button On, Connect, Close;
    TextView statusLabel, Count;

    // BLUETOOTH COMMUNICATIONS
    BluetoothAdapter mAdapter;
    BluetoothDevice mBTDevice = null;
    BluetoothSocket Socket;
    Set<BluetoothDevice> pairedDevices;
    OutputStream outStream = null;
    InputStream inStream = null;


    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Member object for the serial service
     */
    private BluetoothSerialService mSerialService = null;

    // ArrayLists for data
    public static ArrayList<String> List = new ArrayList<>();
    public static ArrayList<Integer> times = new ArrayList<>();
    public static ArrayList<Float> temp = new ArrayList<>();

    // File IO vars
    private String filename;
    private File dataDir;
    private File dataFile;

    // Try to handle timestamp from data
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
        Count = (TextView)findViewById(R.id.text2);

        mAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }

        // Local data file naming and handle
        date = new Date();
        DateFormat df = new SimpleDateFormat("MM-dd-yyyy");
        filename = "/data-" + df.format(date) + ".txt";
        dataDir =  getStorageFile("TemperatureData");
        dataFile = new File(dataDir,filename);

    }

    @Override
    public void onStart() {

        super.onStart();

        if(mSerialService == null)
            setupSerial();

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mSerialService != null) {
            mSerialService.stop();
        }
    }

    /*
     * Setup the serial service
     */
    private void setupSerial() {

        Log.d(TAG, "setupSerial()");
        mSerialService = new BluetoothSerialService(this,mHandler);

    }


    public void connect(View view){

        pairedDevices = mAdapter.getBondedDevices();

        for(BluetoothDevice bt : pairedDevices) {
            if(bt.getName().toUpperCase().contains("EDISON"))
                mBTDevice = bt;
        }

        Log.d(TAG, "Address: " + mBTDevice.getAddress());
        Log.d(TAG, "Name: " + mBTDevice.getName());
        Log.d(TAG, "Name: " + filename);


        // Check out the UUIDs
        ParcelUuid[] uuids = mBTDevice.getUuids();
        for(ParcelUuid uuid: uuids) {

            Log.d(TAG, "UUID\t" + uuid.getUuid().toString());
        }

        mSerialService.connect(mBTDevice);

/*        try {
            BluetoothSocket Socket = mBTDevice.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));

            if (!Socket.isConnected()){
                Socket.connect();
                Log.d(TAG, "Connected");
            }

            outStream = Socket.getOutputStream();
            inStream = Socket.getInputStream();

            while(inStream.available()<1);

            String v  = read();
            statusLabel.setText("Version: " + v);
            Count.setText("Count: 0");

        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.d(TAG,"DEBUG: Couldn't create Socket.");
            e.printStackTrace();
        }*/
    }


    // COLLECT DATA
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void getData(View view) throws IOException {
        String msg = "GET_DATA";
        String data = "";

        if (outStream == null) {
            Log.d(TAG, "ERROR: Bluetooth not connected. Can't read data.");
            return;
        }
        // Wont respond until edison receives
        outStream.write(msg.getBytes());


        while(inStream.available()<1);

        polltime = System.currentTimeMillis();

        // Read data from stream
        data  += read();

        try {
            FileWriter fw  = new FileWriter(dataFile, true);
            fw.write(data,0,data.length());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        getDataFromString(data);

        statusLabel.setText("Data Retreived");

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

            times.add(Integer.parseInt(parts[0]));
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

    private Date getDateFromMillis(int millis) {

        return new Date();
    }
    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothSerialService.STATE_CONNECTED:
                            // Print the status
                            statusLabel.setText("Connected");
                            break;
                        case BluetoothSerialService.STATE_CONNECTING:
                            statusLabel.setText("Connecting...");
                            break;
                        case BluetoothSerialService.STATE_NONE:
                            statusLabel.setText("Not Connected");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    // WRITE ACTION
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    // READ ACTION RETURNS BUFFER
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != getParent()) {
                        Toast.makeText(getParent(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != getParent()) {
                        Toast.makeText(getParent(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

}


