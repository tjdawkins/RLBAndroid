package com.example.fred.bluetoothedison;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import java.io.IOException;
import java.util.ArrayList;

public class displayGraph extends AppCompatActivity {

    private BarGraphSeries<DataPoint> series;
    private ArrayList<String> testArray;
    private ArrayList<Integer> graphArray = new ArrayList<>();
    TextView Version;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_graph);

        Version = (TextView)findViewById(R.id.text1);
        BluetoothActivity blue = new BluetoothActivity();

        testArray = blue.getArray();
        Version.setText("List: " + testArray.get(0));

        //separateInteger(testArray.get(0));


        for(int i = 0; i < testArray.size(); i++)
        {
            separateInteger(testArray.get(i));
        }

        GraphView graph = (GraphView) findViewById(R.id.graphTest);
        series = new BarGraphSeries<DataPoint>();


        graph.addSeries(series);
        for(int i = 0; i < graphArray.size(); i++)
        {
            series.appendData(new DataPoint(i , graphArray.get(i)), true, graphArray.size());
        }

        /*Viewport viewport = graph.getViewport();
        viewport.setYAxisBoundsManual(true);
        viewport.setMaxX(5.0);
        viewport.setMinX(0.0);
        viewport.setScrollable(true);*/

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_display_graph, menu);
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

    public void separateInteger (String s){
        char[] charArray = s.toCharArray();

        String str;
        int result;
        int builderCheck = 0;
        int checkChar;

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < charArray.length; i++)
        {
            checkChar = charArray[i];
            if(Character.isWhitespace(charArray[i]) || (checkChar < 48 || checkChar > 57))
            {
                continue;
            }

            else
            {
                builderCheck++;
                sb.append(charArray[i]);
                if(i+1 >= charArray.length)
                {
                    if(builderCheck > 0)
                    {
                        str = sb.toString();
                        result = Integer.parseInt(str);
                        this.graphArray.add(result);
                    }
                }

                else if(Character.isWhitespace(charArray[i+1]) || (checkChar < 48 || checkChar > 57))
                {
                    str = sb.toString();
                    result = Integer.parseInt(str);
                    this.graphArray.add(result);
                    sb = new StringBuilder();
                    builderCheck = 0;
                }
            }
        }

    }


}