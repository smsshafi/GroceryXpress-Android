package com.groceryxpress.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

public class Utility {
	public static final short IO_ERROR = 0;
	public static final short NOT_FOUND_ERROR = 1;
	public static final short URL_ERROR = 2;
	public static String CONN_ERROR = null;
	public static final HashMap<String, String> networkResources =
		new HashMap<String, String>();
	public static HashMap<String, String> statCategories =
		new HashMap<String, String>();
	public static HashMap<String, String> positionResources =
		new HashMap<String, String>();

	public static InputStream streamFromURL( final String u ) {
		Utility.CONN_ERROR = null;
		InputStream is = null;
		int attempts = 0;

		try {
			while ( ( is == null ) && ( attempts++ < 3 ) ) {
				is = (InputStream) new URL( u ).getContent();
			}

			if ( is == null ) {
				Utility.CONN_ERROR = "Connection error occured";
			}
		} catch ( final FileNotFoundException e ) {
			Utility.CONN_ERROR = null;
		} catch ( final MalformedURLException e ) {
			Utility.CONN_ERROR = "The url you supplied is invalid";
		} catch ( final IOException e ) {
			Utility.CONN_ERROR = "Please check your data connection";
		} catch ( final Exception e ) {
			Utility.CONN_ERROR = "An error has occured";
		}

		return is;
	}
}