/*
 * $Id: Reader.java,v 1.0.alpha March 2014 (C) INRA - DJ $
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.nmrml.reader;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import java.util.*;
import java.lang.Double;
import java.lang.Integer;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.nmrml.reader.*;


// create a nmrML file based on a java object tree generated by the JAXB XJC Tool (JDK 7 and above).
public class Reader {
    private static final String Version = "0.1.O";

    public static void main( String[] args ) {

        Options options = new Options();
        options.addOption("h", "help", false, "prints the help content");
        options.addOption("v", "version", false, "prints the version");
        options.addOption(OptionBuilder
           .withArgName("nmrML file")
           .hasArg()
           .isRequired()
           .withDescription("input  nmrML file")
           .withLongOpt("input")
           .create("i"));
        options.addOption(OptionBuilder
           .withArgName("output text file")
           .hasArg()
           .withDescription("extract FID data onto a text file")
           .withLongOpt("fid")
           .create());
        options.addOption(OptionBuilder
           .withArgName("output text file")
           .hasArg()
           .withDescription("extract Real Spectrum data onto a text file")
           .withLongOpt("real-spectrum")
           .create());

        try {
           Locale.setDefault(new Locale("en", "US"));

           String current_dir = new java.io.File( "." ).getCanonicalPath();

           CommandLineParser parser = new GnuParser();
           CommandLine cmd = parser.parse(options, args);

           String inputFile = cmd.getOptionValue("i");

        /* Read nmrML file */
           NmrMLReader nmrML = new NmrMLReader(new File(inputFile),cmd.hasOption( "fid" ),cmd.hasOption( "real-spectrum" ));
           System.out.println("nmrML version = " + nmrML.getNmrMLVersion());

    /* ACQUISITION PARAMETERS */
           System.out.println("ACQU NumberOfSteadyStateScans = " + 
                              nmrML.acq.getNumberOfSteadyStateScans());
           System.out.println("ACQU NumberOfScans = " + 
                              nmrML.acq.getNumberOfScans());
           System.out.println(String.format("ACQU RelaxationDelay (%s) = %f",
                              nmrML.acq.getRelaxationDelayUnitName(),nmrML.acq.getRelaxationDelay()));
           System.out.println(String.format("ACQU SampleAcquisitionTemperature (%s) = %f",
                              nmrML.acq.getTemperatureUnitName(),nmrML.acq.getTemperature()));
           System.out.println(String.format("ACQU SweepWidth (%s) = %f",
                              nmrML.acq.getSweepWidthUnitName(),nmrML.acq.getSpectralWidthHz()));
           System.out.println(String.format("ACQU IrradiationFrequency (%s) = %f",
                              nmrML.acq.getTransmiterFreqUnitName(),nmrML.acq.getTransmiterFreq()));
           System.out.println(String.format("ACQU EffectiveExcitationField (%s) = %f",
                              nmrML.acq.getSpectralFrequencyUnitName(),nmrML.acq.getSpectralFrequency()));
// ... 

            /* Read FID data */
           if( cmd.hasOption( "fid" ) ) {
                String fileFile = cmd.getOptionValue("fid");
                BufferedWriter outfid = new BufferedWriter(new FileWriter(fileFile));
                for ( int i=0; i< nmrML.fidData.getNumberOfDataPoints(); i++ ) {
                    for (int j=0; j<nmrML.fidData.getYdimension(); j++) {
                        if (j>0) { outfid.write("\t"); }
                        outfid.write(String.format("%f",nmrML.fidData.getYvalues()[j][i]));
                    }
                    outfid.newLine();
                }
                outfid.close();
                System.out.println(String.format("ACQU FID Number of DataPoints = %d", nmrML.fidData.getNumberOfDataPoints()));
                System.out.println(String.format("ACQU FID Dimension = %d", nmrML.fidData.getYdimension()));
           }
    
    /* PROCESSING PARAMETERS */
           if (nmrML.proc != null) {
               if( cmd.hasOption( "real-spectrum" ) ) {
                     System.out.println(String.format("PROC Real Spectrum Number of DataPoints = %d", nmrML.realSpectrum.getNumberOfDataPoints()));
                     System.out.println(String.format("PROC Real Spectrum Dimension = %d", nmrML.realSpectrum.getYdimension()));
                     System.out.println(String.format("PROC PPM Start (ppm) = %f",nmrML.proc.getMaxPpm()));
                     System.out.println(String.format("PROC PPM End (ppm) = %f",nmrML.proc.getMinPpm()));
// ... 
                     String spectrumFile = cmd.getOptionValue("real-spectrum");
                     BufferedWriter out1r = new BufferedWriter(new FileWriter(spectrumFile));
                     for ( int i=0; i< nmrML.realSpectrum.getNumberOfDataPoints(); i++ ) {
                         out1r.write(String.format("%f\t",nmrML.realSpectrum.getXvalues()[i]));
                         for (int j=0; j<nmrML.realSpectrum.getYdimension(); j++) {
                             if (j>0) { out1r.write("\t"); }
                             out1r.write(String.format("%f",nmrML.realSpectrum.getYvalues()[j][i]));
                         }
                         out1r.newLine();
                     }
                     out1r.close();
               }
           }

        } catch(MissingOptionException e){
            boolean help = false;
            boolean version = false;
            try{
              Options helpOptions = new Options();
              helpOptions.addOption("h", "help", false, "prints the help content");
              helpOptions.addOption("v", "version", false, "prints the version");
              CommandLineParser parser = new PosixParser();
              CommandLine line = parser.parse(helpOptions, args);
              if(line.hasOption("h")) help = true;
              if(line.hasOption("v")) version = true;
            } catch(Exception ex){ }
            if(!help && !version) System.err.println(e.getMessage());
            if (help) {
                HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp( "reader" , options );
            }
            if (version) {
                System.out.println("nmrML Reader version = " + Version);
            }
            System.exit(1);
        } catch(MissingArgumentException e){
            System.err.println(e.getMessage());
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp( "App" , options );
            System.exit(1);
        } catch(ParseException e){
            System.err.println("Error while parsing the command line: "+e.getMessage());
            System.exit(1);
        } catch(NullPointerException e){
            System.err.println("Error while parsing the XML file: "+e.getMessage());
            System.exit(1);
        } catch( Exception e ) {
            e.printStackTrace();
        }

    }
}