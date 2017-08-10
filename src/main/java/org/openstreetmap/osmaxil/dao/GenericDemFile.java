package org.openstreetmap.osmaxil.dao;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.gdal.gdal.Band;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.openstreetmap.osmaxil.Application;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GenericDemFile {
    
    @Value("${genericDemFile.filePath}")
    private String filePath;
    
    @Value("${genericDemFile.srid}")
    private int srid;
    
    private Dataset dataset;
    
	private double xUpperLeft, yUpperLeft;
	
	private double xLowerLeft, yLowerLeft;
    
    private List<Band> bands;
    
    static private final Logger LOGGER = Logger.getLogger(Application.class);
    
    void init() {
		gdal.AllRegister();
		LOGGER.info("Opening " + this.filePath);
		this.dataset = gdal.Open(this.filePath, gdalconstConstants.GA_ReadOnly);
		int numBands = dataset.getRasterCount();
		bands = new ArrayList<>();
		LOGGER.info("Number of bands is: " + numBands);
		for (int i = 1; i <= numBands; i++) {
			this.bands.add(dataset.GetRasterBand(i));
		}
		// Store the coordinates of the lower left point
		double[] geotransform = dataset.GetGeoTransform();
		this.xUpperLeft = geotransform[0];
		this.yUpperLeft = geotransform[3];
		LOGGER.info("Upper left coordinates are: " + this.xUpperLeft + " " + this.yUpperLeft);
		this.xLowerLeft = geotransform[0];
		this.yLowerLeft = geotransform[3] + geotransform[5] * dataset.getRasterYSize();
		LOGGER.info("Lower left coordinates are: " + this.xLowerLeft + " " + this.yLowerLeft);
    }
    
    public double getValueByCoordinates(double x, double y, int srid) {
    	if (this.bands == null) {
    		this.init();
    	}
    	int xFile = (int) (x - xUpperLeft) / 5;
    	int yFile = (int) (yUpperLeft - y) / 5;
    	double[] result = new double[1];
		bands.get(0).ReadRaster(xFile, yFile, 1, 1, result);
    	return result[0];
    }
    
    public int getSrid() {
		return srid;
	}
    
}
