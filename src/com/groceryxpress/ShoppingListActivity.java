package com.groceryxpress;

import org.json.JSONArray;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;

import com.groceryxpress.adapters.ShoppingListAdapter;
import com.groceryxpress.asynctasks.GetShoppingListAsyncTask;

public class ShoppingListActivity extends Activity {
	
	public static final int REQUEST_CODE_EDIT_PRODUCT_QUANTITY_IN_SHOPPING_LIST = 1;
	public static final int RESULT_CODE_QUANTITY_CHANGED = 2;
	private String userid;
	private ShoppingListAdapter sla;
	private ProgressDialog progressDialog;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.shopping_list);
		
		userid = GXActivityManager.getUserId(ShoppingListActivity.this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.d("gx", "ShoppingListActivity::onResume");
		if (!GXActivityManager.isShoppingListUpToDate(ShoppingListActivity.this) || sla == null || sla.getCount() == 0) {
			progressDialog = ProgressDialog.show(ShoppingListActivity.this, "", "Fetching your shopping list...", true);
			JSONArray shoppingListJSONArray = GXActivityManager.getShoppingListJSONArray(ShoppingListActivity.this);
			ShoppingListAdapter sla = new ShoppingListAdapter(ShoppingListActivity.this, shoppingListJSONArray);
			ShoppingListActivity.this.sla = sla;
			((ListView)findViewById(R.id.shopping_list)).setAdapter(sla);
			GXActivityManager.setShoppingListUpToDate(ShoppingListActivity.this, true);
			((ListView)findViewById(R.id.shopping_list)).setAdapter(sla);
			progressDialog.dismiss();
		}
		
		
//		if (!GXActivityManager.isShoppingListUpToDate(ShoppingListActivity.this) || sla == null) {
//			new GetShoppingList(ShoppingListActivity.this).execute(userid);
//		} else {
//			if (((ListView)findViewById(R.id.shopping_list)).getAdapter() == null) {
//				
//			}
//		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		progressDialog.dismiss();
	}
	
//	public class GetShoppingList extends GetShoppingListAsyncTask {
//		
//		public GetShoppingList(Context context) {
//			super(context);
//			
//		}
//		@Override
//		protected void onPreExecute() {
//			super.onPreExecute();
//			progressDialog = ProgressDialog.show(ShoppingListActivity.this, "", "Fetching your shopping list...", true);
//		}
//		
//		@Override
//		protected void onPostExecute(JSONArray jArray) {
//			super.onPostExecute(jArray);
//			ShoppingListAdapter sla = new ShoppingListAdapter(ShoppingListActivity.this, jArray);
//			ShoppingListActivity.this.sla = sla;
//			((ListView)findViewById(R.id.shopping_list)).setAdapter(sla);
//			GXActivityManager.setShoppingListUpToDate(ShoppingListActivity.this, true);
//			progressDialog.dismiss();
//		}
//	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d("gx", "ShoppingListActivity::onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE_EDIT_PRODUCT_QUANTITY_IN_SHOPPING_LIST) {
			if(resultCode == RESULT_OK) {
				int new_quantity = data.getIntExtra("new_quantity", -1); //-1 indicates weird state where no quantity was returned in the intent
				if (new_quantity > 0) {
					GXActivityManager.setShoppingListUpToDate(ShoppingListActivity.this, false);
					GXActivityManager.getShoppingListJSONArray(ShoppingListActivity.this, data.getStringExtra("product_id"), String.valueOf(new_quantity));
				}
//				GXActivityManager.setQuantitiesChanged(ShoppingListActivity.this, true);
			} else if (resultCode == RESULT_CANCELED) {
				Log.d("gx", "Cancelled");
			}
		}
	}
}
