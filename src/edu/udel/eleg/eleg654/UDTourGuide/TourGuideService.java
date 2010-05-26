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
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Set;

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
 * Copyright 2010 Aaron Myles Landwehr. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *      
 * THIS SOFTWARE IS PROVIDED BY AARON MYLES LANDWEHR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL AARON MYLES LANDWEHR OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of Aaron Myles Landwehr.
 */
/**
 * This Service provides the backend functionality for the program. This includes basically any
 * work that isn't related to the GUI. First, it handles location updates through the use of the 
 * skyhook API. Using the location updates, It checks to see if we have intersected with points of
 * interests and pushes a notification back to the user if we have via dialog that is managed by the 
 * activity. It also implements the functionality for database updates.
 *
 * @author Aaron Myles Landwehr
 */
public class TourGuideService extends Service implements WPSPeriodicLocationCallback, Runnable
{

	/**
	 * Enables applications to bind to our service once it has started. The normal Binder interface
	 * doesn't allow us to directly communicate with the object we are binding. Since, we know
	 * that both the class that is being bound and the class that is binding exist in the same
	 * process, the class that is binding can just call the bound class's methods directly. So,
	 * we extend the Binder class to allow a method that returns a reference to the class being
	 * bound.
	 */
	public class LocalBinder extends Binder
	{

		/**
		 * Called by the class attempting to bind to us if it wants to access our methods directly.
		 *
		 * @return a reference to our TourGuideService class.
		 */
		TourGuideService getService()
		{
			//return a reference to our service instance.
			return TourGuideService.this;
		}
	}

	/**
	 * Skyhook required callback that occurs when skyhook is ready for more requests.
	 */
	@Override
	public void done()
	{
		//Do nothing, because we don't care about it.
	}

	/**
	 * Skyhook required callback that occurs when skyhook is unable to retrieve the current
	 * location. We simply ask it to continue if this happens.
	 *
	 * @param wpsReturnCode the error code. We don't use it.
	 * @return WPSContinuation.WPS_CONTINUE to signify that we want to keep trying to lock onto
	 *  the current location.
	 */
	@Override
	public WPSContinuation handleError(WPSReturnCode wpsReturnCode)
	{
		//show a toast.
		showToast("Attempting to lock onto your location...");

		//tell skyhook that we want to continue to recieve location updates.
		return WPSContinuation.WPS_CONTINUE;
	}

	/**
	 * Skyhook callback that is called each time it is able to lock onto our current
	 * location. We check the newly returned location with the points of interest
	 * in the database and notify the user if we've hit one.
	 *
	 * @param location our current location in latitude and longitude.
	 * @return WPSContinuation.WPS_CONTINUE to signify that we want to lock onto
	 * the current location again.
	 */
	@Override
	public WPSContinuation handleWPSPeriodicLocation(WPSLocation location)
	{
		//Keep the current location internally, so that it is retrievable via
		//getLocation(). The Activity needs this to draw a point for where we
		//are located.
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
					// Play notification sound for he user.
					this.playSound(R.raw.up6);

					// Show the blurb for the user.
					this.showDialogBlurb(destination.file, destination.name);
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
			// after we exit the blurb radius plus some set amount.
			else if (destination.triggered == true && distance[0] > destination.radius + 5.0)
			{
				// set it to untriggered.
				destination.triggered = false;
			}
		}

		// Force the GUI to animate to the current location, because this doesn't work
		// when called from the MapOverlay.draw() routine.
		MapView mapView = (MapView) tourGuideActivity.findViewById(R.id.mapView);
		MapController mapController = mapView.getController();
		GeoPoint myGeoPointLocation = new GeoPoint((int) (location.getLatitude() * 1E6), (int) (location.getLongitude() * 1E6));
		mapController.animateTo(myGeoPointLocation);

		// Tell skyhook that we want to continue to recieve location updates.
		return WPSContinuation.WPS_CONTINUE;
	}

	/**
	 * Called when someone attempts to bind to us. We return our extended Binder that
	 * allows direct method invocation.
	 *
	 * @param intent the intent provided by the class attempting to bind to us.
	 * @return our extended binder. Please call getService() using it.
	 */
	@Override
	public IBinder onBind(Intent intent)
	{
		//return our binder.
		return localBinder;
	}

	/**
	 * Called when our service is created. We use it for initialization.
	 */
	@Override
	public void onCreate()
	{
		//Call the papa.
		super.onCreate();

		//create object to store the database.
		this.database = new HashSet<PointOfInterest>();
	}

	/**
	 * This should be called by the binder after it binds
	 * to us. We use the activity to send GUI notifications
	 * and to play back sounds.
	 *
	 * @param tourGuideActivity the activity for us to use internally.
	 */
	public void setActivity(TourGuideActivity tourGuideActivity)
	{
		//set the activity.
		this.tourGuideActivity = tourGuideActivity;
	}

	/**
	 * Simply uses the activity to play a sound for the user.
	 *
	 * @param sound the internal resource id of the sound to play.
	 */
	void playSound(final int sound)
	{
		//Insert a runnable object into the activity's message queue.
		//The activity will run the code when the message is processed.
		tourGuideActivity.handler.post(new Runnable()
		{

			/**
			 * Method to run.
			 */
			@Override
			public void run()
			{
				//create a media player object to play sound with.
				MediaPlayer mediaPlayer = MediaPlayer.create(tourGuideActivity, sound);

				//start the sound.
				mediaPlayer.start();

				//wait until it finishes.
				while (mediaPlayer.isPlaying())
				{
					//yield this thread while we wait.
					Thread.yield();
				}

				//release the created media player object.
				mediaPlayer.release();
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
		//Show a progress dialog.
		this.showDialogProgressIndeterminate("Processing database...");

		// open our database for reading each statement.
		FileInputStream inputStream = tourGuideActivity.openFileInput(TourGuideStatics.databaseFile);

		// create a string buffer for each line. We clear it at each newline.
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
					//Attempt to update the file if we are current set to download updates.
					if (isDownloadFiles == true)
					{
						//update the progress dialog.
						this.showDialogProgressIndeterminate("Checking for updates for '" + data[5].trim() + "'...");

						// retrieve the sound file listed on database line.
						// only retrieved when out of date.
						this.retrieveFile(data[5].trim());
					}

					// create a tuple of data for the particular location.
					PointOfInterest point = new PointOfInterest(data[0].trim(), Double.valueOf(data[1].trim()), Double.valueOf(
							data[2].trim()), Double.valueOf(data[3].trim()), Double.valueOf(data[4].trim()), data[5].trim());

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
			{
				break;
			}

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

	/**
	 * Code that is to be run in a thread other than the GUI thread. It updates and processes the
	 * database for the program. It should begin when start() is called.
	 */
	@Override
	public void run()
	{
		try
		{
			//Check to see if we are looking for database updates.
			if (this.isUpdate == true)
			{
				//we are looking for database updates.

				//We want to make sure we are connected before attempting to download updates.
				ConnectivityManager connectivityManager = (ConnectivityManager) tourGuideActivity.getSystemService(
						Context.CONNECTIVITY_SERVICE);

				//make sure we are connected.
				if (connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected())
				{
					//connected- continue with database updates.

					// connected the the internet so we will retrieve a updated DB if needed.
					this.updateDB();

					// connected to the internet so we'll process the DB and update sound files if needed.
					this.processDB(true);
					requestLocationUpdates();
					this.showDialogProgressIndeterminate("UDTourGuide is ready!");
				}
				else
				{
					//we aren't connected. But, we don't want to error out if a database is already installed.

					//check to see if a database is already installed.
					if (tourGuideActivity.getFileStreamPath(TourGuideStatics.databaseFile).exists())
					{
						//db was installed.

						//show warning.
						this.showToast("You need to be connected to the Internet to "
									   + "update the database. Defaulting to non-update mode.");

						//process the database.
						this.processDB(false);

						//request skyhook location updates.
						requestLocationUpdates();

						//we are ready to go. Tell the user!
						this.showDialogProgressIndeterminate("UDTourGuide is ready!");
					}
					else
					{
						//db was NOT installed.

						//throw an error to the user.
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

				//request skyhook location updates.
				requestLocationUpdates();

				//we are ready to go. Tell the user!
				this.showDialogProgressIndeterminate("UDTourGuide is ready!");
			}

			//Hid the progress dialogs.
			hideDialogProgress();
			hideDialogProgressIndeterminate();
		}
		catch (Exception e)
		{
			//We should never get here.

			//Copy the stack trace.
			StackTraceElement elements[] = e.getStackTrace();
			String trace = "";
			for (int i = 0; i < elements.length; i++)
			{
				trace = trace + elements[i].toString() + "\n\n";
			}
			hideDialogProgressIndeterminate();

			//display it to the user.
			this.showDialogExit("Exception:\n" + e.toString() + "\n\n" + trace);
		}
	}

	/**
	 * Tells the activity to display a blurb dialog for us. This consists of
	 * a message telling which location we are close to and the file to play
	 * if the user so wishes.
	 *
	 * @param file the file to play.
	 * @param location the point of interest we are close by.
	 */
	void showDialogBlurb(final String file, final String location)
	{
		//Insert a runnable object into the activity's message queue.
		//The activity will run the code when the message is processed.
		tourGuideActivity.handler.post(new Runnable()
		{

			/**
			 * Method to run.
			 */
			@Override
			public void run()
			{
				//Hackish pre Android 2.2 way of getting the file and location
				// to the dialog.
				TourGuideStatics.DIALOG_BLURB_FILE = file;
				TourGuideStatics.DIALOG_BLURB_LOCATION = location;

				//ask the message handler for the activity to politely display the dialog.
				tourGuideActivity.showDialog(TourGuideStatics.DIALOG_BLURB);
			}
		});
	}

	/**
	 * Tells the activity to display an exit dialog for us. This consists of an exit message
	 * and an okay button.
	 *
	 * @param text the exit message.
	 */
	void showDialogExit(final String text)
	{
		//Insert a runnable object into the activity's message queue.
		//The activity will run the code when the message is processed.
		tourGuideActivity.handler.post(new Runnable()
		{

			/**
			 * Method to run.
			 */
			@Override
			public void run()
			{
				//Hackish pre Android 2.2 way of getting the text
				// to the dialog.
				TourGuideStatics.DIALOG_EXIT_TEXT = text;

				//ask the message handler for the activity to politely display the dialog.
				tourGuideActivity.showDialog(TourGuideStatics.DIALOG_EXIT);
			}
		});
	}

	/**
	 * Tells the activity to display a progress dialog for us. There are two progress dialogs
	 * that we use, one with an indefinite progress and one with a definite progress. This
	 * one is the definite one, so we provide information for it to give the progress amount
	 * to the user.
	 *
	 * @param text the message to be displayed in the dialog.
	 * @param progress the current progress o the dialog relative to the max.
	 * @param max the max progress.
	 */
	void showDialogProgress(final String text, final int progress, final int max)
	{
		//Insert a runnable object into the activity's message queue.
		//The activity will run the code when the message is processed.
		tourGuideActivity.handler.post(new Runnable()
		{

			/**
			 * Method to run.
			 */
			@Override
			public void run()
			{
				//Hackish pre Android 2.2 way of getting the text, and progress bar info
				// to the dialog.
				TourGuideStatics.DIALOG_PROGRESS_TEXT = text;
				TourGuideStatics.DIALOG_PROGRESS_PROGRESS = progress;
				TourGuideStatics.DIALOG_PROGRESS_MAX = max;

				//ask the message handler for the activity to politely display the dialog.
				tourGuideActivity.showDialog(TourGuideStatics.DIALOG_PROGRESS);

				//FIXME: Below is for the Android 2.2 API.
				//Bundle args = new Bundle();
				//args.putString(TourGuideStatics.KEY_TEXT, text);
				//args.putInt(TourGuideStatics.KEY_PROGRESS, progress);
				//args.putInt(TourGuideStatics.KEY_MAX, max);
				//tourGuideActivity.showDialog(TourGuideStatics.DIALOG_PROGRESS, args);
			}
		});
	}

	/**
	 * Tells the activity to display a progress dialog for us. There are two progress dialogs
	 * that we use, one with an indefinite progress and one with a definite progress. This
	 * one is the indefinite one consisting of only a message.
	 *
	 * @param text the message to be displayed in the dialog.
	 */
	void showDialogProgressIndeterminate(final String text)
	{
		//Insert a runnable object into the activity's message queue.
		//The activity will run the code when the message is processed.
		tourGuideActivity.handler.post(new Runnable()
		{

			/**
			 * Method to run.
			 */
			@Override
			public void run()
			{
				//Hackish pre Android 2.2 way of getting the text.
				TourGuideStatics.DIALOG_PROGRESS_TEXT = text;

				//ask the message handler for the activity to politely display the dialog.
				tourGuideActivity.showDialog(TourGuideStatics.DIALOG_PROGRESS_INDETERMINATE);

				//FIXME: Below is for the Android 2.2 API.
				//Bundle args = new Bundle();
				//args.putString(TourGuideStatics.KEY_TEXT, text);
				//tourGuideActivity.showDialog(TourGuideStatics.DIALOG_PROGRESS_INDETERMINATE, args);
			}
		});
	}

	/**
	 * Tells the activity to dismiss the definite progress dialog for us.
	 */
	void hideDialogProgress()
	{
		//Insert a runnable object into the activity's message queue.
		//The activity will run the code when the message is processed.
		tourGuideActivity.handler.post(new Runnable()
		{

			/**
			 * Method to run.
			 */
			@Override
			public void run()
			{
				//ask the message handler for the activity to politely hide the dialog.
				tourGuideActivity.dismissDialog(TourGuideStatics.DIALOG_PROGRESS);
			}
		});
	}

	/**
	 * Tells the activity to dismiss the indefinite progress dialog for us.
	 */
	void hideDialogProgressIndeterminate()
	{
		//Insert a runnable object into the activity's message queue.
		//The activity will run the code when the message is processed.
		tourGuideActivity.handler.post(new Runnable()
		{

			/**
			 * Method to run.
			 */
			@Override
			public void run()
			{
				//ask the message handler for the activity to politely hide the dialog.
				tourGuideActivity.dismissDialog(TourGuideStatics.DIALOG_PROGRESS_INDETERMINATE);
			}
		});
	}

	/**
	 * Tells the activity to pop a toast for us. Really, it's not the activity, but android that
	 * handles this internally.
	 * Yes, pop a freaking toast!
	 *
	 * @param text the text of the toast to pop.
	 */
	void showToast(final String text)
	{
		//Insert a runnable object into the activity's message queue.
		//The activity will run the code when the message is processed.
		tourGuideActivity.handler.post(new Runnable()
		{

			/**
			 * Method to run.
			 */
			@Override
			public void run()
			{
				//create the toast to pop.
				Toast toast = Toast.makeText(tourGuideActivity.getApplicationContext(), text, 0);

				//have android show the toast.
				toast.show();
			}
		});
	}

	/**
	 * Attempts to retrieve an updated database file.
	 *
	 * @throws MalformedURLException
	 *             Thrown if there is an issues creating the url using the specified server and file combination.
	 * @throws IOException
	 *             Thrown if there is an error opening up the HTTP connection.
	 */
	private void updateDB() throws MalformedURLException, IOException
	{
		//show a progress dialog.
		this.showDialogProgressIndeterminate("Updating database...");

		//retrieve the database file.
		retrieveFile(TourGuideStatics.databaseFile);
	}

	/**
	 * Allows other classes to retrieve our current location that we get
	 * via location updates. This should be used by the Activity to display
	 * a point for our current location on the map.
	 *
	 * @return our location in latitude and longitude.
	 */
	public WPSLocation getLocation()
	{
		//return our location.
		return this.location;
	}

	/**
	 * Allows other classes to retrieve the points of interest that we've
	 * processed and store internally. This should be used by the Activity
	 * to display the points of interest on the map. Since we use a hashmap
	 * we can use one of it's properties avoid concurrency problems. Basically,
	 * A ConcurrentModificationException is thrown if the database is being
	 * modified at the same time as it is being read. We can catch that
	 * exception and ignore it.
	 *
	 * @return the list of our points of interests.
	 */
	public Set<PointOfInterest> getDatabase()
	{
		//return our database.
		return this.database;
	}

	/**
	 * Allows other classes to set whether we are to look for updates when
	 * calling the start() method. This should be set by the Activity class
	 * before start() is called by it.
	 *
	 * @param isUpdate whether or not to retrieve updates.
	 */
	public void setUpdate(boolean isUpdate)
	{
		//set whether we retrieve updates or not.
		this.isUpdate = isUpdate;
	}

	/**
	 * This should be called by the class that binds to us so that we start
	 * processing our database and start recieving location updates. Otherwise,
	 * we do nothing. Only does something the first time it is called.
	 */
	public void start()
	{
		//if this method hasn't been run, we run it.
		if (this.isStarted == false)
		{
			//create a thread to run.
			Thread thread = new Thread(this);

			//start the thread.
			thread.start();
		}

		//set this to true, so we never run this method again.
		this.isStarted = true;
	}
	
	/**
	 * Variable to keep track of whether we've run the start() method or not.
	 */
	private boolean isStarted;

	/**
	 * Reference to the activity, we use this to display dialogs and play messages.
	 */
	private TourGuideActivity tourGuideActivity;

	/**
	 * Reference to our internal database of points of interest.
	 */
	private Set<PointOfInterest> database;

	/**
	 * Reference to our current location.
	 */
	private WPSLocation location;

	/**
	 * Reference to our binder object. It enables applications to bind to us.
	 * See our LocalBinder class for more details.
	 */
	private final Binder localBinder = new LocalBinder();

	/**
	 * Whether or not we are to check for database updates.
	 */
	private boolean isUpdate;
}
