package org.openstreetmap.osmaxil.dao.xml.osm;

import javax.xml.bind.annotation.XmlAttribute;

public class OsmXmlMember {

	@XmlAttribute
	public long ref;

	@XmlAttribute
	public String role;

	@XmlAttribute
	public String type;

}
