package com.groceryxpress;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class SearchActivity extends Activity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		TextView tv = new TextView(SearchActivity.this);
		tv.setText("Search");
		setContentView(tv);
	}
}
