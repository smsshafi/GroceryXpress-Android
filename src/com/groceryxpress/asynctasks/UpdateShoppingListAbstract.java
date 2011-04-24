package com.groceryxpress.asynctasks;

import org.json.JSONArray;

import android.content.Context;
import android.os.AsyncTask;

import com.groceryxpress.GXActivityManager;

public abstract class UpdateShoppingListAbstract extends AsyncTask<String, Void, JSONArray> {
	private Context context;
	
	public UpdateShoppingListAbstract(Context context) {
		this.context = context;
	}
	@Override
	protected JSONArray doInBackground(String... params) {
		String product_id = null;
		String new_quantity = null;
		try {
			product_id = params[0];
			new_quantity = params[1];
		} catch (Exception e) {
			
		}
		
		if (product_id != null && new_quantity != null) {
			return GXActivityManager.getShoppingListJSONArray(context, product_id, new_quantity);
		} else {
			return GXActivityManager.getShoppingListJSONArray(context);
		}
	}
	
}
