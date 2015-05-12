package org.geotools.visual;

import java.util.HashMap;

class Data {
	public static final String[] weekdayService = {"A20141207WKD","B20141207WKD","R20140615WKD"};
	public static final String[] satService = {"A20141207SAT","B20141207SAT","R20140615SAT"};
	public static final String[] sunService = {"A20141207SUN","B20141207SUN","R20140615SUN"};
	public static final String[] monService = {"S20140615MON"};
	
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
			return "Error";
		}
	}
	public static HashMap<String,String> csvFiles; 
	//public static final String NYCROAD = "data/NYC/shp/roads.shp";
	public static final String NYCROAD = "data/NYC/nyctsubwayroutes_100627/transformed.shp"; 
	//public static final String WORLD = "data/50m_cultural/ne_50m_admin_0_sovereignty.shp";
	public static final String WORLD = "data/TM_WORLD_BORDERS-0.3/transformedWorld.shp";	
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
