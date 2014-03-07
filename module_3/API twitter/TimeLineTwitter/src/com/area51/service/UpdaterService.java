package com.area51.service;

import java.util.ArrayList;

import com.area51.application.TwitterApplication;
import com.area51.db.DBHelper;
import com.area51.db.DBOperations;
import com.area51.model.Tweet;
import com.area51.utils.ConstantsUtils;
import com.area51.utils.TwitterUtils;


import android.app.Service;
import android.content.ContentValues;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class UpdaterService extends Service {
	
	private final String TAG = UpdaterService.class.getSimpleName();
	
	static final int DELAY = 60000;
	private boolean runFlag = false;
	private Updater updater;
	
	private TwitterApplication application;
	
	private DBOperations dbOperations;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		application = (TwitterApplication) getApplication();
		updater = new Updater();
		dbOperations = new DBOperations(this);
		Log.d(TAG, "onCreated");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		runFlag = false;
		application.setServiceRunningFlag(false);
		updater.interrupt();
		updater = null;
		
		Log.d(TAG, "onDestroyed");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		if(!runFlag){
			runFlag = true;
			application.setServiceRunningFlag(true);
			updater.start();
		}
		
		Log.d(TAG, "onStarted");
		return START_STICKY;
		
	}
	
	/**
	 * Thread that performs the actual update from twitter online service
	 */
	private class Updater extends Thread{
		
		private ArrayList<Tweet> timeline = new ArrayList<Tweet>();
		
		public Updater(){
			super("UpdaterService-UpdaterThread");
		}
		
		@Override
		public void run() {
			UpdaterService updaterService = UpdaterService.this;
			while (updaterService.runFlag) {
				Log.d(TAG, "UpdaterThread running");
				try{
					timeline = TwitterUtils.getTimelineForSearchTerm(ConstantsUtils.TWITTER_TERM);
					
					ContentValues values = new ContentValues();
					for(Tweet tweet : timeline){
						values.clear();
						values.put(DBHelper.C_ID, tweet.getId());
						values.put(DBHelper.C_NAME, tweet.getName());
						values.put(DBHelper.C_SCREEN_NAME, tweet.getScreenName());
						values.put(DBHelper.C_IMAGE_PROFILE_URL, tweet.getProfileImageUrl());
						values.put(DBHelper.C_TEXT, tweet.getText());
						values.put(DBHelper.C_CREATED_AT, tweet.getCreatedAt());
						
						Log.i(TAG, "CREATED AT_SERVICE: " + tweet.getCreatedAt());
						
						dbOperations.insertOrIgnore(values);
					}
					Thread.sleep(DELAY); 
				}catch(InterruptedException e){
					updaterService.runFlag = false;
					application.setServiceRunningFlag(true);
				}
				
			}
		}
	
	} 

}
