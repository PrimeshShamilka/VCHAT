package com.example.flashmos;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.content.DialogInterface;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.BeaconTransmitter;

import java.util.Arrays;
import java.util.UUID;


public class MainActivity extends AppCompatActivity {

    private Button open_camera;
    private Button ble_btn;
    private Button blink_flash;
    private EditText freq_input;
    private EditText bit_input;
    private EditText message_input;
    private Button send_btn;
    private MyFirebaseMessagingService msg_service;
    private CameraManager cameraManager;
    private  boolean isOn = false;
    private String uuid ;
    private FirebaseAuth mAuth;
    FirebaseFirestore db;
    User usr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> msgtask) {
                        if (!msgtask.isSuccessful()) {
                            Log.w("FIREBASE_MSG", "Fetching FCM registration token failed", msgtask.getException());
                            return;
                        }

                        // Get new FCM registration token
                        FirebaseUser firebaseUser = mAuth.getCurrentUser();
                        String uid = firebaseUser.getUid();
                        String displayName = firebaseUser.getDisplayName();
                        DocumentReference docRef = db.collection("users").document(uid);
                        docRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if (task.isSuccessful()) {
                                    DocumentSnapshot document = task.getResult();
                                    if (document.exists()) {
                                        Log.d("firebase", "DocumentSnapshot data: " + document.getData());
                                        usr = document.toObject(User.class);
                                        createBeacon(usr.uuid);
                                    } else {
                                        Log.d("firebase", "No such document");
                                        String token = msgtask.getResult();
                                        Log.d("firebase","NULL");
                                        uuid = generateUUID();
                                        usr = new User(uid,uuid,token,displayName);
                                        docRef.set(usr);
                                        createBeacon(uuid);
                                    }
                                } else {
                                    Log.d("firebase", "get failed with ", task.getException());
                                }
                            }
                        });


                    }
                });
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.BLUETOOTH_SCAN},2);
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},3);
        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.BLUETOOTH},4);
        open_camera = findViewById(R.id.open_camera);
        freq_input = findViewById(R.id.freq_text);
        bit_input = findViewById(R.id.bit_text);
        message_input = findViewById(R.id.message_text);
        send_btn = findViewById(R.id.send_btn);
        bit_input.setText("0100010101010010");
        ble_btn = findViewById(R.id.ble_btn);

        open_camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, CameraActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });
        ble_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, BleActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            }
        });
        send_btn.setOnClickListener(new View.OnClickListener(){

            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                int delay= 500;
                String myString = "";
                String bitStream = "01";
                try{
                    myString = message_input.getText().toString();
                    System.out.println(myString);
                    int c;
                    String bitc;
                    for(int j = 0; j<myString.length();j++){
                        c = (int)myString.charAt(j);
                        if((c-65)>=0 && ((c-65)<26)){
                            bitc = String.format("%6s", Integer.toBinaryString(c-65+10)).replace(" ","0");
                            bitStream+=bitc;
                        }else if((c-48)>=0 && ((c-48)<26)){
                            bitc = String.format("%6s", Integer.toBinaryString(c-48)).replace(" ","0");
                            bitStream+=bitc;
                        }

                    }

                }catch (Exception e){
                    e.printStackTrace();
                }
                bitStream +="10";
                System.out.println(bitStream);
                for(int i=0; i<bitStream.length();i++){
                    if(bitStream.charAt(i)=='1'){
                        try {
                            cameraManager.setTorchMode("0",true);
                            isOn = true;
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }else{
                        try {
                            cameraManager.setTorchMode("0",false);
                            isOn = false;
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        blink_flash = findViewById(R.id.blink_flash);
        blink_flash.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                int delay = 200;
                try{
                    delay = Integer.parseInt(freq_input.getText().toString());
                }catch (Exception e){
                    e.printStackTrace();
                }
                String myString = "0100010101010010";
                try{
                    myString = bit_input.getText().toString();
                }catch (Exception e){
                    e.printStackTrace();
                }
                Log.d("Check","Flash Button");


                for(int i=0; i<myString.length();i++){
                    if(myString.charAt(i)=='1'){
                        try {
                            cameraManager.setTorchMode("0",true);
                            isOn = true;
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }else{
                        try {
                            cameraManager.setTorchMode("0",false);
                            isOn = false;
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }

//                if(!isOn){
//
//                    try {
//                        cameraManager.setTorchMode("0",true);
//                        isOn = true;
//                    } catch (CameraAccessException e) {
//                        e.printStackTrace();
//                    }
//                }else{
//                    try {
//                        cameraManager.setTorchMode("0",false);
//                        isOn = false;
//                    } catch (CameraAccessException e) {
//                        e.printStackTrace();
//                    }
//                }
            }
        });



    }
    private String generateUUID(){
        return UUID.randomUUID().toString();
    }
    private void createBeacon(String uuid){
//        uuid =  generateUUID();
        Beacon beacon = new Beacon.Builder()
                .setId1(uuid)
                .setId2("10")
                .setId3("1")
                .setManufacturer(0x0118) // Radius Networks.  Change this for other beacon layouts
                .setTxPower(-59)
                .setDataFields(Arrays.asList(new Long[] {0l})) // Remove this for beacon layouts without d: fields
                .build();
// Change the layout below for other beacon types
        BeaconParser beaconParser = new BeaconParser()
                .setBeaconLayout("m:2-3=beac,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25");
        BeaconTransmitter beaconTransmitter = new BeaconTransmitter(getApplicationContext(), beaconParser);
        beaconTransmitter.startAdvertising(beacon, new AdvertiseCallback() {

            @Override
            public void onStartFailure(int errorCode) {
                Log.e("BLE_BEACON", "Advertisement start failed with code: "+errorCode);
            }

            @Override
            public void onStartSuccess(AdvertiseSettings settingsInEffect) {
                Log.i("BLE_BEACON", "Advertisement start succeeded.");
            }
        });
    }


}