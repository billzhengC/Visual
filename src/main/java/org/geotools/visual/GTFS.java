package org.geotools.visual;

// import io and util
import java.io.File;
import java.io.IOException;
import java.util.*;
// import swing and awt
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
// import Geotools
import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.map.*;
import org.geotools.referencing.CRS;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
// import JTS Topology Suite
import com.vividsolutions.jts.geom.*;

/*
 * GTFS is the main class of the program which receives processed data from GTFSParser and visualize it using Geotools
 */
public class GTFS implements ActionListener {
	ArrayList<Trajectory> currentTraj; // list of the current active vehicles
	Layer currentLayer = null; // the current layer of the vehicles
	Long currentTime = new Long(0); // current time 
	int currentDate = 0; // current date
	int vehiclesInService; // number of vehicles in service
	Long firstTime, lastTime; // first and last time when vehicles are active
	Timer timer; // swing timer
	MapContent map; // map content
	JMapFrame mapFrame; // map frame
	JLabel timeLabel; // time label
	UserPanel userPanel; // user panel
	SimpleFeatureBuilder featureBuilder;
	boolean timeChangeFlag = true; // flag of changing time
	boolean paused = false; // flag of pause
	static GTFS currentGTFS; // GTFS class
	

	/*
	 * The Main method which starts the  GUI
	 */
	public static void main(String[] args) throws Exception {
		GTFS gtfs = new GTFS();
		currentGTFS = gtfs;
		gtfs.start();
		

	}
	
	/*
	 * Get the current instance of GTFS
	 */
	public static GTFS getGTFS() {
		return currentGTFS;
	}
	
	/*
	 * start the simuation of moving vehicles, which creates a window of the map
	 */
	private void start() throws IOException, FactoryException, TransformException {
		/*
		 * create a swing timer
		 */
		timer = new Timer(2000, this);
		timer.setInitialDelay(0);
		// Set CSV file
		Data.intializeCSVFile();
		// Get coordinates using GTFS parser
		HashMap<String, Trajectory> myTrajMap = GTFSParser.parseTrips(Data.csvFiles);
		// Get list of shape points
		ArrayList<Coordinate> shapePointList = GTFSParser.shapePointList;
		// put coords into Transit Class
		Transit.allTraj = new ArrayList<Trajectory>(myTrajMap.values());
		Transit.intializeTripTimeMap();
		System.out.println("put coords into Transit Done");
		
		/*
		 * calculate the first/last active time of the trips
		 */
		firstTime = new Long(Collections.min(Transit.tripTimeStartMap.values()));
		lastTime = new Long(Collections.max(Transit.tripTimeEndMap.values()));

		// make a map
		map = new MapContent();
		map.setTitle("Test");
		
/*		 
 * 		file not used: the following code adds another layer to the current map from a file.
 * 		// import world 
		 Layer world = layerFromShapeFile(Data.WORLD,Color.BLACK,Color.GRAY);
		 map.addLayer(world);
		// import NYC road
		Layer nycroad = layerFromShapeFile(Data.NYCROAD, Color.BLUE, Color.CYAN);
		map.addLayer(nycroad);*/

		// Create a SimpleFeatureType builder
		SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
		// set the name
		b.setName("Stop");
		// Add a "name" property
		b.add("name", String.class);
		// add a geometry property
		//b.setCRS(DefaultGeographicCRS.WGS84); // set crs first
		b.setCRS(CRS.decode( "EPSG:3857" ));
		b.add("location", Point.class); // then add geometry
		// build the type
		final SimpleFeatureType STOP = b.buildFeatureType();

		// Create a SimpleFeature builder
		featureBuilder = new SimpleFeatureBuilder(STOP);

		
		/*
		 * Create layer of shape points
		 */
		
		// Create a Geometry factory
		GeometryFactory gf = new GeometryFactory();
		// Create a FeatureCollection and put coordList into it
		DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
		// create featurecollection
		int index = 0;
		for (Coordinate coorToFeature : shapePointList) {
			Point pointToFeature = coorToPoint(coorToFeature, gf); // convert coordinate to points
			featureCollection = addFeature(featureCollection, featureBuilder, // add points to feature collection
					String.valueOf(index++), pointToFeature);
		}
		Style style = SLD.createSimpleStyle(featureCollection.getSchema()); // create style for feature collection 
		Layer shapeLayer = new FeatureLayer(featureCollection, style); // create a layer using feature collection and the sty;e
		map.addLayer(shapeLayer); // add layer into the map
		
		/*
		 * intialize the frame of the map
		 */
		mapFrame = new JMapFrame(); // create map frame
		mapFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // set close operation
		mapFrame.enableStatusBar(true);  // enable status bar
		mapFrame.enableToolBar(true); // enable tool bar 
		mapFrame.setLocationRelativeTo(null); // set location in the 
		
		/*
		 * create time Label to display the current time 
		 */
		timeLabel = new JLabel("0"); // create label
		timeLabel.setFont(new Font("Serif", Font.PLAIN, 30)); // set font
		timeLabel.setForeground(Color.BLUE); // set color
		
		/*
		 * create a button to pause
		 */
		JButton pauseButton = new JButton("Pause");
		pauseButton.setFont(new Font("Serif", Font.PLAIN, 30)); // set fond
		pauseButton.addActionListener(new ActionListener(){ // add listener
			public void actionPerformed(ActionEvent e) {
				if (paused) {
					paused = false;
					return;
				}
				paused = true;
			}
		});
		
		/* 
		 * create a textfield for user to enter the time
		 */
		final JTextField timeTextField = new JTextField();
		timeTextField.getDocument().addDocumentListener(new DocumentListener() {
			  public void changedUpdate(DocumentEvent e) {
			  }
			  public void removeUpdate(DocumentEvent e) {
			  }
			  public void insertUpdate(DocumentEvent e) {
				  if (timeTextField.getText().length() == 8) {
					  currentTime = GTFSParser.toElapsedTime(timeTextField.getText()); // process the input
					  if (paused) paused = false; // start the game if it is paused
				  }
			  }
			});
		
		/*
		 *  add time label and pause button to the tool bar
		 */
		JToolBar toolBar = mapFrame.getToolBar();
        toolBar.addSeparator();
        toolBar.add(timeLabel); // add time 
        toolBar.add(pauseButton); // add pause button
        toolBar.add(timeTextField); // add text field
        
        /*
         * start timers and show the user panel
         */
		System.out.println("Timer starts");
		timer.start(); // start timer
		currentTime = firstTime; //  initialize current time
		showAnalysis(); // display user panel
	}

	/*
	 * Display a user panel
	 */	
	void showAnalysis() {
		userPanel = new UserPanel();
		userPanel.start();
	}
	
	/*
	 * update the user panel
	 */
	void updatePanel() {
		userPanel.update();
	}
	
	
	
	/* 
	 * Given a current time, display the potions of all the active vehicles in the map.
	 * It replaces the current map layer with a new one
	 */
	void showCurPoints() throws FactoryException, TransformException {
		GeometryFactory gf = new GeometryFactory();	// Create a Geometry factory to create points

		/*
		 *  Create a FeatureCollection and put coordList into it
		 */
		DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
		ArrayList<Coordinate> coorList = new ArrayList<Coordinate>();
		currentTraj = Transit.activeTrajectories(currentTime); // get current active vehicles
		vehiclesInService = 0;
		for (Trajectory t1 : currentTraj) {
			if (Data.testInService(currentDate,t1.service_id)) {
				coorList.add(t1.getPosition(currentTime)); // add position into the coordinates list
				vehiclesInService++; // update vehicles in service
			}
		}
		/*
		 *  create featurecollection of the new layer
		 */
		int index = 0;
		for (Coordinate coorToFeature : coorList) {
			Point pointToFeature = coorToPoint(coorToFeature, gf); // coordinate to point
			featureCollection = addFeature(featureCollection, featureBuilder, // add points to feature collection
					String.valueOf(index++), pointToFeature);
		}
		System.out.println(index);
		Style style = StyleChange.createStyle2(featureCollection.getSchema(), // create customized style
				Color.RED, Color.GREEN);
		Layer layer = new FeatureLayer(featureCollection, style); // create a layer
		
		if (currentLayer != null)
			map.removeLayer(currentLayer);  
		currentLayer = layer;
		map.addLayer(layer);
		mapFrame.setSize(800, Data.frameHeight); // set map frame size
		mapFrame.setMapContent(map); // set map content 
		mapFrame.setVisible(true); // set the map frame visible
	}

	/* 
	 * implement action Performed: increase the current time by a given value until it reaches boundary(non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (timeChangeFlag && !paused) {
			/*
			 * When a day has passed, go to the next day
			 */
			if (currentTime > lastTime) {
				currentDate = (currentDate+1)%7;
				currentTime = firstTime;
				timeLabel.setText("Current Time: " + Data.intToDate(currentDate) + " "+ toStandardTime(currentTime));
				//timeChangeFlag = false;
				System.out.println("Next day");
			} else
			/*
			 * otherwise, show current points, update user panel and set time label
			 */
				try {
					if (currentTime<firstTime) currentTime = firstTime;
					showCurPoints(); // show current points
					updatePanel(); // update user panel
					timeLabel.setText("Current Time: " + Data.intToDate(currentDate) + " "+ toStandardTime(currentTime)); //update time label					
					currentTime += 60; // go to next timestamp
				} catch (FactoryException e1) {
					e1.printStackTrace();
				} catch (TransformException e1) {
					e1.printStackTrace();
				}
		}
	}
	
	/*
	 * Conver a Long type to hh:mm:ss 
	 */
	
	public String toStandardTime(Long t) {
		long hour = t/3600; // get hour 
		long minute =  (t-hour*3600)/60; // get minute
		long sec = t-hour*3600-minute*60; // get second
		// deal with format
		String sep1 = "", sep2 =":", sep3=":";
		if (hour<10) sep1+="0";
		if (minute<10) sep2+="0";
		if (sec<10) sep3+="0";
		return sep1+ hour + sep2 +minute + sep3 + sec; // return result
	}
	
	/* 
	 * read a shape file and convert it into a layer according to the color selected
	 */
	private Layer layerFromShapeFile(String filename, Color outline, Color fill)
			throws IOException {
		FileDataStore store = FileDataStoreFinder.getDataStore(new File(
				filename)); // read data from file
		SimpleFeatureSource featureSource = store.getFeatureSource(); // extract feature source
		Style style = StyleChange.createStyle2(featureSource.getSchema(),
				outline, fill); // create style
		Layer layer = new FeatureLayer(featureSource, style); // create layer
		return layer; // return layer
	}

	/*
	 * Conver a Coordinate into a Point
	 */
	private Point coorToPoint(Coordinate coor, GeometryFactory gf) {
		return gf.createPoint(coor);
	}
	
	/*
	 * a helper method receives the name and the point, and returns a feature collection 
	 */
	private DefaultFeatureCollection addFeature(DefaultFeatureCollection fc,
			SimpleFeatureBuilder fb, String name, Point point) {
		fb.add(name); // add name
		fb.add(point); // add points
		// create feature and add it
		SimpleFeature feature = fb.buildFeature(null); 
		fc.add(feature);
		return fc;
	}
}