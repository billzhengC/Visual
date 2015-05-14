package org.geotools.visual;

import java.util.HashMap;

class Data {
	public static final int frameHeight = 700; // height of the frame
	
	/*
	 * test whether a service id is in service at a given day
	 */
	public static boolean testInService(int day, String sid) {
		boolean res = false;
		if (day==6)
			if (sid.contains("SUN")) res = true; else;
		else if (day==5)
			if (sid.contains("SAT")) res = true; else;
		else if (day==0)
			if (sid.contains("MON")||sid.contains("WKD")) res = true; else;
		else if (sid.contains("WKD")) res = true;
		
		return res;
	}
	/*
	 * method to conver a int into a date
	 */
	public static String intToDate(int day) {
		switch (day) {
		case 0:
			return "Monday";
		case 1:
			return "Tuesday";			
		case 2:
			return "Wednesday";			
		case 3:
			return "Thursday";			
		case 4:
			return "Friday";			
		case 5:
			return "Saturday";			
		case 6:
			return "Sunday";			
		default:
			return "Error"; // return error otherwise
		}
	}
	public static HashMap<String,String> csvFiles;  // a map to store different csv file
	public static final String NYCROAD = "data/NYC/nyctsubwayroutes_100627/transformed.shp"; // file of nyc road
	public static final String WORLD = "data/TM_WORLD_BORDERS-0.3/transformedWorld.shp"; // file of world
	/*
	 * method to initialize the map of csv files
	 */
	public static void intializeCSVFile() {
		csvFiles = new HashMap<String,String>();
		csvFiles.put("CALENDAER","data/NYC/calendar.txt");
		csvFiles.put("AGENCY","data/NYC/agency.txt");
		csvFiles.put("ROUTES","data/NYC/routes.txt");
		csvFiles.put("TRIPS","data/NYC/trips.txt");
		csvFiles.put("STOPTIMES","data/NYC/stop_times.txt");
		csvFiles.put("STOPS","data/NYC/stops.txt");
		csvFiles.put("SHAPES","data/NYC/shapes.txt");
		
	}
}
