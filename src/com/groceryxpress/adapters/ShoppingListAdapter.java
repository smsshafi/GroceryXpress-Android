package com.groceryxpress.adapters;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.groceryxpress.ProductDetailsActivity;
import com.groceryxpress.R;
import com.groceryxpress.tools.DrawableManager;

public class ShoppingListAdapter extends BaseAdapter {

	JSONArray shoppingListJSON;
	Context context;
	
	public ShoppingListAdapter(Context context, JSONArray shoppingListJSON) {
		this.context = context;
		this.shoppingListJSON = shoppingListJSON;
	}
	
	public int getCount() {
		return shoppingListJSON.length();
	}

	public Object getItem(int index) {
		
		try {
			return shoppingListJSON.get(index);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	public long getItemId(int index) {
		return index;
	}

	public View getView(int index, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = View.inflate(context, R.layout.shopping_list_row, new LinearLayout(context));
		}
		
		TextView productName = (TextView) convertView.findViewById(R.id.product_name);
		TextView productPrice = (TextView) convertView.findViewById(R.id.product_price);
		ImageView productImage = (ImageView) convertView.findViewById(R.id.product_image);
		
		try {
			final JSONObject productInList = shoppingListJSON.getJSONObject(index);
			productName.setText(productInList.getString("title"));
			productPrice.setText(productInList.getString("price"));
			DrawableManager.instance().fetchDrawableOnThread(productInList.getString( "image_url" ), productImage, 0, (Activity)context);
			convertView.setOnClickListener(new View.OnClickListener() {
				
				public void onClick(View arg0) {
					Intent i = new Intent(context, ProductDetailsActivity.class);
					i.putExtra("productJSONString", productInList.toString());
					context.startActivity(i);
					
				}
			});
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return convertView;
	}

}
