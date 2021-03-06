package org.openstreetmap.osmaxil.plugin.matcher;

import java.util.ArrayList;
import java.util.List;

import org.openstreetmap.osmaxil.flow.AbstractUpdaterFlow;
import org.openstreetmap.osmaxil.model.BuildingElement;
import org.openstreetmap.osmaxil.model.BuildingImport;
import org.openstreetmap.osmaxil.model.misc.MatchingElementId;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@Lazy
public class BuildingImportMatcher extends AbstractImportMatcher<BuildingImport> {

	@Override
	public List<MatchingElementId> findMatchingElements(BuildingImport imp, int srid) {
		List<MatchingElementId> result = new ArrayList<MatchingElementId>();
		Long[] ids = new Long[0];
		// Find in PostGIS all buildings matching (ie. containing) the import
		BuildingImport building = (BuildingImport) imp;
		if (building.getGeometryAsWKT() != null) {
			ids = this.findBuildingIDsByGeometry(building.getGeometryAsWKT(), srid);
		} else if (building.getLatitude() != null && building.getLongitude() != null) {
			ids = this.findBuildingIDsByLatLon(building.getLongitude(), building.getLatitude(), srid);
		} else {
			LOGGER.error("Unable to find building because there's no coordinates neither geometry");
		}
		// Parsing the IDs to check if they refers to normal elements (ie. ways) or relations
		StringBuffer sb = new StringBuffer("OSM IDs of matching buildings : [ ");
		for (int i = 0; i < ids.length; i++) {
			MatchingElementId relevantElement = new MatchingElementId();
			// If ID is positive it means it's a normal element (ie. a way)
			if (ids[i] > 0) {
				relevantElement.setOsmId(ids[i]);
				relevantElement.setRelationId(-1);
			}
			// If ID is negative it means it's a multipolygon relations => need to find its relevant outer member
			else {
				LOGGER.debug("A multipolygon relation has been found (" + ids[i] + "), looking for its relevant outer member");
				long relationId = -ids[i];
				String membersString = osmPostgis.getRelationMembers(relationId);
				// TODO Take in account all outer members and not the first one only
				relevantElement.setOsmId(BuildingElement.getOuterOrInnerMemberIds(relationId, membersString, true).get(0));
				relevantElement.setRelationId(relationId);
			}
			result.add(relevantElement);
			sb.append(ids[i] + " ");
		}
		LOGGER.info(sb.toString() + "]");
		return result;
	}

	@Override
	public float computeMatchingImportScore(BuildingImport imp) {
		BuildingElement element = (BuildingElement) imp.getMatchingElement();
		float result = AbstractUpdaterFlow.MIN_MATCHING_SCORE;
		if (imp.getArea() == null) {
			LOGGER.warn("Unable to compute score because building import has NO area");
			return AbstractUpdaterFlow.MIN_MATCHING_SCORE;
		}
		if (imp.getMatchingElement() == null) {
			LOGGER.warn("Unable to compute score because building import has NO element attached");
			return AbstractUpdaterFlow.MIN_MATCHING_SCORE;
		}
		// If no surface are available the maximum score is always returned
		if (!this.useSurface) {
			return AbstractUpdaterFlow.MAX_MATCHING_SCORE;
		}
		// Get element area
		int elementArea = element.getComputedArea();
		// If not yet computed do it and store the result for further matching imports
		if (elementArea == 0) {
			elementArea = this.computeBuildingArea(element);
			element.setComputedArea(elementArea);
		}
		// Compare area between import and element
		if (elementArea > 0) {
			// Returns a float which tends to 1.0 when areas are going closer (and tends to 0.0 if different)
			if (imp.getArea() < elementArea) {
				result = ((float) imp.getArea() / elementArea);
			} else {
				result = ((float) elementArea / imp.getArea());
			}
		}
		// TODO Add other criteria such the "centrality" of the import into the element area
		return result;
	}

	// public BuildingPart createBuildingPart(BuildingImport imp) {
	// BuildingPart bp = new BuildingPart();
	// OsmApiRoot root = new OsmApiRoot();
	// bp.setApiData(root);
	// // TODO create nodes
	// return bp;
	// }
	//
	// public BuildingRelation createBuildingRelation(BuildingElement element) {
	// BuildingRelation br = new BuildingRelation();
	// OsmApiRoot root = new OsmApiRoot();
	// br.setApiData(root);
	// OsmApiRelation relation = new OsmApiRelation();
	// // Reuse tags from orignal element
	// relation.tags = element.getTags();
	// root.relations.add(relation);
	// // Set a negative ID based on the original element ID
	// br.getApiData().ways.get(0).id = - element.getOsmId();
	// return br;
	// }

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Private methods
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private int computeBuildingArea(BuildingElement element) {
		// If the related element belongs to a relation, consider it instead of the element itself (osm2pgsql doesn't store relation members)
		long elementId = element.getOsmId();
		if (element.getRelationId() > 0) {
			elementId = -element.getRelationId(); // reinverse the ID because osm2pgsql stores relations like that
		}
		// TODO use JTS instead of PostGIS ?
		int elementArea = this.osmPostgis.getPolygonArea(elementId);
		// TODO cache it for next imports
		LOGGER.info("OSM building " + element.getOsmId() + " area has been computed: " + elementArea);
		return elementArea;
	}

	private Long[] findBuildingIDsByGeometry(String geometry, int srid) {
		String geom = "ST_GeomFromText('" + geometry + "', " + srid + ")";
		// Transform geometry if it's needed
		if (srid != this.osmPostgis.getSrid()) {
			geom = "ST_Transform(" + geom + ", " + this.osmPostgis.getSrid() + ")";
		}
		String query = "select osm_id, 1 from planet_osm_polygon where building <> '' and  ST_Intersects(way, " + geom + ");";
		LOGGER.debug("Looking in PostGIS for buildings containing geometry: " + query);
		return this.osmPostgis.findElementIdsByQuery(query);
	}

	private Long[] findBuildingIDsByLatLon(double lon, double lat, int srid) {
		String geom = "ST_GeomFromText('POINT(" + lon + " " + lat + ")', " + srid + ")";
		// Transform geometry if it's needed
		if (srid != this.osmPostgis.getSrid()) {
			geom = "ST_Transform(" + geom + ", " + this.osmPostgis.getSrid() + ")";
		}
		// List<Long> result = new ArrayList<Long>();
		String query = "select osm_id, 1 from planet_osm_polygon where building <> '' and  ST_Contains(way, " + geom + ");";
		LOGGER.debug("Looking in PostGIS for buildings containing coords: " + query);
		return this.osmPostgis.findElementIdsByQuery(query);
	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Gettes & Setters
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public boolean isWithSurfaces() {
		return useSurface;
	}

	public void setWithSurfaces(boolean withSurfaces) {
		this.useSurface = withSurfaces;
	}

}
