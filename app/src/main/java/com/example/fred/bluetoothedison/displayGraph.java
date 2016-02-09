package com.example.fred.bluetoothedison;

import android.content.Intent;
import android.graphics.Path;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class displayGraph extends AppCompatActivity implements AdapterView.OnItemSelectedListener {

    private static final String TAG = "displayGraph";
    private ArrayList<Integer> graphArray = new ArrayList<>();
    File dataDir;
    File dataFiles[];
    ArrayList<String> fileNameStr;
    TextView Version;
    Spinner dataSelect;
    LineGraphSeries<DataPoint> series;
    DataPoint points[];
    GraphView graph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_graph);

        // Get the data directory from calling intent (BluetoothActivity)
        Intent intent = getIntent();
        String dataDirStr = intent.getStringExtra(BluetoothActivity.DATA_DIR);

        // Initialize Components from Resources
        Version = (TextView)findViewById(R.id.text1);
        graph = (GraphView) findViewById(R.id.graph);
        dataSelect = (Spinner)findViewById(R.id.dataSelectSpinner);
        // Set the listener
        dataSelect.setOnItemSelectedListener(this);

        // Get list of data files (dates)
        dataDir = new File(dataDirStr);
        dataFiles = dataDir.listFiles();
        fileNameStr = new ArrayList<String>();

        // Add file names in directory to arraylist
        for(File f : dataFiles) {
            Log.d(TAG,"f: " + f);
            Log.d(TAG,"Filename: " +  f.getName());

            fileNameStr.add(f.getName());

        }

        // Create ArrayAdapter to populate file chooser spinner
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_item,fileNameStr);
        dataSelect.setAdapter(spinnerAdapter);

    }



    // Options Menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_graph, menu);
        return true;
    }

    // Implement listeners for menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        //Get selected item for menu
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    // Impelement Listeners for Spinner Listeners
    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        // ActionListener

        String item = (String)parent.getItemAtPosition(pos);
        getDataPointsFromFile(item);

    }

    // Get the data points from selected file
    // populate the data points and show graph
    private void getDataPointsFromFile(String file) {

        // The selected file
        File f = new File(dataDir,file);
        // Array list to store the points
        ArrayList<DataPoint> pointArrayList = new ArrayList<>();
        String data;

        try {
            // Read the file out and drop the points in the arraylist
            BufferedReader b = new BufferedReader(new FileReader(f));
            while ((data = b.readLine()) != null) {

            String points[] = data.split(":");
            pointArrayList.add(new DataPoint(Double.parseDouble(points[0]),
                    Double.parseDouble(points[1])));

            }

            // Populate the data points with the array
            //this.points = (DataPoint[])pointArrayList.toArray();
            this.points = pointArrayList.toArray(new DataPoint[pointArrayList.size()]);

        } catch (IOException e) {
            e.printStackTrace();
        }

        // Set up the series with the data points
        series = new LineGraphSeries<DataPoint>(this.points);

        // Add it to the graph
        graph.addSeries(series);




    }

    // In case nothing was chosen.
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }



}