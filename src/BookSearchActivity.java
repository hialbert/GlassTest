package com.e1gscom.booksearch;

//import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.speech.tts.UtteranceProgressListener;
import android.util.DisplayMetrics;
import android.util.Log;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;

import com.e1gscom.helloglass.R;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.view.WindowUtils;

public class BookSearchActivity extends Activity 
             implements GestureDetector.OnDoubleTapListener, 
                        OnInitListener {
//public class BookSearchActivity extends ActionBarActivity
	static final String TAG = "MainActivity";
	public static String GoogAPIURL = "https://www.googleapis.com/books/v1/";

	EditText  txtBarcode;
	TextView  lblImage1;
	ImageView imgImage1;
	Button    btnDone;
	TextView  txtStatus;
	
	AudioManager mAudio;

	public void DbgMsg(String msg, int msec) {
		Toast.makeText(BookSearchActivity.this, msg,
						Toast.LENGTH_LONG).show();
		Toast.makeText(BookSearchActivity.this, msg,
						Toast.LENGTH_LONG).show();
		if (msec > 0) SystemClock.sleep(msec);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		GBL.at("OnCreate()");
		
		getWindow().requestFeature(WindowUtils.FEATURE_VOICE_COMMANDS);
		
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		int height = displayMetrics.heightPixels;
		int width = displayMetrics.widthPixels;
		if (width > height) {
		    setContentView(R.layout.booksearch_h);
		    GBL.perLine = 35;
		}
		else {
			setContentView(R.layout.booksearch_v);
			GBL.perLine = 20;
		}
		mAudio = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        txtBarcode = (EditText) findViewById(R.id.txtBarcode);
        txtBarcode.setText("");
        txtBarcode.setOnKeyListener(new EditText.OnKeyListener() {
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (interpretBarcodeEvent(view, keyCode, event))
                	return true;
                else
                	return false;
            }
        });
        
        lblImage1 = (TextView) findViewById(R.id.lblImage1);
        lblImage1.setText("no-image");
        imgImage1 = (ImageView) findViewById(R.id.imgImage1);
        imgImage1.setImageResource(R.drawable.no_image);
        
        txtStatus = (TextView) findViewById(R.id.txtStatus);
        txtStatus.setText("");
               
        String barcode = "9780812988406";
        new GglAPISearcher().execute(barcode);
	}

	@Override
	public void onDestroy() {
	    if (mTTS != null) {
	        mTTS.stop();
	        mTTS.shutdown();
            mTTS = null;
	    }
	    super.onDestroy();
	}

	@Override
	public void onPause() {
	    if (mTTS != null) {
	        mTTS.stop();
	        mTTS.shutdown();
            mTTS = null;
	    }
	    super.onPause();
	}
	
	TextToSpeech mTTS = null;
	private boolean mTTSOn = false;
	public boolean playAudioOnDescription()
	{
		if (mTTSOn) {
			txtStatus.setText("Reading text cancelled");
			mTTS.stop();
	        mTTS.shutdown();
			mTTS = null;
			mTTSOn = false;
		}
        else {
        	txtStatus.setText("Translating text ...");
			mTTSOn = true;
			mTTS = new TextToSpeech(this, this);
			mTTS.setOnUtteranceProgressListener(
					new UtteranceProgressListener() {
				@Override
				public void onDone(String utteranceId) {
					Log.w(TAG, "onDone");
					if (mTTS != null) {
						mTTS.stop();
						mTTS.shutdown();
						mTTS = null;
						mTTSOn = false;
					}
				}
				@Override
				public void onError(String utteranceId) {
					Log.w(TAG, "onError");
				}
				@Override
				public void onStart(String utteranceId) {
					Log.w(TAG, "onStart");
				}
			});
		}
		return mTTSOn;
	}
	@Override
	public void onInit(int status) {
		if (status == TextToSpeech.SUCCESS) {
			int result = mTTS.setLanguage(Locale.US);
			if (result == TextToSpeech.LANG_MISSING_DATA || 
				result == TextToSpeech.LANG_NOT_SUPPORTED) {
				txtStatus.setText("Reading text error");
				Log.e("TTS", "This Language is not supported");
			} else {
				txtStatus.setText("Reading text ...");
				mTTS.speak(GBL.bookDescription,
						   TextToSpeech.QUEUE_FLUSH, null);
			}
		} else {
			txtStatus.setText("Reading text error");
			Log.e("TTS", "Initilization Failed!");
		}
	}    
	
	private String barcode = "b a r c o d e";
    private String barcodeTextBuf = "";
	private long prevEventTime = 0;
    public boolean interpretBarcodeEvent(View view, 
    		                int keyCode, KeyEvent event)
    {
        if (event == null) return false;		
        if (event.getAction() == KeyEvent.ACTION_UP) {       	
        	int keychar = event.getUnicodeChar();
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
            	if (barcode.equals(barcodeTextBuf) && !barcode.isEmpty())
            		finish();
                barcode = barcodeTextBuf;
                //String barcode = txtBarcode.getText().toString();
                barcodeTextBuf = "";
                Log.d(TAG, "GET" + barcode);
                txtStatus.setText("Searching "+ barcode + ".");
                new GglAPISearcher().execute(barcode);
                return true;
			} else if ((keychar >= 32) && (keychar < 127)) {
				barcodeTextBuf = barcodeTextBuf + ((char) keychar);
				txtBarcode.setText(barcodeTextBuf);
				return true;
			}
            else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            	GBL.at("OnKey()");
        		long delta = event.getEventTime() - prevEventTime;
        		prevEventTime = event.getEventTime();
        		//DbgMsg("BarCode Event " + event + " " + delta, 0);         		
        		if (delta < 1500) {
        			mAudio.playSoundEffect(Sounds.DISMISSED);
        			stopService(new Intent(this, AppService.class));
        			finish();
        		}
        		else {
        			mAudio.playSoundEffect(Sounds.TAP);
        			playAudioOnDescription();
        			//Intent menuIntent = new Intent(this, MenuActivity.class);
        			//startActivityForResult(menuIntent, 0);
        		}
        		return true;
            }
        }
        return false;
    }

	public class GglAPISearcher extends AsyncTask<String, Integer, String> {
		static final String TAG = "GglAPISearch";

		String barcode = "";
		//ImageView bmImage;
		
		public GglAPISearcher() {
			txtStatus.setText("Searching "+ barcode + "...");
	    }
		protected String doInBackground(String... args) {
			SystemClock.sleep(2000);
			GBL.barcode = args[0];
			try {

				String theUrl = GoogAPIURL + "volumes?q=" + GBL.barcode;
			    String bcRead = GBL.DoHTTPRequest(theUrl);
                Log.v(TAG, "Barcode Read: "+ bcRead);
                return bcRead;
			} catch(Exception e){
				Log.e(TAG, "Error: " + e.getMessage());
				e.printStackTrace();
				return null;
			}
		}
		protected void onProgressUpdate(Integer ...values) {}
		protected void onPostExecute(String result) {
			if (result == null) {
				txtStatus.setText("Error: query result null");
				return;
			}
			try{
				GBL.ProcessResponse(result);
				//GBL.bookImageUrl = 
				//"http://books.google.com/books/content?id=XtcyDwAAQBAJ&"+
				//"printsec=frontcover&img=1&zoom=1&edge=curl&source=gbs_api";
				if (GBL.bookImageUrl!=null && !GBL.bookImageUrl.isEmpty()){
					lblImage1.setText(GBL.GetBookInfoStr());
					//txtStatus.setText("Url: " + GBL.bookImageUrl);					
					new ImageDownloader(imgImage1).execute(GBL.bookImageUrl);
				} else {
					Log.e(TAG,"Error/ProcessResponse: null bookImageUrl");
				}
			}catch(Exception e){
				txtStatus.setText("Error: process response fail");
				Log.e(TAG,"Error/ProcessResponse:" + e.getMessage());
		        e.printStackTrace();
			}
		}
	}
	
	public class ImageDownloader extends AsyncTask<String, Integer, Bitmap> {
		  static final String TAG = "ImageDownloader";
		  ImageView bmImage;
		  public ImageDownloader(ImageView bmImage) {
			  txtStatus.setText("Displaying image ...");
		      this.bmImage = bmImage;
		  }
		  protected Bitmap doInBackground(String... urls) {
		      String urldisplay = urls[0];
		      Bitmap mImg = null;
		      try {
		          InputStream in = new java.net.URL(urldisplay).openStream();
		          mImg = BitmapFactory.decodeStream(in);
		      } catch (Exception e) {
		          Log.e(TAG, "Error" + e.getMessage());
		          e.printStackTrace();
		      }
		      return mImg;
		  }
		  protected void onProgressUpdate(Integer ...values) {}
		  protected void onPostExecute(Bitmap result) {
			  if (result != null) {
			      txtStatus.setText("Image displayed.");
		          bmImage.setImageBitmap(result);
			  }
			  else
				  txtStatus.setText("Error: No image");
		  }
    }

	@Override
	public boolean onSingleTapConfirmed(MotionEvent e) {
		DbgMsg("onSingleTapConfirmed", 0);
		finish();
		return false;
	}

	@Override
	public boolean onDoubleTap(MotionEvent e) {
		DbgMsg("onDoubleTap", 0);
		finish();
		return false;
	}

	@Override
	public boolean onDoubleTapEvent(MotionEvent e) {
		DbgMsg("onDoubleTapEvent", 0);
        finish();
		return true;
	}

    @Override
    public boolean onCreatePanelMenu(int featureId, Menu menu) {
        if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
            getMenuInflater().inflate(R.menu.main, menu);
            return true;
        }
        // Pass through to super to setup touch menu.
        return super.onCreatePanelMenu(featureId, menu);
    }

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		if (featureId == WindowUtils.FEATURE_VOICE_COMMANDS) {
			switch (item.getItemId()) {
			case R.id.read:
				playAudioOnDescription();
				return true;
			case R.id.stop:
				stopService(new Intent(this, AppService.class));
				finish();
				return true;		
			default:
				return true;
			}
		}
        return super.onMenuItemSelected(featureId, item);
	}
	
/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	*/
}
