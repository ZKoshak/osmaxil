package org.openstreetmap.osmaxil.step;

import org.apache.http.annotation.Obsolete;
import org.openstreetmap.osmaxil.dao.ElementStore;
import org.openstreetmap.osmaxil.model.AbstractElement;
import org.openstreetmap.osmaxil.model.AbstractImport;
import org.openstreetmap.osmaxil.plugin.AbstractUpdaterPlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class StatisticsStep extends AbstractStep {

    private int matchedElementsNbr;

    private int updatableElementsNbr;

    private int updatedElementsNbr;

    private int[] matchedElementsNbrByScore;

    private int[] updatableElementsNbrByScore;

    private int[] updatedElementsNbrByScore;

    @Autowired
    private ElementStore elementCache;

    public void generateStats() {
        LOGGER.info("=== Statistics ===");
        if (this.plugin instanceof AbstractUpdaterPlugin) {
            generateUpdatingStats();
        } else if (this.plugin instanceof AbstractUpdaterPlugin) {
            generateMakingStats();
        } 
    }

    private void generateUpdatingStats() {
        // Old basic matching method
        LOGGER.info("*** Statistics with the BASIC matching method ***");
        this.buildUpdatingStatsWithBestMatchingImports();
        displayUpdatingStats();
        // New extended matching method
        LOGGER.info("*** Statistics with the EXTENDED matching method ***");
        for (String updatableTagName : ((AbstractUpdaterPlugin) this.plugin).getUpdatableTagNames()) {
            LOGGER.info("* Statistics for the updatable tag [" + updatableTagName + "]");
            this.buildUpdatingStatsWithBestAccumulatedImports(updatableTagName);
            displayUpdatingStats();
        }
    }
    
    private void generateMakingStats() {
    }
    
    private void displayUpdatingStats() {
        LOGGER.info("Number of matched elements: " + this.matchedElementsNbr);
        LOGGER.info("Number of updatable elements: " + this.updatableElementsNbr);
        LOGGER.info("Number of updated elements: " + this.updatedElementsNbr);
        LOGGER.info("Repartition by matching scores:");
        for (int i = 0; i < 10; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("- score between " + i * 10 + "% and " + (i + 1) * 10 + "% : ");
            sb.append(this.matchedElementsNbrByScore[i]);
            if (this.elementCache.getElements().size()  > 0) {
                sb.append(" (" + 100 * this.matchedElementsNbrByScore[i] / this.elementCache.getElements().size() + "%)");
            }
            sb.append(" elements <= " + this.updatedElementsNbrByScore[i] + " updated");
            sb.append(" (" + this.updatableElementsNbrByScore[i] + " were updatable)");
            LOGGER.info(sb);
        }
    }

    @Obsolete
    private void buildUpdatingStatsWithBestMatchingImports() {
        this.matchedElementsNbr = 0;
        this.updatableElementsNbr = 0;
        this.updatedElementsNbr = 0;
        this.matchedElementsNbrByScore = new int[10];
        this.updatedElementsNbrByScore = new int[10];
        this.updatableElementsNbrByScore = new int[10];
        for (AbstractElement element : this.elementCache.getElements().values()) {
            AbstractImport best = element.getBestMatchingImport();
            if (best == null) {
                LOGGER.warn("Element " + element.getOsmId() + " doesn't have any best matching import !!");
            } else {
                for (int i = 0; i < 10; i++) {
                    if (best.getMatchingScore() <= (i + 1) * 0.1) {
                        this.matchedElementsNbr++;
                        this.matchedElementsNbrByScore[i]++;
                        if (element.isUpdated()) {
                            this.updatedElementsNbr++;
                            this.updatedElementsNbrByScore[i]++;
                        }
                        boolean updatable = false;
                        for (String tagName : ((AbstractUpdaterPlugin) this.plugin).getUpdatableTagNames()) {
                            if (((AbstractUpdaterPlugin) this.plugin).isElementTagUpdatable(element, tagName)) {
                                updatable = true;
                            }
                        }
                        if (updatable) {
                            this.updatableElementsNbr++;
                            this.updatableElementsNbrByScore[i]++;
                        }
                        break;
                    }
                }
            }
        }
    }

    private void buildUpdatingStatsWithBestAccumulatedImports(String updatableTagName) {
        this.matchedElementsNbr = 0;
        this.updatableElementsNbr = 0;
        this.updatedElementsNbr = 0;
        this.matchedElementsNbrByScore = new int[10];
        this.updatedElementsNbrByScore = new int[10];
        this.updatableElementsNbrByScore = new int[10];
        for (AbstractElement element : this.elementCache.getElements().values()) {
            Float bestTotalScore = element.getBestTotalScoreByTagName(updatableTagName);
            if (bestTotalScore == null) {
                LOGGER.warn("Element " + element.getOsmId() + " doesn't have any best total matching score !!");
            } else {
                boolean ok = false;
                for (int i = 0; i < 10; i++) {
                    if (bestTotalScore <= (i + 1) * 0.1) {
                        ok = true;
                        this.matchedElementsNbr++;
                        this.matchedElementsNbrByScore[i]++;
                        if (element.isUpdated()) {
                            this.updatedElementsNbr++;
                            this.updatedElementsNbrByScore[i]++;
                        }
                        if (((AbstractUpdaterPlugin) this.plugin).isElementTagUpdatable(element, updatableTagName)) {
                            this.updatableElementsNbr++;
                            this.updatableElementsNbrByScore[i]++;
                        }
                        break;
                    }                    
                }
                if (!ok) {
                    LOGGER.error("Stats issue with element " + element.getOsmId());
                }
            }
        }
    }

}