package nz.ac.auckland.zlu883.furoicontroltester;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by zhlucc on 12/11/2015.
 */
public class BluetoothManager {

    private static BluetoothManager Instance;
    private String logTag = "BluetoothManager";
    private UUID btUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String robotMacAddress_parrot = "00:12:12:27:20:53";
    private String robotMacAddress_home = "18:6D:99:00:00:00";
    private List<BluetoothListener> btListeners = new ArrayList<BluetoothListener>();

    public static BluetoothManager getInstance() {
        if (Instance == null) {
            Instance = new BluetoothManager();
        }
        return Instance;
    }

    private BluetoothDevice btDevice;
    private BluetoothSocket btSocket;
    private BtIoThread btThread;

    private BluetoothManager() {

        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            if (!btAdapter.isEnabled()) {
                // request enable bt
            }
            Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getAddress().equals(robotMacAddress_home)) {
                        btDevice = device;
                    }
                }
            }
        } else {
            //does not support bluetooth
        }
        btAdapter.cancelDiscovery();
    }

    public void registerListener(BluetoothListener l) {
        btListeners.add(l);
    }

    public void btConnect() {
        try {
            btSocket = btDevice.createRfcommSocketToServiceRecord(btUUID);
            btSocket.connect();
            btThread = new BtIoThread(btSocket);
            btThread.start();
        } catch (Exception e) {
            Log.e(logTag, e.toString());
        }
    }

    public void btDisconnect() {
        try {
            btSocket.close();
        } catch (Exception e) {
            Log.e(logTag, e.toString());
        }
    }

    private class BtIoThread extends Thread {

        private final BluetoothSocket btSocket;
        private final InputStream btInputStream;
        private final OutputStream btOutputStream;

        public BtIoThread(BluetoothSocket socket) {
            btSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (Exception e) {
                Log.e(logTag, e.toString());
            }

            btInputStream = tmpIn;
            btOutputStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];

            while (true) {
                try {
                    String s = "";
                    int bytesRead = btInputStream.read(buffer);
                    for (int i = 0; i < btListeners.size(); i++) {
                        btListeners.get(i).dataReceived(buffer, bytesRead);
                    }
                } catch (IOException e) {
                    Log.e(logTag, e.toString());
                    break;
                }
            }
        }

        public void send(byte[] command) {
            try {
                for (int i = 0; i < command.length; i++) {
                    Log.i("Cmd", String.format("%02X", command[i]));
                }
                btOutputStream.write(command);
                btOutputStream.flush();
                btOutputStream.flush();
                btOutputStream.flush();
            } catch (Exception e) {
                Log.e(logTag, e.toString());
            }
        }
    }

    public void btSend(byte[] command) {
        if (btThread != null) {
            btThread.send(command);
        }
    }

}
