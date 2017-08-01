package org.openstreetmap.osmaxil.plugin.updater;

import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.openstreetmap.osmaxil.Exception;
import org.openstreetmap.osmaxil.model.AbstractElement;
import org.openstreetmap.osmaxil.model.AbstractImport;
import org.openstreetmap.osmaxil.model.ElementType;
import org.openstreetmap.osmaxil.model.misc.MatchingElementId;
import org.openstreetmap.osmaxil.model.xml.osm.OsmXmlRoot;
import org.openstreetmap.osmaxil.plugin.AbstractPlugin;
import org.openstreetmap.osmaxil.plugin.common.matcher.AbstractImportMatcher;
import org.openstreetmap.osmaxil.plugin.common.scorer.AbstractMatchingScorer;
import org.openstreetmap.osmaxil.plugin.common.scorer.ScoringStatsGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public abstract class AbstractUpdaterPlugin<ELEMENT extends AbstractElement, IMPORT extends AbstractImport> extends
        AbstractPlugin<ELEMENT, IMPORT> {

    // =========================================================================
    // Instance variables
    // =========================================================================

    protected Map<Long, ELEMENT> matchedElements = new Hashtable<Long, ELEMENT>();

    protected Map<Long, ELEMENT> updatableElements = new Hashtable<Long, ELEMENT>();

    protected int counterForMatchedImports;

    protected int counterForUpdatedElements;
    
    protected Map<String, Integer> countersByTagName = new HashMap<String, Integer>();

    @Autowired
    private ScoringStatsGenerator scoringStatsGenerator;

    // =========================================================================
    // Static variables
    // =========================================================================

    static public final float MIN_MATCHING_SCORE = 0.0f;

    static public final float MAX_MATCHING_SCORE = 1.0f;

    // =========================================================================
    // Abstract methods
    // =========================================================================

    abstract protected String[] getUpdatableTagNames();

    abstract protected boolean isElementTagUpdatable(ELEMENT element, String tagName);

    abstract protected boolean updateElementTag(ELEMENT element, String tagName);

    abstract protected ELEMENT instanciateElement(long osmId);

    abstract protected float getMinimalMatchingScore();

    // =========================================================================
    // Public methods
    // =========================================================================

    @Override
    public void process() {
        // For each loaded import, bind it with its matching elements
        int importNbr = 0;
        for (IMPORT imp : this.loadedImports) {
            LOGGER.info("Binding import #" + importNbr + ": " + imp);
            if (imp == null) {
                LOGGER.warn("Import is null, skipping it...");
                break;
            }
            this.associateImportsWithElements(imp);
            LOGGER.info(LOG_SEPARATOR);
        }
        // For each matched element, compute its matching score
        int elementNbr = 0;
        for (ELEMENT element : this.matchedElements.values()) {
            LOGGER.info("Computing matching score for element #" + elementNbr + ": " + element);
            if (element == null) {
                LOGGER.warn("Element is null, skipping it...");
                break;
            }
            this.computeMatchingScores(element);
            LOGGER.info(LOG_SEPARATOR);
        }
    }

    @Override
    public void synchronize() {
        for (ELEMENT element : this.matchedElements.values()) {
            int elementNbr = 0;
            try {
                LOGGER.info("Updating element #" + elementNbr + ": " + element);
                if (element == null) {
                    LOGGER.warn("Element is null, skipping it...");
                    return;
                }
                this.updateElement(element);
            } catch (java.lang.Exception e) {
                LOGGER.error("Synchronization of element " + element.getOsmId() + " has failed: ", e);
            }
            LOGGER.info(LOG_SEPARATOR);
        }
    }

    @Override
    public void displayProcessingStatistics() {
        LOGGER_FOR_STATS.info("=== Processing statistics ===");
        LOGGER_FOR_STATS.info("Total of imports which match one or more matching elements: " + this.counterForMatchedImports);
        LOGGER_FOR_STATS.info("Total of missed imports: " + (counterForLoadedImports - this.counterForMatchedImports));
        LOGGER_FOR_STATS.info("Total of elements which have one or more matching imports: " + this.matchedElements.size());
        this.scoringStatsGenerator.displayStatsByMatchingScore((Collection<AbstractElement>) matchedElements.values());
    }

    @Override
    public void displaySynchronizingStatistics() {
        LOGGER_FOR_STATS.info("=== Synchronizing statistics ===");
        LOGGER_FOR_STATS.info("Total of updated elements: " + this.counterForUpdatedElements);
        LOGGER_FOR_STATS.info("Total of updates by tag:");
        for (String updatableTagName : this.getUpdatableTagNames()) {
            LOGGER_FOR_STATS.info("\t - total of updates on the tag [" + updatableTagName + "]: "
                    + this.countersByTagName.get(updatableTagName));
        }
    }

    // =========================================================================
    // Private methods
    // =========================================================================

    @PostConstruct
    private void init() {
        LOGGER.info("Init of " + this.getClass().getName());
        for (String updatableTagName : this.getUpdatableTagNames()) {
            this.countersByTagName.put(updatableTagName, 0);
        }
    }
    
    private void associateImportsWithElements(IMPORT imp) {
        // Find relevant element
        List<MatchingElementId> matchingElementIds = this.getMatcher().findMatchingElements(imp,
                this.getParser().getSrid());
        if (matchingElementIds.size() > 0) {
            this.counterForMatchedImports++;
        }
        // For each matching elements
        for (MatchingElementId matchingElementId : matchingElementIds) {
            long osmId = matchingElementId.getOsmId();
            // Skip negative IDs (ie. multipolygon relations whose outer member has not been found)
            if (osmId < 0) {
                break;
            }
            // Get related element from the cache or create it
            ELEMENT element = null;
            try {
                element = this.getOrCreateElement(matchingElementId);
            } catch (Exception e) {
                LOGGER.error("Skipping element id=" + osmId + " (" + e.getMessage() + ")");
                break;
            }
            LOGGER.info(element);
            // And bind the import to it
            element.getMatchingImports().add(imp);
            imp.setMatchingElement(element);
            StringBuilder sb = new StringBuilder("Matching imports are now : [ ");
            for (AbstractImport i : element.getMatchingImports()) {
                sb.append(i.getId() + " ");
            }
            LOGGER.info(sb.append("]").toString());
        }
    }

    private ELEMENT getOrCreateElement(MatchingElementId relevantElementId) throws Exception {
        long osmId = relevantElementId.getOsmId();
        ELEMENT element = this.matchedElements.get(osmId);
        if (element == null) {
            // Fetch data from OSM API
            OsmXmlRoot apiData = this.osmStandardApi.readElement(osmId, ElementType.Way);
            if (apiData == null) {
                throw new Exception("Unable to fetch data from OSM API for element#" + osmId);
            }
            // Instanciate a new element
            element = this.instanciateElement(osmId);
            element.setRelationId(relevantElementId.getRelationId());
            element.setApiData(apiData);
            this.matchedElements.put(osmId, element);
            // Store original values for each updatable tag
            for (String tagName : this.getUpdatableTagNames()) {
                element.getOriginalValuesByTagNames().put(tagName, element.getTagValue(tagName));
            }
        } /*
           * else { // If element was already present refresh its data element.setApiData(apiData); }
           */
        return element;
    }

    private void computeMatchingScores(ELEMENT element) {
        try {
            // Compute a matching score for each import matching the element
            for (AbstractImport imp : element.getMatchingImports()) {
                imp.setMatchingScore(this.getMatcher().computeMatchingImportScore((IMPORT) imp));
            }
            // Compute a global matching score for the element
            element.setMatchingScore(this.getScorer().computeElementMatchingScore(element));
        } catch (java.lang.Exception e) {
            LOGGER.error("Process of element " + element.getOsmId() + " has failed: ", e);
        }
        LOGGER.info(LOG_SEPARATOR);
    }

    private void updateElement(ELEMENT element) {
        // Check if its best matching score is enough
        if (element.getMatchingScore() < this.getMinimalMatchingScore()) {
            LOGGER.info("Element cannot be updated because its matching score is " + element.getMatchingScore()
                    + " (min=" + this.getMinimalMatchingScore() + ")");
            return;
        }
        boolean needToSync = false;
        for (String updatableTagName : this.getUpdatableTagNames()) {
            LOGGER.debug("* Updating data for the tag " + updatableTagName);
            // Check if tag is updatable
            if (this.isElementTagUpdatable(element, updatableTagName)) {
                boolean updated = this.updateElementTag(element, updatableTagName);
                if (updated) {
                    needToSync = true;
                    Integer counter = this.countersByTagName.get(updatableTagName);
                    counter++;
                    this.countersByTagName.put(updatableTagName, counter);
                }
            }
        }
        // Do the update sync only if needed
        if (needToSync) {
            boolean success = false;
            if ("api".equals(this.synchronizationMode)) {
                success = this.osmStandardApi.writeElement(element, ElementType.Way);
            } else if ("gen".equals(this.synchronizationMode)) {
                success = this.osmXmlFile.writeToFile("" + element.getOsmId(), element.getApiData());
            }
            if (success) {
                this.counterForUpdatedElements++;
                element.setAltered(true);
                LOGGER.debug("Ok element has been updated");
            }
        } else {
            LOGGER.info("Element cannot be updated (maybe original value(s) exist(s))");
        }
    }
}
