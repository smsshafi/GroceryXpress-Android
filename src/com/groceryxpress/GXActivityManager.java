package com.groceryxpress;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class GXActivityManager extends Activity {
	private static SharedPreferences preferences;
	
	public synchronized static SharedPreferences getPreferences( final Context context ) {
		if ( GXActivityManager.preferences == null ) {
			GXActivityManager.preferences =
				context.getSharedPreferences( "NBAPreferences",
						Context.MODE_PRIVATE );
		}

		return GXActivityManager.preferences;
	}
}
