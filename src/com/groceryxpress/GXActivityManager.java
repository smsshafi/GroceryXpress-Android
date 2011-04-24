package com.groceryxpress;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.util.Log;

public class GXActivityManager extends Activity {
	private static SharedPreferences preferences;
	private static JSONArray shoppingListJSONArray = null;
	
	public synchronized static String getUserId(final Context context) {
		return GXActivityManager.getPreferences(context).getString("userid", "0");
	}
	
	public synchronized static void setUserId(final Context context, final String userid) {
		final Editor prefEditor = GXActivityManager.getPreferences(context).edit();
		prefEditor.putString("userid", userid);
		prefEditor.commit();
	}
	
	public synchronized static SharedPreferences getPreferences( final Context context ) {
		if ( GXActivityManager.preferences == null ) {
			GXActivityManager.preferences =
				context.getSharedPreferences( "GXPreferences", Context.MODE_PRIVATE );
		}

		return GXActivityManager.preferences;
	}
	
	public synchronized static boolean isShoppingListUpToDate(final Context context) {
		SharedPreferences sp = getPreferences(context);
		return sp.getBoolean("isShoppingListUpToDate", false);
	}
	
	public synchronized static void setShoppingListUpToDate(final Context context, boolean bool) {
		final Editor prefEditor = getPreferences(context).edit();
		prefEditor.putBoolean("isShoppingListUpToDate", bool);
		prefEditor.commit();
	}
	
	public synchronized static boolean haveQuantitiesChanged(final Context context) {
		return GXActivityManager.getPreferences(context).getBoolean("haveQuantitiesChanged", true);
	}
	
	public synchronized static void setQuantitiesChanged(final Context context, boolean bool) {
		final Editor prefEditor = GXActivityManager.getPreferences(context).edit();
		prefEditor.putBoolean("haveQuantitiesChanged", bool);
		prefEditor.commit();
	}
	
	public synchronized static JSONArray getShoppingListJSONArray(final Context context) {
		return getShoppingListJSONArray(context, null, null);
	}
	
	public synchronized static JSONArray getShoppingListJSONArray(final Context context, final String product_id, final String quantity) {
		Log.d("gx", "Begin Updating shopping list");
		if (isShoppingListUpToDate(context) && shoppingListJSONArray != null && !haveQuantitiesChanged(context)) {
			Log.d("gx", "Completed Updating shopping list");
			return shoppingListJSONArray;
		} else {
			HttpClient httpclient = new DefaultHttpClient();
		    HttpPost httppost;

		    try {
		        // Add your data
		        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		        nameValuePairs.add(new BasicNameValuePair("userid", GXActivityManager.getUserId(context)));
		        if (product_id != null && quantity != null) {
		        	nameValuePairs.add(new BasicNameValuePair("product_id", product_id));
		        	nameValuePairs.add(new BasicNameValuePair("quantity", quantity));
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
		        shoppingListJSONArray = new JSONArray(stream.toString());
		        setShoppingListUpToDate(context, true);
		        setQuantitiesChanged(context, false);
		        Log.d("gx", "Completed Updating shopping list");
		        return shoppingListJSONArray;
		        
		    } catch (JSONException e) {
		    	e.printStackTrace();
		    } catch (IOException e) {
		    	e.printStackTrace();
		    }
		    
		    return null;
		}
	}
}
