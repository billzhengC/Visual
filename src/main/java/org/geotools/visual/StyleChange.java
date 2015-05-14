package org.geotools.visual;

import java.awt.Color;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

class StyleChange {
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

		// create a partially opaque outline stroke
		Stroke stroke = styleFactory.createStroke(
				filterFactory.literal(outlineColor), filterFactory.literal(1),
				filterFactory.literal(0.5));

		// create a partial opaque fill
		Fill fill = styleFactory.createFill(filterFactory.literal(fillColor),
				filterFactory.literal(0.5));

		/*
		 * Setting the geometryPropertyName arg to null signals that we want to
		 * draw the default geomettry of features
		 */
		PolygonSymbolizer sym = styleFactory.createPolygonSymbolizer(stroke,
				fill, null);

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

		/*
		 * Setting the geometryPropertyName arg to null signals that we want to
		 * draw the default geomettry of features
		 */
		LineSymbolizer sym = styleFactory.createLineSymbolizer(stroke, null);

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

		mark.setStroke(styleFactory.createStroke(
				filterFactory.literal(outlineColor), filterFactory.literal(1)));

		mark.setFill(styleFactory.createFill(filterFactory.literal(fillColor)));

		gr.graphicalSymbols().clear();
		gr.graphicalSymbols().add(mark);
		gr.setSize(filterFactory.literal(5));

		/*
		 * Setting the geometryPropertyName arg to null signals that we want to
		 * draw the default geomettry of features
		 */
		PointSymbolizer sym = styleFactory.createPointSymbolizer(gr, null);

		Rule rule = styleFactory.createRule();
		rule.symbolizers().add(sym);
		FeatureTypeStyle fts = styleFactory
				.createFeatureTypeStyle(new Rule[] { rule });
		Style style = styleFactory.createStyle();
		style.featureTypeStyles().add(fts);

		return style;
	}
}
