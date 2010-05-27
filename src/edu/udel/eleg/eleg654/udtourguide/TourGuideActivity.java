package edu.udel.eleg.eleg654.udtourguide;

import java.io.FileInputStream;
import java.util.ConcurrentModificationException;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.widget.Toast;

import com.google.android.maps.GeoPoint;
import com.google.android.maps.MapActivity;
import com.google.android.maps.MapController;
import com.google.android.maps.MapView;
import com.skyhookwireless.wps.WPSLocation;

/*
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
 * 
 * @author Aaron Myles Landwehr
 */
public class TourGuideActivity extends MapActivity
{
	/**
	 * This class allows us to implement map overlays on top of google maps. We use it to display points of interest and our current
	 * location.
	 */
	private class MapOverlay extends com.google.android.maps.Overlay
	{
		/**
		 * Draw method that we override to display what we want to.
		 * 
		 * @param canvas
		 *            The Canvas upon which to draw.
		 * @param mapView
		 *            the MapView that requested the draw.
		 * @param shadow
		 *            If true, draw the shadow layer. If false, draw the overlay contents.
		 * @param when
		 *            The timestamp of the draw.
		 * 
		 * @return True if you need to be drawn again right away; false otherwise. Default implementation returns false.
		 */
		@Override
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when)
		{
			// call the mama.
			super.draw(canvas, mapView, shadow);

			// get the current location from our service.
			WPSLocation location = TourGuideActivity.this.tourGuideService.getLocation();

			// make sure a location was already retrieved.
			if (location != null)
			{
				// location was retrieved, so let us set it.

				// get the GeoPoint representation of our location.
				GeoPoint myGeoPointLocation = new GeoPoint((int) (location.getLatitude() * 1E6), (int) (location.getLongitude() * 1E6));

				// create a pixel point to hold the location in.
				Point myLocationPoint = new Point();

				// translate the location to pixels.
				mapView.getProjection().toPixels(myGeoPointLocation, myLocationPoint);

				// create new paint to use to draw the location.
				Paint paint = new Paint();
				paint.setColor(255);
				paint.setStyle(Paint.Style.FILL);
				paint.setARGB(255, 0, 0, 255);

				// draw the point.
				canvas.drawCircle(myLocationPoint.x, myLocationPoint.y, 5, paint);
			}

			try
			{
				// for each point of interest in the database display circles on the map.
				for (PointOfInterest destination : TourGuideActivity.this.tourGuideService.getDatabase())
				{

					// get the geoPoint representation of our point of
					// interest's location.
					GeoPoint geoPointLocation = new GeoPoint((int) (destination.gpsCenterPoint.getLatitude() * 1E6),
							(int) (destination.gpsCenterPoint.getLongitude() * 1E6));

					// get the geoPoint representation of our point of
					// interest's radius.
					GeoPoint geoPointRadius = new GeoPoint((int) (destination.gpsRadiusPoint.getLatitude() * 1E6),
							(int) (destination.gpsRadiusPoint.getLongitude() * 1E6));

					// Create pixel points to hold both location and radius
					// in.
					Point locationPoint = new Point();
					Point radiusPoint = new Point();

					// Translate the geopoint's to pixels.
					mapView.getProjection().toPixels(geoPointLocation, locationPoint);
					mapView.getProjection().toPixels(geoPointRadius, radiusPoint);

					// get the radius we need for the point of interest by
					// using
					// the distance formula.
					float screenRadius = (float) Math.sqrt((Math.pow(radiusPoint.x - locationPoint.x, 2.0) + Math.pow(radiusPoint.y
							- locationPoint.y, 2.0)));

					// create paint for the point of interest.
					Paint paint = new Paint();
					paint.setColor(255);
					paint.setStyle(Paint.Style.FILL);
					paint.setARGB(64, 255, 0, 0);
					// draw the point of interest.
					canvas.drawCircle(locationPoint.x, locationPoint.y, screenRadius, paint);
				}
			}
			catch (ConcurrentModificationException e)
			{
				// we just use this guy, so allow concurrent modification, because the worst that can happen is that
				// some points don't get displayed for one frame.
				// just ignore it.
			}

			// redraw.
			return true;
		}
	}

	/**
	 * Callback class that occurs everytime we bind to a service(because we pass a reference to the instance of this class when binding). We
	 * use it to start up our service to do work(update and process it's database) as well as initialize map overlays to be displayed on
	 * google maps.
	 */
	private ServiceConnection connectionToMyService = new ServiceConnection()
	{
		/**
		 * callback method that occurs when the service is connected.
		 */
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder)
		{
			// get a reference to our service.
			TourGuideActivity.this.tourGuideService = ((TourGuideService.LocalBinder) iBinder).getService();

			// set whether or not our service is to update the local DB.
			TourGuideActivity.this.tourGuideService.setUpdate(TourGuideActivity.this.isUpdate);

			// give the service our current context.
			TourGuideActivity.this.tourGuideService.setActivity(TourGuideActivity.this.tourGuideActivity);

			// start the service into processing it's database.
			// note: the service was created prior to us connecting to it,
			// we create this start() method ourselves simply to fork a thread
			// for updates.
			TourGuideActivity.this.tourGuideService.start();

			// get the map view from the resource id.
			MapView mapView = (MapView) TourGuideActivity.this.findViewById(R.id.mapView);

			// set the map to use its built in zoom controls so we can zoom.
			mapView.setBuiltInZoomControls(true);

			// set the map to be clickable otherwise we can't click the zoom controls
			// or move around.
			mapView.setClickable(true);

			// get the map controller to zoom in as far as possible.
			MapController mapControl = mapView.getController();
			mapControl.setZoom(21);

			// create a map overlay.
			MapOverlay mapOverlay = new MapOverlay();

			// clear any current overlays(they may be associated with a previous instance
			// of our activity because of orientation change, etc- so we make sure to clear
			// them.
			mapView.getOverlays().clear();

			// add our new map overlay.
			mapView.getOverlays().add(mapOverlay);
		}

		/**
		 * Callback method that occurs when our service is disconnected. We don't use this be are required to Override it.
		 */
		@Override
		public void onServiceDisconnected(ComponentName componentName)
		{
			// set the service to null.
			TourGuideActivity.this.tourGuideService = null;
		}
	};

	/**
	 * Whether or not to update the database. Sent to our service once we bind to it.
	 */
	private boolean isUpdate;

	/**
	 * media player we use to play sounds with.
	 */
	private MediaPlayer mediaPlayer = new MediaPlayer();

	/**
	 * Reference to our activity and context so that our inner classes can use it.
	 */
	private TourGuideActivity tourGuideActivity = this;

	/**
	 * Reference to the service so that we can directly call its methods. We basically need to start it via its start() method and get the
	 * current location from it as well as points of interests to draw.
	 */
	private TourGuideService tourGuideService;

	/**
	 * message handler we use to run Runnable code that is sent from other threads. Currently used to display dialogs and play sounds.
	 */
	Handler handler = new Handler();

	/**
	 * Called when our activity is created. This can occur on application start or orientation change. Or pretty much whenever Android feels
	 * like it. We uses it to setup our map view and to show the start dialog if the program just started or to bind to our service if it
	 * has already been created in the case where orientation changes occurred.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		// call the papa.
		super.onCreate(savedInstanceState);

		// set our current content to be displayed.
		this.setContentView(R.layout.main);

		// check to see if the application just started.
		if (savedInstanceState == null)
		{
			// application just started.

			// prefetch dialogs to avoid errors later.
			this.prefetchDialogs();

			// check if the DB exists and display start dialog if it does.
			if (this.getFileStreamPath(TourGuideStatics.databaseFile).exists())
			{
				// DB exists, ask the user if they want to update it.
				this.showDialog(TourGuideStatics.DIALOG_START);
			}
			else
			{
				// DB doesn't exist. Force install the database.

				// set that we are updating.
				this.isUpdate = true;

				// start our service.
				this.startService(new Intent(this.tourGuideActivity, TourGuideService.class));

				// bind to our service so we can call it's methods later.
				this.bindService(new Intent(this.tourGuideActivity, TourGuideService.class), this.connectionToMyService, BIND_AUTO_CREATE);

				// pop a toast.
				Toast toast = Toast.makeText(this, "Local database hasn't been installed yet, downloading from the internet...", 0);
				toast.show();
			}
		}
		else
		{
			// application didn't just start. We already have a running service.

			// just bind to our service.
			this.bindService(new Intent(this, TourGuideService.class), this.connectionToMyService, BIND_AUTO_CREATE);
		}
	}

	/**
	 * we use this to prefetch our progress dialogs, because we don't want Android throwing errors if we attempt to dismiss a dialog we've
	 * never shown(it wouldn't have been created yet). This way it is created always.
	 */
	private void prefetchDialogs()
	{
		// show the progress dialogs
		this.showDialog(TourGuideStatics.DIALOG_PROGRESS_INDETERMINATE);
		this.showDialog(TourGuideStatics.DIALOG_PROGRESS);

		// dismiss the progress dialogs.
		this.dismissDialog(TourGuideStatics.DIALOG_PROGRESS_INDETERMINATE);
		this.dismissDialog(TourGuideStatics.DIALOG_PROGRESS);
	}

	/**
	 * Google requires this, because it wants to know 'for accounting purposes' whether or not you are currently displaying any route
	 * information.
	 */
	@Override
	protected boolean isRouteDisplayed()
	{
		// we are not displaying any routes.
		return false;
	}

	/**
	 * This method allows us to manage what happens when android creates our dialogs. We use it to set things like texts, buttons, etc. We
	 * current use a hack to create dynamic dialog text, etc because Android 2.1 and lower APIs have no way to give this guy information to
	 * change the dialog. So instead we use static variables that are not thread safe.
	 * 
	 * Android 2.2. Solves this issue. This will be changed when I have access to a phone with API8.
	 */
	@Override
	// FIXME: protected Dialog onCreateDialog(int id, Bundle args)
	protected Dialog onCreateDialog(int id)
	{
		Dialog dialog;
		switch (id)
		{
			// create our exit dialog.
			case TourGuideStatics.DIALOG_EXIT:
			{
				AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
				alertBuilder.setCancelable(false);
				alertBuilder.setMessage(TourGuideStatics.DIALOG_EXIT_TEXT);
				alertBuilder.setPositiveButton("Exit", new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface arg0, int arg1)
					{
						System.exit(0);
					}
				});
				dialog = alertBuilder.create();
				break;
			}
				// create our startup dialog.
			case TourGuideStatics.DIALOG_START:
			{
				// use the alert dialog builder with our activity as the context.
				AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);

				// make the dialog unable to be backed out of by the user.
				alertBuilder.setCancelable(false);

				// write the dialog message.
				alertBuilder.setMessage("Welcome to UDTourGuide!\n" + "Do you want to update the database?\n(Requires Internet Access)");

				// setup what happens when the Yes button is clicked.
				alertBuilder.setPositiveButton("Yes", new OnClickListener()
				{
					/**
					 * Code that is run when the button is clicked.
					 */
					@Override
					public void onClick(DialogInterface arg0, int arg1)
					{
						// user asked for updates. to set it.
						TourGuideActivity.this.isUpdate = true;

						// start our service.
						TourGuideActivity.this.startService(new Intent(TourGuideActivity.this.tourGuideActivity, TourGuideService.class));

						// bind to our service using our activity as the context.
						TourGuideActivity.this.bindService(new Intent(TourGuideActivity.this.tourGuideActivity, TourGuideService.class),
								TourGuideActivity.this.connectionToMyService, BIND_AUTO_CREATE);

						// pop a toast.
						Toast toast = Toast.makeText(TourGuideActivity.this.tourGuideActivity,
								"Don't worry, internet access is only needed for updates and google maps!", 0);
						toast.show();
					}
				});

				// setup what happens when the No button is clicked.
				alertBuilder.setNegativeButton("No Thanks", new OnClickListener()
				{
					/**
					 * Code that is run when the button is clicked.
					 */
					@Override
					public void onClick(DialogInterface arg0, int arg1)
					{
						// user doesn't want updates. so unset it.
						TourGuideActivity.this.isUpdate = false;

						// start our service.
						TourGuideActivity.this.startService(new Intent(TourGuideActivity.this.tourGuideActivity, TourGuideService.class));

						// bind to our service using our activity as the context.
						TourGuideActivity.this.bindService(new Intent(TourGuideActivity.this.tourGuideActivity, TourGuideService.class),
								TourGuideActivity.this.connectionToMyService, BIND_AUTO_CREATE);
					}
				});

				// actually create the dialog from the builder.
				dialog = alertBuilder.create();
				break;
			}
				// create our blurb dialog.
			case TourGuideStatics.DIALOG_BLURB:
			{
				// use the alert dialog builder to create our alert dialog.
				AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);

				// make the alert dialog not backoutable.
				alertBuilder.setCancelable(false);

				// set a default message- we need this to be before creation.
				alertBuilder.setMessage("");

				// tell the builder we want a play button- don't worry, we setup
				// its callback in onPrepareDialog().
				alertBuilder.setPositiveButton("Play", null);

				// tell the builder we want a No button- don't worry, we setup
				// its callback in onPrepareDialog().
				alertBuilder.setNegativeButton("No Thanks", null);

				// create the alert dialog.
				dialog = alertBuilder.create();
				break;
			}
				// create our determinate progress dialog. i.e. x out of 100.
			case TourGuideStatics.DIALOG_PROGRESS:
			{
				// create a new progress dialog.
				ProgressDialog progressDialog = new ProgressDialog(this.tourGuideActivity);

				// set its style determinate with a horizontal progress bar.
				progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

				// make it non-cancelable.
				progressDialog.setCancelable(false);

				// make sure it's not indeterminate(otherwise we get a progress bar that never fills in).
				progressDialog.setIndeterminate(false);

				// setup the text of our message- though we have to do it again in onPrepareDialog() anyway.
				progressDialog.setMessage(TourGuideStatics.DIALOG_PROGRESS_TEXT);

				// setup the progress we've made- though we have to do it again in onPrepareDialog() anyway.
				progressDialog.setProgress(TourGuideStatics.DIALOG_PROGRESS_PROGRESS);

				// setup the total so android knows our precentage done- though we have to do it again in onPrepareDialog() anyway.
				progressDialog.setMax(TourGuideStatics.DIALOG_PROGRESS_MAX);

				// Android 2.2 stuff below:
				// FIXME: progressDialog.setMessage(args.getString(TourGuideStatics.KEY_TEXT));
				// FIXME: progressDialog.setProgress(args.getInt(TourGuideStatics.KEY_PROGRESS));
				// FIXME: progressDialog.setMax(args.getInt(TourGuideStatics.KEY_MAX));

				// set the dialog to return to our newly created progress dialog.
				dialog = progressDialog;
				break;
			}
				// create our indeterminate progress dialog.
			case TourGuideStatics.DIALOG_PROGRESS_INDETERMINATE:
			{
				// create a new progress dialog.
				ProgressDialog progressDialog = new ProgressDialog(this.tourGuideActivity);

				// set it to the spinner looking one(which is only indeterminate afaik).
				progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

				// make it uncancelable.
				progressDialog.setCancelable(false);

				// make it indeterminate for good measure.
				progressDialog.setIndeterminate(true);

				// set the text- even though we have to do this in onPrepareDialog() again anyway.
				progressDialog.setMessage(TourGuideStatics.DIALOG_PROGRESS_TEXT);

				// Android 2.2 stuff below:
				// FIXME: progressDialog.setMessage(args.getString(TourGuideStatics.KEY_TEXT));

				// set the dialog to return our newly created progress dialog.
				dialog = progressDialog;
				break;
			}
			default:
			{
				// WTF: who could this non existent dialog?
				dialog = null;
			}
		}

		// return our newly created dialog.
		return dialog;
	}

	/**
	 * This callback is called everytime the Android activity gets destroyed. Which, is quite often. We use to detect when the back button
	 * has been pressed in which case we end our app so that we don't wast the GPS.
	 */
	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		// is the application about to die?
		if (this.isFinishing())
		{
			// Red warrior is about to die!
			System.exit(0);
		}
	}

	/**
	 * This method allows us to manage what happens after showDialog() is called. We use it to modify the text and some other things in our
	 * dialogs. I.e. we have dynamic dialogs.
	 * 
	 * We current use a hack to create dynamic dialog text, etc because Android 2.1 and lower APIs have no way to give this guy information
	 * to change the dialog. So instead we use static variables that are not thread safe.
	 * 
	 * Android 2.2. Solves this issue. This will be changed when I have access to a phone with API8.
	 */
	@Override
	// FIXME: protected void onPrepareDialog(int id, Dialog dialog, Bundle args)
	protected void onPrepareDialog(int id, Dialog dialog)
	{
		switch (id)
		{
			// if we are showing an exit dialog.
			case TourGuideStatics.DIALOG_EXIT:
			{
				// just reset the message.
				((AlertDialog) dialog).setMessage(TourGuideStatics.DIALOG_EXIT_TEXT);
				break;
			}
				// if we are showing a blurb dialog.
			case TourGuideStatics.DIALOG_BLURB:
			{
				// reset the message to say about the new location we are near.
				((AlertDialog) dialog).setMessage("Near '" + TourGuideStatics.DIALOG_BLURB_LOCATION + "'\nPlay blurb?");

				// setup the code to run when the play button is clicked.
				((AlertDialog) dialog).setButton("Play", new OnClickListener()
				{
					/**
					 * code to run.
					 */
					@Override
					public void onClick(DialogInterface arg0, int arg1)
					{
						try
						{
							// reset the media player associated with this activity and play the file.
							TourGuideActivity.this.mediaPlayer.reset();
							TourGuideActivity.this.mediaPlayer.setDataSource(new FileInputStream(TourGuideActivity.this.tourGuideActivity
									.getFileStreamPath(TourGuideStatics.DIALOG_BLURB_FILE)).getFD());
							TourGuideActivity.this.mediaPlayer.prepare();
							TourGuideActivity.this.mediaPlayer.start();
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				});
				break;
			}
				// if we are showing a determinate progress dialog.
			case TourGuideStatics.DIALOG_PROGRESS:
			{
				// reset the text.
				((ProgressDialog) dialog).setMessage(TourGuideStatics.DIALOG_PROGRESS_TEXT);

				// reset the progress.
				((ProgressDialog) dialog).setProgress(TourGuideStatics.DIALOG_PROGRESS_PROGRESS);

				// reset the max
				((ProgressDialog) dialog).setMax(TourGuideStatics.DIALOG_PROGRESS_MAX);

				// Android 2.2 stuff below:
				// FIXME: ((ProgressDialog) dialog).setMessage(args.getString(TourGuideStatics.KEY_TEXT));
				// FIXME: ((ProgressDialog) dialog).setProgress(args.getInt(TourGuideStatics.KEY_PROGRESS));
				// FIXME: ((ProgressDialog) dialog).setMax(args.getInt(TourGuideStatics.KEY_MAX));
				break;
			}
				// if we are showing an indeterminate progress dialog.
			case TourGuideStatics.DIALOG_PROGRESS_INDETERMINATE:
			{
				// set the text.
				((ProgressDialog) dialog).setMessage(TourGuideStatics.DIALOG_PROGRESS_TEXT);

				// Android 2.2 stuff below:
				// FIXME: ((ProgressDialog) dialog).setMessage(args.getString(TourGuideStatics.KEY_TEXT));
				break;
			}
		}
	}

	/**
	 * This callback is called when the user hits the home button. We are killing the app if that occurs because I don't think it is useful
	 * running in the back- ground, nor do I think the Activity life-cycle necessarily makes sense.
	 */
	@Override
	protected void onUserLeaveHint()
	{
		super.onUserLeaveHint();

		// Blue warrior needs food badly!
		System.exit(0);
	}
}