package com.groceryxpress.asynctasks;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.groceryxpress.GXActivityManager;

public abstract class GetShoppingListAsyncTask extends AsyncTask<String, Void, Void> {
	
	private Context context;
	
	public GetShoppingListAsyncTask(Context context) {
		this.context = context;
	}
	
	@Override
	protected Void doInBackground(String... params) {
		// Create a new HttpClient and Post Header
	    HttpClient httpclient = new DefaultHttpClient();
	    HttpPost httppost;

	    try {
	        // Add your data
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
	        nameValuePairs.add(new BasicNameValuePair("userid", params[0]));
	        String product_id = null;
	        int new_quantity = -1;
	        try {
	        	product_id = params[1];
	        	new_quantity = Integer.valueOf(params[2]);
	        	nameValuePairs.add(new BasicNameValuePair("product_id", params[1]));
	        	nameValuePairs.add(new BasicNameValuePair("quantity", params[2]));
	        } catch (IndexOutOfBoundsException e){
	        }
	        
	        if (product_id != null && new_quantity != -1) {
	        	httppost = new HttpPost("http://www.groceryxpress.net/api/updateproductquantityinshoppinglist.php"); 
	        } else {
	        	httppost = new HttpPost("http://www.groceryxpress.net/api/shoppinglist.php");
	        }
	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

	        // Execute HTTP Post Request
	        HttpResponse response = httpclient.execute(httppost);
	        
	        Log.d("gx", "Fetching shopping list: " + httppost.getURI());
	        InputStream ips  = response.getEntity().getContent();
	        StringBuffer stream = new StringBuffer();
	        byte[] b = new byte[4096];
	        for (int n; (n = ips.read(b)) != -1;) {
	        	stream.append(new String(b, 0, n));
	        }

	        ips.close();
	        
	        Editor prefEditor = GXActivityManager.getPreferences(context).edit();
	        prefEditor.putString("shopping_list", stream.toString());
	        prefEditor.commit();
	        
	    } catch (ClientProtocolException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	    	((Activity)context).runOnUiThread(new Runnable() {
				
				public void run() {
					Toast.makeText(context, "Please check your data connection.", Toast.LENGTH_LONG).show();
				}
			});
	    	
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
		return null;
	}
	
}
