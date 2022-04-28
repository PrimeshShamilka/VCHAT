package com.example.flashmos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;


import androidx.core.app.ActivityCompat;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.RecyclerView;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import android.provider.Settings.Secure;

import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;


public class BleActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothLeScanner bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
    private Button scan_btn ;
    private ListView lstview;
    private String[] listData = {"Test","Hello"};
    private boolean scanning = false;
    private Handler handler = new Handler();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private CollectionReference userCol = db.collection("users");
    private LeDeviceListAdapter leDeviceListAdapter;
    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;


    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bluetoothAdapter.setName("Blah");
        setContentView(R.layout.activity_ble);
        scan_btn = findViewById(R.id.StartScanButton);
        leDeviceListAdapter = new LeDeviceListAdapter();
        lstview = findViewById(R.id.ble_list);
        lstview.setAdapter(leDeviceListAdapter);
        scan_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanLeDevice();
            }
        });



    }


    private void scanLeDevice() {
        if (!scanning) {
            // Stops scanning after a predefined scan period.
            Log.d("BLE_SCAN", "Scanning");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {

                    scanning = false;
                    if (ActivityCompat.checkSelfPermission(BleActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                        Log.d("BLE_SCAN", "Request Permissions");
                        ActivityCompat.requestPermissions(BleActivity.this,new String[]{Manifest.permission.BLUETOOTH_SCAN},2);
                        ActivityCompat.requestPermissions(BleActivity.this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},3);
                        ActivityCompat.requestPermissions(BleActivity.this,new String[]{Manifest.permission.BLUETOOTH},4);
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    Log.d("BLE_SCAN", "Stop Scanning");
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
            }, SCAN_PERIOD);

            scanning = true;
            Log.d("BLE_SCAN", "Start Scanning");
            bluetoothLeScanner.startScan(leScanCallback);
        } else {
            scanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanFailed(int errorCode) {
                    super.onScanFailed(errorCode);
                    Log.d("BLE_SCAN", "Scan Failed");
                    Log.d("BLE_SCAN", Integer.toString(errorCode));
                }
                @RequiresApi(api = Build.VERSION_CODES.N)
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);

                    leDeviceListAdapter.addDevice(result.getDevice());
                    leDeviceListAdapter.notifyDataSetChanged();
                    System.out.println("ONSCANRESULT");
                    Log.d("BLE_SCAN", result.getDevice().getAddress());
                }
            };

        private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private final LayoutInflater mInflator;
        public LeDeviceListAdapter() {

            super();
            Log.d("BLE_VIEW", "Le Adapter");
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = BleActivity.this.getLayoutInflater();
        }
        @RequiresApi(api = Build.VERSION_CODES.N)
        @SuppressLint("MissingPermission")
        public void addDevice(BluetoothDevice device) {
            if(!mLeDevices.contains(device)) {
                ParcelUuid[] uuids = device.getUuids();
                if(uuids !=null){
//                    userCol.whereEqualTo("uuid")
                    mLeDevices.add(device);
                }

            }
        }
        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }
        public void clear() {
            mLeDevices.clear();
        }
        @Override
        public int getCount() {
            return mLeDevices.size();
        }
        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }
        @Override
        public long getItemId(int i) {
            return i;
        }
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.ble_item, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.blei_addr);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.blei_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }
            BluetoothDevice device = mLeDevices.get(i);
            @SuppressLint("MissingPermission") final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("Unknown");
            viewHolder.deviceAddress.setText(device.getAddress());
            return view;
        }
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}