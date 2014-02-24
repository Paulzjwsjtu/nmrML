/*
 * Copyright (c) 2013. EMBL, European Bioinformatics Institute
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

package org.nmrml.parser.bruker;

import org.nmrml.parser.Acqu;

import java.math.BigInteger;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteOrder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reader for Bruker's acqu and acqus files
 *
 * @author Luis F. de Figueiredo
 *
 * User: ldpf
 * Date: 14/01/2013
 * Time: 14:12
 *
 */
public class BrukerAcquReader implements AcquReader {

    private BufferedReader inputAcqReader;


    // parameters from acqu
    private final static Pattern REGEXP_SFO1 = Pattern.compile("\\#\\#\\$SFO1= (-?\\d+\\.\\d+)"); //transmitter frequency
    private final static Pattern REGEXP_BF1 = Pattern.compile("\\#\\#\\$BF1= (-?\\d+\\.\\d+)"); //magnetic field frequency of channel 1
    private final static Pattern REGEXP_SFO2 = Pattern.compile("\\#\\#\\$SFO2= (-?\\d+\\.\\d+)"); //decoupler frequency
    private final static Pattern REGEXP_SFO3 = Pattern.compile("\\#\\#\\$SFO3= (\\d+\\.\\d+)"); //second decoupler frequency
    private final static Pattern REGEXP_O1 = Pattern.compile("\\#\\#\\$O1= (\\d+\\.\\d+)"); //frequency offset in Hz
    private final static Pattern REGEXP_SW = Pattern.compile("\\#\\#\\$SW= (\\d+\\.\\d+)"); //spectral width (ppm)
    private final static Pattern REGEXP_SW_H = Pattern.compile("\\#\\#\\$SW_h= (\\d+\\.\\d+)"); //spectral width (Hz)
    private final static Pattern REGEXP_TD = Pattern.compile("\\#\\#\\$TD= (\\d+)"); //acquired points (real+imaginary)
    private final static Pattern REGEXP_DECIM = Pattern.compile("\\#\\#\\$DECIM= (-?\\d+)"); //DSP decimation factor
    private final static Pattern REGEXP_DSPFVS = Pattern.compile("\\#\\#\\$DSPFVS= (-?\\d+)"); //DSP firmware version
    // obtain the GRPDLY from the acqus and not from acqu...
    private final static Pattern REGEXP_GRPDLY = Pattern.compile("\\#\\#\\$GRPDLY= (-?\\d+)"); //DSP group delay
    private final static Pattern REGEXP_BYTORDA = Pattern.compile("\\#\\#\\$BYTORDA= (\\d+)"); //byte order
    // variables not yet defined in Experiment
    private final static Pattern REGEXP_AQ_MODE = Pattern.compile("\\#\\#\\$AQ\\_mod= (\\d+)"); //acquisition mode
    private final static Pattern REGEXP_DIGMOD = Pattern.compile("\\#\\#\\$DIGMOD= (\\d+)"); //filter type
    private final static Pattern REGEXP_NUMBEROFSCANS = Pattern.compile("\\#\\#\\$NS= (\\d+)"); //number of scans
    private final static Pattern REGEXP_DUMMYSCANS = Pattern.compile("\\#\\#\\$DS= (\\d+)"); //number of dummy (steady state) scans

    private final static Pattern REGEXP_RELAXATIONDELAY = Pattern.compile("\\#\\#\\$D= (.+)"); // relaxation delay ##$D= (0..63)
    private final static Pattern REGEXP_RELAXATIONDELAY_VALUES = Pattern.compile("\\d+\\.?\\d? (\\d+\\.?\\d?).+"); // relaxation delay D1

    private final static Pattern REGEXP_PULSEWIDTH = Pattern.compile("\\#\\#\\$P= (.+)"); // pulseWidth ##$P= (0..63)
    private final static Pattern REGEXP_PULSEWIDTH_VALUES = Pattern.compile("\\d+\\.?\\d?\\d? (\\d+\\.?\\d?\\d?).+"); // pulseWidth P1

    private final static Pattern REGEXP_SPINNINGRATE = Pattern.compile("\\#\\#\\$MASR= (\\d+)"); // spinning rate
    //TODO review REGEXP_PULPROG
    // examples of REGEXP_PULPROG : <zg> <cosydfph> <bs_hsqcetgpsi>; basically a word between < >
    private final static Pattern REGEXP_PULPROG = Pattern.compile("\\#\\#\\$PULPROG= (.+)"); //pulse program
    //TODO review REGEXP_NUC1
    // examples of REGEXP_NUC1 : <1H>; basically <isotope number + element>
    private final static Pattern REGEXP_NUC_INDEX = Pattern.compile("\\#\\#\\$NUC(\\d)=.+"); // index of the nucleus
    private final static Pattern REGEXP_NUC1 = Pattern.compile("\\#\\#\\$NUC1= (.+)"); // observed nucleus
    //TODO review REGEXP_INSTRUM
    // examples of REGEXP_INSTRUM : <amx500> ; basically <machine name>
    private final static Pattern REGEXP_INSTRUM = Pattern.compile("\\#\\#\\$INSTRUM= (.+)"); // instrument name
    private final static Pattern REGEXP_DTYPA = Pattern.compile("\\#\\#\\$DTYPA= (\\d+)"); //data type (0 -> 32 bit int, 1 -> 64 bit double)
    // examples of REGEXP_SOLVENT : <DMSO> ; basically <solvent name>
    private final static Pattern REGEXP_SOLVENT = Pattern.compile("\\#\\#\\$SOLVENT= (.+)"); // solvent name
    //TODO review REGEXP_PROBHD
    // examples of REGEXP_PROBHD : <32> <>; basically <digit?>
    private final static Pattern REGEXP_PROBHD = Pattern.compile("\\#\\#\\$PROBHD= <(.+)"); // probehead
    // examples of REGEXP_ORIGIN : Bruker Analytik GmbH; basically a name
    private final static Pattern REGEXP_TITLE = Pattern.compile("\\#\\#TITLE= (.+), (.+)\t\t(.+)"); // origin
    //TODO review REGEXP_ORIGIN
    // examples of REGEXP_ORIGIN : Bruker Analytik GmbH; basically a name
    private final static Pattern REGEXP_ORIGIN = Pattern.compile("\\#\\#ORIGIN= (.+)"); // origin
    //TODO review REGEXP_OWNER
    // examples of REGEXP_OWNER : guest; basically the used ID
    private final static Pattern REGEXP_OWNER = Pattern.compile("\\#\\#OWNER= (.+)"); // owner
    private final static Pattern REGEXP_METAINFO = Pattern.compile("\\$\\$ (.+)"); // owner

    private final static Pattern REGEXP_TEMPERATURE = Pattern.compile("\\#\\#\\$TE= (\\d+\\.?\\d?)"); // temperature in Kelvin

    public BrukerAcquReader() {
    }
    
    public BrukerAcquReader(File acquFile) throws IOException {
        inputAcqReader = new BufferedReader(new FileReader(acquFile));        
    }
    
    public BrukerAcquReader(InputStream acqFileInputStream) {
        inputAcqReader = new BufferedReader(new InputStreamReader(acqFileInputStream));
    }

    public BrukerAcquReader(String filename) throws IOException {
        this(new File(filename));
        // required parameters so far...
        // AquiredPoints: FidReader
        // SpectraWidth: FidReader
        // transmiterFreq: FidReader
        //
    }

    @Override
    public Acqu read() throws Exception {
        Matcher matcher;
        Acqu acquisition = new Acqu(Acqu.Spectrometer.BRUKER);
        String line = inputAcqReader.readLine();
        while (inputAcqReader.ready() && (line != null)) {
            /* //magnetic field frequency of channel 1 */
            if (REGEXP_BF1.matcher(line).find()) {
                matcher = REGEXP_BF1.matcher(line);
                matcher.find();
                acquisition.setSpectralFrequency(Double.parseDouble(matcher.group(1)));
            }
            /* magnetic field ?? */
            if (REGEXP_SFO1.matcher(line).find()) {
                matcher = REGEXP_SFO1.matcher(line);
                matcher.find();
                acquisition.setTransmiterFreq(Double.parseDouble(matcher.group(1)));
            }
            if (REGEXP_SFO2.matcher(line).find()) {
                matcher = REGEXP_SFO2.matcher(line);
                matcher.find();
                acquisition.setDecoupler1Freq(Double.parseDouble(matcher.group(1)));
            }
            if (REGEXP_SFO3.matcher(line).find()) {
                matcher = REGEXP_SFO3.matcher(line);
                matcher.find();
                acquisition.setDecoupler2Feq(Double.parseDouble(matcher.group(1)));
            }
            /* frequency offset in Hz */
            if (REGEXP_O1.matcher(line).find()) {
                matcher = REGEXP_O1.matcher(line);
                matcher.find();
                acquisition.setFreqOffset(Double.parseDouble(matcher.group(1)));
            }
            /* sweep width in ppm*/
            if (REGEXP_SW.matcher(line).find()) {
                matcher = REGEXP_SW.matcher(line);
                matcher.find();
                acquisition.setSpectralWidth(Double.parseDouble(matcher.group(1)));
            }
            /* sweep width in Hertz*/
            if (REGEXP_SW_H.matcher(line).find()) {
                matcher = REGEXP_SW_H.matcher(line);
                matcher.find();
                acquisition.setSpectralWidthHz(Double.parseDouble(matcher.group(1)));
            }
            /* number of data points */
            if (REGEXP_TD.matcher(line).find()) {
                matcher = REGEXP_TD.matcher(line);
                matcher.find();
                acquisition.setAquiredPoints(Integer.parseInt(matcher.group(1)));
            }
            if (REGEXP_DECIM.matcher(line).find()) {
                matcher = REGEXP_DECIM.matcher(line);
                matcher.find();
                acquisition.setDspDecimation(Integer.parseInt(matcher.group(1)));
            }
            if (REGEXP_DSPFVS.matcher(line).find()) {
                matcher = REGEXP_DSPFVS.matcher(line);
                matcher.find();
                acquisition.setDspFirmware(Integer.parseInt(matcher.group(1)));
            }
            if (REGEXP_GRPDLY.matcher(line).find()) {
                matcher = REGEXP_GRPDLY.matcher(line);
                matcher.find();
                acquisition.setDspGroupDelay(Double.parseDouble(matcher.group(1)));
            }
            /* byte order */
            if (REGEXP_BYTORDA.matcher(line).find()) {
                matcher = REGEXP_BYTORDA.matcher(line);
                matcher.find();
                switch (Integer.parseInt(matcher.group(1))){
                    case 0 :
                        acquisition.setByteOrder(ByteOrder.LITTLE_ENDIAN);
                        break;
                    case 1 :
                        acquisition.setByteOrder(ByteOrder.BIG_ENDIAN);
                        break;
                    default:
                        break;
                }
            }
            /* integer type */
            if (REGEXP_DTYPA.matcher(line).find()) {
                matcher = REGEXP_DTYPA.matcher(line);
                matcher.find();
                acquisition.set32Bit((Pattern.compile("0").matcher(matcher.group(1)).find()));
                switch (Integer.parseInt(matcher.group(1))) {
                    case 0:
                        acquisition.setBiteSyze(4);   // 32 bits integer - 4 octets
                        break;
                    case 1:
                        acquisition.setBiteSyze(8);   // 64 bits integer - 8 octets
                        break;
                    default:
                        acquisition.setBiteSyze(4);   // 32 bits integer
                        break;
                }
            }
            if (REGEXP_AQ_MODE.matcher(line).find()) {
                matcher = REGEXP_AQ_MODE.matcher(line);
                matcher.find();
                acquisition.setAcquisitionMode(Integer.parseInt(matcher.group(1)));
            }
            if (REGEXP_DIGMOD.matcher(line).find()) {
                matcher = REGEXP_DIGMOD.matcher(line);
                matcher.find();
                acquisition.setFilterType(Integer.parseInt(matcher.group(1)));
            }
            /* relaxation delay */
            if(REGEXP_RELAXATIONDELAY.matcher(line).find()){
                line = inputAcqReader.readLine();
                if(REGEXP_RELAXATIONDELAY_VALUES.matcher(line).find()){
                    matcher = REGEXP_RELAXATIONDELAY_VALUES.matcher(line);
                    matcher.find();
                    acquisition.setRelaxationDelay(Double.parseDouble(matcher.group(1)));
                }
            }
            /* pulse width */
            if(REGEXP_PULSEWIDTH.matcher(line).find()){
                line = inputAcqReader.readLine();
                if(REGEXP_PULSEWIDTH_VALUES.matcher(line).find()){
                    matcher = REGEXP_PULSEWIDTH_VALUES.matcher(line);
                    matcher.find();
                    acquisition.setPulseWidth(Double.parseDouble(matcher.group(1)));
                }
            }
            /* spinning rate */
            if(REGEXP_SPINNINGRATE.matcher(line).find()){
                matcher = REGEXP_SPINNINGRATE.matcher(line);
                matcher.find();
                acquisition.setSpiningRate(Integer.parseInt(matcher.group(1)));
            }
            /* temperature of the experiment */
            if(REGEXP_TEMPERATURE.matcher(line).find()) {
                matcher = REGEXP_TEMPERATURE.matcher(line);
                matcher.find();
                acquisition.setTemperature(Double.parseDouble(matcher.group(1)));
            }
            /* number of scans */
            if (REGEXP_NUMBEROFSCANS.matcher(line).find()) {
                matcher = REGEXP_NUMBEROFSCANS.matcher(line);
                matcher.find();
                acquisition.setNumberOfScans(BigInteger.valueOf(Long.parseLong(matcher.group(1))));
                /* number of dummy (steady state) scans */
            }
            if (REGEXP_DUMMYSCANS.matcher(line).find()) {
                matcher = REGEXP_DUMMYSCANS.matcher(line);
                matcher.find();
                acquisition.setNumberOfSteadyStateScans(BigInteger.valueOf(Long.parseLong(matcher.group(1))));
            }
            if (REGEXP_PULPROG.matcher(line).find()) {
                matcher = REGEXP_PULPROG.matcher(line);
                matcher.find();
                acquisition.setPulseProgram(matcher.group(1).replace("<", "").replace(">", ""));
            }
            /* observed nucleus */
            if (REGEXP_NUC1.matcher(line).find()) {
                matcher = REGEXP_NUC1.matcher(line);
                matcher.find();
                acquisition.setObservedNucleus(matcher.group(1).replace("<", "").replace(">", ""));
            }
            if (REGEXP_INSTRUM.matcher(line).find()) {
                matcher = REGEXP_INSTRUM.matcher(line);
                matcher.find();
                acquisition.setInstrumentName(matcher.group(1).replace("<", "").replace(">", ""));
            }
            if (REGEXP_SOLVENT.matcher(line).find()) {
                matcher = REGEXP_SOLVENT.matcher(line);
                matcher.find();
                acquisition.setSolvent(matcher.group(1).replace("<", "").replace(">", ""));
            }
            /* extract instrument metadata */
            if (REGEXP_PROBHD.matcher(line).find()) {
                matcher = REGEXP_PROBHD.matcher(line);
                matcher.find();
                acquisition.setProbehead(matcher.group(1).replace("<", "").replace(">", ""));
            }
            /* extract software metadata */
            if(REGEXP_TITLE.matcher(line).find()){
                matcher = REGEXP_TITLE.matcher(line);
                matcher.find();
                acquisition.setSoftware(matcher.group(2));
                acquisition.setSoftVersion(matcher.group(3));
            }
            /* add the file format to the source files*/
            if (REGEXP_ORIGIN.matcher(line).find()) {
                matcher = REGEXP_ORIGIN.matcher(line);
                matcher.find();
                acquisition.setOrigin(matcher.group(1));
            }
            /* extract contact */
            if (REGEXP_OWNER.matcher(line).find()) {
                matcher = REGEXP_OWNER.matcher(line);
                matcher.find();
                acquisition.setOwner(matcher.group(1));
            }
            /* extract email from "metadata" */
            if(REGEXP_METAINFO.matcher(line).find()){
                matcher=REGEXP_METAINFO.matcher(line);
                matcher.find();
                for(String token : matcher.group(1).split(" ")){
                   if(token.contains("@")){
                       acquisition.setEmail(token);
                       break;
                    }
                }
            }
            line = inputAcqReader.readLine();
        }
        inputAcqReader.close();
        return acquisition;
    }


}
