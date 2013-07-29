RasterLib
=========

Java library to read [Esri Raster Float format](http://help.arcgis.com/en/arcgisdesktop/10.0/help/index.html#//001200000006000000)

## Build and package

    $ mvn install

## Creating raster float files

The best way to create a raster float file is from a GeoProcessing task in ArcMap, where you can convert a set of features to a raster.
Then the raster can be converted to a float format.

![Feature To Raster](https://dl.dropboxusercontent.com/u/2193160/FeatureToRaster.png)

![Raster To Float](https://dl.dropboxusercontent.com/u/2193160/RasterToFloat.png)
