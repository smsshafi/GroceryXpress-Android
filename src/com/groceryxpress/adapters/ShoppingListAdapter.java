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
import android.widget.Toast;

import com.groceryxpress.GXActivityManager;
import com.groceryxpress.ProductDetailsActivity;
import com.groceryxpress.R;
import com.groceryxpress.ShoppingListActivity;
import com.groceryxpress.tools.DrawableManager;

public class ShoppingListAdapter extends BaseAdapter {

	JSONArray shoppingListJSON;
	Context context;
	
	public ShoppingListAdapter(Context context, JSONArray shoppingListJSON) {
		this.context = context;
		
		if ( shoppingListJSON == null) {
			this.shoppingListJSON = new JSONArray();
		} else {
			this.shoppingListJSON = shoppingListJSON;
		}
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

	public View getView(final int index, View convertView, ViewGroup parent) {
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
					JSONArray jArray = GXActivityManager.getShoppingListJSONArray(context);
					JSONObject productInListForOnClickListener = null;
					try {
						productInListForOnClickListener = jArray.getJSONObject(index);
						i.putExtra("productJSONString", productInListForOnClickListener.toString());
						((Activity)context).startActivityForResult(i, ShoppingListActivity.REQUEST_CODE_EDIT_PRODUCT_QUANTITY_IN_SHOPPING_LIST);
					} catch (JSONException e) {
						e.printStackTrace();
						Toast.makeText(context, "Could not parse JSON.", Toast.LENGTH_LONG).show();
					}
					
				}
			});
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return convertView;
	}

}
