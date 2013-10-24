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

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.mpxj.MPXJException;
import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.mpp.MPPReader;

import org.apache.poi.poifs.filesystem.DirectoryNode;
import org.apache.poi.poifs.filesystem.NPOIFSFileSystem;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.tika.exception.TikaException;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.microsoft.SummaryExtractor;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Tika Parser for Microsoft Project MPP files (OLE2 based)
 */
public class MPPParser extends AbstractParser {
    private static final long serialVersionUID = 5830815858263640677L;
    
    private static List<MediaType> TYPES = Arrays.asList(new MediaType[] {
            MediaType.application("vnd.ms-project")
    });

    public Set<MediaType> getSupportedTypes(ParseContext context) {
        return new HashSet<MediaType>(TYPES);
    }

    public void parse(InputStream stream, ContentHandler handler,
            Metadata metadata, ParseContext context)
            throws IOException, TikaException, SAXException {
        // Open the OLE2 Container, re-using containers or files if available
        TikaInputStream tstream = TikaInputStream.cast(stream);
        DirectoryNode root = null;
        try {
            if (tstream != null) {
                Object container = tstream.getOpenContainer();
                if (container != null && container instanceof POIFSFileSystem) {
                    root = ((POIFSFileSystem)container).getRoot();
                } else if (container != null && container instanceof NPOIFSFileSystem) {
                    root = ((NPOIFSFileSystem)container).getRoot();
                } else if (tstream.hasFile()) {
                    root = new NPOIFSFileSystem(tstream.getFile()).getRoot();
                }
            }
            if (root == null) {
                root = new NPOIFSFileSystem(stream).getRoot();
            }
        } catch(IOException e) {
            throw new TikaException("Error reading MPP file", e);
        }
        
        // Open the MPP Resource
        MPPReader reader = new MPPReader();
        ProjectFile project = null;
        try {
            project = reader.read(root);
        } catch(MPXJException e) {
            throw new TikaException("Error reading MPP file", e);
        }
        
        // Handle the Metadata in the OLE2 Container
        new SummaryExtractor(metadata).parseSummaries(root);

        // Extract helpful information out
        ProjectFileProcessor.parse(project, handler, metadata, context);
    }
}
