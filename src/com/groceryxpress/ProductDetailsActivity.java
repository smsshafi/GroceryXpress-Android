package com.groceryxpress;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.groceryxpress.tools.DrawableManager;

public class ProductDetailsActivity extends Activity {

	private JSONObject productJSON;
	private int old_quantity;
	private int new_quantity;
	private String product_id;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.product_details);
		
		try {
			productJSON = new JSONObject(this.getIntent().getStringExtra("productJSONString"));
			product_id = this.productJSON.getString("id");
			((TextView)findViewById(R.id.product_name)).setText(this.productJSON.getString("full_title"));
			((TextView)findViewById(R.id.product_desc)).setText(this.productJSON.getString("full_description"));
			old_quantity = Integer.valueOf(this.productJSON.getString("quantity"));
			new_quantity = old_quantity;
			((TextView)findViewById(R.id.product_quantity)).setText(String.valueOf(old_quantity));
			
			((ImageButton)findViewById(R.id.button_add)).setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					((TextView)findViewById(R.id.product_quantity)).setText(String.valueOf(++new_quantity));
				}
			});
			
			((ImageButton)findViewById(R.id.button_delete)).setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					if(new_quantity > 1) {
						((TextView)findViewById(R.id.product_quantity)).setText(String.valueOf(--new_quantity));
					}
				}
			});
			
			DrawableManager.instance().fetchDrawableOnThread(productJSON.getString("image_url"), (ImageView)findViewById(R.id.product_image), 0, ProductDetailsActivity.this);
			
		} catch (JSONException e) {
			productJSON = null;
			Toast.makeText(ProductDetailsActivity.this, "Could not fetch product details", Toast.LENGTH_LONG).show();
		}
		
	}
	
	@Override
	protected void onDestroy() {
		
		super.onDestroy();
	}
	
	@Override
	public void onBackPressed() {
		if (new_quantity != old_quantity) {
			Intent data = new Intent();
			data.putExtra("new_quantity", new_quantity);
			data.putExtra("product_id", product_id);
			setResult(Activity.RESULT_OK, data);
		} else {
			setResult(Activity.RESULT_CANCELED);
		}
		super.onBackPressed();
	}
	
}
