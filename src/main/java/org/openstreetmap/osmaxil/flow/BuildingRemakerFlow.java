package org.openstreetmap.osmaxil.flow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openstreetmap.osmaxil.dao.xml.osm.OsmXmlMember;
import org.openstreetmap.osmaxil.dao.xml.osm.OsmXmlNd;
import org.openstreetmap.osmaxil.dao.xml.osm.OsmXmlNode;
import org.openstreetmap.osmaxil.dao.xml.osm.OsmXmlRelation;
import org.openstreetmap.osmaxil.dao.xml.osm.OsmXmlRoot;
import org.openstreetmap.osmaxil.dao.xml.osm.OsmXmlTag;
import org.openstreetmap.osmaxil.dao.xml.osm.OsmXmlWay;
import org.openstreetmap.osmaxil.model.AbstractImport;
import org.openstreetmap.osmaxil.model.BuildingElement;
import org.openstreetmap.osmaxil.model.BuildingImport;
import org.openstreetmap.osmaxil.model.ElementTag;
import org.openstreetmap.osmaxil.model.ElementType;
import org.openstreetmap.osmaxil.model.misc.ElementIdWithParentFlags;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Point;

@Component("BuildingRemaker")
@Lazy
public class BuildingRemakerFlow extends AbstractRemakerFlow<BuildingElement, BuildingImport> {

	// =========================================================================
	// Instance variables
	// =========================================================================

	private Map<Long, OsmXmlRoot> newBuildingsByRemakableBuilding = new HashMap<>();

	private List<ElementIdWithParentFlags> oldNodesToDelete = new ArrayList<>();

	private Map<String, OsmXmlNode> newNodesByCoordinates = new HashMap<>();

	// =========================================================================
	// Overrided methods
	// =========================================================================

	@Override
	protected BuildingElement instanciateElement(long osmId) {
		return new BuildingElement(osmId);
	}

	@Override
	protected boolean isElementRemakable(BuildingElement element) {
		// TODO Check if element has only common tags for the Cadastre import or if it's complex structure ?
		return true;
	}

	@Override
	protected void processElement(BuildingElement element) {
		LOGGER.debug("Building XML for remaking of element #" + element.getOsmId() + ":");
		this.newBuildingsByRemakableBuilding.put(element.getOsmId(), this.buildNewBuildings(element));
		this.oldNodesToDelete.addAll(this.buildNodesToDelete(element));
	}

	@Override
	protected void buildDataForCreation() {
		OsmXmlRoot root = new OsmXmlRoot();
		// Merge all new buildings (with their new nodes)
		for (Long id : this.newBuildingsByRemakableBuilding.keySet()) {
			OsmXmlRoot data = this.newBuildingsByRemakableBuilding.get(id);
			root.nodes.addAll(data.nodes);
			root.ways.addAll(data.ways);
			root.relations.addAll(data.relations);
		}
		this.dataForCreation = root;
	}

	@Override
	protected void buildDataForDeletion() {
		OsmXmlRoot root = new OsmXmlRoot();
		// Merge deletions of all remakable buildings
		for (BuildingElement element : this.remakableElements.values()) {
			OsmXmlWay way = new OsmXmlWay();
			way.id = element.getOsmId();
			way.action = "delete";
			way.version = element.getApiData().ways.get(0).version;
			way.changeset = element.getApiData().ways.get(0).changeset;
			root.ways.add(way);
		}
		// Merge deletions of nodes of all remakable buildings
		for (ElementIdWithParentFlags e : this.oldNodesToDelete) {
			// TODO Check if they can be deleted
			if (true) {
				OsmXmlNode node = new OsmXmlNode();
				node.id = e.getOsmId();
				node.action = "delete";
				root.nodes.add(node);
			}
		}
		this.dataForDeletion = root;
	}

	@Override
	public void displayProcessingStatistics() {
		super.displayProcessingStatistics();
		LOGGER_FOR_STATS.info("Remaking data has been prepared as follow:");
		LOGGER_FOR_STATS.info("\tRemakable buildings: " + this.remakableElements.values().size() + "");
		LOGGER_FOR_STATS.info("\tNew buildings: " + this.newBuildingsByRemakableBuilding.size() + "");
		LOGGER_FOR_STATS.info("\tNew nodes: " + this.newNodesByCoordinates.size() + "");
	}

	// =========================================================================
	// Private methods
	// =========================================================================

	private List<ElementIdWithParentFlags> buildNodesToDelete(BuildingElement element) {
		ArrayList<ElementIdWithParentFlags> result = new ArrayList<>();
		for (OsmXmlNd nd : element.getApiData().ways.get(0).nds) {
			ElementIdWithParentFlags node = new ElementIdWithParentFlags();
			node.setOsmId(nd.ref);
			node.setType(ElementType.Node);
			// TODO use OverPass API to request all ways referencing current point ?
			List<Long> relatedWayIds = new ArrayList<>();
			for (Long relatedWayId : relatedWayIds) {
				ElementIdWithParentFlags.Parent parent = node.new Parent();
				parent.setOsmId(relatedWayId);
				parent.setFlag(false);
				node.getParents().add(parent);
			}
			result.add(node);
		}
		return result;
	}

	private OsmXmlRoot buildNewBuildings(BuildingElement element) {
		OsmXmlRoot root = new OsmXmlRoot();
		// Create the relation
		OsmXmlRelation relation = new OsmXmlRelation();
		relation.version = 0;
		relation.uid = 0;
		// Reuse all tags from original element
		relation.tags = element.getTags();
		// Set a negative ID based on the original element ID
		relation.id = -element.getOsmId();
		LOGGER.debug("\tBuilding new relation#" + relation.id);
		// Add it into the root relation list
		root.relations.add(relation);
		// For each matching import:
		for (AbstractImport imp : element.getMatchingImports()) {
			BuildingImport bi = (BuildingImport) imp;
			// Create a new building part
			OsmXmlWay part = new OsmXmlWay();
			root.ways.add(part);
			// Set a negative ID based on the original element ID + index
			part.id = -this.idGenerator.getId();
			LOGGER.debug("\tBuilding part id=" + part.id);
			part.visible = "true";
			part.version = 0;
			part.uid = 0;
			// Add the building:part tag
			part.tags.add(new OsmXmlTag("building:part", "yes"));
			// Add the building:level tag
			Integer levels = bi.getLevels() + 1; // US way of levels counting
			part.tags.add(new OsmXmlTag(ElementTag.BUILDING_LEVELS, levels.toString()));
			// Add member into the relation
			OsmXmlMember member = new OsmXmlMember();
			member.ref = part.id;
			member.role = "part";
			member.type = "way";
			relation.members.add(member);
			// For each point
			// long firstNodeId = 0;
			for (int i = 0; i < bi.getPoints().size(); i++) {
				Point point = bi.getPoints().get(i);
				// Try to get it from the internal cache
				String key = point.getX() + "," + point.getY();
				OsmXmlNode node = this.newNodesByCoordinates.get(key);
				if (node == null) {
					// Create a new node (into the root)
					node = new OsmXmlNode();
					node.id = -this.idGenerator.getId();
					node.visible = "true";
					node.uid = 0;
					node.version = 0;
					node.lon = Double.toString(point.getX());
					node.lat = Double.toString(point.getY());
					root.nodes.add(node);
					this.newNodesByCoordinates.put(key, node);
					LOGGER.debug("\t\tPoint id=" + node.id + " x=" + point.getX() + " y=" + point.getY());
				}
				// Create new node reference (into the building part)
				OsmXmlNd nd = new OsmXmlNd();
				nd.ref = node.id;
				part.nds.add(nd);
				// Keep id of the first node
				// if (firstNodeId == 0) {
				// firstNodeId = node.id;
				// }
			}
			// Don't forget to close the way with the first node
			// OsmXmlNd nd = new OsmXmlNd();
			// nd.ref = firstNodeId;
			// part.nds.add(nd);
		}
		return root;
	}
}
