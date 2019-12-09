/*
 * The MIT License
 *
 * Copyright 2019 Nicholas Rodie.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package GarminZwiftMerge;

import com.garmin.fit.FileEncoder;
import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Fit;
import com.garmin.fit.LapMesg;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.Mesg;
import com.garmin.fit.MesgDefinition;
import com.garmin.fit.MesgDefinitionListener;
import com.garmin.fit.MesgListener;
import com.garmin.fit.MesgNum;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;
import com.garmin.fit.SubSport;
import java.io.File;
import java.util.HashMap;
import java.util.List;

/**
 * Fit Decode listener for processing specific Fit file messages from a Garmin file
 * 
 * @author Nicholas Rodie
 */
public class GZMGarminListener implements MesgListener, MesgDefinitionListener {
    
    private final int VIRTUAL_ACTIVITY = SubSport.VIRTUAL_ACTIVITY.getValue();            
    private Double _ascent, _descent, _distOffset, _lapAscent, _lapDescent, _lastAlt, _totDist, _lapDist, _maxSpd, _maxLapSpd;
    private final FileEncoder _encoder;
    private Integer  _endTime, _fileIndex, _lapTime, _lastTime, _startTime; 
    private HashMap<Integer, HashMap<Integer, Double>> _records;
    private final List<GZMZwiftFile> _zwiftFiles;
        
    /**
    * Garmin Fit file listener class constructor
    * 
    * @param zwiftFiles A list of GZMZwiftFile instances
    * @param outputFilePath The path this listener will write Fit file to
    */
    public GZMGarminListener(List<GZMZwiftFile> zwiftFiles, String outputFilePath) {

        _ascent = _descent = _lapAscent = _lapDescent = _lastAlt = _totDist = _lapDist = _maxSpd = _maxLapSpd = 0.0;
        _distOffset = null;
        _encoder = new FileEncoder(new File(outputFilePath), Fit.ProtocolVersion.V2_0);
        _endTime = zwiftFiles.get(0).getSessionFinished();
        _lapTime = _lastTime = _startTime = _fileIndex = 0;
        _zwiftFiles = zwiftFiles;        
        _records = new HashMap<>();
        zwiftFiles.forEach(zwiftFile -> {
            _records.putAll(zwiftFile.getRecords());
        });
    }
    
    /**
    * The close method must be called to complete the Fit file write process
    */
    public void close() {
        _encoder.close();
    }

    /**
     * Merges Zwift location data with Garmin data.
     * 
     * Reads known input and writes new output.
     */
    @Override
    public void onMesg(Mesg mesg) { 

        int type = mesg.getNum(); 

        // ignore any custom developer messages by only using Fit library types
        if(MesgNum.getStringFromValue(type).equals("")) return;

        switch (type) {

            case MesgNum.RECORD:                            

                Integer timestamp = mesg.getFieldIntegerValue(RecordMesg.TimestampFieldNum);
                HashMap<Integer, Double> record = _records.get(timestamp);

                // ignore records that dont match a Zwift timestamp
                if(record == null) return;                

                // first zwift file instance
                else if(_startTime == 0) {
                    _startTime = timestamp;
                    _lapTime = timestamp;
                    _lastAlt = record.get(RecordMesg.AltitudeFieldNum);
                }
                
                // additional zwift file instances
                else if(_endTime < timestamp) {
                    _fileIndex++;                    
                    _endTime = _zwiftFiles.get(_fileIndex).getSessionFinished();

                    // add distance from previous file
                    _distOffset = _totDist;

                    // reset alt so not big jumps from change in location
                    _lastAlt = record.get(RecordMesg.AltitudeFieldNum);

                    // start a new lap                                
                    _lapAscent = _lapDescent = _maxLapSpd = 0.0;
                    _lapTime = timestamp;
                    _lapDist = _totDist;
                }

                // calculate altitudes
                Double altitude = record.get(RecordMesg.AltitudeFieldNum);
                if(altitude > _lastAlt) {
                    _ascent += altitude - _lastAlt;
                    _lapAscent += altitude - _lastAlt;
                }
                else if(altitude < _lastAlt) {
                    _descent += _lastAlt - altitude;
                    _lapDescent += _lastAlt - altitude;
                }   

                // distance offset removes any Zwift distance not covered by Garmin timestamps
                // or adds any difference when combining input files
                if(_distOffset == null) _distOffset = record.get(RecordMesg.DistanceFieldNum) * -1;
                _totDist = record.get(RecordMesg.DistanceFieldNum) + _distOffset;

                // watch for max speeds
                Double speed = record.get(RecordMesg.SpeedFieldNum);
                if(speed > _maxSpd) _maxSpd = speed;
                if(speed > _maxLapSpd) _maxLapSpd = speed;

                // update the selected Garmin fields
                // elevation
                mesg.setFieldValue(RecordMesg.AltitudeFieldNum, altitude);
                mesg.setFieldValue(RecordMesg.EnhancedAltitudeFieldNum, altitude);
                // distance
                mesg.setFieldValue(RecordMesg.DistanceFieldNum, _totDist);
                // location
                mesg.setFieldValue(RecordMesg.PositionLatFieldNum, record.get(RecordMesg.PositionLatFieldNum));
                mesg.setFieldValue(RecordMesg.PositionLongFieldNum, record.get(RecordMesg.PositionLongFieldNum));
                // speed
                mesg.setFieldValue(RecordMesg.SpeedFieldNum, speed);
                mesg.setFieldValue(RecordMesg.EnhancedSpeedFieldNum, speed);

                _lastAlt = altitude;
                _lastTime = timestamp;
                break;

            case MesgNum.LAP:

                // calculate lap values
                int lapTime = _lastTime - _lapTime;
                Double lapDist = _totDist - _lapDist;

                // update the selected Garmin fields
                // elevation
                mesg.setFieldValue(LapMesg.TotalAscentFieldNum, _lapAscent);
                mesg.setFieldValue(LapMesg.TotalDescentFieldNum, _lapDescent);
                // distance
                mesg.setFieldValue(LapMesg.TotalDistanceFieldNum, lapDist);
                // speed
                mesg.setFieldValue(LapMesg.AvgSpeedFieldNum, lapDist / lapTime);
                mesg.setFieldValue(LapMesg.MaxSpeedFieldNum, _maxLapSpd);
                mesg.setFieldValue(LapMesg.EnhancedAvgSpeedFieldNum, lapDist / lapTime);
                mesg.setFieldValue(LapMesg.EnhancedMaxSpeedFieldNum, _maxLapSpd);                            
                // time
                mesg.setFieldValue(LapMesg.TotalElapsedTimeFieldNum, lapTime);
                mesg.setFieldValue(LapMesg.TotalTimerTimeFieldNum, lapTime);
                // set virtual
                mesg.setFieldValue(LapMesg.SubSportFieldNum, VIRTUAL_ACTIVITY);

                _lapAscent = _lapDescent = _maxLapSpd = 0.0;
                _lapTime = _lastTime;
                _lapDist = _totDist;
                break;

            case MesgNum.SESSION:

                int elapsedTime = _lastTime - _startTime;
                // update the selected Garmin fields
                // elevation
                mesg.setFieldValue(SessionMesg.TotalAscentFieldNum, _ascent);
                mesg.setFieldValue(SessionMesg.TotalDescentFieldNum, _descent);
                // speed
                mesg.setFieldValue(SessionMesg.AvgSpeedFieldNum, _totDist / elapsedTime);
                mesg.setFieldValue(SessionMesg.MaxSpeedFieldNum, _maxSpd);
                mesg.setFieldValue(SessionMesg.EnhancedAvgSpeedFieldNum, _totDist / elapsedTime);
                mesg.setFieldValue(SessionMesg.EnhancedMaxSpeedFieldNum, _maxSpd);
                // distance
                mesg.setFieldValue(SessionMesg.TotalDistanceFieldNum, _totDist);
                // time
                mesg.setFieldValue(SessionMesg.TotalElapsedTimeFieldNum, elapsedTime);
                mesg.setFieldValue(SessionMesg.TotalTimerTimeFieldNum, elapsedTime);
                //set virtual
                mesg.setFieldValue(SessionMesg.SubSportFieldNum, VIRTUAL_ACTIVITY);
                
                break;
                
            case MesgNum.FILE_ID:

                Integer manufacturer = mesg.getFieldIntegerValue(FileIdMesg.ManufacturerFieldNum);
                if(manufacturer != Manufacturer.GARMIN)
                    throw new GZMRuntimeException(GarminZwiftMerge.ERR_MSG_MANUFACTURER_GARMIN);      
                break;

            default:
                break;                     
        }
        _encoder.write(mesg); 
    }

    /**
     * Copies known Fit message definitions to new file.
     */
    @Override
    public void onMesgDefinition(MesgDefinition mesg) {
        if(!MesgNum.getStringFromValue(mesg.getNum()).equals("")) _encoder.write(mesg);
    }
}