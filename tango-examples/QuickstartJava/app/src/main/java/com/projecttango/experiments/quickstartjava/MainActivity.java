/*
 * Copyright 2014 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.projecttango.experiments.quickstartjava;

import java.util.ArrayList;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.Tango.OnTangoUpdateListener;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoErrorException;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoOutOfDateException;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.projecttango.quickstartjava.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Main Activity for the Tango Java Quickstart. Demonstrates establishing a
 * connection to the {@link Tango} service and printing the {@link TangoPose}
 * data to the LogCat. Also demonstrates Tango lifecycle management through
 * {@link TangoConfig}.
 */
public class MainActivity extends Activity {
    private static final String TAG = MainActivity.class.getSimpleName();

//  DEBUG STRINGS
    private static final String sTranslationFormat = "Translation: %f, %f, %f";
    private static final String sRotationFormat = "Rotation: %f, %f, %f, %f";
    /*
    private static final String sTransY = "Current Location: %f";
    private static final String sRotAng = "Current Ang: %f";
    private static final String sRotAng2 = "Current Ang2: %f";
    */

    private double yCurr = 0.0;
    private double angCurr = 0.0;
    private double totalAng = 0.0;
    private double ang2Curr = 0.0;
    private double totalAng2 = 0.0;

/*  MORE DEBUGS
    private TextView mTranslationTextView;
    private TextView mRotationTextView;
    private TextView yCurrTextView;
    private TextView rotAngTextView;
    private TextView rot2AngTextView;

    */
    public WebView myWebView;

    private Tango mTango;
    private TangoConfig mConfig;
    private boolean mIsTangoServiceConnected;
    private boolean mIsProcessing = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    /* DEBUG
        mTranslationTextView = (TextView) findViewById(R.id.translation_text_view);
        mRotationTextView = (TextView) findViewById(R.id.rotation_text_view);
        yCurrTextView = (TextView) findViewById(R.id.ycurr_text_view);
        rotAngTextView = (TextView) findViewById(R.id.rotang_text_view);
        rot2AngTextView = (TextView) findViewById(R.id.rot2ang_text_view);
        */


        // Instantiate Tango client
        mTango = new Tango(this);

        // Set up Tango configuration for motion tracking
        // If you want to use other APIs, add more appropriate to the config
        // like: mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true)
        mConfig = mTango.getConfig(TangoConfig.CONFIG_TYPE_CURRENT);
        mConfig.putBoolean(TangoConfig.KEY_BOOLEAN_MOTIONTRACKING, true);

        myWebView = (WebView) findViewById(R.id.webview);

        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        //final MyJavaScriptInterface myJavaScriptInterface = new MyJavaScriptInterface(this);
        //myWebView.addJavascriptInterface(myJavaScriptInterface, "forward");

        myWebView.loadUrl("http://www.tunaroundandseacom.domain.com");

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Lock the Tango configuration and reconnect to the service each time
        // the app
        // is brought to the foreground.
        super.onResume();
        if (!mIsTangoServiceConnected) {
            startActivityForResult(
                    Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_MOTION_TRACKING),
                    Tango.TANGO_INTENT_ACTIVITYCODE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == Tango.TANGO_INTENT_ACTIVITYCODE) {
            // Make sure the request was successful
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this,
                        "This app requires Motion Tracking permission!",
                        Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            try {
                setTangoListeners();
            } catch (TangoErrorException e) {
                Toast.makeText(this, "Tango Error! Restart the app!",
                        Toast.LENGTH_SHORT).show();
            }
            try {
                mTango.connect(mConfig);
                mIsTangoServiceConnected = true;
            } catch (TangoOutOfDateException e) {
                Toast.makeText(getApplicationContext(),
                        "Tango Service out of date!", Toast.LENGTH_SHORT)
                        .show();
            } catch (TangoErrorException e) {
                Toast.makeText(getApplicationContext(),
                        "Tango Error! Restart the app!", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // When the app is pushed to the background, unlock the Tango
        // configuration and disconnect
        // from the service so that other apps will behave properly.
        try {
            mTango.disconnect();
            mIsTangoServiceConnected = false;
        } catch (TangoErrorException e) {
            Toast.makeText(getApplicationContext(), "Tango Error!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setTangoListeners() {
        // Select coordinate frame pairs
        ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<TangoCoordinateFramePair>();
        framePairs.add(new TangoCoordinateFramePair(
                TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE,
                TangoPoseData.COORDINATE_FRAME_DEVICE));

        // Add a listener for Tango pose data
        mTango.connectListener(framePairs, new OnTangoUpdateListener() {

            @SuppressLint("DefaultLocale")
            @Override
            public void onPoseAvailable(TangoPoseData pose) {
                Log.d("anything really","here");
                if (mIsProcessing) {
                    Log.i(TAG, "Processing UI");
                    return;
                }
                mIsProcessing = true;
                
                //Format Translation and Rotation data
                final String translationMsg = String.format(sTranslationFormat,
                        pose.translation[0], pose.translation[1],
                        pose.translation[2]);
                final String rotationMsg = String.format(sRotationFormat,
                        pose.rotation[0], pose.rotation[1], pose.rotation[2],
                        pose.rotation[3]);


                //Calculation for Translation along Y axis
                if ((pose.translation[1]-yCurr) > 1)
                {
                    /*post rotate move forward*/
                    yCurr += 1;
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            myWebView.evaluateJavascript("forward();", null);
                        }
                    });
                    //myWebView.loadUrl("javascript:forward()");
                    //myWebView.evaluateJavascript("forward();", null);
                }
                else if ((pose.translation[1]-yCurr) < -1) //if zCurr is < -2
                {
                    /*post move back*/
                    //myWebView.evaluateJavascript("backward();",null);
                    yCurr -= 1;
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            myWebView.evaluateJavascript("backward();", null);
                        }
                    });
                }
                //End Translation

                //Rotation Angle Calculation
                totalAng = Math.atan2(2.0*(
                        pose.rotation[0]*pose.rotation[1] + pose.rotation[3]*pose.rotation[2]),
                        pose.rotation[3]*pose.rotation[3] + pose.rotation[0]*pose.rotation[0]
                                - pose.rotation[1]*pose.rotation[1] - pose.rotation[2]*pose.rotation[2]);
                //Conversion to degrees
                totalAng = totalAng * (180/Math.PI);
                //Calculation for Rotation
                if ((totalAng-angCurr) > 15)
                {
                    /*post rotate right*/
                    angCurr += 15;
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            myWebView.evaluateJavascript("right();", null);
                        }
                    });
                }
                else if ((totalAng-angCurr) < -15) //if zCurr is < -2
                {
                    /*post rotate left*/
                    angCurr -= 15;
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            myWebView.evaluateJavascript("left();", null);
                        }
                    });
                }
                //End Rotation

                //Rotation Angle TWO Calculation
                totalAng2 = Math.atan2(2.0 * (
                                pose.rotation[1] * pose.rotation[2] + pose.rotation[3] * pose.rotation[0]),
                        pose.rotation[3] * pose.rotation[3] - pose.rotation[0] * pose.rotation[0]
                                - pose.rotation[1] * pose.rotation[1] + pose.rotation[2] * pose.rotation[2]);


                //Conversion to degrees
                totalAng2 = totalAng2 * (180/Math.PI) -90;
                //Calculation for Rotation
                if ((totalAng2-ang2Curr) > 15)
                {
                    /*post rotate right*/
                    ang2Curr += 15;
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            myWebView.evaluateJavascript("up();", null);
                        }
                    });
                }
                else if ((totalAng2-ang2Curr) < -15) //if zCurr is < -2
                {
                    /*post rotate left*/
                    ang2Curr -= 15;
                    Handler handler = new Handler(Looper.getMainLooper());
                    handler.post(new Runnable() {
                        public void run() {
                            myWebView.evaluateJavascript("down();", null);
                        }
                    });
                }
                //End Rotation

                /* DEBUG
                final String zMsg = String.format(sTransY,yCurr);
                final String aMsg = String.format(sRotAng,angCurr);
                final String aMsg2 = String.format(sRotAng2,ang2Curr);
                */


                // Output to LogCat
                String logMsg = translationMsg + " | " + rotationMsg;
                Log.i(TAG, logMsg);

                // Display data in TextViews. This must be done inside a
                // runOnUiThread call because
                // it affects the UI, which will cause an error if performed
                // from the Tango
                // service thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        /* debug
                        mTranslationTextView.setText(translationMsg);
                        mRotationTextView.setText(rotationMsg);
                        yCurrTextView.setText(zMsg);
                        rotAngTextView.setText(aMsg);
                        rot2AngTextView.setText(aMsg2);
                        */
                        mIsProcessing = false;
                    }
                });
            }

            @Override
            public void onXyzIjAvailable(TangoXyzIjData arg0) {
                // Ignoring XyzIj data
            }

            @Override
            public void onTangoEvent(TangoEvent arg0) {
                // Ignoring TangoEvents
            }

			@Override
			public void onFrameAvailable(int arg0) {
				// Ignoring onFrameAvailable Events
				
			}

        });
    }

}
