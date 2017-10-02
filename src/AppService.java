package com.e1gscom.booksearch;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.google.android.glass.timeline.LiveCard;
import com.google.android.glass.timeline.LiveCard.PublishMode;

public class AppService extends Service {
    private static final String TAG = "AppService";
    private static final String LIVE_CARD_ID = "BookSearch";
    
    private AppRenderer mCallback;
    private LiveCard mLiveCard;

    @Override
    public void onCreate() {
    	Log.e(TAG, "onCreate");
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
    	Log.e(TAG, "onBind");
        return null;
    }

    private void createBasicLiveCard() {
        mLiveCard = new LiveCard(this, LIVE_CARD_ID);            
        // Keep track of the callback to remove it before unpublishing.
        mCallback = new AppRenderer(this);
        mLiveCard.setDirectRenderingEnabled(true).getSurfaceHolder().addCallback(mCallback);
            
        //Intent menuIntent = new Intent(this, MenuActivity.class);
        //mLiveCard.setAction(PendingIntent.getActivity(this, 0, menuIntent, 0));
        //mLiveCard.publish(PublishMode.REVEAL);
        Intent bsIntent = new Intent(this, BookSearchActivity.class);
        mLiveCard.setAction(PendingIntent.getActivity(this, 0, bsIntent, 0));
        mLiveCard.publish(PublishMode.REVEAL);
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    	Log.e(TAG,  "onStartCommand");
        if (mLiveCard == null) {
            Log.e(TAG, "onStartCommand: true");
            createBasicLiveCard();
            Log.e(TAG, "Done publishing LiveCard");
        } 
        else {
        	Log.e(TAG, "onStartCommand: false");
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.e(TAG, "OnDestroy()");
    	
        if (mLiveCard != null && mLiveCard.isPublished()) {
            Log.e(TAG, "OnDestroy: true");
            if (mCallback != null) {
                mLiveCard.getSurfaceHolder().removeCallback(mCallback);
            }
            mLiveCard.unpublish();
            mLiveCard = null;
        }
        else {
            Log.e(TAG, "OnDestroy: false");
        }
        super.onDestroy();
    }
}