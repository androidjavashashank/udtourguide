package edu.udel.eleg.eleg654.udtourguide;

final public class TourGuideStatics
{
	public static String server = "http://www.snaphat.com:80/udtourguide/";

	public static String databaseFile = "locations.db";

	public static final int DIALOG_EXIT = -1;

	public static final int DIALOG_START = 0;

	public static final int DIALOG_BLURB = 1;

	public static final int DIALOG_PROGRESS = 2;

	public static final int DIALOG_PROGRESS_INDETERMINATE = 3;

	public static final int UPDATE_DATABASE = 1;

	public static final int DONT_UPDATE_DATABASE = -1;

	public static String DIALOG_EXIT_TEXT;

	public static String DIALOG_BLURB_FILE;

	public static String DIALOG_BLURB_LOCATION;

	public static String DIALOG_PROGRESS_TEXT;

	public static int DIALOG_PROGRESS_AMOUNT;

	public static int DIALOG_PROGRESS_TOTAL;
}
