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
import java.util.Date;
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
import static org.apache.tika.utils.DateUtils.formatDate;
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

       // Start our document
       XHTMLContentHandler xhtml =
               new XHTMLContentHandler(handler, metadata);
       xhtml.startDocument();

       // Walk the tree of tasks, printing them out
       xhtml.element("h2", "Tasks");
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
            xhtml.startElement("li", "id", task.getID().toString());
            xhtml.startElement("div", "class", "task");

            // Name
            String name = buildName("Task", task.getName(), task.getID());
            xhtml.startElement("div", "class", "name");
            xhtml.element("b", name);
            xhtml.endElement("div");

            // Dates
            xhtml.startElement("div", "class", "fromTo");
            xhtml.characters("From ");
            xhtml.characters(buildDate(task.getStart()));
            xhtml.characters(" to ");
            xhtml.characters(buildDate(task.getFinish()));
            xhtml.endElement("div");

            // TODO Further information and further dates

            // Resources
            for (ResourceAssignment ra : task.getResourceAssignments()) {
                Resource resource = ra.getResource();
                if (resource != null) {
                    usedResources.add(resource.getID());

                    xhtml.startElement("div", "class", "resource");
                    xhtml.element("i", buildName("Resource", resource.getName(), resource.getID()));
                    xhtml.endElement("div");
                }
            }

            xhtml.endElement("div");

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
    protected static String buildDate(Date when) {
        if (when == null) {
            return "(unknown)";
        }
        return formatDate(when);
    }
}
