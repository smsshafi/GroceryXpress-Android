package com.groceryxpress;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ShoppingListActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TextView tv = new TextView(ShoppingListActivity.this);
		tv.setText("Shopping List");
		setContentView(tv);
	}
}
