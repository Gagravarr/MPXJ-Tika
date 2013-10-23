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

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.mpp.MPPReader;
import net.sf.mpxj.mpx.MPXWriter;
import net.sf.mpxj.reader.ProjectReader;
import net.sf.mpxj.writer.ProjectWriter;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.xml.sax.ContentHandler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

@RunWith(JUnit4.class)
public class TestMPPParser {
    @Test
    @Ignore
    public void DummyTest() {
        assertEquals(true, true);
        fail("Need to implement this");
    }
    
    @Test
    public void BasicProcessing() throws Exception {
        InputStream mpp2003 = getTestFile("testPROJECT2003.mpp");
        InputStream mpp2007 = getTestFile("testPROJECT2007.mpp");
        
        MPPParser parser = new MPPParser();
        ContentHandler handler = new BodyContentHandler(); 
        
        // Call Tika on the 2003 file
        parser.parse(mpp2003, handler, new Metadata(), new ParseContext());
        
        // Call Tika on the 2007 file
        parser.parse(mpp2007, handler, new Metadata(), new ParseContext());
    }
    
    @Test
    public void AutoDetectParsing() throws Exception {
        // TODO
    }
    
    protected static InputStream getTestFile(String name) throws Exception {
        InputStream s = TestMPPParser.class.getResourceAsStream("/test-files/" + name);
        assertNotNull("Test file not found: " + name, s);
        return s;
    }
}
