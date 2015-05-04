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

import com.opencsv.CSVReader;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineSegment;

class GTFSParser {
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
	
	public static HashMap<String,Trajectory> parseTrips(HashMap<String,String> csvFileNames) throws IOException {
		HashMap<String,Trajectory> trajMap = new HashMap<String,Trajectory>();
		// read input from CSV
		ArrayList<Map<String,String>> tripList = readCSV(new FileReader(csvFileNames.get("TRIPS")));
		//ArrayList<Map<String,String>> serviceList = readCSV(new FileReader(csvFileNames.get("CALENDAER")));
		ArrayList<Map<String,String>> shapeList = readCSV(new FileReader(csvFileNames.get("SHAPES")));
		ArrayList<Map<String,String>> stopList = readCSV(new FileReader(csvFileNames.get("STOPS")));
		ArrayList<Map<String,String>> stoptimeList = readCSV(new FileReader(csvFileNames.get("STOPTIMES")));
		// intermediate product: connection between shapeid and tripid
		HashMap<String,ArrayList<String>> shapeTripMap = new HashMap<String,ArrayList<String>>(); 
		//get trip info and create hashmap for all trips 
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
			Coordinate loc = new Coordinate(stop_lon,stop_lat);
				if (stopIdTupleMap.get(stopid)==null) continue; //****TEST
			ArrayList<Tuple> tupleList= stopIdTupleMap.get(stopid);
			for (Tuple t:tupleList ) {
				String tripid = t.tripid;
				Long time = t.time;
				trajMap.get(tripid).trajectory.put(time, loc);
			}
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
	
	public static Coordinate getLinearCoordinate(Coordinate a,Long ta, Coordinate b, Long tb, Long t) {
		return new Coordinate(a.x+(t-ta)*(b.x-a.x)/(tb-ta),a.y+(t-ta)*(b.y-a.y)/(tb-ta));
		
	}
	
	
	// convert "HH:MM:SS" into the seconds elasped from midnight
	private static Long toElapsedTime(String time) {
		StringTokenizer timeToken = new StringTokenizer(time,":");
		Long hours = Long.valueOf(timeToken.nextToken());
		Long mins = Long.valueOf(timeToken.nextToken());
		Long secs = Long.valueOf(timeToken.nextToken());
		return hours*3600 + mins*60 + secs;
	}
}

class Trajectory {
	String trip_id;
	String service_id;
	Trajectory (String trip_id,String service_id) {
		this.service_id = service_id;
		this.trip_id = trip_id;
	}   
	SortedMap<Long,Coordinate> trajectory = new TreeMap<Long,Coordinate>();
	
	Coordinate getPosition(Long time) {
		if (!this.isActive(time)) return null;
		Coordinate coor = null;
		Long thisTime, nextTime;
		Iterator<Long> keyItr= trajectory.keySet().iterator();
		thisTime = keyItr.next();
		while (keyItr.hasNext()) {
			nextTime = keyItr.next();
			if (time<nextTime) 
				coor = GTFSParser.getLinearCoordinate(trajectory.get(thisTime),thisTime,trajectory.get(nextTime),nextTime,time);
			thisTime = nextTime;
		}
		return coor;
	}
	boolean isActive(Long time) {
		if (trajectory.firstKey() <= time && time<=trajectory.lastKey()) return true;
		else return false;
	}
}

class Transit {
	public static ArrayList<Trajectory> allTraj;
	public static HashMap<String,Long> tripTimeStartMap = new HashMap<String,Long>();
	public static HashMap<String,Long> tripTimeEndMap = new HashMap<String,Long>();
	public static void intializeTripTimeMap() {
		for (Trajectory t:allTraj) {
			String tripid = t.trip_id;
			Long timeStart = new Long(t.trajectory.firstKey());
			Long timeEnd = new Long(t.trajectory.lastKey());
			tripTimeStartMap.put(tripid, timeStart);
			tripTimeEndMap.put(tripid, timeEnd);
		}
	}
	public static ArrayList<Trajectory> activeTrajectories(Long time) {
		ArrayList<Trajectory> activeTraj = new ArrayList<Trajectory>();
		for (Trajectory traj: allTraj) {
			Long timeStart = tripTimeStartMap.get(traj.trip_id);
			Long timeEnd = tripTimeEndMap.get(traj.trip_id);
			if (timeStart<=time && time<=timeEnd) {
				activeTraj.add(traj);
			}
		}
		return activeTraj;
	}
}