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
		String lFixme = new String();
		File.Feature f = this.json.features[this.count];
		File.Feature.Properties p = f.properties;
		result.setId(this.count + 1);
		result.setLongitude(f.geometry.coordinates[0]);
		result.setLatitude(f.geometry.coordinates[1]);
		if (p.standortnr != null){
			result.setReference(p.kennzeich + "::" + p.standortnr);
		}
		if (p.art_bot != null) {
			String[] taxon_parse = {};
			String lGenus = new String();
			int word_count=1;
			for (int i=0; i < p.art_bot.length(); i++) {
				if (p.art_bot.charAt(i) == ' ') {
					word_count++;
				}
			}
			if (word_count == 1) {
				lGenus = p.art_bot;
				if (lFixme != null) {
					lFixme = lFixme + "; определить вид";
				} else {
					lFixme = "определить вид";
				}
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
				lGenus = taxon_parse[0];
				if (word_count > 2 || taxon_parse[1].charAt(0) == '\'' || taxon_parse[1].equals("Hybriden") || taxon_parse[1].equals("Hybride") || taxon_parse[1].equals("Resista")) {
					if (taxon_parse[1].equals("x") || taxon_parse[1].equals("×") || taxon_parse[1].equals("X")) {
						result.setSpecies("×"+taxon_parse[2]);
						if (word_count > 3) {
							String lTaxon = taxon_parse[0] + " " + "×"+taxon_parse[2];
							for (int i = 3; i < word_count; i++) {lTaxon = lTaxon + " " + taxon_parse[i];}
							result.setTaxon(lTaxon);
						}
					} else {
						result.setTaxon(p.art_bot);
						if (taxon_parse[1].charAt(0) != '\'' ) {
							result.setSpecies(taxon_parse[1]);
						} else {
							if (lFixme != null) {
								lFixme = lFixme + "; определить вид";
							} else {
								lFixme = "определить вид";
							}
						}
					}
				} else {
					if (!(taxon_parse[1].equals("spec."))) {
						result.setSpecies(taxon_parse[1]);
					} else {
						if (lFixme != null) {
							lFixme = lFixme + "; определить вид";
						} else {
							lFixme = "определить вид";
						}
					}
				}
			}
			switch (lGenus) {
				case "Pinus":
				case "Abies":
				case "Cedrus":
				case "Cephalotaxus":
				case "Chamaecyparis":
				case "Cryptomeria":
				case "Cupressocyparis":
				case "Juniperus":
				case "Pseudotsuga":
				case "Picea":
				case "Sequoiadendron":
				case "Taxus":
				case "Thuja":
				case "Tsuga":
						result.setGenus(lGenus);
						result.setLeafType("needleleaved");
						result.setLeafCycle("evergreen");
						break;
				case "Metasequoia":
						result.setGenus(lGenus);
						result.setLeafType("needleleaved");
						result.setLeafCycle("deciduous");
						break;
				case "Acer":
				case "Aesculus":
				case "Ailanthus":
				case "Alnus":
				case "Amelanchier":
				case "Betula":
				case "Caragana":
				case "Carpinus":
				case "Castanea":
				case "Catalpa":
				case "Celtis":
				case "Cercidiphyllum":
				case "Cercis":
				case "Cladrastis":
				case "Cornus":
				case "Corylus":
				case "Crataegus":
				case "Cydonia":
				case "Davidia":
				case "Elaeagnus":
				case "Euodia":
				case "Fagus":
				case "Fraxinus":
				case "Ginkgo":
				case "Gleditsia":
				case "Gymnocladus":
				case "Juglans":
				case "Koelreuteria":
				case "Laburnum":
				case "Liquidambar":
				case "Liriodendron":
				case "Magnolia":
				case "Malus":
				case "Mespilus":
				case "Morus":
				case "Ostrya":
				case "Parrotia":
				case "Paulownia":
				case "Phellodendron":
				case "Platanus":
				case "Populus":
				case "Prunus":
				case "Ptelea":
				case "Pterocarya":
				case "Pyrus":
				case "Quercus":
				case "Salix":
				case "Sophora":
				case "Sorbus":
				case "Syringa": // может будет надо перенести к возможным кустам
				case "Tilia":
				case "Ulmus":
				case "Zelkova":
						result.setGenus(lGenus);
						result.setLeafType("broadleaved");
						result.setLeafCycle("deciduous");
						break;
				case "Buxus":
				case "Ilex":
						result.setGenus(lGenus);
						result.setLeafType("broadleaved");
						result.setLeafCycle("evergreen");
						break;
				case "Cotinus":
				case "Hippophae":
				case "Rhamnus":
				case "Rhus":
				case "Robinia":
				case "Sambucus":
				case "Tamarix":
						result.setGenus(lGenus);
						result.setLeafType("broadleaved");
						result.setLeafCycle("deciduous");
						if (lFixme != null) {
							lFixme = lFixme + "; возможно куст";
						} else {
							lFixme = "возможно куст";
						}
						break;
				case "Larix":
						result.setGenus(lGenus);
						result.setLeafType("needleleaved");
						result.setLeafCycle("deciduous");
						break;
				default:
						break;
			}
		}
		if (p.baumhoehe > 0) {
			result.setHeight(p.baumhoehe);
		} else {
			if (lFixme != null) {
				lFixme = lFixme + "; измерить высоту";
			} else {
				lFixme = "измерить высоту";
			}
		}
		if (p.stammumfg > 0) {
			result.setCircumference((float)p.stammumfg);
		} else {
			if (lFixme != null) {
				lFixme = lFixme + "; измерить обxват ствола";
			} else {
				lFixme = "измерить обxват ствола";
			}
		}
		if (p.kronedurch > 0) {
			result.setCrown(p.kronedurch);
		} else {
			if (lFixme != null) {
				lFixme = lFixme + "; измерить диаметр кроны";
			} else {
				lFixme = "измерить диаметр кроны";
			}
		}
		if (p.pflanzjahr != null) {
			result.setPlantingYear(Integer.parseInt(p.pflanzjahr));
		}
		if (p.standalter > 0) {
			result.setTreeYears(p.standalter);
		}
		if (lFixme != null) {
			result.setFixme(lFixme);
		}
		this.count++;
		return result;
	}

}
