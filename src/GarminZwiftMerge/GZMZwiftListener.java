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

import com.garmin.fit.FileIdMesg;
import com.garmin.fit.Manufacturer;
import com.garmin.fit.Mesg;
import com.garmin.fit.MesgListener;
import com.garmin.fit.MesgNum;
import com.garmin.fit.RecordMesg;
import com.garmin.fit.SessionMesg;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * Fit Decode listener for processing specific fit file messages from a Zwift file.
 * 
 * @author Nicholas Rodie
 */
public class GZMZwiftListener implements MesgListener {
    
    private final List<Integer> _fields;
    private final GZMZwiftFile _zwiftFile;

    /**
    * Zwift Fit file listener class constructor
    * 
    * @param path The path of the Fit file this listener is reading
    */
    public GZMZwiftListener(String path) {                
        _fields = new ArrayList<>(Arrays.asList(
                RecordMesg.AltitudeFieldNum,
                RecordMesg.DistanceFieldNum,
                RecordMesg.PositionLatFieldNum,
                RecordMesg.PositionLongFieldNum,
                RecordMesg.SpeedFieldNum));
        _zwiftFile = new GZMZwiftFile(path);
    }
    
    /**
    * Get the GZMZwiftFile instance
    * 
    * @return The GZMZwiftFile instance
    */
    public GZMZwiftFile getZwiftFile() {
        return _zwiftFile;
    }

    /**
     * Reads Zwift data.
     * 
     * Reads and stores selected Zwift fields.
     */
    @Override
    public void onMesg(Mesg mesg) { 

        switch (mesg.getNum()) {

            case MesgNum.RECORD:

                HashMap<Integer, Double> record = new HashMap<>();   
                for(int i = 0; i < _fields.size(); i++ ) {

                    int field = _fields.get(i);
                    Double value = mesg.getFieldDoubleValue(field);

                    // ignore records with missing fields
                    if(value != null) record.put(field, value);                
                    else return;
                }

                Integer timestamp = mesg.getFieldIntegerValue(RecordMesg.TimestampFieldNum);
                _zwiftFile.putRecord(timestamp, record);  
                return;
                
            case MesgNum.FILE_ID:

                Integer manufacturer = mesg.getFieldIntegerValue(FileIdMesg.ManufacturerFieldNum);
                if(manufacturer == Manufacturer.ZWIFT) _zwiftFile.setManufacturer(manufacturer);
                else throw new GZMRuntimeException(GarminZwiftMerge.ERR_MSG_MANUFACTURER_ZWIFT);
                
                Integer timeCreated = mesg.getFieldIntegerValue(FileIdMesg.TimeCreatedFieldNum);
                if(timeCreated != null) _zwiftFile.setTimeCreated(timeCreated);
                else throw new GZMRuntimeException(GarminZwiftMerge.ERR_MSG_GET_FIT_CREATED);                
                return;

            case MesgNum.SESSION:

                Integer sessionFinished = mesg.getFieldIntegerValue(SessionMesg.TimestampFieldNum);
                if(sessionFinished != null) _zwiftFile.setSessionFinished(sessionFinished);
                else throw new GZMRuntimeException(GarminZwiftMerge.ERR_MSG_GET_FIT_SESSION);  
        }
    }
}
