package org.pitestidea.reader;

import com.intellij.openapi.project.Project;
import org.pitestidea.model.CoverageImpact;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

public class CoverageFileReader {
    /**
     * Reads and parses mutation lines from the file generated from pitest, and sends each
     * line individually to a recorder.
     *
     * @param file to read and parse
     * @param className in file
     * @param recorder to send results to
     */
    public static void read(Project project, File file, ICoverageRecorder recorder) {
        try {
            readFull(project, file, recorder);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    static void readFull(Project project, File file, ICoverageRecorder recorder) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.parse(file);

        NodeList nodeList = document.getElementsByTagName("mutation");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element)nodeList.item(i);
            int lineNumber = Integer.parseInt(node.getElementsByTagName("lineNumber").item(0).getTextContent());
            String description = node.getElementsByTagName("description").item(0).getTextContent();
            CoverageImpact impact = CoverageImpact.valueOf(node.getAttribute("status"));
            String sourceFile = node.getElementsByTagName("sourceFile").item(0).getTextContent();
            //String className = IdeaDiscovery.findVirtualFileByFQN(project,sourceFile
            String className = node.getElementsByTagName("mutatedClass").item(0).getTextContent();
            recorder.record(className, impact,lineNumber,description);
        }
    }
}
