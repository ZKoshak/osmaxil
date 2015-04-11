package org.openstreetmap.osmaxil.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.annotation.Obsolete;
import org.apache.log4j.Logger;
import org.openstreetmap.osmaxil.Application;
import org.openstreetmap.osmaxil.data.api.OsmApiRoot;
import org.openstreetmap.osmaxil.data.api.OsmApiTag;

public abstract class AbstractElement {

    private long osmId;
    
    private long relationId;
    
    private OsmApiRoot apiData;
    
    private boolean updated;

    private List<AbstractImport> matchingImports;

    @Obsolete
    private AbstractImport bestMatchingImport;
    
    private Map<String, Map<String, List<AbstractImport>>> importsByTagValuesByTagNames;
    
    private Map<String, Map<String, Float>> totalScoresByTagValuesByTagNames;
    
    private Map<String, String> originalValuesByTagNames;
    
    abstract public List<OsmApiTag> getTags();
    
    abstract public void updateChangeset(long changesetId);
    
    static protected final Logger LOGGER = Logger.getLogger(Application.class);
    
    public AbstractElement(long osmId) {
        this.osmId = osmId;
        this.updated = false;
        this.matchingImports = new ArrayList<AbstractImport>();
        this.importsByTagValuesByTagNames = new HashMap<String, Map<String,List<AbstractImport>>>();
        this.totalScoresByTagValuesByTagNames = new HashMap<String, Map<String,Float>>();
        this.originalValuesByTagNames = new HashMap<String, String>();
    }

    @Override
    public String toString() {
        return "OSM building has id=[" + this.getOsmId() + "]";
    }
    
    public String getTagValue(String key) {
        List<OsmApiTag> tags = this.getTags();
        if (tags == null) {
            //LOGGER.warn("Unable to get tag value of " + key + " for element " + this.getOsmId() + " because its tag list is null !!");
            return null;
        }
        for (OsmApiTag tag : tags) {
            if (tag.k.equals(key)) {
                return tag.v;
            }
        }
        return null;
    }

    public boolean setTagValue(String key, String value) {
        for (OsmApiTag tag : this.getTags()) {
            if (tag.k.equals(key)) {
                tag.v = value;
                return true;
            }
        }
        OsmApiTag tag = new OsmApiTag();
        tag.k = key;
        tag.v = value;
        this.getTags().add(tag);
        return false;
    }
    
    public Map<String, List<AbstractImport>> getMatchingImportsByTagValuesByTagName(String tagName) {
        Map<String, List<AbstractImport>> result = this.importsByTagValuesByTagNames.get(tagName);
        if (result == null) {
            result = new HashMap<String, List<AbstractImport>>();
            this.importsByTagValuesByTagNames.put(tagName, result);
        }
        return result;
    }
    
    public Map<String, Float> getTotalScoresByTagValuesByTagName(String tagName) {
        Map<String, Float> result = this.totalScoresByTagValuesByTagNames.get(tagName);
        if (result == null) {
            result = new HashMap<String, Float>();
            this.totalScoresByTagValuesByTagNames.put(tagName, result);
        }
        return result;
    }
    
    public Float getBestTotalScoreByTagName(String tagName) {
        Float bestTotalScore = null;
        for (Float totalScore : this.getTotalScoresByTagValuesByTagName(tagName).values()) {
            if (bestTotalScore == null || bestTotalScore < totalScore) {
                bestTotalScore = totalScore;
            }
        }
        return bestTotalScore;
    }
    
    public String getBestTagValueByTagName(String tagName) {
        String bestTagValue = null;
        Float bestTotalScore = null;
        Map<String, List<AbstractImport>> map = this.getMatchingImportsByTagValuesByTagName(tagName);
        for (String tagValue : map.keySet()) {
            List<AbstractImport> importList = map.get(tagValue);
            // TODO use precalculated total scores
            float totalScore = 0;
            for (AbstractImport imp : importList) {
                totalScore += imp.getMatchingScore();
            }
            if (bestTotalScore == null || bestTotalScore < totalScore) {
                bestTotalScore = totalScore;
                bestTagValue = tagValue;
            }
        }
        return bestTagValue;
    }
    
    // Convenient methods

    public String getName() {
        return (String) this.getTagValue(ElementTagNames.NAME);
    }
    
    // Getters & Setters

    public long getOsmId() {
        return osmId;
    }

    public void setOsmId(long osmId) {
        this.osmId = osmId;
    }
    
    public boolean isUpdated() {
        return this.updated;
    }
    
    public void setUpdated(boolean updated) {
        this.updated = updated;
    }

    public OsmApiRoot getApiData() {
        return apiData;
    }

    public void setApiData(OsmApiRoot apiData) {
        this.apiData = apiData;
    }

    public List<AbstractImport> getMatchingImports() {
        return matchingImports;
    }

    public void setMatchingImports(List<AbstractImport> matchingImports) {
        this.matchingImports = matchingImports;
    }

    @Obsolete
    public AbstractImport getBestMatchingImport() {
        return bestMatchingImport;
    }

    @Obsolete
    public void setBestMatchingImport(AbstractImport bestMatchingImport) {
        this.bestMatchingImport = bestMatchingImport;
    }

    public long getRelationId() {
        return relationId;
    }

    public void setRelationId(long relationId) {
        this.relationId = relationId;
    }

    public Map<String, String> getOriginalValuesByTagNames() {
        return originalValuesByTagNames;
    }

    public void setOriginalValuesByTagNames(Map<String, String> orignalValuesByTagNames) {
        this.originalValuesByTagNames = orignalValuesByTagNames;
    }

}
