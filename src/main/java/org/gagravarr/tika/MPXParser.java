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
import net.sf.mpxj.mpx.MPXReader;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.parser.AbstractParser;
import org.apache.tika.parser.ParseContext;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Tika Parser for Microsoft Project MPX files (Text based)
 */
public class MPXParser extends AbstractParser {
    private static final long serialVersionUID = -4791025107910605527L;

    private static List<MediaType> TYPES = Arrays.asList(new MediaType[] {
            MediaType.application("x-project")
    });

    public Set<MediaType> getSupportedTypes(ParseContext context) {
        return new HashSet<MediaType>(TYPES);
    }

    public void parse(InputStream stream, ContentHandler handler,
            Metadata metadata, ParseContext context)
            throws IOException, TikaException, SAXException {
        MPXReader reader = new MPXReader();
        ProjectFile project = null;

        try {
            project = reader.read(stream);
        } catch(MPXJException e) {
            throw new TikaException("Error reading MPX file", e);
        }

        // Extract helpful information out
        ProjectFileProcessor.parse(project, handler, metadata, context);
    }
}
