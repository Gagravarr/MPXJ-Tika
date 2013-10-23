/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gagravarr.tika;

import java.io.File;
import java.io.InputStream;
import java.util.Set;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.mpx.MPXWriter;
import net.sf.mpxj.reader.ProjectReader;
import net.sf.mpxj.writer.ProjectWriter;

import org.apache.tika.config.TikaConfig;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.xml.sax.ContentHandler;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public class TestProjectParsers {
    public static MediaType MPP_TYPE = MediaType.application("vnd.ms-project");
    public static MediaType MPX_TYPE = MediaType.application("x-project");
    
    @Test
    @Ignore
    public void DummyTest() {
        assertEquals(true, true);
        fail("Need to implement this");
    }
    
    @Test
    public void BasicProcessing() throws Exception {
        InputStream mpx = getTestFile("testPROJECT.mpx");
        InputStream mpp2003 = getTestFile("testPROJECT2003.mpp");
        InputStream mpp2007 = getTestFile("testPROJECT2007.mpp");
        
        MPPParser mppParser = new MPPParser();
        MPXParser mpxParser = new MPXParser();
        ContentHandler handler = new BodyContentHandler(); 
        
        // Call Tika on the mpx file
        mpxParser.parse(mpx, handler, new Metadata(), new ParseContext());
        
        // Call Tika on the 2003 file
        mppParser.parse(mpp2003, handler, new Metadata(), new ParseContext());
        
        // Call Tika on the 2007 file
        mppParser.parse(mpp2007, handler, new Metadata(), new ParseContext());
    }
    
    @Test
    public void AutoDetectParsing() throws Exception {
        Set<MediaType> types;
        
        // Check the parsers claims to support the right things
        MPPParser mppParser = new MPPParser();
        types = mppParser.getSupportedTypes(new ParseContext());
        assertTrue("Not found in " + types, types.contains(MPP_TYPE));
        assertFalse("Shouldn't be in " + types, types.contains(MPX_TYPE));
        
        MPXParser mpxParser = new MPXParser();
        types = mpxParser.getSupportedTypes(new ParseContext());
        assertTrue("Not found in " + types, types.contains(MPX_TYPE));
        assertFalse("Shouldn't be in " + types, types.contains(MPP_TYPE));
        
        // Check that the Auto Detect parser claims to support them both
        Parser autoDetectParser = TikaConfig.getDefaultConfig().getParser();
        types = autoDetectParser.getSupportedTypes(new ParseContext());
        assertTrue("Not found in " + types, types.contains(MPP_TYPE));
        assertTrue("Not found in " + types, types.contains(MPX_TYPE));

        
        // Check the parsing works
        Metadata metadata = new Metadata();
        ContentHandler handler = new BodyContentHandler(); 
        
        
        // Ask for a MPP to be processed
        InputStream mpp2003 = getTestFile("testPROJECT2003.mpp");
        autoDetectParser.parse(mpp2003, handler, metadata, new ParseContext());

        // Check it worked
        // TODO
        
        
        // Ask for a MPX to be processed
        InputStream mpx = getTestFile("testPROJECT.mpx");
        autoDetectParser.parse(mpx, handler, metadata, new ParseContext());

        // Check it worked
        // TODO
    }
    
    protected static InputStream getTestFile(String name) throws Exception {
        InputStream s = TestProjectParsers.class.getResourceAsStream("/test-files/" + name);
        assertNotNull("Test file not found: " + name, s);
        return s;
    }
}
