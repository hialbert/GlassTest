package com.e1gscom.helloglass;

//import android.support.v7.app.ActionBarActivity;
import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.DisplayMetrics;
import android.util.Log;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.InputStream;

import com.google.android.glass.media.Sounds;

public class BookSearchActivity extends Activity 
                       implements GestureDetector.OnDoubleTapListener {
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
	    Toast.makeText(BookSearchActivity.this, msg, Toast.LENGTH_LONG).show();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
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
            	//if (barcode.equals(barcodeTextBuf) && !barcode.isEmpty())
            	//	finish();
                barcode = barcodeTextBuf;
                barcodeTextBuf = "";
                Log.d(TAG, "GET" + barcode);
                txtStatus.setText("Searching "+ barcode + "...");
                new GglAPISearcher().execute(barcode);
                return true;
			} else if ((keychar >= 32) && (keychar < 127)) {
				barcodeTextBuf = barcodeTextBuf + ((char) keychar);
				txtBarcode.setText(barcodeTextBuf);
				return true;
			}
            else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
        		long delta = event.getEventTime() - prevEventTime;
        		prevEventTime = event.getEventTime();
        		DbgMsg("Exiting ...", 0);         		
        		if (delta < 1500) {
        			mAudio.playSoundEffect(Sounds.DISMISSED);
        			finish();     
        		}
        		return true;
            }
        }
        return false;
    }

	public class GglAPISearcher extends AsyncTask<String, Integer, String> {
		static final String TAG = "GglAPISearch";

		String barcode = "";
		
		public GglAPISearcher() {
		      //this.bmImage = bmImage;
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
					txtStatus.setText("Url: " + GBL.bookImageUrl);
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
				  Log.e(TAG, "No image!!");
		          bmImage.setImageBitmap(result);
			  }
			  else
				  txtStatus.setText("Error: No image!!");
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
}
