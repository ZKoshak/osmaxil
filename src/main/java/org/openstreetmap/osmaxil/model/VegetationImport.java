package org.openstreetmap.osmaxil.model;

public class VegetationImport extends AbstractImport {

	private String genus;

	private String species;

	private String taxon;

	private String leafCycle;

	private String leafType;

	private Float height;

	private Float circumference;

	private Float crown;

	private Integer plantingYear;

	private Float treeYears;

	private String denotation;

	private String reference;

	private String source;

	private String fixme;

	@Override
	public String getValueByTagName(String tagName) {
		if (ElementTag.GENUS.equals(tagName)) {
			return this.genus.toString();
		} else if (ElementTag.SPECIES.equals(tagName)) {
			return this.species.toString();
		} else if (ElementTag.TAXON.equals(tagName)) {
			return this.taxon.toString();
		}
		return null;
	}

	@Override
	public String toString() {
		return "VegetationImport with id=[" + this.id + "], coords=[" + this.latitude + ", " + this.longitude + "], genus=[" + this.genus + "], species=["
				+ this.species + "], taxon=[" + this.taxon + "], leaf_cycle=[" + this.leafCycle + "], leaf_type=[" + this.leafType + "], denotation=["
				+ this.denotation + "], planted_date=[" + this.plantingYear + "], tree:years=[" + this.treeYears + "], height=[" + this.height + "], circumference=["
				+ this.circumference + "], diameter_crown=[" + this.crown + "], source=[" + this.source + "], fixme=[" + this.fixme + "], ref=["  + this.reference + "]";
	}

	public String getReference() {
		return reference;
	}

	public void setReference(String reference) {
		this.reference = reference;
	}

	public String getGenus() {
		return genus;
	}

	public void setGenus(String genus) {
		this.genus = genus;
	}

	public String getSpecies() {
		return species;
	}

	public void setSpecies(String subType) {
		this.species = subType;
	}

	public String getTaxon() {
		return taxon;
	}

	public void setTaxon(String taxon) {
		this.taxon = taxon;
	}

	public String getLeafCycle() {
		return leafCycle;
	}

	public void setLeafCycle(String leafCycle) {
		this.leafCycle = leafCycle;
	}

	public String getLeafType() {
		return leafType;
	}

	public void setLeafType(String leafType) {
		this.leafType = leafType;
	}

	public Float getHeight() {
		return height;
	}

	public void setHeight(Float height) {
		this.height = height;
	}

	public Float getCircumference() {
		return circumference;
	}

	public void setCircumference(Float circumference) {
		this.circumference = circumference;
	}

	public Float getCrown() {
		return crown;
	}

	public void setCrown(Float crown) {
		this.crown = crown;
	}

	public Integer getPlantingYear() {
		return plantingYear;
	}

	public void setPlantingYear(Integer plantingYear) {
		this.plantingYear = plantingYear;
	}

	public Float getTreeYears() {
		return treeYears;
	}

	public void setTreeYears(Float treeYears) {
		this.treeYears = treeYears;
	}

	public String getDenotation() {
		return denotation;
	}

	public void setDenotation(String denotation) {
		this.denotation = denotation;
	}
}
