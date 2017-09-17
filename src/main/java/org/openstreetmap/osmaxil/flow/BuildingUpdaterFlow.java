package org.openstreetmap.osmaxil.flow;

import javax.annotation.PostConstruct;

import org.openstreetmap.osmaxil.model.AbstractImport;
import org.openstreetmap.osmaxil.model.BuildingElement;
import org.openstreetmap.osmaxil.model.BuildingImport;
import org.openstreetmap.osmaxil.model.ElementTag;
import org.openstreetmap.osmaxil.service.matcher.AbstractImportMatcher;
import org.openstreetmap.osmaxil.service.matcher.BuildingImportMatcher;
import org.openstreetmap.osmaxil.service.parser.ParisBuildingImportParser;
import org.openstreetmap.osmaxil.service.selector.AbstractMatchingScoreSelector;
import org.openstreetmap.osmaxil.service.selector.CumulativeOnSameValueMatchingScoreSelector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component("BuildingUpdater") @Lazy
public class BuildingUpdaterFlow extends AbstractUpdaterFlow<BuildingElement, BuildingImport> {

    @Autowired
    private ParisBuildingImportParser parser;
    
    @Autowired
    @Qualifier("BuildingMatcher")
    private BuildingImportMatcher matcher;
    
    @Autowired
    private CumulativeOnSameValueMatchingScoreSelector<BuildingElement> selector;
    
    private static final String UPDATABLE_TAG_NAMES[] = new String[] {ElementTag.BUILDING_LEVELS};
    
    private static final String MATCHING_TAG_NAME = ElementTag.BUILDING_LEVELS;
    
    // =========================================================================
    // Overrided methods
    // =========================================================================

    @Override
    protected boolean isElementTagUpdatable(BuildingElement element, String tagName) {
        // Building tags are updatable only if it doesn't have an original value
        return element.getOriginalValuesByTagNames().get(tagName) == null;
    }
    
    @Override
    protected boolean updateElementTag(BuildingElement element, String tagName) {
        AbstractImport bestImport = this.selector.getBestMatchingImportByElement(element);
        String tagValue = bestImport.getValueByTagName(tagName);
        if (tagValue == null) {
            LOGGER.warn("Cannot update tag because best import tag value is null for " + tagName);
            return false;
        }
        boolean updated = false;
        if (ElementTag.HEIGHT.equals(tagName)) {
            // Adding +1 to levels because OSM use the US way to count building levels
            element.setLevels(Integer.parseInt(tagValue) + 1);
            LOGGER.info("===> Updating levels to " + (tagValue + 1));
            updated = true;
        }
        return updated;
    }
    
    @Override
    protected BuildingElement instanciateElement(long osmId) {
        return new BuildingElement(osmId);
    }

    @Override
    protected ParisBuildingImportParser getParser() {
        return parser;
    }
    
    @Override
    protected AbstractImportMatcher<BuildingImport> getMatcher() {
        return this.matcher;
    }

    @Override
    protected AbstractMatchingScoreSelector<BuildingElement> getScorer() {
      return this.selector;
    }
    
    @Override
    protected String[] getUpdatableTagNames() {
        return UPDATABLE_TAG_NAMES;
    }
    
    // =========================================================================
    // Private methods
    // =========================================================================

    @PostConstruct
    private void init() {
        this.selector.setMatchingTagName(MATCHING_TAG_NAME);
        this.matcher.setWithSurfaces(true);
    }
    
}
