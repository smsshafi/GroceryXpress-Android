package com.groceryxpress.tools;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import android.app.Activity;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

// This class implements a cache of images.  Images are fetched on background threads.
public class DrawableManager 
{
	// TODO: Consider implementing this cache as a Least Recently Used cache.  If the cache exceeds a certain
	// size, then start removing the items that have been used the least recently.
	
	// TODO: Consider using a more explicit form of thread pool rather than just creating mad numbers of threads
	// on the heap.  I think Android handles it well, but it still makes me somewhat antsy.
	
	private static final int DOWNLOAD_FAIL_INTERVAL = 5000;
	private static final int MAX_DOWNLOAD_THREADS = 8;
	
	private static boolean logMessages = false;
	
	// Maps URLs (as strings) to drawable objects
	private final Map<String, Drawable> _drawableMap;
	
	// Maps URLs (as strings) to a list of views waiting for that URL to be fetched
	private final Map<String, ArrayList<Runnable>> _runnablesMap;
	
	// Maps thread IDs (as strings) to the URL that thread is trying to download
	private final Map<Integer, DownloadThread> _downloadThreadMap;
	
	// Queue of download threads. Size is programatically limited to MAX_DOWNLOAD_THREADS
	private final LinkedList<Integer> _downloadThreadIdQueue;
	
	private final LinkedList<DownloadThread> _interruptedDownloadThreadQueue;
	
	private static DrawableManager _instance = null;

	private static int _currentDownloadThreadId = 0;

	// Monitor object for the class' static fields
	private static Object _staticLock = new Object();
	
	// Monitor object for the class' instance fields
	private Object _lock = new Object();
		
	public static DrawableManager instance() {
		synchronized(_staticLock) {
			if (_instance == null) {
				_instance = new DrawableManager();
			}
		}
		return _instance;
	}

    private DrawableManager() {
    	synchronized (_lock) {
        	_drawableMap = new HashMap<String, Drawable> ();
        	_runnablesMap = new HashMap<String, ArrayList<Runnable>> ();
        	_downloadThreadMap = new TreeMap<Integer, DownloadThread> ();
        	_downloadThreadIdQueue = new LinkedList<Integer>();
        	_interruptedDownloadThreadQueue = new LinkedList<DownloadThread>();
		}
    }
    
    // A TimerThread runs a timer and attempts to interrupt a DownloadThread if it takes too long.
    // If the timer thread interrupts a DownloadThread, then it starts another DownloadThread
    // to retry the download.
    private class TimerThread extends Thread {
    	
    	// The download thread that this timer thread is responsible for
    	private int _downloadThreadId;
    	private String _url;
    	private WeakReference<Activity> _activityRef;
    	
    	public TimerThread( int downloadThreadId, String url, Activity activity ) {
    		_downloadThreadId = downloadThreadId;
    		_url = url;
    		_activityRef = new WeakReference<Activity>( activity );
    		
    		if (logMessages) {
	    		Log.d( "fandango", String.format( "DrawableManager:TimerThread: new timer thread id:%d", _downloadThreadId ));
    		}
    	}
    	
    	public void run() {
    		
    		try {
				sleep( DOWNLOAD_FAIL_INTERVAL );
			} catch (InterruptedException e) {
				return;
			}
			
			// If our download thread is still in the queue, then kill it and launch another to retry
			// the download
			DownloadThread downloadThread = null;
			synchronized( _lock ) {
				boolean isDownloadThreadStillActive = _downloadThreadMap.containsKey( _downloadThreadId );
				
				// If the download thread is gone, then it's successfully finished the download
				// and we don't need to do anything else here.
				if( !isDownloadThreadStillActive ) {
		    		if (logMessages) {
			    		Log.d( "fandango", String.format( "DrawableManager:TimerThread: ending stale timer id:%d", _downloadThreadId ));
		    		}
					return;
				}
				
				downloadThread = _downloadThreadMap.get( _downloadThreadId );
			}
    		if (logMessages) {
	    		Log.d( "fandango", String.format( "DrawableManager:TimerThread: interrupting download thread id:%d", _downloadThreadId ));
    		}
    		
			downloadThread.interrupt();
			
			// Retry download
			if( _activityRef != null ) {
				Activity a = _activityRef.get();
				if( a != null ) {
					launchDownloadThread( a, _url );
				}
			}
    	}
    }
    
    // A DownloadThread thread is responsible for downloading one image from the internet
    private class DownloadThread extends Thread {

    	private int _downloadThreadId = 0;
    	private String _url = null;
    	private WeakReference<Activity> _activityRef = null;
    	
    	public DownloadThread( String url, Activity activity ) {
    		_url = url;
    		_activityRef = new WeakReference<Activity>( activity );
    		
    		// Add ourselves to the map of download threads
			synchronized( _lock ) {
    			_downloadThreadId = _currentDownloadThreadId;
    			_currentDownloadThreadId += 1;
    			if (_downloadThreadIdQueue.size() == MAX_DOWNLOAD_THREADS) {
    				Integer threadIdToDelete;
    				DownloadThread downloadThreadToKill;
    				while(_downloadThreadIdQueue.size() >= MAX_DOWNLOAD_THREADS) {
    					threadIdToDelete = _downloadThreadIdQueue.removeFirst();
    					_downloadThreadMap.remove( threadIdToDelete );
    					downloadThreadToKill = _downloadThreadMap.get( threadIdToDelete );
    					_interruptedDownloadThreadQueue.add(downloadThreadToKill);
    					if (downloadThreadToKill != null) {
    						downloadThreadToKill.interrupt();
    					}
    					Log.v( "FANDANGO DEBUG", "DownloadThread::DownloadThread::Queue overflow. Removing thread id = " + threadIdToDelete);
    				}
    			}
				_downloadThreadMap.put( _downloadThreadId, this );
				_downloadThreadIdQueue.add(_downloadThreadId);
				
			}
			
    		if (logMessages) {
				Log.d( "fandango", String.format( "DrawableManager:DownloadThread: new thread id:%d url:%s", _downloadThreadId, _url ));
    		}
    	}
		
		public void run() {
			
    		if (logMessages) {
    			Log.d( "fandango", String.format( "DrawableManager:DownloadThread:run: id:%d thread launched", _downloadThreadId ));
        	}
			
			synchronized (_lock) {
	    		// If the image is already in the cache, then no work is required
		    	if (_drawableMap.containsKey( _url )) {

		    		if (logMessages) {
			    		Log.d( "fandango", String.format( "DrawableManager:DownloadThread:run: id:%d preemptive exit", _downloadThreadId ));
		    		}
			    		
		    		_downloadThreadMap.remove( _downloadThreadId );
		    		freeInterruptedThreads();
					resolveRunnables();
		    		return;
		    	}
	    	}

	    	// Kick off a thread to cancel this download request if it takes too long
			if( _activityRef == null ) {
				expireRunnablesForUrl( _url );
				freeInterruptedThreads();
				return;
			}
			Activity a = _activityRef.get();
			if( a == null ) {
				expireRunnablesForUrl( _url );
				freeInterruptedThreads();
				return;
			}
	    	TimerThread timerThread = new TimerThread( _downloadThreadId, _url, a );
	    	timerThread.start();
	    	
    		if (logMessages) {
	    		Log.d( "fandango", String.format( "DrawableManager:DownloadThread:run: id:%d fetching image", _downloadThreadId ));
    		}

	    	// Fetch image from network
			boolean fetchResult = fetchDrawable();

    		if (logMessages) {
    			Log.d( "fandango", String.format( "DrawableManager:DownloadThread:run: id:%d finished fetching image", _downloadThreadId ));
    		}
			
			// Remove this thread from the list of incomplete download threads since we've successfully fetched the image
			synchronized (_lock) {
				_downloadThreadMap.remove( _downloadThreadId );
	    	}

			// Exit if a timer thread has interrupted us
			if( isInterrupted() ) {
	    		if (logMessages) {
					Log.d( "fandango", String.format( "DrawableManager:DownloadThread:run: id:%d was interrupted!", _downloadThreadId ));
	    		}
				expireRunnablesForUrl( _url );
				freeInterruptedThreads();
				return;
			}
			
			// Since we've completed the download, we don't need the timer thread for this download thread anymore.
			timerThread.interrupt();
			
			if( !fetchResult ) {
				// Attempt returned error.  Try again.
				try {
					sleep( 100 );
				} catch (InterruptedException e) {	
					//Swallow it
				}
	    		if (logMessages) {
					Log.d( "fandango", String.format( "DrawableManager:DownloadThread:run: id:%d will attempt retry", _downloadThreadId ));
	    		}
	    		
	    		if( _activityRef != null ) {
	    			Activity aa = _activityRef.get();
	    			if( aa != null ) {
	    				launchDownloadThread( a, _url );
	    			}
	    		}
			}
			
    		if (logMessages) {
    			Log.d( "fandango", String.format( "DrawableManager:DownloadThread:run: id:%d resolving draws", _downloadThreadId ));
    		}
    			
			// Fetch successfully completed!
    		freeInterruptedThreads();
    		
			resolveRunnables();
		}
		
		private void freeInterruptedThreads() {
			synchronized(_lock) {
				if (_interruptedDownloadThreadQueue.size() > 0) {
					DownloadThread queuedThread = _interruptedDownloadThreadQueue.removeFirst();
					if (queuedThread != null) {
						queuedThread.run();
					}
				}
			}
		}
		
	    private boolean fetchDrawable() {    	
	    	boolean completed = false;
	    	
	    	Drawable drawable = null;

	    	// Attempt to fetch image from network
	    	try {
	    		InputStream is = getStreamFromURL();
		 		
		 		if( is == null ) return false;
		 		
				final ByteArrayOutputStream imageBufferOutputStream = new ByteArrayOutputStream( 16 * 1024 );
				try {
					// Download the whole image into a byte buffer
				    byte[] buf = new byte[ 512 ];
				    int totalBytesWritten = 0;
				    while( true ) {
				    	
				    	if( interrupted() ) {
				    		if (logMessages) {
				    			Log.d( "fandango", String.format( "DrawableManager:DownloadThread:fetchDrawable: id:%d interrupted during download of image data", _currentDownloadThreadId ));
				    		}
				    		return false;
				    	}
				    	
				    	int bytesRead = is.read( buf );
				    	if( bytesRead == -1 ) break;
				    	imageBufferOutputStream.write(buf, 0, bytesRead);
				    	totalBytesWritten += bytesRead;
				    }
//				    imageBufferOutputStream.flush();
//				    imageBufferOutputStream.close();
		    		if (logMessages) {
		    			Log.d( "fandango", String.format( "DrawableManager:DownloadThread:fetchDrawable: id:%d wrote %d bytes to file", _downloadThreadId, totalBytesWritten));
		    		}
		    			
				    final byte[] imageBuffer = imageBufferOutputStream.toByteArray();
				    final ByteArrayInputStream imageBufferInputStream = new ByteArrayInputStream( imageBuffer );
				    
				    // Create a drawable object from the downloaded input stream
					drawable = Drawable.createFromStream(imageBufferInputStream, "src");
					if( drawable == null ) {

						// If the downloaded input stream was invalid somehow, dump it to the SD Card so
						// we can take a look at what the server decided to send us.
//						final String DATE_FORMAT_NOW = "yyyyMMddHHmmss";
//						final Calendar cal = Calendar.getInstance();
//						final SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
//						final String dateString = sdf.format(cal.getTime());
//						final String filename = String.format( "/sdcard/FANDANGO_%s_%d.png", dateString, _downloadThreadId );
//
//						Log.e( "fandango", String.format( "DrawableManager:fetchDrawable: id:%d bad input file %s", _downloadThreadId, filename) );
//
//						FileOutputStream writer = new FileOutputStream( filename );
//						writer.write( imageBuffer );
//						writer.flush();
//						writer.close();
						
						return false;
					} else {
			    		if (logMessages) {
							Log.d( "fandango", String.format( "DrawableManager:DownloadThread:fetchDrawable: id:%d obtained valid drawable image", _downloadThreadId ));
			    		}
					}

					completed = true;
				} catch (IOException e) {
					Log.e( "fandango", String.format( "DrawableManager:DownloadThread:fetchDrawable: id:%d got exception on opening file:%s", _downloadThreadId, e.getLocalizedMessage()));
					return false;
				} finally {
					imageBufferOutputStream.flush();
					imageBufferOutputStream.close();
					if (!completed) {
						Log.v("FANDANGO DEBUG",
								"Closing partially written stream");
					}
				}
	    	}
			catch( IOException e ) {
				// There was some kind of terrible exception while trying to fetch this
				// image.  We do NOT want to retry downloading it, but instead we will
				// stuff a NULL in the drawable cache so the default image will get used.
				Log.e( "fandango", String.format( "DrawableManager:DownloadThread:run: id:%d got exception:%s", _downloadThreadId, e.getLocalizedMessage() ));
				return false;
			}
			
			synchronized (_lock) {
				// Store image in the cache
				_drawableMap.put( _url, drawable );
			}
			
			return true;
	    }
	    
	    // Establishes a connection to the server and returns an InputStream that can
	    // be used to download the image
		private InputStream getStreamFromURL() throws IOException {
		
			InputStream is = null;
			
			try {
				HttpURLConnection connection = (HttpURLConnection) ( new URL( _url ).openConnection() );
				connection.setConnectTimeout( DOWNLOAD_FAIL_INTERVAL );
				connection.setReadTimeout( DOWNLOAD_FAIL_INTERVAL );

	    		if (logMessages) {
					Log.d( "fandango", String.format( "DrawableManager:DownloadThread:getStreamFromUrl: id:%d making connection", _downloadThreadId ));
	    		}

				// Open a connection with the server
				connection.connect();

				final int code = connection.getResponseCode();

				if( code != 200 ) {
		    		if (logMessages) {
						Log.d( "fandango", String.format( "DrawableManager:DownloadThread:getStreamFromUrl: id:%d got HTTP response", _downloadThreadId, code ));
		    		}
					throw new IOException( String.format( "Got HTTP status code %d", code) );
				}

	    		if (logMessages) {
	    			Log.d( "fandango", String.format( "DrawableManager:DownloadThread:getStreamFromUrl: id:%d getting input stream", _downloadThreadId ));
	    		}

				// Get an input stream that can be used to download the data
				is = connection.getInputStream();
				if (is != null) {
		    		if (logMessages) {
		    			Log.d( "fandango", String.format( "DrawableManager:DownloadThread:getStreamFromUrl: id:%d got input stream!", _downloadThreadId ));
		    		}
					
					// Successfully set up input stream.  Return it!
					return is;
				}
			}
			
			catch( SocketTimeoutException e )
			{
				// If there was a timeout, then return null so we can retry.
				// Other exceptions are allowed to throw because we DON'T want to retry forever
				// if, for example, the file doesn't exist.
				Log.e( "fandango", String.format( "DrawableManager:DownloadThread:getStreamFromUrl: id:%d socket timeout connection: %s", _downloadThreadId, e.getLocalizedMessage() ));
				return null;
			}
			
    		if (logMessages) {
				Log.d( "fandango", String.format( "DrawableManager:DownloadThread:getStreamFromUrl: id:%d could not download image", _downloadThreadId ));
    		}
			return null; 
		}
		
		private void resolveRunnables() {
			
			if( _activityRef == null ) {
				return;
			}
			Activity a = _activityRef.get();
			if( a == null ) {
				return;
			}
			a.runOnUiThread(new Runnable() {
				public void run()  {
					synchronized( _lock ) {
			    		if (logMessages) {
			    			Log.d( "fandango", String.format( "DrawableManager:DownloadThread:resolveRunnables: id:%d executing runnable items", _downloadThreadId ));
			    		}
			    			
						// Execute all of the runnables waiting for this image.  Each runnable is responsible
						// for drawing the image to one view on the screen
		    			ArrayList<Runnable> runnables = _runnablesMap.get( _url );
		    			if( runnables != null ) {
							for( Runnable r : runnables ) {
								try {
									r.run();
								} catch( Exception e ) {
									Log.e("fandango", "DrawableManager:resolveRunnables: exception: " + e.getLocalizedMessage() );
								}
							}
		    			}
					}
				}
			});
		}
    }
    
    private void launchDownloadThread( final Activity activity, final String url ) {
    	
    	// Create background thread to fetch an image
    	DownloadThread thread = new DownloadThread( url, activity );    	
    	thread.start();
    }
    
    // Makes a copy of the bitmap so it can be drawn on the screen and resized without affecting the original
    private BitmapDrawable copyBitmapDrawable (BitmapDrawable src){
    	if (src != null) {
    		BitmapDrawable copy = new BitmapDrawable( src.getBitmap() );
    		return copy;
    	} else {
    		return null;
    	}
    }
    
    // Returns a Runnable object that will draw an image to a view.  Runnables are used since they must be
    // stored in lists for future execution.  Multiple Runnables can wait for one image to be finished fetching
    // if the poster exists in more than one place on the screen.  Only one network access will be made and
    // will populate all the views.
    private Runnable makeRunnable(final String urlString, final ImageView imageView, final int defaultImage ) {
    	Runnable r = new Runnable() {
			public void run() {
				// Don't do anything if the view is gone
				int windowVisibility = imageView.getWindowVisibility();
				if (windowVisibility != View.VISIBLE) {
					return;
				}
				
				Drawable drawable = null;
				synchronized( _lock ) {
					drawable = _drawableMap.get( urlString );
				}
					
    			if (drawable == null) {
    				// Image wasn't found.  Set image view to default image.
    				setViewToDefaultImage(imageView, defaultImage);
    				return;
    			} else {
    				
    				// Ensure that the URL stored in the view is the same one we just fetched
    				String tagUrl = (String) imageView.getTag();
    				if( !tagUrl.equals( urlString ) ) {
    					String errMsg = String.format(
        						"DrawableManager:makeRunnable:run: found mismatched URL on view. tagUrl=%s imgUrl=%s",
        						tagUrl, urlString );
    					Log.e( "fandango", errMsg );

    					// It's better to draw no image to this mismatched view than the wrong image.
    					setViewToDefaultImage(imageView, defaultImage);
    				} else {

	    				// Set image view to the image we fetched
	    				imageView.setImageDrawable( copyBitmapDrawable( (BitmapDrawable) drawable) );
    				}
    			}
			}
		};
		return r;
    }
    
    // Sets the view to the given default image
    private void setViewToDefaultImage( final ImageView imageView, final int defaultImage ) {
		if(defaultImage != 0) {
			imageView.setImageResource( defaultImage );
		} else {
			imageView.setImageDrawable( null );
		}
    }
    
    // Fetch the given image from the network (or cache) and draw it in the given view.  If the image doesn't exist,
    // then use the given image resource as a default.  When images are fetched, they are stored in a cache.
    public void fetchDrawableOnThread (final String urlString, final ImageView imageView, final int defaultImage, final Activity activity) {
    	if (urlString == null || urlString.length() == 0) {
   			// No image given.  Set image view to default image.
    		if (defaultImage != 0) {
    			imageView.setImageResource(defaultImage);
    		} else {
    			imageView.setImageDrawable(null);
    		}
			return;
    	}
    	
    	final String url = urlString.replace(" ", "%20");
    	
    	imageView.setTag( url );
    	
    	synchronized( _lock ) {
    		// If the image is already in the cache, then return it here
	    	if (_drawableMap.containsKey(url)) {
	    		BitmapDrawable drawable = (BitmapDrawable)_drawableMap.get(url);
	    		
	    		if (drawable == null) {
	    			// Null image in cache -- use default
		    		if (defaultImage != 0) {
		    			imageView.setImageResource(defaultImage);
		    		} else {
		    			imageView.setImageDrawable(null);
		    		}
	    		} else {
	    			imageView.setImageDrawable( copyBitmapDrawable( (BitmapDrawable) drawable ) );
	    		}
	    		return;
	    	}

	    	Runnable r = makeRunnable(url, imageView, defaultImage );
	    	
	    	// Put a placeholder image on the drawable until the real image is loaded
    		if (defaultImage != 0) {
    			imageView.setImageResource(defaultImage);
    		} else {
    			imageView.setImageDrawable(null);
    		}

	    	// Image is not already in cache.  Check if there's an outstanding fetch for that image
	    	ArrayList<Runnable> runnables = _runnablesMap.get( url );
	    	if (runnables != null) {
	    		// There's an outstanding fetch for this image.  Put us into the list of views
	    		// waiting for the image.
	    		runnables.add(r);
	    		return;
	    	}
	    	
			// Create a new list to store the runnable objects into and 
			// store it into the map against the given URL
			runnables = new ArrayList<Runnable>();
			_runnablesMap.put( url, runnables );
			
			// Add this request to the list of runnable objects waiting for this
			// image fetch to return
			runnables.add(r);
	    }
    	
    	// Kick off the first download attempt
    	launchDownloadThread( activity, url );
    }
    
    private void expireRunnablesForUrl( String url ) {
    	synchronized ( _lock ) {
    		ArrayList<Runnable> runnables = _runnablesMap.get( url );
    		if( runnables == null ) {
    			// No list of runnables to clean up
    			return;
    		}
    		
    		_runnablesMap.remove( url );
		}
    }
    
}