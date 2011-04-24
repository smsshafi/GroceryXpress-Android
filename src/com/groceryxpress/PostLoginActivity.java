package com.groceryxpress;

import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TabHost;

public class PostLoginActivity extends TabActivity {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        Resources res = getResources();
        
        TabHost tabHost = getTabHost();  // The activity TabHost
        TabHost.TabSpec spec;  // Resusable TabSpec for each tab
        Intent intent;  // Reusable Intent for each tab

        // Create an Intent to launch an Activity for the tab (to be reused)
        intent = new Intent().setClass(this, ShoppingListActivity.class);

        // Initialize a TabSpec for each tab and add it to the TabHost
        spec = tabHost.newTabSpec("list").setIndicator("Shopping List",
                          res.getDrawable(R.drawable.cart))
                      .setContent(intent);
        tabHost.addTab(spec);

        // Do the same for the other tabs
        intent = new Intent().setClass(this, SearchActivity.class);
        spec = tabHost.newTabSpec("search").setIndicator("Search",
                          res.getDrawable(R.drawable.search))
                      .setContent(intent);
        tabHost.addTab(spec);

        tabHost.setCurrentTab(0);
    }
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		final int m = R.menu.main_menu;
		this.getMenuInflater().inflate(m, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.menu_logout:
			GXActivityManager.setUserId(PostLoginActivity.this, "");
			startActivity(new Intent(PostLoginActivity.this, LoginActivity.class));
			finish();
		}
		return super.onOptionsItemSelected(item);
	}
}
