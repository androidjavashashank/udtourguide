package edu.udel.eleg.eleg654.UDTourGuide;

import android.location.Location;

/**
 * Class that encapsulates a point of interest. It contains the following important information: point of interest' name, location and
 * radius fps points, radius distance, file, and triggered status for a point of interest. It attempts to follow the immutable design
 * pattern where possible.
 * 
 * @author Aaron Myles Landwehr
 */
final class PointOfInterest
{
	/**
	 * The point of interest's name.
	 */
	final String name;
	/**
	 * The GPS coordinates that end the line segment that starts from location. The actual GPS point is important, because we need to
	 * convert it to a pixel location on screen and calculate the pixel radius between the center point and this point to display it as
	 * circle on the map. You may wonder why we can't use radius in meters and somehow convert that to pixels. Well, it is because scale of
	 * the map changes as you move toward poles so the amount of pixels that represent a certain amount of meters depend on where you are on
	 * the map. See the Mercator projection(which is what google maps uses) for details.
	 */
	final Location gpsRadiusPoint;
	/**
	 * The GPS coordinates that are for the center of the point of interest.
	 */
	final Location gpsCenterPoint;
	/**
	 * The radius in meters between the two gps points.
	 */
	final float radius;
	/**
	 * The name of the sound file associated with the point of interest.
	 */
	final String file;
	/**
	 * Whether or not the point of interest's sound file has been triggered. Resets when we are a certain distance outside of the radius.
	 */
	boolean triggered;

	/**
	 * Constructor to simply set all of the data associated with a point of interest.
	 * 
	 * @param name
	 *            the name of the point of interest.
	 * @param locationLatitude
	 *            the center point latitude for the location.
	 * @param locationLongitude
	 *            the center point longitude for the location.
	 * @param radiusLatitude
	 *            the radius point for the latitude of the location.
	 * @param radiusLongitude
	 *            the radius point for the longitude of the location.
	 * @param file
	 *            the name of the file associated with the location.
	 */
	public PointOfInterest(String name, Double locationLatitude, double locationLongitude, double radiusLatitude, double radiusLongitude,
			String file)
	{
		// Set all the fields.
		this.triggered = false;
		this.name = name;
		this.gpsCenterPoint = new Location("");
		this.gpsCenterPoint.setLatitude(locationLatitude);
		this.gpsCenterPoint.setLongitude(locationLongitude);
		this.gpsRadiusPoint = new Location("");
		this.gpsRadiusPoint.setLatitude(radiusLatitude);
		this.gpsRadiusPoint.setLongitude(radiusLongitude);
		this.radius = gpsCenterPoint.distanceTo(gpsRadiusPoint);
		this.file = file;
	}
}
