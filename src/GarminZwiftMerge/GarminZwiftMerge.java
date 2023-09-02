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

import com.garmin.fit.MesgListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Standalone or API class for merging Zwift location data into a Garmin Fit file.
 * 
 * @author Nicholas Rodie
 */
public class GarminZwiftMerge { 
        
    // command line switches
    public static final String ARG_FORCE = "-f";
    public static final String ARG_GARMIN = "-g";
    public static final String ARG_HELP_UNIX = "--help";
    public static final String ARG_HELP_WIN = "/?";
    public static final String ARG_OUTPUT = "-o";
    public static final String ARG_OUTPUT_FORCE = "-of";
    public static final String ARG_ZWIFT = "-z";
    
    // error messages
    public static final String ERR_MSG_ARGS_FORMAT = "Error reading input arguments.";
    public static final String ERR_MSG_FILE_NOT_FOUND = "File not found: "; 
    public static final String ERR_MSG_FIT_FILE_INTEGRITY = "Fit file failed integrity check: "; 
    public static final String ERR_MSG_FORCE_OVERWRITE = "You must supply the force switch \"-f\" to overwrite existing output file: "; 
    public static final String ERR_MSG_GET_FIT_CREATED = "Invalid creation time in Fit file: ";
    public static final String ERR_MSG_GET_FIT_SESSION = "Invalid session end time in Fit file: ";
    public static final String ERR_MSG_MANUFACTURER_GARMIN = "Fit file manufacturer does not match Garmin: ";
    public static final String ERR_MSG_MANUFACTURER_ZWIFT = "Fit file manufacturer does not match Zwift: ";
    public static final String ERR_MSG_OUTPUT_FILE_WRITE = "Can not write output file: ";
    public static final String ERR_MSG_OVERWRITE_GARMIN = "Overwriting the Garmin input file is forbidden.";       
    public static final String ERR_MSG_PATH_GARMIN = "You must provide one Garmin input file."; 
    public static final String ERR_MSG_PATH_ZWIFT = "You must provide at least one Zwift input file.";
    public static final String ERR_MSG_STREAM_CLOSE = "Error closing file stream: ";
    public static final String ERR_MSG_UNEXPECTED_EXCEPTION = "Caught an unexpected exception.";
    
    public static final String MSG_MERGE_SUCCESS = "Merge completed successfully."; 
    
    /**
     * The message displayed to the user when they enter --help or /? command line input
     */
    public static final String USAGE_MESSAGE = String.format("\nMerges Zwift file/s location into a Garmin file.\n\n"
            + "garmin-zwift-merge.jar %8$s %2$s %9$s %3$s [%5$s %4$s] [%6$s]\n\n"
            + " %8$s %2$-17s The Garmin Fit file. Requires 1 only Garmin file.\n\n"
            + " %9$s %3$-17s The Zwift Fit file/s. Requires 1 or more Zwift files.\n"
            + " %1$-20s The order of the files is not important. They will be sorted.\n\n"
            + " %5$s %4$-17s Specify the output file name.\n"
            + " %1$-20s Defaults to the name of the first Zwift file with .merged.fit\n\n"
            + " %6$-20s Allows overwrite of existing zwift/output file.\n"
            + " %1$-20s Does not allow overwrite of Garmin file.\n"
            + " %1$-20s Can be combined with %4$s switch eg. %7$s\n\n"
            + " Note that paths must be quoted if they contain spaces.\n"
            + " e.g \"C\\dir name\\valid_path.fit\" C:\\dir\\invalid path.fit\n",
            "", "garmin_file", "zwift_file/s", "output_file", ARG_OUTPUT, ARG_FORCE, ARG_OUTPUT_FORCE, ARG_GARMIN, ARG_ZWIFT);
    //       1        2             3               4             5           6            7               8          9
    
    private boolean _allowOverwrite = false;
    private String _garminFilePath = null;
    private final List<GZMListener> _listeners = new ArrayList<>();
    private String _outputFilePath = null;
    private List<String> _zwiftFilePaths = new ArrayList<>();
    private List<GZMZwiftFile> _zwiftFiles = new ArrayList<>(); 
        
    /**
    * Add a listener for callbacks.
    * 
    * @param listener The listener to add.
    */
    public void addListener(GZMListener listener) {
        _listeners.add(listener);
    }
    
    /**
    * Add a Zwift Fit input file path to the Zwift file list
    * 
    * @param path The path of the input Fit file
    */
    public void addZwiftFilePath(String path) {
        _zwiftFilePaths.add(path);
    }
    
    private void generateOutputPath() {        
        String zwiftPath = _zwiftFiles.get(0).getPath();
        StringBuilder outputPath = new StringBuilder(zwiftPath); 
        outputPath.insert(zwiftPath.length() - 3, "merged.");
        _outputFilePath = outputPath.toString(); 
    }  

    /**
    * Check if allowed to overwrite existing file
    * 
    * @return The allowOverwrite status
    */
    public boolean getAllowOverwrite() {
        return _allowOverwrite;
    } 
    
    /**
    * Get the Garmin Fit input file path
    * 
    * @return The Garmin Fit input file path
    */
    public String getGarminFilePath() {
        return _garminFilePath;
    }
    
    /**
    * Get the Fit output file path
    * 
    * @return The Fit output file path
    */
    public String getOutputFilePath() {
        return _outputFilePath;
    } 
    
    /**
    * Get the list of Zwift Fit input file paths
    * 
    * @return The Zwift Fit input file path list
    */
    public List<String> getZwiftFilePaths() {
        return _zwiftFilePaths;
    } 
    
    /**
    * Get the list of GZMZwiftFile instances
    * 
    * @return The list of GZMZwiftFile instances
    */
    public List<GZMZwiftFile> getZwiftFiles() {
        return _zwiftFiles;
    }
    
    /**
    * Static entry point to allow running from command line
    * 
    * @param args The command line arguments
    */
    public static void main(String[] args) throws GZMRuntimeException { 
        
        if(args.length == 0) return; 

        class Listener implements GZMListener {  
            
            @Override
            public void onError(GZMRuntimeException e) {
                System.err.println(e.getMessage());
            }            
            @Override
            public void onSuccess() {                
                System.out.println(MSG_MERGE_SUCCESS);                
            }
        }         
        
        List<String> argList = new ArrayList<>();
        argList.addAll(Arrays.asList(args)); 
        GarminZwiftMerge app = new GarminZwiftMerge();         
        app.addListener(new Listener());
        
        try {
            while(argList.size() > 0) {            
                switch (argList.get(0)) {
                    
                    case ARG_FORCE :
                        argList.remove(0);
                        app.setAllowOverwrite(true);
                        break;
                        
                    case ARG_OUTPUT_FORCE :
                        app.setAllowOverwrite(true);
                    case ARG_OUTPUT :
                        argList.remove(0);
                        app.setOutputFilePath(argList.remove(0));
                        break; 
                        
                    case ARG_GARMIN:
                        argList.remove(0);
                        app.setGarminFilePath(argList.remove(0));
                        break;
                        
                    case ARG_HELP_UNIX :
                    case ARG_HELP_WIN :
                        System.out.println(USAGE_MESSAGE);
                        return;
                        
                    case ARG_ZWIFT:
                        argList.remove(0);
                    default:
                        app.addZwiftFilePath(argList.remove(0));
                        break;
                }
            }
        }
        catch (IndexOutOfBoundsException e) {
            throw new GZMRuntimeException(ERR_MSG_ARGS_FORMAT);
        }        
        app.merge();
    }
    
    /**
    * Merge the Fit files specified by set path methods
    */
    public void merge() throws GZMRuntimeException {
        try {
            mergeWithExceptions();
            if(_listeners.size() > 0)
                _listeners.forEach(l -> l.onSuccess());
        }
        catch (GZMRuntimeException e) {
            if(_listeners.size() > 0)
                _listeners.forEach(l -> l.onError(e));
            else throw new GZMRuntimeException(e.getMessage());
        }
        // catch unexpected exceptions
        catch (Exception e) {
            GZMRuntimeException unexpectedException = new GZMRuntimeException(ERR_MSG_UNEXPECTED_EXCEPTION, e);
            if(_listeners.size() > 0)
                _listeners.forEach(l -> l.onError((GZMRuntimeException) unexpectedException));
            else throw unexpectedException;
        }
    }
    
    /**
    * Merge the Fit files specified by set path methods
    * 
    * @param allowOverwrite Set to allow overwrite of existing file
    */
    public void merge(boolean allowOverwrite) throws GZMRuntimeException {
        _allowOverwrite = allowOverwrite;
        merge();
    }
    
    private void mergeWithExceptions() {            
        try {
            // check input requirements
            if(_zwiftFilePaths.isEmpty()) throw new GZMRuntimeException(ERR_MSG_PATH_ZWIFT);
            else if(_garminFilePath == null) throw new GZMRuntimeException(ERR_MSG_PATH_GARMIN);
            
            // read Zwift files
            _zwiftFiles = new ArrayList<>();                
            _zwiftFilePaths.forEach(path -> {
                GZMZwiftListener listener = new GZMZwiftListener(path);
                GZMFitFileReader reader = new GZMFitFileReader((MesgListener) listener);                        
                reader.read(path);  
                _zwiftFiles.add(listener.getZwiftFile());
            });

            // sort the files by time created
            Comparator<GZMZwiftFile> compareByTimeCreated = (GZMZwiftFile a, GZMZwiftFile b)
                    -> a.getTimeCreated().compareTo(b.getTimeCreated());                                    
            Collections.sort(_zwiftFiles, compareByTimeCreated);

            // set output path if not specified            
            if(_outputFilePath == null) generateOutputPath();
            
            // else make sure not trying to overwrite Garmin input file
            else if(_outputFilePath.compareToIgnoreCase(_garminFilePath) == 0)
                throw new GZMRuntimeException(ERR_MSG_OVERWRITE_GARMIN);
            
            // throw errors for bad path & bad permissions
            if(!new File(_outputFilePath).exists() || _allowOverwrite) {
                assert(new FileWriter(_outputFilePath) != null);
            }                
            else throw new GZMRuntimeException(ERR_MSG_FORCE_OVERWRITE + _outputFilePath);
            
            // read Garmin file and output merged file
            GZMGarminListener listener = new GZMGarminListener(_zwiftFiles, _outputFilePath);
            GZMFitFileReader reader = new GZMFitFileReader((MesgListener) listener);                        
            reader.read(_garminFilePath);
            listener.close();
        }
        catch (IOException e) {
            throw new GZMRuntimeException(ERR_MSG_OUTPUT_FILE_WRITE + _outputFilePath);
        }        
    }
    
    /**
    * Set if allowed to overwrite existing file
    * 
    * @param allowOverwrite The boolean value to set the flag
    */
    public void setAllowOverwrite(boolean allowOverwrite) {        
        _allowOverwrite = allowOverwrite;
    }
    
    /**
    * Set the Garmin Fit input file path
    * 
    * @param path The path of the Garmin Fit input file
    */
    public void setGarminFilePath(String path) {        
        _garminFilePath = path;
    }
    
    /**
    * Set the Fit file path to write output to
    * 
    * @param path The path of the output Fit file
    */
    public void setOutputFilePath(String path) {        
        _outputFilePath = path;
    }
    
    /**
    * Set the Zwift Fit input file path list
    * 
    * @param pathList The list of Zwift Fit input file paths
    */
    public void setZwiftFilePathList(List<String> pathList) {
        _zwiftFilePaths = pathList;
    }
}
