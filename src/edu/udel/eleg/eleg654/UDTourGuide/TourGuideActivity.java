package edu.udel.eleg.eleg654.UDTourGuide;

import java.io.FileInputStream;
import java.util.ConcurrentModificationException;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
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
import com.google.android.maps.Overlay;
import com.skyhookwireless.wps.WPSLocation;

public class TourGuideActivity extends MapActivity
{
	class MapOverlay extends com.google.android.maps.Overlay
	{
		@Override
		public boolean draw(Canvas canvas, MapView mapView, boolean shadow, long when)
		{
			super.draw(canvas, mapView, shadow);
			WPSLocation location = null;
			
			if(tourGuideService != null)
				location  = tourGuideService.getLocation();

			if (location != null)
			{
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
				if(tourGuideService != null)
				for (PointOfInterest destination : tourGuideService.getDatabase())
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
				// just ignore it.
			}
			return true;
		}
	}

	@Override
	protected boolean isRouteDisplayed()
	{
		// TODO Auto-generated method stub
		return false;
	}

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		
		if (savedInstanceState == null)
		{
			if (getFileStreamPath(TourGuideStatics.databaseFile).exists())
			{
				this.showDialog(TourGuideStatics.DIALOG_START);
			}
			else
			{
				this.isUpdate = true;
				startService(new Intent(tourGuideActivity, TourGuideService.class));
				bindService(new Intent(tourGuideActivity, TourGuideService.class), connectionToMyService, BIND_AUTO_CREATE);

				Toast toast = Toast.makeText(this,
						"Local database hasn't been installed yet, downloading from the internet...", 0);
				toast.show();
			}
		}
		else
		{
			bindService(new Intent(this, TourGuideService.class), connectionToMyService, BIND_AUTO_CREATE);
		}
	}

	@Override
	protected Dialog onCreateDialog(int id)
	{
		Dialog dialog;
		switch (id)
		{
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
			case TourGuideStatics.DIALOG_START:
			{
				AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
				alertBuilder.setCancelable(false);
				alertBuilder.setMessage("Welcome to UDTourGuide!\n" + "Do you want to update the database?\n(Requires Internet Access)");
				alertBuilder.setPositiveButton("Yes", new OnClickListener()
				{
					@Override
					public void onClick(DialogInterface arg0, int arg1)
					{
						isUpdate = true;
						startService(new Intent(tourGuideActivity, TourGuideService.class));
						bindService(new Intent(tourGuideActivity, TourGuideService.class), connectionToMyService, BIND_AUTO_CREATE);

						Toast toast = Toast.makeText(tourGuideActivity,
								"Don't worry, internet access is only needed for updates and google maps!", 0);
						toast.show();
					}
				});
				alertBuilder.setNegativeButton("No Thanks", new OnClickListener()
				{

					@Override
					public void onClick(DialogInterface arg0, int arg1)
					{
						isUpdate = false;
						startService(new Intent(tourGuideActivity, TourGuideService.class));
						bindService(new Intent(tourGuideActivity, TourGuideService.class), connectionToMyService, BIND_AUTO_CREATE);
					}
				});
				dialog = alertBuilder.create();
				break;
			}
			case TourGuideStatics.DIALOG_BLURB:
			{
				AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
				alertBuilder.setCancelable(false);
				alertBuilder.setMessage("test");
				alertBuilder.setPositiveButton("Play", null);
				alertBuilder.setNegativeButton("No Thanks", null);
				dialog = alertBuilder.create();
				break;
			}
			case TourGuideStatics.DIALOG_PROGRESS:
			{
				ProgressDialog progressDialog = new ProgressDialog(tourGuideActivity);
				progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
				progressDialog.setCancelable(false);
				progressDialog.setIndeterminate(false);
				progressDialog.setMessage(TourGuideStatics.DIALOG_PROGRESS_TEXT);
				progressDialog.setProgress(TourGuideStatics.DIALOG_PROGRESS_AMOUNT);
				progressDialog.setMax(TourGuideStatics.DIALOG_PROGRESS_TOTAL);
				dialog = progressDialog;
				break;
			}
			case TourGuideStatics.DIALOG_PROGRESS_INDETERMINATE:
			{
				ProgressDialog progressDialog = new ProgressDialog(tourGuideActivity);
				progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				progressDialog.setCancelable(false);
				progressDialog.setIndeterminate(true);
				progressDialog.setMessage(TourGuideStatics.DIALOG_PROGRESS_TEXT);
				dialog = progressDialog;
				break;
			}
			default:
			{
				dialog = null;
			}
		}
		return dialog;
	}
	
	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		//if(mapView != null)
		//{
		//	mapView.getOverlays().clear();
		//	mapView.invalidate();
		//}
		if(isFinishing())
		{
			System.exit(0);
		}
	}

	@Override
	protected void onUserLeaveHint()
	{ 
	   super.onUserLeaveHint(); 
	   System.exit(0);
	} 

	@Override
	protected void onPrepareDialog(int id, Dialog dialog)
	{
		switch (id)
		{
			case TourGuideStatics.DIALOG_EXIT:
			{
				((AlertDialog) dialog).setMessage(TourGuideStatics.DIALOG_EXIT_TEXT);
				break;
			}
			case TourGuideStatics.DIALOG_BLURB:
			{
				((AlertDialog) dialog).setMessage("Near '" + TourGuideStatics.DIALOG_BLURB_LOCATION + "'\nPlay blurb?");
				((AlertDialog) dialog).setButton("Play", new OnClickListener()
				{

					@Override
					public void onClick(DialogInterface arg0, int arg1)
					{
						try
						{
							mediaPlayer.reset();
							mediaPlayer.setDataSource(new FileInputStream(tourGuideActivity
									.getFileStreamPath(TourGuideStatics.DIALOG_BLURB_FILE)).getFD());
							mediaPlayer.prepare();
							mediaPlayer.start();
						}
						catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				});
				break;
			}
			case TourGuideStatics.DIALOG_PROGRESS:
			{
				((ProgressDialog) dialog).setMessage(TourGuideStatics.DIALOG_PROGRESS_TEXT);
				((ProgressDialog) dialog).setProgress(TourGuideStatics.DIALOG_PROGRESS_AMOUNT);
				((ProgressDialog) dialog).setMax(TourGuideStatics.DIALOG_PROGRESS_TOTAL);
				break;
			}
			case TourGuideStatics.DIALOG_PROGRESS_INDETERMINATE:
			{
				((ProgressDialog) dialog).setMessage(TourGuideStatics.DIALOG_PROGRESS_TEXT);
				break;
			}
		}
	}

	private MediaPlayer mediaPlayer = new MediaPlayer();
	Handler handler = new Handler();
	private TourGuideActivity tourGuideActivity = this;
	private boolean isUpdate;
	private TourGuideService tourGuideService;

	private ServiceConnection connectionToMyService = new ServiceConnection()
	{

		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder)
		{
			tourGuideService = ((TourGuideService.LocalBinder) iBinder).getService();
			
			tourGuideService.setUpdate(isUpdate);
			tourGuideService.setActivity(tourGuideActivity);
			tourGuideService.start();
			
			MapView mapView = (MapView) findViewById(R.id.mapView);

			mapView.setBuiltInZoomControls(true);
			mapView.setClickable(true);

			MapController mapControl = mapView.getController();
			mapControl.setZoom(21);

			MapOverlay mapOverlay = new MapOverlay();
			mapView.getOverlays().clear();
			mapView.getOverlays().add(mapOverlay);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName)
		{
			tourGuideService = null;
		}
	};
}