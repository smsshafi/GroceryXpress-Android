package com.groceryxpress;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class LoginActivity extends Activity {
	
	private EditText emailEditText, passwordEditText;
	private ProgressDialog progressDialog;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        
        SharedPreferences sp = GXActivityManager.getPreferences(LoginActivity.this);
        
        if (!sp.getString("userid", "").equals("")) {
        	startActivity(new Intent(LoginActivity.this, PostLoginActivity.class));
    		finish();
        }
        
        Button loginButton = (Button)findViewById(R.id.login);
        emailEditText = (EditText)findViewById(R.id.email);
        passwordEditText = (EditText)findViewById(R.id.password);
        
        loginButton.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				new LoginAsyncTask().execute();
				
			}
		});
    }
    
    public class LoginAsyncTask extends AsyncTask<Void, Void, JSONObject> {
    	@Override
    	protected void onPreExecute() {
    		progressDialog = ProgressDialog.show(LoginActivity.this, "", "Logging in...", true);
    	}
    	
    	@Override
    	protected JSONObject doInBackground(Void... params) {
    		// Create a new HttpClient and Post Header
    	    HttpClient httpclient = new DefaultHttpClient();
    	    HttpPost httppost = new HttpPost("http://www.groceryxpress.net/api/authenticate.php");

    	    try {
    	        // Add your data
    	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
    	        nameValuePairs.add(new BasicNameValuePair("email", emailEditText.getText().toString()));
    	        nameValuePairs.add(new BasicNameValuePair("password", passwordEditText.getText().toString()));
    	        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

    	        // Execute HTTP Post Request
    	        HttpResponse response = httpclient.execute(httppost);
    	        
    	        InputStream ips  = response.getEntity().getContent();
    	        BufferedReader buf = new BufferedReader(new InputStreamReader(ips,"UTF-8"));
    	        if(response.getStatusLine().getStatusCode()!=HttpStatus.SC_OK) {
    	            throw new Exception(response.getStatusLine().getReasonPhrase());
    	        }
    	        StringBuilder sb = new StringBuilder();
    	        String s;
    	        while(true )
    	        {
    	            s = buf.readLine();
    	            if(s==null || s.length()==0)
    	                break;
    	            sb.append(s);

    	        }
    	        buf.close();
    	        ips.close();
    	        
    	        JSONObject jObject = new JSONObject(sb.toString());
    	        
    	        return jObject;
    	        
    	        
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
    	protected void onPostExecute(JSONObject jObject) {
    		super.onPostExecute(jObject);
    		progressDialog.dismiss();
    		
    		try {
	    		if (jObject.getBoolean("error") == true) {
	    			Toast.makeText(LoginActivity.this, jObject.getString("message"), Toast.LENGTH_LONG).show();
	    		} else {
	    			GXActivityManager.setUserId(LoginActivity.this, jObject.getString("userid"));
	    			startActivity(new Intent(LoginActivity.this, PostLoginActivity.class));
	    			finish();
	    		}
    		} catch (Exception e) {
    			
    		}
    	}
    }
}