package org.geotools.visual;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;

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

public class GTFS {

	public static void main(String[] args) throws Exception {        
		// Set CSV file 
		Data.intialzeCSVFile();
		// Get coordinates using GTFS parser
		HashMap<String,Trajectory> myTrajMap = GTFSParser.parseTrips(Data.csvFiles);
		// put coordinates into a list 
		ArrayList<Coordinate> coorList = new ArrayList<Coordinate>();
		for (Map.Entry<String,Trajectory> entry: myTrajMap.entrySet()) {
			SortedMap<Long,Coordinate> trajMap_temp = entry.getValue().trajectory;
			for(Map.Entry<Long,Coordinate> entry2: trajMap_temp.entrySet()) {
				coorList.add(entry2.getValue());
			}
		}
				
		
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
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(STOP);
        // Create a Geometry factory
        GeometryFactory gf = new GeometryFactory();
//        // Set the source and target CRS
//        CoordinateReferenceSystem sourceCRS = CRS.decode( "EPSG:4326" ); // WGS84
//        CoordinateReferenceSystem targetCRS = CRS.decode( "EPSG:3857" ); // Mercator
//        // Get the transform function
//        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS, true);
//        // Transform the point from WGS 84 to Mercator
//        point = (Point) JTS.transform( point , transform );
        
        // Create a FeatureCollection and put coordList into it
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
        int index = 0;
        for(Coordinate coorToFeature: coorList) {
        	 Point pointToFeature = coorToPoint(coorToFeature,gf);
        	 featureCollection = addFeature(featureCollection,featureBuilder,String.valueOf(index++),pointToFeature);
        }
        System.out.println(index);
       
        
        MapContent map = new MapContent();
        map.setTitle("Test");
        // import world
        Layer world = layerFromShapeFile(Data.WORLD,Color.BLACK,Color.GRAY);
        map.addLayer(world);
        
        // import NYC road
        Layer nycroad = layerFromShapeFile(Data.NYCROAD,Color.BLUE,Color.CYAN);
        map.addLayer(nycroad);
        
        //Style style = SLD.createSimpleStyle(featureCollection.getSchema());
        Style style = StyleChange.createStyle2(featureCollection.getSchema(), Color.RED, Color.GREEN);
        Layer layer = new FeatureLayer(featureCollection, style);
        map.addLayer(layer);
        JMapFrame.showMap(map);
        
    }
	
	private static Layer layerFromShapeFile(String filename,Color outline,Color fill) throws IOException {
		FileDataStore store = FileDataStoreFinder.getDataStore(new File(filename));
        SimpleFeatureSource featureSource = store.getFeatureSource();      
        Style style = StyleChange.createStyle2(featureSource.getSchema(),outline,fill);
        Layer layer = new FeatureLayer(featureSource, style);
        return layer;
	}
	
	private static Point coorToPoint(Coordinate coor,GeometryFactory gf) {
		return gf.createPoint(coor);
	}
	private static DefaultFeatureCollection addFeature(DefaultFeatureCollection fc,SimpleFeatureBuilder fb, String name,Point point) {
		fb.add(name);
		fb.add(point);
		SimpleFeature feature = fb.buildFeature(null);
		fc.add(feature);		
		return fc;
	}
}