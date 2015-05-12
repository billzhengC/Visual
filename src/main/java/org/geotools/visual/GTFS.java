package org.geotools.visual;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.Collections;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.data.JFileDataStoreChooser;
import org.geotools.swing.event.MapMouseEvent;
import org.geotools.swing.tool.CursorTool;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * @author Bill
 * @GFTS Displays a window showing the active vehicles at any time
 */
public class GTFS implements ActionListener {
	ArrayList<Trajectory> currentTraj;
	Layer currentLayer = null;
	Long currentTime = new Long(0);
	int currentDate = 0;
	int vehiclesInService;
	Long firstTime, lastTime;
	Timer timer;
	MapContent map;
	JMapFrame mapFrame;
	JLabel timeLabel;
	UserPanel userPanel;
	SimpleFeatureBuilder featureBuilder;
	boolean timeChangeFlag = true;
	boolean paused = false;
	static GTFS currentGTFS; 
	

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

		firstTime = new Long(Collections.min(Transit.tripTimeStartMap.values()));
		lastTime = new Long(Collections.max(Transit.tripTimeEndMap.values()));

		// make a map
		map = new MapContent();
		map.setTitle("Test");
		
/*		  // import world 
		 Layer world = layerFromShapeFile(Data.WORLD,Color.BLACK,Color.GRAY);
		 map.addLayer(world);*/
		 

/*		// import NYC road
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
			Point pointToFeature = coorToPoint(coorToFeature, gf);
			featureCollection = addFeature(featureCollection, featureBuilder,
					String.valueOf(index++), pointToFeature);
		}
		Style style = SLD.createSimpleStyle(featureCollection.getSchema());
		Layer shapeLayer = new FeatureLayer(featureCollection, style);
		map.addLayer(shapeLayer);			
		
		
		
/*		 // Set the source and target CRS CoordinateReferenceSystem 
		CoordinateReferenceSystem sourceCRS = CRS.decode( "EPSG:4326" ); // WGS84 CoordinateReferenceSystem
		CoordinateReferenceSystem targetCRS = CRS.decode( "EPSG:3857" ); // Mercator 
		// Get the transform function MathTransform 
		 transform = CRS.findMathTransform(sourceCRS, targetCRS, true); // Transform the point from WGS 84 to Mercator 
		 point = (Point) JTS.transform( point, transform );*/
		 
		mapFrame = new JMapFrame();
		mapFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		mapFrame.enableStatusBar(true);
		mapFrame.enableToolBar(true);
		mapFrame.setLocationRelativeTo(null);
		
		// create time Label to display the current time 
		timeLabel = new JLabel("0"); 
		timeLabel.setFont(new Font("Serif", Font.PLAIN, 30));
		timeLabel.setForeground(Color.BLUE);
		
		//create a button to pause
		JButton pauseButton = new JButton("Pause");
		pauseButton.setFont(new Font("Serif", Font.PLAIN, 30));
		pauseButton.addActionListener(new ActionListener(){
			public void actionPerformed(ActionEvent e) {
				if (paused) {
					paused = false;
					return;
				}
				paused = true;
			}
		});
		
		// create a textfield
		final JTextField timeTextField = new JTextField();
		timeTextField.getDocument().addDocumentListener(new DocumentListener() {
			  public void changedUpdate(DocumentEvent e) {
			  }
			  public void removeUpdate(DocumentEvent e) {
			  }
			  public void insertUpdate(DocumentEvent e) {
				  if (timeTextField.getText().length() == 8) {
					  currentTime = GTFSParser.toElapsedTime(timeTextField.getText());
					  if (paused) paused = false;
				  }
			  }
			});
		
//		 mapFrame.getMapPane().setCursorTool(
//                 new CursorTool() {
//
//                     @Override
//                     public void onMouseClicked(MapMouseEvent ev) {
//                         SelectionLab.selectFeatures(mapFrame, ev);
//                     }
//                 });
		// add time label and pause button to the tool bar
		JToolBar toolBar = mapFrame.getToolBar();
        toolBar.addSeparator();
        toolBar.add(timeLabel);
        toolBar.add(pauseButton);
        toolBar.add(timeTextField);       
        
		System.out.println("Timer starts");
		timer.start();
		currentTime = firstTime+36000;
		showAnalysis();

	}

	
	
	void showAnalysis() {
		userPanel = new UserPanel();
		userPanel.start();
		
	}
	
	void updatePanel() {
		userPanel.update();
	}
	
	
	
	/* 
	 * Given a current time, display the potions of all the active vehicles in the map
	 */
	void showCurPoints() throws FactoryException, TransformException {
		// Create a Geometry factory
		GeometryFactory gf = new GeometryFactory();

		// Create a FeatureCollection and put coordList into it
		DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
		// put coorList
		ArrayList<Coordinate> coorList = new ArrayList<Coordinate>();
		currentTraj = Transit.activeTrajectories(currentTime);
		vehiclesInService = 0;
		for (Trajectory t1 : currentTraj) {
//			for (Map.Entry<Long, Coordinate> entry2 : t1.trajectory.entrySet()) {
//				coorList.add(entry2.getValue());
//			}
			if (Data.testInService(currentDate,t1.service_id)) {
				coorList.add(t1.getPosition(currentTime));
				vehiclesInService++;
			}
		}
		// create featurecollection
		int index = 0;
		for (Coordinate coorToFeature : coorList) {
			Point pointToFeature = coorToPoint(coorToFeature, gf);
			featureCollection = addFeature(featureCollection, featureBuilder,
					String.valueOf(index++), pointToFeature);
		}
		System.out.println(index);
		// Style style = SLD.createSimpleStyle(featureCollection.getSchema());
		Style style = StyleChange.createStyle2(featureCollection.getSchema(),
				Color.RED, Color.GREEN);
		Layer layer = new FeatureLayer(featureCollection, style);
		if (currentLayer != null)
			map.removeLayer(currentLayer);
		currentLayer = layer;
		map.addLayer(layer);
		mapFrame.setSize(800, 600);
		mapFrame.setMapContent(map);
		mapFrame.setVisible(true);
	}

	/* 
	 * implement action Performed: increase the current time by a given value until it reaches boundary(non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	public void actionPerformed(ActionEvent e) {
		if (timeChangeFlag && !paused) {
			if (currentTime > lastTime) {
				currentDate = (currentDate+1)%7;
				currentTime = firstTime;
				timeLabel.setText("Current Time: " + Data.intToDate(currentDate) + " "+ toStandardTime(currentTime));
				//timeChangeFlag = false;
				System.out.println("Next day");
			} else
				try {
					if (currentTime<firstTime) currentTime = firstTime;
					showCurPoints();
					updatePanel();
					timeLabel.setText("Current Time: " + Data.intToDate(currentDate) + " "+ toStandardTime(currentTime));					
					currentTime += 4000;
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
		long hour = t/3600;
		long minute =  (t-hour*3600)/60;
		long sec = t-hour*3600-minute*60;
		String sep1 = "", sep2 =":", sep3=":";
		if (hour<10) sep1+="0";
		if (minute<10) sep2+="0";
		if (sec<10) sep3+="0";
		return sep1+ hour + sep2 +minute + sep3 + sec;
	}
	
	/* 
	 * read a shape file and convert it into a layer according to the color selected
	 */
	private Layer layerFromShapeFile(String filename, Color outline, Color fill)
			throws IOException {
		FileDataStore store = FileDataStoreFinder.getDataStore(new File(
				filename));
		SimpleFeatureSource featureSource = store.getFeatureSource();
		Style style = StyleChange.createStyle2(featureSource.getSchema(),
				outline, fill);
		Layer layer = new FeatureLayer(featureSource, style);
		return layer;
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
		fb.add(name);
		fb.add(point);
		SimpleFeature feature = fb.buildFeature(null);
		fc.add(feature);
		return fc;
	}
}