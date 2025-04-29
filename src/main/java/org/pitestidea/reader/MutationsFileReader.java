package org.pitestidea.reader;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.pitestidea.model.MutationImpact;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;

/**
 * Reads files output from PIT and generates mutations line-by-line.
 */
public class MutationsFileReader {
    /**
     * Reads and parses mutation lines from the file generated from pitest, and sends each
     * line individually to a recorder.
     *
     * @param project context
     * @param file to read and parse
     * @param recorder to send results to
     */
    public static void read(Project project, File file, IMutationsRecorder recorder) {
        try {
            readFull(project, file, recorder);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            throw new RuntimeException(e);
        }
    }

    private static void readFull(Project project, File file, IMutationsRecorder recorder) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.parse(file);

        NodeList nodeList = document.getElementsByTagName("mutation");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element)nodeList.item(i);
            int lineNumber = Integer.parseInt(node.getElementsByTagName("lineNumber").item(0).getTextContent());
            String description = node.getElementsByTagName("description").item(0).getTextContent();
            MutationImpact impact = MutationImpact.valueOf(node.getAttribute("status"));
            String sourceFile = node.getElementsByTagName("sourceFile").item(0).getTextContent();
            String filePath = node.getElementsByTagName("mutatedClass").item(0).getTextContent();
            int ix = filePath.lastIndexOf('.');
            if (ix > 0) {
                filePath = filePath.substring(0,ix);
            }

            String pkg = filePath;

            String pfx = "";
            if (filePath.indexOf('.') > 0) {
                pfx = filePath.replace('.','/');
            }
            filePath = pfx + '/' + sourceFile;

            VirtualFile virtualFile = findFromPath(project,filePath);
            if (virtualFile == null) {
                throw new RuntimeException("Unable to find file " + filePath);
            }

            recorder.record(pkg, virtualFile, impact,lineNumber,description);
        }
        recorder.postProcess();
    }

    private static VirtualFile findFromPath(Project project, String filePath) {
        VirtualFile[] projectSourceRoots = ProjectRootManager.getInstance(project).getContentSourceRoots();
        for (VirtualFile sourceRoot : projectSourceRoots) {
            VirtualFile file = sourceRoot.findFileByRelativePath(filePath);
            if (file != null) {
                return file;
            }
        }
        return null;
    }
}
