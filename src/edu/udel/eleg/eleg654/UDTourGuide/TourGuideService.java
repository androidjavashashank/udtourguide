package edu.udel.eleg.eleg654.udtourguide;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.skyhookwireless.wps.WPSAuthentication;
import com.skyhookwireless.wps.WPSContinuation;
import com.skyhookwireless.wps.WPSLocation;
import com.skyhookwireless.wps.WPSPeriodicLocationCallback;
import com.skyhookwireless.wps.WPSReturnCode;
import com.skyhookwireless.wps.XPS;

/**
 * 
 * @author Aaron Myles Landwehr
 */
public class TourGuideService extends Service implements WPSPeriodicLocationCallback, Runnable
{
	public class LocalBinder extends Binder
	{
		TourGuideService getService()
		{
			return TourGuideService.this;
		}
	}

	@Override
	public void done()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public WPSContinuation handleError(WPSReturnCode wpsReturnCode)
	{
		showToast("Attempting to lock onto your location...");
		return WPSContinuation.WPS_CONTINUE;
	}
	
	boolean trigger = false;
	
	@Override
	public WPSContinuation handleWPSPeriodicLocation(WPSLocation location)
	{
		this.location = location;
		// for each point of interest in the database.
		for (PointOfInterest destination : this.database)
		{
			// get the distance between our location and the point of interest.
			float distance[] = new float[1];
			Location.distanceBetween(this.location.getLatitude(), this.location.getLongitude(), destination.gpsCenterPoint.getLatitude(),
					destination.gpsCenterPoint.getLongitude(), distance);

			// If we were not already triggered
			// then check if our distance is less than the point of interest's radius.
			if (destination.triggered == false && distance[0] < destination.radius)
			{
				// distance is less.

				// set our point of interest as a triggered so we don't retrigger it on the next location change.
				destination.triggered = true;

				// Make sure the file we need to play exists before showing a blurb popup.
				if (tourGuideActivity.getFileStreamPath(destination.file).exists())
				{
					// if (tourGuideActivity != null)
					// {
					// Play notification sound for he user.
					this.playSound(R.raw.up6);

					// Show the blurb for the user.
					this.showDialogBlurb(destination.file, destination.name);
					// }
					// else
					// {
					// this.playSound(destination.file);
					// }
				}
				else
				{
					// for some reason the file didn't exist in the DB. Corrupt DB maybe?
					// Warn the user.
					this.showToast("Warning: attempted to play file'" + destination.file + "' but it did not exist.\n "
							+ "Please restart UDTourGuide while connected " + "to the internet to update the DB.");
				}
			}
			// If we already triggered our blurb, we need to set it to untriggered
			// after we exist the blurb radius plus some set amount.
			else if (destination.triggered == true && distance[0] > destination.radius + 5.0)
			{
				// set it to untriggered.
				destination.triggered = false;
			}
		}

		MapView mapView = (MapView) tourGuideActivity.findViewById(R.id.mapView);
		MapController mapController = mapView.getController();
		GeoPoint myGeoPointLocation = new GeoPoint((int) (location.getLatitude() * 1E6), (int) (location.getLongitude() * 1E6));
		mapController.animateTo(myGeoPointLocation);

		return WPSContinuation.WPS_CONTINUE;
	}

	@Override
	public IBinder onBind(Intent intent)
	{
		return binder;
	}

	@Override
	public void onCreate()
	{
		super.onCreate();
		this.database = new HashSet<PointOfInterest>();
	}

	public void setActivity(TourGuideActivity tourGuideActivity)
	{
		this.tourGuideActivity = tourGuideActivity;
	}

	void playSound(final int sound)
	{
		tourGuideActivity.handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				MediaPlayer mediaPlayer = MediaPlayer.create(tourGuideActivity, sound);
				mediaPlayer.start();
				while (mediaPlayer.isPlaying())
				{
					Thread.yield();
				}
				mediaPlayer.release();
			}
		});
	}

	void playSound(final String sound)
	{
		tourGuideActivity.handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				try
				{
					MediaPlayer mediaPlayer = new MediaPlayer();
					mediaPlayer.setDataSource(new FileInputStream(tourGuideActivity.getFileStreamPath(sound)).getFD());
					mediaPlayer.prepare();
					mediaPlayer.start();

					while (mediaPlayer.isPlaying())
					{
						Thread.yield();
					}
					mediaPlayer.release();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Processes the database and stores the points of interest found within into an in memory database.
	 * 
	 * @param isDownloadFiles
	 *            Whether or not to attempt to download sound files referenced in the DB.
	 * 
	 * @throws FileNotFoundException
	 *             Thrown if the database file is missing.
	 * @throws IOException
	 *             Thrown if there is a problem reading the database.
	 */
	private void processDB(boolean isDownloadFiles) throws FileNotFoundException, IOException
	{
		this.showDialogProgressIndeterminate("Processing database...");

		// open our database for reading each statement.
		FileInputStream inputStream = tourGuideActivity.openFileInput(TourGuideStatics.databaseFile);

		// create a string buffer for each line. We clear it at each
		// newline.
		StringBuffer line = new StringBuffer();

		// read in a byte.
		int ch = inputStream.read();

		// loop for every byte in the file.
		while (true)
		{
			// if it is a new line marker or EOF, we need to process the line.
			if (ch == '\n' || ch == -1)
			{
				// break up our comma separated data.
				String data[] = line.toString().trim().split(",");

				// make sure we have 6 pieces of data on the line.
				if (data.length == 6)
				{
					if (isDownloadFiles == true)
					{
						this.showDialogProgressIndeterminate("Checking for updates for '" + data[5].trim() + "'...");
						// retrieve the sound file listed on database line.
						// only retrieved when out of date.
						this.retrieveFile(data[5].trim());
					}

					// create a tuple of data for the particular location.
					PointOfInterest point = new PointOfInterest(data[0].trim(), Double.valueOf(data[1].trim()), Double.valueOf(data[2]
							.trim()), Double.valueOf(data[3].trim()), Double.valueOf(data[4].trim()), data[5].trim());

					// store it in our list of location points.
					this.database.add(point);
				}
				// If data length is 1 this could be an empty line.
				// if it is, we don't want to throw errors.
				else if (data.length == 1)
				{
					// check to see if this is an empty line.
					if (data[0].length() != 0)
					{
						// not an empty line, throw error.
						this.showDialogExit("Malformed data found in online database.\nPlease, restart and update the database.");
					}
				}
				// if we reach here, then this was not an empty line
				// AND doesn't contain the right number of fields.
				else
				{
					// malformed line, throw error.
					this.showDialogExit("Malformed data found in online database.\nPlease, restart and update the database.");
				}

				// clear our line buffer.
				line = new StringBuffer();
			}

			// if negative one, EOF so break out.
			if (ch == -1)
				break;

			// append the byte to the line.
			line.append((char) ch);

			// read in the next piece of data.
			ch = inputStream.read();
		}

		// done reading our file so close it.
		inputStream.close();
	}

	/**
	 * Method that simply requests location updates for us. We call this directly. We use the criteria for ACCURACY_FINE and don't care
	 * about how much power it takes.
	 */
	void requestLocationUpdates()
	{
		XPS xps = new XPS(tourGuideActivity);
		WPSAuthentication wpsAuthentication = new WPSAuthentication("snaphat", "udel");
		xps.getXPSLocation(wpsAuthentication, 0, 1000, this);
	}

	/**
	 * Retrieves a file from the HTTP server that this program uses. It is TourGuideActivity.server.
	 * 
	 * @param file
	 *            The file to retrieve from the server.
	 * 
	 * @throws MalformedURLException
	 *             Thrown if there is an issues creating the url using the specified server and file combination.
	 * @throws IOException
	 *             Thrown if there is an error opening up the HTTP connection.
	 */
	private void retrieveFile(String file) throws MalformedURLException, IOException
	{
		// create a url using our server and the file we want to retrieve.
		URL url = new URL(TourGuideStatics.server + file);

		// create an http connection for getting the length and last modified
		// date.
		HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

		// get the last modified date of the remote file.
		long modifiedDate = httpConn.getLastModified();

		int length = httpConn.getContentLength();

		// get the local file so that we can check when it was last modified
		// compared to the remote file.
		File localFile = tourGuideActivity.getFileStreamPath(file);

		// check the dates if they match, we are NOT downloading the file.
		if (modifiedDate != localFile.lastModified())
		{
			// date didn't match so we have to download the file.

			// get an input stream to read the file in.
			InputStream inputStream = new BufferedInputStream(httpConn.getInputStream());

			// create a file to write the read in file too.
			OutputStream outputStream = tourGuideActivity.openFileOutput(file, 0);

			// current position in the file.
			int currentPosition = 0;

			// read data from the remote stream.
			byte data[] = new byte[1024 * 10];
			int amountRead = inputStream.read(data);
			while (amountRead != -1)
			{
				// keep track of our position in the file.
				currentPosition = currentPosition + amountRead;

				this.showDialogProgress("Downloading '" + file + "'...", currentPosition, length);

				// write the data to the local stream.
				// outputStream.write(data);
				outputStream.write(data, 0, amountRead);

				// read more data from the remote stream.
				amountRead = inputStream.read(data);
			}

			// done reading so close both files.
			inputStream.close();
			outputStream.close();
			hideDialogProgress();

			// set the last modified date bc we don't want to retrieve files that have the same date.
			localFile.setLastModified(modifiedDate);
		}

		// drop the http connection.
		httpConn.disconnect();
	}

	@Override
	public void run()
	{
		try
		{
			if (this.isUpdate == true)
			{
				ConnectivityManager connectivityManager = (ConnectivityManager) tourGuideActivity
						.getSystemService(Context.CONNECTIVITY_SERVICE);

				if (connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected())
				{
					// connected the the internet so we will retrieve a updated DB if needed.
					this.updateDB();

					// connected to the internet so we'll process the DB and update sound files if needed.
					this.processDB(true);
					requestLocationUpdates();
					this.showDialogProgressIndeterminate("UDTourGuide is ready!");
				}
				else
				{
					if (tourGuideActivity.getFileStreamPath(TourGuideStatics.databaseFile).exists())
					{
						this.showToast("You need to be connected to the Internet to "
								+ "update the database. Defaulting to non-update mode.");
						this.processDB(false);
						requestLocationUpdates();
						this.showDialogProgressIndeterminate("UDTourGuide is ready!");
					}
					else
					{
						this.showDialogExit("Local database hasn't been installed.\n"
								+ "\nPlease connect to the Internet then restart UDTourGuide to install the database.");
					}
				}
			}
			else
			{
				// not connected to the internet.
				// so we are just going to process the local copy of our DB.
				this.processDB(false);
				requestLocationUpdates();
				this.showDialogProgressIndeterminate("UDTourGuide is ready!");
			}
			hideDialogProgressIndeterminate();
		}
		catch (Exception e)
		{
			StackTraceElement elements[] = e.getStackTrace();
			String trace = "";
			for (int i = 0; i < elements.length; i++)
			{
				trace = trace + elements[i].toString() + "\n\n";
			}
			//hideDialogProgressIndeterminate();
			this.showDialogExit("Exception:\n" + e.toString() + "\n\n" + trace);
		}
	}

	void showDialogBlurb(final String file, final String location)
	{

		tourGuideActivity.handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				TourGuideStatics.DIALOG_BLURB_FILE = file;
				TourGuideStatics.DIALOG_BLURB_LOCATION = location;
				tourGuideActivity.showDialog(TourGuideStatics.DIALOG_BLURB);
			}
		});
	}

	void showDialogExit(final String text)
	{

		tourGuideActivity.handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				TourGuideStatics.DIALOG_EXIT_TEXT = text;
				tourGuideActivity.showDialog(TourGuideStatics.DIALOG_EXIT);
			}
		});
	}

	void showDialogProgress(final String text, final int amount, final int total)
	{
		tourGuideActivity.handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				TourGuideStatics.DIALOG_PROGRESS_TEXT = text;
				TourGuideStatics.DIALOG_PROGRESS_AMOUNT = amount;
				TourGuideStatics.DIALOG_PROGRESS_TOTAL = total;
				tourGuideActivity.showDialog(TourGuideStatics.DIALOG_PROGRESS);
			}
		});
	}

	void showDialogProgressIndeterminate(final String text)
	{

		tourGuideActivity.handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				TourGuideStatics.DIALOG_PROGRESS_TEXT = text;
				tourGuideActivity.showDialog(TourGuideStatics.DIALOG_PROGRESS_INDETERMINATE);
			}
		});
	}

	void hideDialogProgress()
	{
		tourGuideActivity.handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				tourGuideActivity.dismissDialog(TourGuideStatics.DIALOG_PROGRESS);
			}
		});
	}

	void hideDialogProgressIndeterminate()
	{
		tourGuideActivity.handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				tourGuideActivity.dismissDialog(TourGuideStatics.DIALOG_PROGRESS_INDETERMINATE);
			}
		});
	}

	void showToast(final String text)
	{
		tourGuideActivity.handler.post(new Runnable()
		{
			@Override
			public void run()
			{
				Toast toast = Toast.makeText(tourGuideActivity.getApplicationContext(), text, 0);
				toast.show();
			}
		});
	}

	private void updateDB() throws Exception
	{
		this.showDialogProgressIndeterminate("Updating database...");

		retrieveFile(TourGuideStatics.databaseFile);
	}

	public WPSLocation getLocation()
	{
		return this.location;

	}

	public Set<PointOfInterest> getDatabase()
	{
		return this.database;

	}
	
	public void setUpdate(boolean isUpdate)
	{
		this.isUpdate = isUpdate;
	}
	
	public void start()
	{
		if(thread == null)
		{
			thread = new Thread(this);
			thread.start();	
		}
	}

	private TourGuideActivity tourGuideActivity;
	private Set<PointOfInterest> database;
	private WPSLocation location;
	private final Binder binder = new LocalBinder();
	private Thread thread;
	private boolean isUpdate = false;
}
