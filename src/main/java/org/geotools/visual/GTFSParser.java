package org.geotools.visual;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.opencsv.CSVReader;
import com.vividsolutions.jts.geom.Coordinate;

class GTFSParser {
	static ArrayList<Coordinate> shapePointList;
	static ArrayList<Map<String,String>> tripList;
	static ArrayList<Map<String,String>> shapeList;
	static ArrayList<Map<String,String>> serviceList;
	static ArrayList<Map<String,String>> stopList;
	static ArrayList<Map<String,String>> stoptimeList;
	static HashMap<String,Trajectory> trajMap;
	
	
	protected static ArrayList<Map<String,String>> readCSV(Reader csv) throws IOException {
		ArrayList<Map<String,String>> csvList = new ArrayList<Map<String,String>>();
		CSVReader myreader = new CSVReader(csv);
		String[] firstLine = myreader.readNext();
		int itemNum = firstLine.length;
		Map<String,String> lineMap;
		String[] nextLine;
		while ((nextLine = myreader.readNext()) != null) {
			lineMap = new HashMap<String,String>();
			for (int i=0; i<itemNum; i++) {
				lineMap.put(firstLine[i], nextLine[i]);
			}
			csvList.add(lineMap);
		}
		myreader.close();
		return csvList;				
	}
	
	/*
	 *  parse CSV file into a map (tripid-trajectory)
	 */
	
	public static HashMap<String,Trajectory> parseTrips(HashMap<String,String> csvFileNames) throws IOException, FactoryException, TransformException {
		trajMap = new HashMap<String,Trajectory>();
		/*
		 *  read input from CSV
		 */
		tripList = readCSV(new FileReader(csvFileNames.get("TRIPS")));
		serviceList = readCSV(new FileReader(csvFileNames.get("CALENDAER")));
		shapeList = readCSV(new FileReader(csvFileNames.get("SHAPES")));
		stopList = readCSV(new FileReader(csvFileNames.get("STOPS")));
		stoptimeList = readCSV(new FileReader(csvFileNames.get("STOPTIMES")));
		// intermediate product: connection between shapeid and tripid
		HashMap<String,ArrayList<String>> shapeTripMap = new HashMap<String,ArrayList<String>>(); 
		/*
		 * get trip info and create hashmap for all trips 
		 */
		for (Map<String,String> trip: tripList) {
			String tripid = trip.get("trip_id");
			String serviceid = trip.get("service_id");
			String shapeid = trip.get("shape_id");
			if (shapeTripMap.get(shapeid) == null) {
				ArrayList<String> temp_list = new ArrayList<String>();
				temp_list.add(tripid);
				shapeTripMap.put(shapeid, temp_list);
			}
			else {
				ArrayList<String> temp_list = shapeTripMap.get(shapeid);
				temp_list.add(tripid);
				shapeTripMap.put(shapeid, temp_list);
			}
			Trajectory myTraj = new Trajectory(tripid,serviceid);
			trajMap.put(tripid, myTraj);
		}
		// intermediate product: use stopid to connect stop time and stop location
	    // define inner class Tuple to combine time and tripid
		class Tuple {
			final Long time;
			final String tripid;
			Tuple(Long time, String tripid) {
				this.time = time;
				this.tripid = tripid;
			}
		}
		HashMap<String,ArrayList<Tuple>> stopIdTupleMap = new HashMap<String,ArrayList<Tuple>>(); 
		// import time of each stop
		for (Map<String,String> stoptime: stoptimeList) {
			String tripid = stoptime.get("trip_id");
			String stopid = stoptime.get("stop_id");
			Long time = toElapsedTime(stoptime.get("departure_time"));
			trajMap.get(tripid).trajectory.put(time, null);
			trajMap.get(tripid).trajectoryWithName.put(time, null);
			// fill the intermediate map
			ArrayList<Tuple> temp_arrL;
			if (stopIdTupleMap.get(stopid)==null) temp_arrL = new ArrayList<Tuple>();
			else temp_arrL = stopIdTupleMap.get(stopid);
			temp_arrL.add(new Tuple(time,tripid));
			stopIdTupleMap.put(stopid, temp_arrL);
		}
		// import location of each stop
		for (Map<String,String> stop: stopList) {
			String stopid = stop.get("stop_id");
			double stop_lat = Double.parseDouble(stop.get("stop_lat"));
			double stop_lon = Double.parseDouble(stop.get("stop_lon"));
			// Coordinate loc = new Coordinate(stop_lon,stop_lat);
			Coordinate loc = new Coordinate(stop_lat,stop_lon);
			loc = transCoor(loc);
				if (stopIdTupleMap.get(stopid)==null) continue; //****TEST
			ArrayList<Tuple> tupleList= stopIdTupleMap.get(stopid);
			for (Tuple t:tupleList ) {
				String tripid = t.tripid;
				Long time = t.time;
				trajMap.get(tripid).trajectory.put(time, loc);
				trajMap.get(tripid).trajectoryWithName.put(time, stop.get("stop_name"));				
			}
		}

		
		shapePointList = new ArrayList<Coordinate>();
		for (Map<String,String> shape: shapeList) {
			double shape_lat = Double.parseDouble(shape.get("shape_pt_lat"));
			double shape_lon = Double.parseDouble(shape.get("shape_pt_lon"));
			Coordinate loc_temp = transCoor(new Coordinate(shape_lat,shape_lon));
			shapePointList.add(loc_temp);
		}
		
		
		
/*		// construct a shape_id --> coordinates map
		HashMap<String,ArrayList<Coordinate>> shapePointMap = new HashMap<String,ArrayList<Coordinate>>();
		for (Map<String,String> shape: shapeList) {
			String shapeid = shape.get("shape_id");
			double shape_lat = Double.parseDouble(shape.get("shape_pt_lat"));
			double shape_lon = Double.parseDouble(shape.get("shape_pt_lon"));
			Coordinate loc_temp = new Coordinate(shape_lon,shape_lat);
			if (!shapePointMap.containsKey(shapeid)) {
				shapePointMap.put(shapeid, new ArrayList<Coordinate>());
				shapePointMap.get(shapeid).add(loc_temp);				
			}
			else {
				shapePointMap.get(shapeid).add(loc_temp);
			}
		}
		// iterate over the map
		for (Map.Entry<String,ArrayList<Coordinate>> entry: shapePointMap.entrySet()) {
			String shapeid = entry.getKey();
			ArrayList<Coordinate> cos = entry.getValue();
			ArrayList<String> tripWithSameShape = shapeTripMap.get(shapeid);
			if (tripWithSameShape==null) continue; //TEST**
			for (String tripid_temp: tripWithSameShape) {
				trajMap.get(tripid_temp).trajectory = trajIntoShape(trajMap.get(tripid_temp).trajectory,cos);
			}
		}		
		//**TEST
		for (Map.Entry<String,Trajectory> entry: trajMap.entrySet()) {
			System.out.print(entry.getKey()+"  ");
			System.out.println(entry.getValue().trajectory);
		}*/
		return trajMap;
	}
	

	
	
	// combine shapes and stops
	private static SortedMap<Long,Coordinate> trajIntoShape(SortedMap<Long,Coordinate> tmap, ArrayList<Coordinate> cos) {
		SortedMap<Long,Coordinate> mapProcessed = new TreeMap<Long,Coordinate>();
		// TODO: get stops projections and insert them into Shape Coordiates; then, get time for shape cos 
		return mapProcessed;
	}
	
	/*
	 * transfet Coordinate from WGS84 to Mercator
	 */
	private static Coordinate transCoor(Coordinate coor) throws FactoryException, TransformException {
		Coordinate dest = null;
		CoordinateReferenceSystem wgsCRS = CRS.decode( "EPSG:4326" ); // WGS84 CoordinateReferenceSystem
		CoordinateReferenceSystem merCRS = CRS.decode( "EPSG:3857" ); // Mercator 
		MathTransform wgsToMerTransform = CRS.findMathTransform(wgsCRS, merCRS, true);
		MathTransform merTowgsTransform = CRS.findMathTransform(merCRS, wgsCRS, true);
		dest = JTS.transform(coor, null, wgsToMerTransform );
		if (dest==null) System.out.println("nullEXception!!");
		return dest;
	}
	
	/*
	 * get linear interpolation of coordinates
	 */
	public static Coordinate getLinearCoordinate(Coordinate a,Long ta, Coordinate b, Long tb, Long t){
		return new Coordinate(a.x+(t-ta)*(b.x-a.x)/(tb-ta),a.y+(t-ta)*(b.y-a.y)/(tb-ta));	
	}
	
	
	/*
	 *  convert "HH:MM:SS" into the seconds elasped from midnight
	 */
	public static Long toElapsedTime(String time) {
		StringTokenizer timeToken = new StringTokenizer(time,":");
		Long hours = Long.valueOf(timeToken.nextToken());
		Long mins = Long.valueOf(timeToken.nextToken());
		Long secs = Long.valueOf(timeToken.nextToken());
		return hours*3600 + mins*60 + secs;
	}
}

/*
 * Trajectory is a class stores information of the trajectory of a trip
 */

class Trajectory {
	String trip_id; // trip id
	String service_id; // service
	// construtor: initialize trip id and service id
	Trajectory (String trip_id,String service_id) {
		this.service_id = service_id;
		this.trip_id = trip_id;
	}   
	SortedMap<Long,Coordinate> trajectory = new TreeMap<Long,Coordinate>(); // tree map which projects a time to a location of the vehicles
	SortedMap<Long,String> trajectoryWithName = new TreeMap<Long,String>(); // tree map which projects a time to the name of stops
	
	/*
	 *  get position of the vehicle at a given time
	 */
	Coordinate getPosition(Long time) throws FactoryException, TransformException {
		if (!this.isActive(time)) return null;
		Coordinate coor = null;
		Long thisTime, nextTime;
		Iterator<Long> keyItr= trajectory.keySet().iterator();
		thisTime = keyItr.next();
		/*
		 * use linear interporlation to calculation the location 
		 */
		while (keyItr.hasNext()) {
			nextTime = keyItr.next();
			if (time<nextTime) {
				coor = GTFSParser.getLinearCoordinate(trajectory.get(thisTime),thisTime,trajectory.get(nextTime),nextTime,time);
				return coor;
			}
			thisTime = nextTime;
		}
		return coor;
	}
	/*
	 *  test if the vehicle is active at a given time
	 */
	boolean isActive(Long time) {
		if (trajectory.firstKey() <= time && time<=trajectory.lastKey()) return true;
		else return false;
	}
}

/*
 *  the transit contains infomration about the vehicles
 */
class Transit {
	public static ArrayList<Trajectory> allTraj; // list of all trajectory
	public static HashMap<String,Long> tripTimeStartMap = new HashMap<String,Long>(); // map a trip with its start time
	public static HashMap<String,Long> tripTimeEndMap = new HashMap<String,Long>(); // map a trip with its end time
	/*
	 * intialize tripTimeStartMap&tripTimeEndMap
	 */
	public static void intializeTripTimeMap() {
		for (Trajectory t:allTraj) { // loop over all trajectory
			String tripid = t.trip_id; // get trip id
			Long timeStart = new Long(t.trajectory.firstKey()); // get start time
			Long timeEnd = new Long(t.trajectory.lastKey()); // get end time
			/*
			 * put into two maps
			 */
			tripTimeStartMap.put(tripid, timeStart);
			tripTimeEndMap.put(tripid, timeEnd);
		}
	}
	/*
	 * a method to return all active trajtories at a given time
	 */
	public static ArrayList<Trajectory> activeTrajectories(Long time) {
		ArrayList<Trajectory> activeTraj = new ArrayList<Trajectory>(); // intialize the list
		// loop over all trajectory
		for (Trajectory traj: allTraj) {
			// if the given time falls in the range of a vehicle's active time, then add it to the list
			Long timeStart = tripTimeStartMap.get(traj.trip_id); 
			Long timeEnd = tripTimeEndMap.get(traj.trip_id);
			if (timeStart<=time && time<=timeEnd) {
				activeTraj.add(traj);
			}
		}
		return activeTraj;
	}
}