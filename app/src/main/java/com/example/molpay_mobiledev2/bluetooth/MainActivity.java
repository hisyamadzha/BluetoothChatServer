package com.example.molpay_mobiledev2.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BluetoothService";
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final int REQUEST_ENABLE_BT = 1;
    BluetoothAdapter bluetoothAdapter;
    AcceptThread acceptThread;
    ConnectedThread connectedThread;

    Boolean IsConnected = false;
    Boolean IsPressed = false;
    EditText _send_data;
    TextView _view_data;
    Menu _menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        _send_data =findViewById(R.id.editText);
        _view_data = findViewById(R.id.textView);
        _view_data.setMovementMethod(new ScrollingMovementMethod());


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        this._menu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(IsPressed || bluetoothAdapter == null)return true;

        switch (item.getItemId()){
            case R.id.onConnection:
                if(!IsConnected){
                    if(acceptThread != null) {
                        acceptThread.cancel();
                        acceptThread = null;
                    }
                    acceptThread = new AcceptThread();
                    acceptThread.start();
                }else {
                    if(acceptThread != null){
                        acceptThread.cancel();
                        acceptThread = null;
                    }
                    if(connectedThread != null){
                        connectedThread.cancel();
                        connectedThread = null;
                    }
                    IsConnected = false;
                    runOnUiThread(()-> {
                        _view_data.append("\nDisconnected");
                        _menu.getItem(0).setIcon(android.R.drawable.button_onoff_indicator_off);
                    });
                }

                break;
            default:
                return true;
        }

        return(super.onOptionsItemSelected(item));
    }

    public void SendMessage(View v) {
        byte[] bytes = _send_data.getText().toString().getBytes(Charset.defaultCharset());
        if(bluetoothAdapter == null)return;
        if(!IsConnected){
            runOnUiThread(()-> _view_data.append("\nService device not connected"));
            return;
        }
        connectedThread.write(bytes);
    }

    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            // Create a new listening server socket
            try {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord("appname", MY_UUID_INSECURE);
            } catch (IOException e) {
                Log.e(TAG, "Socket's listen() method failed", e);
                runOnUiThread(()-> {
                    _view_data.append("\n");
                    _view_data.append(Html.fromHtml("<font color='#FF0000'>"+ e.getMessage()
                            + "</font>"));
                });
            }

            mmServerSocket = tmp;
        }

        public void run() {
            BluetoothSocket socket = null;

            try {
                IsPressed = true;
                // This is a blocking call and will only return on a
                // successful connection or an exception
                runOnUiThread(()-> _view_data.append("\nStart listening . . ."));
                socket = mmServerSocket.accept();
                runOnUiThread(()-> _view_data.append("\nConnected"));

                IsPressed = false;
                IsConnected = true;
                runOnUiThread(()->_menu.getItem(0).setIcon(android.R.drawable.button_onoff_indicator_on));

            } catch (IOException e) {
                Log.e(TAG, "Socket's accept() method failed", e);
                runOnUiThread(()-> {
                    _view_data.append("\n");
                    _view_data.append(Html.fromHtml("<font color='#FF0000'>"+ e.getMessage()
                            + "</font>"));
                });
                IsConnected = false;
                runOnUiThread(()-> {
                    _view_data.append("\nDisconnected");
                    _menu.getItem(0).setIcon(android.R.drawable.button_onoff_indicator_off);
                });
            }

            //talk about this is in the 3rd
            if (socket != null) {
                connected(socket);
            }
        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
                runOnUiThread(()-> {
                    _view_data.append("\n");
                    _view_data.append(Html.fromHtml("<font color='#FF0000'>"+ e.getMessage()
                            + "</font>"));
                });
            }
        }
    }

    private void connected(BluetoothSocket mmSocket) {
        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(mmSocket);
        connectedThread.start();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams; using temp objects because
            // member streams are final.
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating input stream", e);
                runOnUiThread(()-> {
                    _view_data.append("\n");
                    _view_data.append(Html.fromHtml("<font color='#FF0000'>"+ e.getMessage()
                            + "</font>"));
                });
            }
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when creating output stream", e);
                runOnUiThread(()-> {
                    _view_data.append("\n");
                    _view_data.append(Html.fromHtml("<font color='#FF0000'>"+ e.getMessage()
                            + "</font>"));
                });
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run(){
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                // Read from the InputStream
                try {
                    bytes = mmInStream.read(buffer);
                    final String incomingMessage = new String(buffer, 0, bytes);

                    Log.d(TAG, "InputStream: " + incomingMessage);
                    runOnUiThread(()-> {
                        _view_data.append("\n");
                        _view_data.append(Html.fromHtml("<font color='#00FF00'>"+ incomingMessage
                                + "</font>"));
                    });
                } catch (IOException e) {
                    IsConnected = false;
                    Log.d(TAG, "Input stream was disconnected", e);
                    runOnUiThread(()-> {
                        _view_data.append("\n");
                        _view_data.append(Html.fromHtml("<font color='#FF0000'>"+ e.getMessage()
                                + "</font>"));
                        _view_data.append("\nConnection lost");
                        _menu.getItem(0).setIcon(android.R.drawable.button_onoff_indicator_off);
                    });
                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            runOnUiThread(()-> {
                _view_data.append("\n");
                _view_data.append(Html.fromHtml("<font color='#0000FF'>"+ text
                        + "</font>"));
            });

            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error occurred when sending data", e);
                runOnUiThread(()-> {
                    _view_data.append("\n");
                    _view_data.append(Html.fromHtml("<font color='#FF0000'>"+ e.getMessage()
                            + "</font>"));
                });
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Could not close the connect socket", e);
                runOnUiThread(()-> {
                    _view_data.append("\n");
                    _view_data.append(Html.fromHtml("<font color='#FF0000'>"+ e.getMessage()
                            + "</font>"));
                });
            }
        }
    }
}
