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
import com.groceryxpress.asynctasks.UpdateShoppingListAbstract;

public class ShoppingListActivity extends Activity {
	
	public static final int REQUEST_CODE_EDIT_PRODUCT_QUANTITY_IN_SHOPPING_LIST = 1;
	public static final int RESULT_CODE_QUANTITY_CHANGED = 2;
	private ShoppingListAdapter sla;
	
	private boolean dontResume = false;
	private ProgressDialog progressDialog;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.shopping_list);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (dontResume) {
			dontResume = false;
			return;
		}
		Log.d("gx", "ShoppingListActivity::onResume");
		if (!GXActivityManager.isShoppingListUpToDate(ShoppingListActivity.this) || sla == null || sla.getCount() == 0) {
			new UpdateShoppingList(ShoppingListActivity.this).execute(new String());
		}
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		progressDialog.dismiss();
	}
	
	public class UpdateShoppingListOnQuantityChange extends UpdateShoppingListAbstract {
		
		public UpdateShoppingListOnQuantityChange(Context context) {
			super(context);
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = ProgressDialog.show(ShoppingListActivity.this, "", "Updating your shopping list...", true);
		}
		
		@Override
		protected void onPostExecute(JSONArray shoppingListJSONArray) {
			progressDialog.dismiss();
		}
	}
	
	public class UpdateShoppingList extends UpdateShoppingListAbstract {

		public UpdateShoppingList(Context context) {
			super(context);
		}
		
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			progressDialog = ProgressDialog.show(ShoppingListActivity.this, "", "Updating your shopping list...", true);
		}
		
		@Override
		protected void onPostExecute(JSONArray shoppingListJSONArray) {
			ShoppingListAdapter sla = new ShoppingListAdapter(ShoppingListActivity.this, shoppingListJSONArray);
			ShoppingListActivity.this.sla = sla;
			((ListView)findViewById(R.id.shopping_list)).setAdapter(sla);
			GXActivityManager.setShoppingListUpToDate(ShoppingListActivity.this, true);
			((ListView)findViewById(R.id.shopping_list)).setAdapter(sla);
			progressDialog.dismiss();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d("gx", "ShoppingListActivity::onActivityResult");
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQUEST_CODE_EDIT_PRODUCT_QUANTITY_IN_SHOPPING_LIST) {
			if(resultCode == RESULT_OK) {
				int new_quantity = data.getIntExtra("new_quantity", -1); //-1 indicates weird state where no quantity was returned in the intent
				if (new_quantity > 0) {
					GXActivityManager.setShoppingListUpToDate(ShoppingListActivity.this, false);
					String[] params = new String[2];
					params[0] = data.getStringExtra("product_id");
					params[1] = String.valueOf(new_quantity);
					dontResume = true;
					new UpdateShoppingListOnQuantityChange(ShoppingListActivity.this).execute(params);
				}
			} else if (resultCode == RESULT_CANCELED) {
				Log.d("gx", "Cancelled");
			}
		}
	}
}
