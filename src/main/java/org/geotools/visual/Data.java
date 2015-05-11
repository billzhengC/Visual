package org.geotools.visual;

import java.util.HashMap;

import org.geotools.geometry.GeometryBuilder;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

class Data {
	public static GeometryBuilder gbuilderWGS84;
	public static GeometryBuilder gbuilderMercator;
	
	public static HashMap<String,String> csvFiles; 
//	public static final String NYCROAD = "data/NYC/shp/roads.shp";
	public static final String NYCROAD = "data/NYC/nyctsubwayroutes_100627/nyctsubwayroutes_100627.shp"; 
	//public static final String WORLD = "data/50m_cultural/ne_50m_admin_0_sovereignty.shp";
	public static final String WORLD = "data/TM_WORLD_BORDERS-0.3/TM_WORLD_BORDERS-0.3.shp";	
	public static void intializeCSVFile() {
		csvFiles = new HashMap<String,String>();
		csvFiles.put("CALENDAER","data/NYC/calendar.txt");
		csvFiles.put("AGENCY","data/NYC/agency.txt");
		csvFiles.put("ROUTES","data/NYC/routes.txt");
		csvFiles.put("TRIPS","data/NYC/trips.txt");
		csvFiles.put("STOPTIMES","data/NYC/stop_times.txt");
		csvFiles.put("STOPS","data/NYC/stops.txt");
		csvFiles.put("SHAPES","data/NYC/shapes.txt");
//		csvFiles.put("CALENDAER","data/Dublin/calendar.txt");
//		csvFiles.put("AGENCY","data/Dublin/agency.txt");
//		csvFiles.put("ROUTES","data/Dublin/routes.txt");
//		csvFiles.put("TRIPS","data/Dublin/trips.txt");
//		csvFiles.put("STOPTIMES","data/Dublin/stop_times.txt");
//		csvFiles.put("STOPS","data/Dublin/stops.txt");
//		csvFiles.put("SHAPES","data/Dublin/shapes.txt");
	}
	public static void intializeCRS() throws FactoryException {
		CoordinateReferenceSystem wgsCRS = CRS.decode( "EPSG:4326" ); // WGS84 CoordinateReferenceSystem
		CoordinateReferenceSystem merCRS = CRS.decode( "EPSG:3857" ); // Mercator
		gbuilderWGS84 = new GeometryBuilder(wgsCRS);
		gbuilderMercator = new GeometryBuilder(merCRS);
	}
}
