package edu.udel.eleg.eleg654.udtourguide;

import android.location.Location;

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
 * Class that encapsulates a point of interest. It contains the following important information: point of interest' name, location and
 * radius GPS points, radius distance, file to play back, and triggered status for a point of interest. It attempts to follow the immutable
 * design pattern where possible.
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
	 * The radius in meters between the two GPS points.
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
