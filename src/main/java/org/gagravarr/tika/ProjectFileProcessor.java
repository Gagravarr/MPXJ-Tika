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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.mpxj.ProjectFile;
import net.sf.mpxj.Resource;
import net.sf.mpxj.ResourceAssignment;
import net.sf.mpxj.Task;

import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.XHTMLContentHandler;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Turns Microsoft Project files into Tika Metadata + XHTML
 */
public class ProjectFileProcessor {
    public static void parse(ProjectFile project, ContentHandler handler,
            Metadata metadata, ParseContext context)
            throws IOException, TikaException, SAXException {
       // To keep track of the resources that people claim
       Set<Integer> usedResources = new HashSet<Integer>();
       
       // Walk the tree of tasks, printing them out
       XHTMLContentHandler xhtml =
               new XHTMLContentHandler(handler, metadata);
       xhtml.startDocument();
       handleTasks(project.getChildTasks(), xhtml, usedResources);

       // Print out any spare resources at the end
       xhtml.element("h2", "Un-Used Resources");
       xhtml.startElement("ul");
       boolean hasUnUsed = false;
       for (Resource resource : project.getAllResources()) {
           if (! usedResources.contains(resource.getID())) {
               hasUnUsed = true;
               String name = buildName("Resource", resource.getName(), resource.getID());
               xhtml.element("li", name);
           }
       }
       if (! hasUnUsed) {
           xhtml.startElement("li");
           xhtml.element("i", "None");
           xhtml.endElement("li");
       }
       xhtml.endElement("ul");
       
       // Mark this as completed
       xhtml.endDocument();
    }
    
    protected static void handleTasks(List<Task> tasks, XHTMLContentHandler xhtml, 
            Set<Integer> usedResources) throws SAXException
    {
        xhtml.startElement("ol");
        for (Task task : tasks) {
            String name = buildName("Task", task.getName(), task.getID());
            
            xhtml.startElement("li", "id", task.getID().toString());
            
            xhtml.element("b", name);
            // TODO Dates

            // Do Resources
            for (ResourceAssignment ra : task.getResourceAssignments()) {
                Resource resource = ra.getResource();
                if (resource != null) {
                    // TODO Do this better
                    usedResources.add(resource.getID());
                    xhtml.element("i", resource.getName());
                }
            }
            
            // Do Child Tasks
            List<Task> childTasks = task.getChildTasks();
            if (childTasks != null && !childTasks.isEmpty()) {
                handleTasks(childTasks, xhtml, usedResources);
            }
            
            // Task complete
            xhtml.endElement("li");
        }
        xhtml.endElement("ol");
    }
    
    protected static String buildName(String what, String name, Integer id) {
        if (name != null) {
            return name;
        }
        return "(" + what + " with ID " + id + ")";
    }
}
