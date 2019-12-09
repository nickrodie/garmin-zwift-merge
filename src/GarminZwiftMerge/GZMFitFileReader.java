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

import com.garmin.fit.Decode;
import com.garmin.fit.MesgListener;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

/**
 * Helper class to verify and read a Fit file.
 * 
 * @author Nicholas Rodie
 */
public class GZMFitFileReader {
    
    private final MesgListener _listener;

    /**
    * Fit file read helper class constructor.
    * 
    * @param listener A Fit message listener
    */
    public GZMFitFileReader(MesgListener listener) {
        _listener = listener;
    }
    
    /**
    * Fit file read helper method 
    * 
    * @param path The path of the Fit file to read
    */
    public void read(String path) {
              
        try (InputStream stream = new FileInputStream(path);
                FileChannel channel = ((FileInputStream) stream).getChannel()) {
                
            Decode decode = new Decode(); 
            // verify Fit file integrity
            if(!decode.checkFileIntegrity((InputStream) stream))
                throw new GZMRuntimeException(GarminZwiftMerge.ERR_MSG_FIT_FILE_INTEGRITY + path);

            // reset stream
            channel.position(0);
            decode.nextFile();

            // read with listener
            decode.addListener(_listener);
            decode.read(stream);
        }
        catch (FileNotFoundException e) {
            throw new GZMRuntimeException(GarminZwiftMerge.ERR_MSG_FILE_NOT_FOUND + path);
        }
        catch (IOException e) {
            throw new GZMRuntimeException(GarminZwiftMerge.ERR_MSG_STREAM_CLOSE + path);
        }
    }    
}