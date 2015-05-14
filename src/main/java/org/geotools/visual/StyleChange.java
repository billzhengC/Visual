package org.geotools.visual;

import java.awt.Color;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.*;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;

import com.vividsolutions.jts.geom.*;

/*
 * StyleChange is a helper class which changes style of the map content
 */
class StyleChange {
	/*
	 * get factory of style and filter
	 */
	static StyleFactory styleFactory = CommonFactoryFinder
			.getStyleFactory(null);
	static FilterFactory filterFactory = CommonFactoryFinder
			.getFilterFactory(null);
	static Color outlineColor;
	static Color fillColor;
	protected static Style createStyle2(SimpleFeatureType schema,Color outline,Color fill) {
		outlineColor = outline;
		fillColor = fill;

		Class geomType = schema.getGeometryDescriptor().getType().getBinding();
		/*
		 * change style of polygon, line and point respectively
		 */
		if (Polygon.class.isAssignableFrom(geomType)
				|| MultiPolygon.class.isAssignableFrom(geomType)) {
			return createPolygonStyle();

		} else if (LineString.class.isAssignableFrom(geomType)
				|| MultiLineString.class.isAssignableFrom(geomType)) {
			return createLineStyle();

		} else {
			return createPointStyle();
		}
	}

	/**
	 * Create a Style to draw polygon features with given color
	 */
	private static Style createPolygonStyle() {

		// make stroke and fill
		Stroke stroke = styleFactory.createStroke(
				filterFactory.literal(outlineColor), filterFactory.literal(1),
				filterFactory.literal(0.5));
		Fill fill = styleFactory.createFill(filterFactory.literal(fillColor),
				filterFactory.literal(0.5));
		PolygonSymbolizer sym = styleFactory.createPolygonSymbolizer(stroke,
				fill, null);
		/*
		 * set rules and styles
		 */
		Rule rule = styleFactory.createRule();
		rule.symbolizers().add(sym);
		FeatureTypeStyle fts = styleFactory
				.createFeatureTypeStyle(new Rule[] { rule });
		Style style = styleFactory.createStyle();
		style.featureTypeStyles().add(fts);

		return style;
	}

	/**
	 * Create a Style to draw line features
	 */
	private static Style createLineStyle() {
		Stroke stroke = styleFactory.createStroke(
				filterFactory.literal(outlineColor), filterFactory.literal(1));
		LineSymbolizer sym = styleFactory.createLineSymbolizer(stroke, null);
		/*
		 * set rules and styles
		 */
		Rule rule = styleFactory.createRule();
		rule.symbolizers().add(sym);
		FeatureTypeStyle fts = styleFactory
				.createFeatureTypeStyle(new Rule[] { rule });
		Style style = styleFactory.createStyle();
		style.featureTypeStyles().add(fts);

		return style;
	}

	/**
	 * Create a Style to draw point features 
	 */
	private static Style createPointStyle() {
		Graphic gr = styleFactory.createDefaultGraphic();
		Mark mark = styleFactory.getCircleMark();
		// set stroke and fill
		mark.setStroke(styleFactory.createStroke(
				filterFactory.literal(outlineColor), filterFactory.literal(1)));
		mark.setFill(styleFactory.createFill(filterFactory.literal(fillColor)));
		gr.graphicalSymbols().clear();
		gr.graphicalSymbols().add(mark);
		gr.setSize(filterFactory.literal(5));
		PointSymbolizer sym = styleFactory.createPointSymbolizer(gr, null);
		/*
		 * set rule and stle
		 */
		Rule rule = styleFactory.createRule();
		rule.symbolizers().add(sym);
		FeatureTypeStyle fts = styleFactory
				.createFeatureTypeStyle(new Rule[] { rule });
		Style style = styleFactory.createStyle();
		style.featureTypeStyles().add(fts);

		return style;
	}
}
