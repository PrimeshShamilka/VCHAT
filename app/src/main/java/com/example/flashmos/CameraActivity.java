package com.example.flashmos;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;

public class CameraActivity extends AppCompatActivity  implements CameraBridgeViewBase.CvCameraViewListener2 {
    private static final int PERMISSIONS_READ_CAMERA = 1;
    private static final String TAG = "MyCam";
    private Button capture_signal;
    private TextView msg_view;
    private List<Integer> signal = new ArrayList<Integer>();
    private List<Long> t = new ArrayList<Long>();
    private List<Long> intensity = new ArrayList<Long>();
    private List<Long> diffIntensity = new ArrayList<Long>();
    private List<Integer> bitStream = new ArrayList<Integer>();
    CameraBridgeViewBase cameraBridgeViewBase;
    BaseLoaderCallback baseLoaderCallback;
    boolean isCapture = false;
    long startedTime;

    Mat frame;
    int counter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.CAMERA},1);
        setContentView(R.layout.activity_camera);
        msg_view = findViewById(R.id.msg_view);
        cameraBridgeViewBase = (CameraBridgeViewBase) findViewById(R.id.CameraView);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setMaxFrameSize(200,200);
        cameraBridgeViewBase.setCvCameraViewListener(this);
        //System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        baseLoaderCallback = new BaseLoaderCallback(this) {
            @Override
            public void onManagerConnected(int status) {
                super.onManagerConnected(status);

                switch(status){

                    case BaseLoaderCallback.SUCCESS:
                        Log.d("Check","OpenCv enable view");
                        cameraBridgeViewBase.enableView();
                        break;
                    default:
                        super.onManagerConnected(status);
                        break;
                }


            }

        };
         capture_signal= findViewById(R.id.capture_signal);
        capture_signal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               if(!isCapture){
                   signal = new ArrayList<Integer>();
                   t = new ArrayList<Long>();
                   intensity = new ArrayList<Long>();


                   isCapture = true;
                   capture_signal.setText("Stop");

               }else{
                   isCapture = false;
//                   System.out.println(t);
//                   System.out.println(signal);
                   capture_signal.setText("Start");
                   t.set(0,new Long(0));
                    processSignal();
               }
            }
        });


    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraBridgeViewBase.setCameraPermissionGranted();  // <------ THIS!!!
                } else {
                    // permission denied
                }
                return;
            }
        }
    }
    private void processSignal(){
//        System.out.println(t);
//        System.out.println(intensity);
//        System.out.println(signal);
        System.out.println(t.size());
        System.out.println(intensity.size());
        diffIntensity = new ArrayList<Long>();
        diffIntensity.add((long)0);

        for(int i=0;i<t.size() && i<signal.size()-1;i++){
            diffIntensity.add((long)((intensity.get(i+1)-intensity.get(i))/(t.get(i+1)-t.get(i))));
        }
        System.out.println(diffIntensity.size());
        System.out.println(intensity);
        System.out.println(diffIntensity);
        ArrayList newSignal = new ArrayList<Long>();
        bitStream = new ArrayList<Integer>();
        long oneStartTime;
        long zeroStartTime = (long)0;
        int pulseWidth;
        int ind = 0;
        while(true){
            ind++;
            if(ind >= diffIntensity.size()){
                break;
            }
            if(diffIntensity.get(ind)>diffIntensity.get(ind-1) && diffIntensity.get(ind)>0){
                newSignal.add(1);
                oneStartTime = t.get(ind);
                if(!bitStream.isEmpty()){
                    pulseWidth = (int)(oneStartTime - zeroStartTime);
                    for(int i=0;i<Math.round((float)pulseWidth/500.0);i++){
                        bitStream.add(0);
                    }
                }

                while(true){
                    ind++;
                    if(ind == diffIntensity.size()){
                        break;
                    }
                    if(intensity.get(ind)==0 || intensity.get(ind)<10) {
                        zeroStartTime = t.get(ind);
                        pulseWidth = (int)(t.get(ind)-oneStartTime);
                        for(int i=0;i<Math.round((float)pulseWidth/500.0);i++){
                            bitStream.add(1);
                        }
                        newSignal.add(0);
                        break;


                    }
                    newSignal.add(1);

                }


            }else{
                newSignal.add(0);
            }
        }
        newSignal.add(0);


        System.out.println(newSignal);
        System.out.println(bitStream);
        decodeMessage(bitStream);

//        for(int i=0;i<t.size() && i<signal.size();i++){
//            System.out.println(String.format("%d = %d",t.get(i),signal.get(i)));
//        }
    }
    private void decodeMessage(List<Integer> bits){
        String msg = "";
        String charBits = "";
        for(int i =1;i<bits.size()-1;i++){
            charBits+= Integer.toString(bits.get(i));
            if(i%6==0 && i !=0){
                System.out.println(charBits);
                int charVal = Integer.parseInt(charBits,2);
                if(charVal>=10){
                    msg+= (char)(charVal-10+65);
                }else{
                    msg+= (char)(charVal+48);
                }

                charBits = "";
            }
        }
        msg_view.setText(msg);
        System.out.println(msg);
    }
    private void detectFlash(CameraBridgeViewBase.CvCameraViewFrame inputFrame){
//            Mat  new_frame = inputFrame.gray();
//            if(frame != null){
//                Core.absdiff(new_frame,frame,frame);
////                Imgproc.threshold(new_frame,new_frame,5,255,Imgproc.THRESH_BINARY);
//            }else{
//                frame = new_frame;
//            }
            frame = inputFrame.gray();

            Mat hist = new Mat();
            List<Mat> frames = new ArrayList<Mat>();
            frames.add(frame);
            float[] range = {0, 256}; //the upper boundary is exclusive
            MatOfFloat histRange = new MatOfFloat(range);
            Imgproc.calcHist(frames,new MatOfInt(0),new Mat(),hist,new MatOfInt(256),histRange,false);
            float[] histData = new float[(int) (hist.total() * hist.channels())];
            hist.get(0, 0, histData);
            int cutoff = 250;
            long sum = 0;
            for (int i =cutoff;i<256;i++){
                sum+= histData[i];
            }
            System.out.print("Intensity");
            System.out.print(sum);
            System.out.println();
            if(sum>50000){
                if(isCapture){
                            signal.add(1);
                        }
            }else{
                if(isCapture){
                    signal.add(0);
                }
            }
            if(isCapture){
                intensity.add(sum);
                if(t.isEmpty()){
                    t.add(System.currentTimeMillis()-startedTime);
                }else{
                    t.add(System.currentTimeMillis()-startedTime-t.get(0));
                }

            }


//            Imgproc.threshold(frame,frame,200,255,Imgproc.THRESH_BINARY);
//            Mat hierachy = new Mat();
//            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
//
//            Imgproc.findContours(frame,contours,hierachy,Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
//            boolean test = false;
//
//            if(contours.size()<10){
//                for (int i=0; i<contours.size();i++){
//                    double cont_area = Imgproc.contourArea(contours.get(i));
//                    if(cont_area>15000){
//                        System.out.println(cont_area);
//                        if(isCapture){
//                            signal.add(1);
//                        }
//                        break;
//                    }else{
//                        if(isCapture){
//                            signal.add(0);
//                        }
//                    }
//                }
//                if(contours.size()==0){
//                    if(isCapture){
//                        signal.add(0);
//                    }
//                }
//            }
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        System.out.println(counter);
        if (counter % 1 == 0){
            this.detectFlash(inputFrame);
        }
        if(counter==60){
            System.out.println("*************************************************");
            System.out.println(60.0/((System.currentTimeMillis()-startedTime)/1000.0));
        }

        counter = counter + 1;





        return frame;
    }


    @Override
    public void onCameraViewStarted(int width, int height) {
        startedTime = System.currentTimeMillis();
        Log.d("Check",String.format("Cam View Started %s * %s",width,height));

    }


    @Override
    public void onCameraViewStopped() {

    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!OpenCVLoader.initDebug()){
            Log.d("Check","OpenCv doesn’t configured successfully");
            Toast.makeText(getApplicationContext(),"There's a problem, yo!", Toast.LENGTH_SHORT).show();
        }

        else
        {
            Log.d("Check","OpenCv configured successfully");
            baseLoaderCallback.onManagerConnected(baseLoaderCallback.SUCCESS);
        }



    }

    @Override
    protected void onPause() {
        super.onPause();
        if(cameraBridgeViewBase!=null){

            cameraBridgeViewBase.disableView();
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (cameraBridgeViewBase!=null){
            cameraBridgeViewBase.disableView();
        }
    }
}