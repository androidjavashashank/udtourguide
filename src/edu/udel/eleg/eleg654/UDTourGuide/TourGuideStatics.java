package edu.udel.eleg.eleg654.udtourguide;

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
 * Simple static variables needed throughout the project. The first two are the server path and the database name. These two will always be
 * needed. The rest are related to dialog usage in the program. There are a number of entries that I consider to be hacks that won't be
 * needed when the Android 2.2 API is used.
 * 
 * @author Aaron Myles Landwehr
 */
final public class TourGuideStatics
{
	public static String server = "http://www.snaphat.com:80/udtourguide/";
	public static String databaseFile = "locations.db";

	/**
	 * Different types of dialogs
	 */
	public static final int DIALOG_EXIT = -1;
	public static final int DIALOG_START = 0;
	public static final int DIALOG_BLURB = 1;
	public static final int DIALOG_PROGRESS = 2;
	public static final int DIALOG_PROGRESS_INDETERMINATE = 3;

	/**
	 * Hackish way to get different dialog associated information to change when onPrepareDialog() is called. We set this stuff before
	 * calling showDialog()
	 * 
	 * In android 2.2 this is not needed because, Android 2.2 allows Bundles of arguments to be passed to showDialog(). So we can pass data
	 * that way.
	 * 
	 * Note: THIS COULD FAIL IF THESE STATICS WERE CHANGED BEFORE A DIALOG WAS PROCESSED BY ANDROID.
	 */
	public static String DIALOG_EXIT_TEXT = "";
	public static String DIALOG_BLURB_FILE = "";
	public static String DIALOG_BLURB_LOCATION = "";
	public static String DIALOG_PROGRESS_TEXT = "";
	public static int DIALOG_PROGRESS_PROGRESS = 1;
	public static int DIALOG_PROGRESS_MAX = 1;

	/**
	 * Below is for the Android 2.2 API. We ideally want to uses these in the future.
	 */
	// FIXME: public static String KEY_TEXT = "text";
	// FIXME: public static String KEY_PROGRESS = "progress";
	// FIXME: public static String KEY_MAX = "max";
}
