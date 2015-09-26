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

import static org.apache.tika.utils.DateUtils.formatDate;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.mpxj.ChildTaskContainer;
import net.sf.mpxj.Duration;
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

       // Start our document
       XHTMLContentHandler xhtml =
               new XHTMLContentHandler(handler, metadata);
       xhtml.startDocument();

       // Walk the tree of tasks, printing them out
       xhtml.element("h2", "Tasks");
       handleChildTasks(project, xhtml, usedResources);

       // Find any un-used resources
       // TODO Should we display all resources here instead, with notes etc?
       Set<Resource> spareResources = new HashSet<Resource>();
       for (Resource resource : project.getAllResources()) {
           if (! usedResources.contains(resource.getID())) {
               if (resource.getID() == 0 && resource.getName() == null) {
                   // Special case, skip this
               } else {
                   spareResources.add(resource);
               }
           }
       }

       // Print out any spare resources at the end
       if (! spareResources.isEmpty()) {
           xhtml.element("h2", "Un-Used Resources");
           xhtml.startElement("ul");
           for (Resource resource : spareResources) {
               String name = buildName("Resource", resource.getName(), resource.getID());
               xhtml.element("li", name);
           }
           xhtml.endElement("ul");
       }

       // Mark this as completed
       xhtml.endDocument();
    }
    
    protected static void handleChildTasks(ChildTaskContainer parentTask, XHTMLContentHandler xhtml,
            Set<Integer> usedResources) throws SAXException {
        List<Task> tasks = parentTask.getChildTasks();
        if (tasks != null && ! tasks.isEmpty()) {
            xhtml.startElement("ol");

            for (Task task : tasks) {
                xhtml.startElement("li", "id", task.getID().toString());

                // Firstly, output the task details
                xhtml.startElement("div", "class", "task");
                handleTask(task, xhtml, usedResources);
                xhtml.endElement("div");

                // Then recurse into children (if any)
                handleChildTasks(task, xhtml, usedResources);

                xhtml.endElement("li");
            }

            xhtml.endElement("ol");
        }
    }

    protected static void handleTask(Task task, XHTMLContentHandler xhtml, 
            Set<Integer> usedResources) throws SAXException {
        // Name
        String name = buildName("Task", task.getName(), task.getID());
        xhtml.startElement("div", "class", "name");
        xhtml.element("b", name);
        xhtml.endElement("div");

        // Notes
        if (task.getNotes() != null) {
            xhtml.startElement("div", "class", "notes");
            xhtml.characters(task.getNotes());
            xhtml.endElement("div");
        }

        // Dates
        handleDates("Planned", task.getStart(), task.getFinish(), task.getDuration(), xhtml);
        handleDates("Actual", task.getActualStart(), task.getActualFinish(), task.getActualDuration(), xhtml);
        handleDates("Baseline", task.getBaselineStart(), task.getBaselineFinish(), task.getBaselineDuration(), xhtml);
        handleDates("Earliest", task.getEarlyStart(), task.getEarlyFinish(), null, xhtml);
        handleDates("Latest", task.getLateStart(), task.getLateFinish(), null, xhtml);

        // TODO Further information

        // Resources
        for (ResourceAssignment ra : task.getResourceAssignments()) {
            Resource resource = ra.getResource();
            if (resource != null) {
                usedResources.add(resource.getID());

                xhtml.startElement("div", "class", "resource");
                xhtml.element("i", buildName("Resource", resource.getName(), resource.getID()));

                // TODO What about Notes on the resource itself?
                if (ra.getNotes() != null) {
                    xhtml.startElement("div", "class", "notes");
                    xhtml.characters(ra.getNotes());
                    xhtml.endElement("div");
                }

                xhtml.endElement("div");
            }
        }
    }
    
    /**
     * Render a date range
     */
    protected static void handleDates(String what, Date start, Date finish,
            Duration duration, XHTMLContentHandler xhtml) throws SAXException {
        if (start == null && finish == null) {
            // Assume there's nothing there, and skip
            return;
        }

        String cls = what.toLowerCase() + "Dates";

        xhtml.startElement("div", "class", "fromTo " + cls);
        xhtml.characters(what);
        xhtml.characters(" from ");
        xhtml.characters(buildDate(start));
        xhtml.characters(" to ");
        xhtml.characters(buildDate(finish));

        if (duration != null) {
            xhtml.characters(" taking ");
            xhtml.characters(buildDuration(duration));
        }

        xhtml.endElement("div");
    }

    // TODO Handler for Hyperlinks on things

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
    protected static String buildDuration(Duration duration) {
        StringBuffer sb = new StringBuffer();

        int dInt = (int)duration.getDuration();
        if (duration.getDuration() == (double)dInt) {
            sb.append(dInt);
        } else {
            sb.append(duration.getDuration());
        }

        sb.append(' ');

        switch(duration.getUnits()) {
          case MINUTES:
            sb.append("Minute");
            break;
          case HOURS:
              sb.append("Hour");
              break;
          case DAYS:
              sb.append("Day");
              break;
          case WEEKS:
              sb.append("Week");
              break;
          default:
              sb.append(duration.getUnits().getName());
        }
        if (duration.getDuration() != 1.0) {
            sb.append('s');
        }

        return sb.toString();
    }
}
