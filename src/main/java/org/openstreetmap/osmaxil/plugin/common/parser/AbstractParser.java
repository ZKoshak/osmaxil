package org.openstreetmap.osmaxil.plugin.common.parser;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.openstreetmap.osmaxil.Application;
import org.openstreetmap.osmaxil.model.AbstractImport;
import org.springframework.stereotype.Repository;

@Repository
public abstract class AbstractParser<IMPORT extends AbstractImport> implements Iterator<IMPORT> {

    static protected final Logger LOGGER = Logger.getLogger(Application.class);
    
    abstract public int getSrid();
    
}
