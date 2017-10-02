package com.e1gscom.helloglass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;

public class GBL
{	
	public static String TAG = "E1Barcode-TS";
		
	public static String httpResponseString = "";
	public static String bookTitle = "";
	public static String bookDescription = "";
	public static String bookImageUrl = "";
	public static String barcode = "";
	public static int perLine = 20;
	
	public static void ClearData() {
		httpResponseString = "";
		bookTitle = "";
		bookDescription = "";
		bookImageUrl = "";
		barcode = "";	
	}
	
	public static String GetBookInfoStr()
	{
		char[] cArray = bookDescription.toCharArray();
		int prev = 0; int curr = 0;
		while ((curr + perLine) < cArray.length) {
			curr += perLine;
			while ((curr > prev) && (cArray[curr] != ' ')) curr--;
			if (curr == prev) {
				curr += perLine;
				prev = curr;
			}
			else {
				cArray[curr] = '\n'; curr = curr+1; prev = curr;
			}
		}
		String infoStr = bookTitle + "\n\n" + String.valueOf(cArray);
		return infoStr;
	}
	
	public static String DoHTTPRequest(String request)
			throws MalformedURLException, IOException {
		Log.v(TAG, "HTTP Request: " + request);
		StringBuilder response = new StringBuilder();
		URL url = new URL(request);
		HttpURLConnection httpconn = (HttpURLConnection) url.openConnection();
		if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK) {
			BufferedReader input = new BufferedReader(new InputStreamReader(
					httpconn.getInputStream()), 8192);
			String strLine = null;
			while ((strLine = input.readLine()) != null) {
				response.append(strLine);
			}
			input.close();
		}
		httpResponseString = response.toString();
		return response.toString();
	}
	
	public static void ProcessResponse(String resp) 
               throws IllegalStateException, IOException, 
                      JSONException, NoSuchAlgorithmException
    {
		Log.v(TAG, "HTTP Response: ");
		bookTitle = "";
		bookDescription = "";
		bookImageUrl = "";
		JSONObject responseObject = new JSONObject(resp);
		JSONArray jitems = responseObject.getJSONArray("items");		
		if (jitems == null) {
			Log.e(TAG, "     NULL results on json items");
		} else {
			Log.v(TAG, "     number of items:" + jitems.length());
			for (int i = 0; i < jitems.length(); i++) {
				JSONObject jitem = jitems.getJSONObject(i);
				/*Iterator<?> keys = jitem.keys();
				while (keys.hasNext()){
					String key = (String) keys.next();
					if (key.equals("industryIdentifiers")) {
						return;
					}
				}*/
				JSONObject jvolumeInfo = jitem.getJSONObject("volumeInfo");
				JSONArray jindustryIndentifiers = 
				     jvolumeInfo.getJSONArray("industryIdentifiers");
				for (int j=0; j < jindustryIndentifiers.length(); j++) {
					String id = 
					        jindustryIndentifiers.getJSONObject(j).getString("identifier");
					if (id.equals(barcode)) {
						bookTitle = jvolumeInfo.getString("title");
						bookDescription = jvolumeInfo.getString("description");
						JSONObject jimageLinks = jvolumeInfo.getJSONObject("imageLinks");
						bookImageUrl = jimageLinks.getString("thumbnail");
						return;
					}
				}
			}
		}
	}
	
}

