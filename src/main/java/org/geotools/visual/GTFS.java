package org.geotools.visual;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.Collections;

import javax.swing.JFrame;
import javax.swing.Timer;

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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class GTFS implements ActionListener{
	private Layer currentLayer = null;
	private Long currentTime = new Long(0);
	private Long firstTime, lastTime;
	private Timer timer;
	private MapContent map;
	private JMapFrame mapFrame;
	private SimpleFeatureBuilder featureBuilder;
	boolean timeChangeFlag = true;

	public static void main(String[] args) throws Exception {        
		GTFS gtfs = new GTFS();
		gtfs.start();
        
    }
	private void start() throws IOException {
		timer = new Timer(2000,this);
		timer.setInitialDelay(0);
		// Set CSV file 
		Data.intialzeCSVFile();
		// Get coordinates using GTFS parser
		HashMap<String,Trajectory> myTrajMap = GTFSParser.parseTrips(Data.csvFiles);
		// put coords into Transit Class
		Transit.allTraj = new ArrayList<Trajectory>(myTrajMap.values());
		Transit.intializeTripTimeMap();
		System.out.println("put coords into Transit Done");

		firstTime = new Long(Collections.min(Transit.tripTimeStartMap.values()));
		lastTime = new Long(Collections.max(Transit.tripTimeEndMap.values()));

		
		// make a map
		map = new MapContent();
        map.setTitle("Test");
/*        // import world
        Layer world = layerFromShapeFile(Data.WORLD,Color.BLACK,Color.GRAY);
        map.addLayer(world);*/
        
        // import NYC road
//        Layer nycroad = layerFromShapeFile(Data.NYCROAD,Color.BLUE,Color.CYAN);
//        map.addLayer(nycroad);
        
     // Create a SimpleFeatureType builder
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        //set the name
        b.setName( "Stop" );
        // Add a "name" property
        b.add( "name", String.class );
        //add a geometry property
        b.setCRS( DefaultGeographicCRS.WGS84 ); // set crs first
        b.add( "location", Point.class ); // then add geometry
        //build the type
        final SimpleFeatureType STOP = b.buildFeatureType();
        
        // Create a SimpleFeature builder
        featureBuilder = new SimpleFeatureBuilder(STOP);

/*        // Set the source and target CRS
        CoordinateReferenceSystem sourceCRS = CRS.decode( "EPSG:4326" ); // WGS84
        CoordinateReferenceSystem targetCRS = CRS.decode( "EPSG:3857" ); // Mercator
        // Get the transform function
        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);
        // Transform the point from WGS 84 to Mercator
        point = (Point) JTS.transform( point , transform );*/
        mapFrame = new JMapFrame();
        //mapFrame.setSize(800,500);
        mapFrame.setPreferredSize(new Dimension(800,500));
        mapFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE );
        //mapFrame.enableLayerTable( true );
        mapFrame.enableStatusBar(true);
        mapFrame.enableToolBar(true);
		System.out.println("Timer starts");
		timer.start();
		currentTime = firstTime;
	}
	
	void showCurPoints(){
        // Create a Geometry factory
        GeometryFactory gf = new GeometryFactory();
        
        // Create a FeatureCollection and put coordList into it
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
        // put coorList
        ArrayList<Coordinate> coorList = new ArrayList<Coordinate>();
		for (Trajectory t1:Transit.activeTrajectories(currentTime)){
			for(Map.Entry<Long,Coordinate> entry2: t1.trajectory.entrySet()) {
				coorList.add(entry2.getValue());
			}
		}
		// create featurecollection
        int index = 0;
        for(Coordinate coorToFeature: coorList) {
        	 Point pointToFeature = coorToPoint(coorToFeature,gf);
        	 featureCollection = addFeature(featureCollection,featureBuilder,String.valueOf(index++),pointToFeature);
        }
        System.out.println(index);
        //Style style = SLD.createSimpleStyle(featureCollection.getSchema());
        Style style = StyleChange.createStyle2(featureCollection.getSchema(), Color.RED, Color.GREEN);
        Layer layer = new FeatureLayer(featureCollection, style);
        if (currentLayer != null) map.removeLayer(currentLayer);
        currentLayer = layer;
        map.addLayer(layer);
        mapFrame.pack();
        mapFrame.setMapContent(map);
        mapFrame.setVisible(true);
	}
	public void actionPerformed(ActionEvent e) {
		if (timeChangeFlag) {
			currentTime += 3600;
			if (currentTime > lastTime) {
				timeChangeFlag = false;
				System.out.println("Stops");
			}
			else showCurPoints();

		}
		
	}
	
	private Layer layerFromShapeFile(String filename,Color outline,Color fill) throws IOException {
		FileDataStore store = FileDataStoreFinder.getDataStore(new File(filename));
        SimpleFeatureSource featureSource = store.getFeatureSource();      
        Style style = StyleChange.createStyle2(featureSource.getSchema(),outline,fill);
        Layer layer = new FeatureLayer(featureSource, style);
        return layer;
	}
	
	private Point coorToPoint(Coordinate coor,GeometryFactory gf) {
		return gf.createPoint(coor);
	}
	private DefaultFeatureCollection addFeature(DefaultFeatureCollection fc,SimpleFeatureBuilder fb, String name,Point point) {
		fb.add(name);
		fb.add(point);
		SimpleFeature feature = fb.buildFeature(null);
		fc.add(feature);		
		return fc;
	}
}