package com.groceryxpress;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.groceryxpress.tools.DrawableManager;

public class ProductDetailsActivity extends Activity {

	private JSONObject productJSON;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.product_details);
		
		try {
			productJSON = new JSONObject(this.getIntent().getStringExtra("productJSONString"));
			((TextView)findViewById(R.id.product_name)).setText(this.productJSON.getString("full_title"));
			((TextView)findViewById(R.id.product_desc)).setText(this.productJSON.getString("full_description"));
			DrawableManager.instance().fetchDrawableOnThread(productJSON.getString("image_url"), (ImageView)findViewById(R.id.product_image), 0, ProductDetailsActivity.this);
		} catch (JSONException e) {
			productJSON = null;
			Toast.makeText(ProductDetailsActivity.this, "Could not fetch product details", Toast.LENGTH_LONG).show();
		}
		
	}
}
