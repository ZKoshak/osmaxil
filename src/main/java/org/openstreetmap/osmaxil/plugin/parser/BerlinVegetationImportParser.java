package org.openstreetmap.osmaxil.plugin.parser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.annotation.PostConstruct;

import org.openstreetmap.osmaxil.model.VegetationImport;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;

@Component("BerlinVegetationImportParser")
@Lazy

public class BerlinVegetationImportParser extends AbstractImportParser<VegetationImport> {

	private int count;

	File json;

	class File {
		String type;
		String name;
		Crs crs;
		class Crs {
			public Crs() {}
			String type;
			Properties properties;
			class Properties {
				public Properties() {}
				String name;
			}
		}
		Feature[] features;
		class Feature {
			public Feature() {}
			String type;
			Properties properties;
			class Properties {
				public Properties() {};
				String gml_id;
				String standortnr;
				String kennzeich;
				String namenr;
				String art_dtsch;
				String art_bot;
				String gattung_deutsch;
				String gattung;
				String strname;
				String hausnr;
				String zusatz;
				String pflanzjahr;
				float standalter;
				float kronedurch ;
				int stammumfg;
				float baumhoehe;
				String bezirk;
				String eigentuemer;
			}
			Geometry geometry;
			class Geometry {
				public Geometry() {};
				String type;
				double[] coordinates;
			}
		}
	}

	@PostConstruct
	public void init() throws IOException {
		LOGGER.info("Init of BerlinVegetationImportParser");
		String fileContent = new String(Files.readAllBytes(Paths.get(this.filePath)), Charset.forName("UTF-8"));
		Gson gson = new Gson();
		this.json = gson.fromJson(fileContent, File.class);
		LOGGER.info("Ok " + json.features.length + " has been loaded");
	}

	@Override
	public boolean hasNext() {
		return count < this.json.features.length;
	}

	@Override
	public VegetationImport next() {
		VegetationImport result = new VegetationImport();
		File.Feature f = this.json.features[this.count];
		File.Feature.Properties p = f.properties;
		result.setId(this.count + 1);
		result.setLongitude(f.geometry.coordinates[0]);
		result.setLatitude(f.geometry.coordinates[1]);
		if (p.standortnr != null){
			result.setReference(p.kennzeich + " :: " + p.standortnr);
		}
		if (p.art_bot != null) {
			String[] taxon_parse = {};
			int word_count=1;
			for (int i=0; i < p.art_bot.length(); i++) {
				if (p.art_bot.charAt(i) == ' ') {
					word_count++;
				}
			}
			if (word_count == 1) {
				result.setGenus(p.art_bot);
			} else {
				taxon_parse = new String[word_count];
				int word = 0;
				taxon_parse[word] = "";
				for (int i = 0; i < p.art_bot.length(); i++) {
					if(p.art_bot.charAt(i) != ' '){
						taxon_parse[word] = taxon_parse[word]+p.art_bot.charAt(i);
					} else {
						word++;
						taxon_parse[word] = "";
					}
				}
				result.setGenus(taxon_parse[0]);
				if (word_count > 2 || taxon_parse[1].charAt(0) == '\'') {
					if (taxon_parse[1].equals("x") || taxon_parse[1].equals("×") || taxon_parse[1].equals("X")) {
						result.setSpecies("×"+taxon_parse[2]);
						if (word_count > 3) {
							String Taxon = taxon_parse[0] + " " + "×"+taxon_parse[2];
							for (int i = 3; i < word_count; i++) {Taxon = Taxon + " " + taxon_parse[i];}
							result.setTaxon(Taxon);
						}
					} else {
						result.setTaxon(p.art_bot);
						if (taxon_parse[1].charAt(0) != '\'' ) {
							result.setSpecies(taxon_parse[1]);
						}
					}
				} else {
					if (!(taxon_parse[1].equals("spec."))) {
						result.setSpecies(taxon_parse[1]);
					}
				}
			}
		}
		if (p.baumhoehe > 0) {
			result.setHeight(p.baumhoehe);
		}
		if (p.kronedurch > 0) {
			result.setCrown(p.kronedurch);
		}
		if (p.stammumfg > 0) {
			result.setCircumference(p.stammumfg);
		}
		if (p.standalter != null) {
			result.setPlantingYear(p.standalter);
		}
		this.count++;
		return result;
	}

}
