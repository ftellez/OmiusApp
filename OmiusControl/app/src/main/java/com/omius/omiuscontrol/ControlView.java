package com.omius.omiuscontrol;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

import android.provider.Settings;

import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
//import android.support.v7.app.AlertDialog;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.text.method.HideReturnsTransformationMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class ControlView extends AppCompatActivity {

    // Camera variables
    public static final int MY_PERMISSIONS_REQUEST_CAMERA = 100;
    public static final String ALLOW_KEY = "ALLOWED";
    public static final String CAMERA_PREF = "camera_pref";

    GraphView graph;
    private BluetoothAdapter mBtAdapter;
    private BluetoothSocket btSocket;
    private BluetoothServerSocket btSocketServer;
    public String BtAddress = null;
    private ConnectedThread mConnectedThread = null;
    final int handlerState = 0;
    // UUID service - This is the type of Bluetooth device that the BT module is
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    Handler bluetoothIn;

    private StringBuilder recDataString = new StringBuilder();
    String coord1 = null;
    String coord2 = null;
    String eraseSub;
    int lineEnding;
    DataPoint[] dataBattery = new DataPoint[]{new DataPoint(0, 0)};
    LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataBattery);
    int graphPoints = 0;

    public ArrayList<Double> Points = new ArrayList<Double>();

    //-------------------------------------------------
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_view);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        graph = (GraphView) findViewById(R.id.graph);
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        graph.addSeries(series);

        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {                                     //if message is what we want
                    String readMessage = (String) msg.obj;                                                                // msg.arg1 = bytes from connect thread
                    recDataString.append(readMessage);                                      //keep appending to string until ~
                    lineEnding = recDataString.indexOf("\r\n");
                    if (lineEnding > 0)
                        recDataString.replace(lineEnding,lineEnding+4,"");
                    int endOfLineIndex = recDataString.indexOf(",");                    // determine the end-of-line

                    if (coord1 == null) {
                        if (endOfLineIndex > 0) {                                           // make sure there data before ~
                            //String dataInPrint = recDataString.substring(0, endOfLineIndex);    // extract string
                            //int dataLength = dataInPrint.length();                          //get length of data received
                            String chk = recDataString.substring(0, endOfLineIndex);
                            if (chk.equals("0")) {
//                                Toast.makeText(getBaseContext(),chk,Toast.LENGTH_SHORT).show();
                                eraseSub = recDataString.substring(0, endOfLineIndex + 1);
                                int i = recDataString.indexOf(eraseSub);
                                if (i != -1) {
                                    try {
                                        recDataString.delete(i, eraseSub.length());                    //clear all string data
                                    } catch (StringIndexOutOfBoundsException ex) {
                                        Toast.makeText(getBaseContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            } else {
//                                Toast.makeText(getBaseContext(),chk,Toast.LENGTH_SHORT).show();
                                coord1 = chk;
                                eraseSub = recDataString.substring(0, endOfLineIndex + 1);
                                int i = recDataString.indexOf(eraseSub);
                                if (i != -1) {
                                    try {
                                        recDataString.delete(i, eraseSub.length());                    //clear all string data
                                    } catch (StringIndexOutOfBoundsException ex) {
                                        Toast.makeText(getBaseContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                    } else {
                        if (coord2 == null) {
                            if (endOfLineIndex > 0) {                                           // make sure there data before ~
                                String chk = recDataString.substring(0, endOfLineIndex);
//                                Toast.makeText(getBaseContext(),chk,Toast.LENGTH_SHORT).show();
                                coord2 = chk;
                                eraseSub = recDataString.substring(0, endOfLineIndex + 1);
                                int i = recDataString.indexOf(eraseSub);
                                if (i != -1) {
                                    try {
                                        recDataString.delete(i, eraseSub.length());                    //clear all string data
                                    } catch (StringIndexOutOfBoundsException ex) {
                                        Toast.makeText(getBaseContext(), ex.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                }
                                Points.add(Double.parseDouble(coord1));
                                Points.add(Double.parseDouble(coord2));
                                coord1 = null;
                                coord2 = null;
                                RefreshGraph();
                            }
                        }
                    }
                }
            }
        };

        // Camera
        Button cambtn = (Button)findViewById(R.id.CameraBtn);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            if (getFromPref(this, ALLOW_KEY)) {
                showSettingsAlert();
            } else if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    showAlert();
                } else {
                    ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA}, MY_PERMISSIONS_REQUEST_CAMERA );
                }
            }
        } else {
            cambtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openCamera();
                }
            });
        }
    }

    //---------------------------------------------------------

    // Camera Functions

    public static void saveToPreferences(Context context, String key, Boolean allowed) {
        SharedPreferences myPrefs = context.getSharedPreferences(CAMERA_PREF, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = myPrefs.edit();
        prefsEditor.putBoolean(key, allowed);
        prefsEditor.commit();
    }

    public static Boolean getFromPref(Context context, String key) {
        SharedPreferences myPrefs = context.getSharedPreferences(CAMERA_PREF, Context.MODE_PRIVATE);
        return (myPrefs.getBoolean(key, false));
    }

    private void showAlert() {
        AlertDialog alertDialog = new AlertDialog.Builder(ControlView.this).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage("App needs to access the Camera.");
        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "DONT ALLOW",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        finish();
                    }
                });

        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "ALLOW",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        ActivityCompat.requestPermissions(ControlView.this,
                                new String[]{Manifest.permission.CAMERA},
                                MY_PERMISSIONS_REQUEST_CAMERA);
                    }
                });
        alertDialog.show();
    }

    private void showSettingsAlert() {
        AlertDialog alertDialog = new AlertDialog.Builder(ControlView.this).create();
        alertDialog.setTitle("Alert");
        alertDialog.setMessage("App needs to access the Camera.");

        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "DONT ALLOW",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        //finish();
                    }
                });
        alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "SETTINGS",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        startInstalledAppDetailsActivity(ControlView.this);
                    }
                });
        alertDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_CAMERA: {
                for (int i = 0, len = permissions.length; i < len; i++) {
                    String permission = permissions[i];

                    if (grantResults[i] == PackageManager.PERMISSION_DENIED) {
                        boolean
                                showRationale =
                                ActivityCompat.shouldShowRequestPermissionRationale(
                                        this, permission);

                        if (showRationale) {
                            showAlert();
                        } else if (!showRationale) {
                            // user denied flagging NEVER ASK AGAIN
                            // you can either enable some fall back,
                            // disable features of your app
                            // or open another dialog explaining
                            // again the permission and directing to
                            // the app setting
                            saveToPreferences(ControlView.this, ALLOW_KEY, true);
                        }
                    }
                }
            }
            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    public static void startInstalledAppDetailsActivity(final Activity context) {
        if (context == null) { return; }

        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
        i.setData(Uri.parse("package:" + context.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(i);
    }

    private void openCamera() {
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        startActivity(intent);
    }
    //----------------------------------------------------------

    public void RefreshGraph() {
        DataPoint addedData = new DataPoint(Points.get(graphPoints), Points.get(graphPoints+1));
        series.appendData(addedData,true,25);
        graph.addSeries(series);
        graphPoints = graphPoints + 2;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_control_view, menu);
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

    @Override
    public void onResume() {
        super.onResume();
        if (btSocket == null) {
            ConnectDeviceBluetooth();
        } else {
            try {
                btSocket.close();
            } catch (IOException e4) {
                Toast.makeText(getBaseContext(), "ERROR - Could not close Bluetooth socket", Toast.LENGTH_SHORT).show();
            }
            ConnectDeviceBluetooth();
        }
    }

    public void ConnectDeviceBluetooth() {
        //It is best to check BT status at onResume in case something has changed while app was paused etc
        checkBTState();

        // Get a set of currently paired devices and append to pairedDevices list
        Set<BluetoothDevice> pairedDevices = mBtAdapter.getBondedDevices();

        // Initialize array adapter for paired devices
        final ArrayList<String> mPairedDevicesArrayAdapter = new ArrayList<String>();

        // Add previously paired devices to the array
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                mPairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
            }

            final AlertDialog dialog;
            final AlertDialog.Builder alertDialog = new AlertDialog.Builder(ControlView.this);
            LayoutInflater inflater = getLayoutInflater();
            View convertView = (View) inflater.inflate(R.layout.custom, null);
            alertDialog.setView(convertView);
            alertDialog.setTitle("Bluetooth Device List: ");
            ListView lv = (ListView) convertView.findViewById(R.id.listView1);
            final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mPairedDevicesArrayAdapter);
            lv.setAdapter(adapter);
            dialog = alertDialog.show();

            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                    Toast.makeText(getBaseContext(), "Connecting...", Toast.LENGTH_SHORT).show();
                    // Get the device MAC address, which is the last 17 chars in the View
                    String info = adapter.getItem(position);
                    String address = info.substring(info.length() - 17);
                    BtAddress = address;

                    // Set up a pointer to the remote device using its address.
                    BluetoothDevice device = mBtAdapter.getRemoteDevice(BtAddress);

                    //Attempt to create a bluetooth socket for comms
                    try {
                        btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
//                        btSocketInsecure = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                    } catch (IOException e1) {
                        Toast.makeText(getBaseContext(), "ERROR - Could not create Bluetooth socket", Toast.LENGTH_SHORT).show();
                    }

                    ConnectSocket(btSocket);

                    dialog.dismiss();
                }
            });
        } else {
            mPairedDevicesArrayAdapter.add("no devices paired");
        }
    }

    public void ConnectSocket(BluetoothSocket BtSocket) {
        // Establish the connection.
//          btSocketServer = BtSocket;


        try {
            Toast.makeText(getBaseContext(), "Conectando....", Toast.LENGTH_SHORT).show();
            BtSocket.connect();
        } catch (IOException e) {
            try {
                BtSocket.close();        //If IO exception occurs attempt to close socket
            } catch (IOException e2) {
                Toast.makeText(getBaseContext(), "ERROR - Could not close Bluetooth socket", Toast.LENGTH_SHORT).show();
            }
        }


        //When activity is resumed, attempt to send a piece of junk data ('x') so that it will fail if not connected
        // i.e don't wait for a user to press button to recognise connection failure
        if (mConnectedThread == null) {
            mConnectedThread = new ConnectedThread(btSocket);
            mConnectedThread.start();
        }


        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called
        //--------mConnectedThread.write("x");

        final AlertDialog addressConf = new AlertDialog.Builder(ControlView.this)
                .setTitle("Bluetooth Address")
                .setMessage("Connection Ready!")
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .show();
    }

    //method to check if the device has Bluetooth and if it is on.
    //Prompts the user to turn it on if it is off
    private void checkBTState() {
        // Check device has Bluetooth and that it is turned on
        mBtAdapter = BluetoothAdapter.getDefaultAdapter(); // CHECK THIS OUT THAT IT WORKS!!!
        if (mBtAdapter == null) {
            Toast.makeText(getBaseContext(), "Device does not support Bluetooth", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            if (!mBtAdapter.isEnabled()) {
                //Prompt user to turn on Bluetooth
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket ArrivingSocket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = ArrivingSocket.getInputStream();
                tmpOut = ArrivingSocket.getOutputStream();
            } catch (IOException e) {
                Toast.makeText(getBaseContext(), "ERROR: I/O Streams Failed.", Toast.LENGTH_SHORT).show();
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}