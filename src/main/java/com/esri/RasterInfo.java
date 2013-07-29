package com.esri;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.regex.Pattern;

/**
 */
public class RasterInfo
{
    public double minx;
    public double miny;
    public int maxr;
    public int maxc;
    public int ncols;
    public int nrows;
    public float nodata;
    public double cellSize;
    public boolean isLSBFirst;
    public float minLimit;
    public float maxLimit;
    public float minData;
    public float maxData;
    public float delData;
    public float[] data;

    private interface DataReader
    {
        public float readData(final DataInputStream dataInputStream) throws IOException;
    }

    private final class LSBDataReader implements DataReader
    {
        @Override
        public float readData(final DataInputStream dataInputStream) throws IOException
        {
            final int b0 = dataInputStream.readUnsignedByte();
            final int b1 = dataInputStream.readUnsignedByte();
            final int b2 = dataInputStream.readUnsignedByte();
            final int b3 = dataInputStream.readUnsignedByte();
            final int nume = (b3 << 24) | (b2 << 16) | (b1 << 8) | b0;
            return Float.intBitsToFloat(nume);
        }
    }

    private final class MSBDataReader implements DataReader
    {
        @Override
        public float readData(final DataInputStream dataInputStream) throws IOException
        {
            return dataInputStream.readFloat();
        }
    }

    public RasterInfo()
    {
    }

    public void loadRaster(
            final String path) throws IOException
    {
        loadRasterHeader(path);
        loadRasterData(path);
    }

    public void loadRasterData(final String path) throws IOException
    {
        final DataInputStream dataInputStream = new DataInputStream(new FileInputStream(path));
        try
        {
            final DataReader dataReader;
            if (this.isLSBFirst)
            {
                dataReader = new LSBDataReader();
            }
            else
            {
                dataReader = new MSBDataReader();
            }

            int index = 0;
            int maxr = 0;
            int maxc = 0;
            float minData = Float.POSITIVE_INFINITY;
            float maxData = Float.NEGATIVE_INFINITY;
            double currN = 0.0;
            double currM = 0.0;
            double currS = 0.0;
            float[] data = new float[this.nrows * this.ncols];
            for (int r = 0; r < this.nrows; r++)
            {
                for (int c = 0; c < this.ncols; c++)
                {
                    final float datum = dataReader.readData(dataInputStream);
                    data[index++] = datum;
                    if (datum != this.nodata)
                    {
                        final double nextM = currM + (datum - currM) / ++currN;
                        currS += (datum - currM) * (datum - nextM);
                        currM = nextM;

                        minData = Math.min(minData, datum);
                        if (datum > maxData)
                        {
                            maxData = datum;
                            maxr = r;
                            maxc = c;
                        }
                    }
                }
            }
            double variance = currN > 0 ? currS / (currN - 1) : 0.0;
            double stddev = Math.sqrt(variance);

            this.maxr = maxr;
            this.maxc = maxc;
            this.minLimit = (float) (currM - stddev);
            this.maxLimit = (float) (currM + stddev);
            this.minData = minData;
            this.maxData = maxData;
            this.delData = maxData - minData;
            this.data = data;
        }
        finally
        {
            dataInputStream.close();
        }
    }

    public void loadRasterHeader(
            final String fltPath) throws IOException
    {
        final String hdrPath = fltPath.replace(".flt", ".hdr");
        final LineNumberReader lineReader = new LineNumberReader(new FileReader(hdrPath));
        try
        {
            final Pattern pattern = Pattern.compile("\\s+");
            String line = lineReader.readLine();
            while (line != null)
            {
                final String[] tokens = pattern.split(line);
                if (tokens.length == 2)
                {
                    final String key = tokens[0];
                    final String val = tokens[1];
                    if ("xllcorner".equalsIgnoreCase(key))
                    {
                        this.minx = Double.parseDouble(val);
                    }
                    else if ("yllcorner".equalsIgnoreCase(key))
                    {
                        this.miny = Double.parseDouble(val);
                    }
                    else if ("cellsize".equalsIgnoreCase(key))
                    {
                        this.cellSize = Double.parseDouble(val);
                    }
                    else if ("ncols".equalsIgnoreCase(key))
                    {
                        this.ncols = Integer.parseInt(val);
                    }
                    else if ("nrows".equalsIgnoreCase(key))
                    {
                        this.nrows = Integer.parseInt(val);
                    }
                    else if ("nodata_value".equalsIgnoreCase(key))
                    {
                        this.nodata = Float.parseFloat(val);
                    }
                    else if ("byteorder".equalsIgnoreCase(key))
                    {
                        this.isLSBFirst = "lsbfirst".equalsIgnoreCase(val);
                    }
                }
                line = lineReader.readLine();
            }
        }
        finally
        {
            lineReader.close();
        }
    }

    public int toGridX(final double worldX)
    {
        return (int) ((worldX - minx) / cellSize);
    }

    public int toGridY(final double worldY)
    {
        return nrows - (int) ((worldY - miny) / cellSize);
    }

    public double toWorldX(final double gridX)
    {
        return minx + gridX * cellSize;
    }

    public double toWorldY(final double gridY)
    {
        return miny + (nrows - gridY) * cellSize;
    }
}
