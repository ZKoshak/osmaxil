package org.openstreetmap.osmaxil.plugin.matcher;

import java.util.List;

import org.apache.log4j.Logger;
import org.openstreetmap.osmaxil.Application;
import org.openstreetmap.osmaxil.dao.OsmPostgisDB;
import org.openstreetmap.osmaxil.model.AbstractImport;
import org.openstreetmap.osmaxil.model.misc.MatchingElementId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public abstract class AbstractImportMatcher<IMPORT extends AbstractImport> {

	@Autowired
	protected OsmPostgisDB osmPostgis;

	@Value("${matcher.useSurface}")
	protected boolean useSurface;

	static protected final Logger LOGGER = Logger.getLogger(Application.class);

	abstract public List<MatchingElementId> findMatchingElements(IMPORT imp, int srid);

	public abstract float computeMatchingImportScore(IMPORT imp);

}
