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

import java.util.HashMap;

/**
 * Class to hold select data from a Fit file.
 * 
 * @author Nicholas Rodie
 */
public class GZMZwiftFile {
    
    private Integer _manufacturer, _sessionFinished, _timeCreated;
    private String _path;
    private HashMap<Integer, HashMap<Integer, Double>> _records;
    
    public GZMZwiftFile(String path) {
        _path = path;
        _manufacturer = _sessionFinished = _timeCreated = 0;
        _records = new HashMap<>();
    }
    
    /**
    * Get the Fit file manufacturer value
    * 
    * @return The Fit file manufacturer value
    */
    public Integer getManufacturer() {
        return _manufacturer;
    }
    
    /**
    * Get the Fit file path
    * 
    * @return The Fit file path
    */
    public String getPath() {
        return _path;
    }
    
    /**
    * Get the Fit file records recorded by the listener
    * 
    * @return The Fit file records
    */
    public HashMap<Integer, HashMap<Integer, Double>> getRecords() {
        return _records;
    }
    
    /**
    * Get the Fit file session finished time
    * 
    * @return The Fit file session finished time
    */
    public Integer getSessionFinished() {
        return _sessionFinished;
    }
    
     /**
    * Get the Fit file creation time
    * 
    * @return The Fit file creation time
    */
    public Integer getTimeCreated() {
        return _timeCreated;
    }
    
    /**
    * Add a Fit file record of field values to the records
    * 
    * @param timestamp The timestamp of this record
    * @param record A HashMap of Fit file record field values
    */
    public void putRecord(Integer timestamp, HashMap<Integer, Double> record) {
        _records.put(timestamp, record);
    }    
    
    /**
    * Set the Fit file manufacturer value
    * 
    * @param manufacturer The Fit file manufacturer value
    */
    public void setManufacturer(Integer manufacturer) {
        _manufacturer = manufacturer;
    }
    
    /**
    * Set the Fit file session finished time
    * 
    * @param sessionFinished The Fit file session finished time
    */
    public void setSessionFinished(Integer sessionFinished) {
        _sessionFinished = sessionFinished;
    }
    
    /**
    * Set the Fit file time created value
    * 
    * @param timeCreated The Fit file time created value
    */
    public void setTimeCreated(Integer timeCreated) {
        _timeCreated = timeCreated;
    }
    
    
    
}
