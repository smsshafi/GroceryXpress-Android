package com.groceryxpress;

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
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ListView;

import com.groceryxpress.adapters.ShoppingListAdapter;

public class ShoppingListActivity extends Activity {
	
	private String userid;
	private ProgressDialog progressDialog;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.shopping_list);
		
		userid = GXActivityManager.getPreferences(ShoppingListActivity.this).getString("userid", "0");
		new GetShoppingList().execute(userid);
	}
	
	public class GetShoppingList extends AsyncTask<String, Void, ShoppingListAdapter> {
		
		

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = ProgressDialog.show(ShoppingListActivity.this, "", "Fetching your shopping list...", true);
		}
		
		@Override
		protected ShoppingListAdapter doInBackground(String... userid) {
			// Create a new HttpClient and Post Header
    	    HttpClient httpclient = new DefaultHttpClient();
    	    HttpPost httppost = new HttpPost("http://www.groceryxpress.net/api/shoppinglist.php");

    	    try {
    	        // Add your data
    	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
    	        nameValuePairs.add(new BasicNameValuePair("userid", userid[0]));
    	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

    	        // Execute HTTP Post Request
    	        HttpResponse response = httpclient.execute(httppost);
    	        
    	        InputStream ips  = response.getEntity().getContent();
    	        StringBuffer stream = new StringBuffer();
    	        byte[] b = new byte[4096];
    	        for (int n; (n = ips.read(b)) != -1;) {
    	        	stream.append(new String(b, 0, n));
    	        }

    	        ips.close();
    	        
    	        JSONArray jArray = new JSONArray(stream.toString());
    	        ShoppingListAdapter sla = new ShoppingListAdapter(ShoppingListActivity.this, jArray);
    	        
    	        return sla;
    	    } catch (ClientProtocolException e) {
    	        e.printStackTrace();
    	    } catch (IOException e) {
    	    	e.printStackTrace();
    	    } catch (Exception e) {
    	    	e.printStackTrace();
    	    }
			return null;
		}
		
		@Override
		protected void onPostExecute(ShoppingListAdapter sla) {
			super.onPostExecute(sla);
			((ListView)findViewById(R.id.shopping_list)).setAdapter(sla);
			progressDialog.dismiss();
		}
		
	}
}
