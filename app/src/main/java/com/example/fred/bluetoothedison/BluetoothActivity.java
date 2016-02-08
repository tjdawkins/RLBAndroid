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
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    public static ArrayList<Integer> times = new ArrayList<>();
    public static ArrayList<Float> temp = new ArrayList<>();
    private String filename;
    private File dataDir;
    private File dataFile;
    Date date;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        Connect = (Button)findViewById(R.id.button1);
        On = (Button)findViewById(R.id.button2);
        Close = (Button)findViewById(R.id.button3);
        Version = (TextView)findViewById(R.id.text1);
        Count = (TextView)findViewById(R.id.text2);
        date = new Date();
        Adapter = BluetoothAdapter.getDefaultAdapter();
        DateFormat df = new SimpleDateFormat("MM-DD-yyyy");

        filename = "/data-" + df.format(date) + ".txt";
        dataDir =  getStorageFile("TemperatureData");
        dataFile = new File(dataDir,filename);

    }

    // COLLECT DATA
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void getData(View view) throws IOException {
        String msg = "ON";
        String data = "";

        if (outStream == null) {
            Log.d(TAG, "ERROR: Bluetooth not connected. Can't read data.");
            return;
        }
        // Wont respond until edison receives
        outStream.write(msg.getBytes());


        while(inStream.available()<1);

        data  += read();

        try {
            FileWriter fw  = new FileWriter(dataFile, true);
            fw.write(data,0,data.length());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        getDataFromString(data);

        Version.setText("Data Retreived");

    }


    public void connect(View view){
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
            BluetoothSocket Socket = Device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"));

            if (!Socket.isConnected()){
                Socket.connect();
                Log.d(TAG, "Connected");
            }

            outStream = Socket.getOutputStream();
            inStream = Socket.getInputStream();

            while(inStream.available()<1);

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

        for(String line : lines) {

            String parts[] = line.split(":");

            times.add(Integer.parseInt(parts[0]));
            temp.add(Float.parseFloat(parts[1]));
        }
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

