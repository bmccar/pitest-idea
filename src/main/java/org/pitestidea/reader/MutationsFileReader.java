package org.pitestidea.reader;

import com.intellij.openapi.diagnostic.Logger;
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
import java.util.HashSet;
import java.util.Set;

/**
 * Reads files output from PIT and generates mutations line-by-line.
 */
public class MutationsFileReader {
    private static final Logger LOGGER = Logger.getInstance(MutationsFileReader.class);

    /**
     * Reads and parses mutation lines from the file generated from pitest and sends each
     * line individually to a recorder.
     *
     * @param project  context
     * @param file     to read and parse
     * @param recorder to send results to
     */
    public static void read(Project project, File file, IMutationsRecorder recorder) throws InvalidMutatedFileException {
        try {
            readFull(project, file, recorder);
        } catch (InvalidMutatedFileException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidMutatedFileException(e);
        }
    }

    private static void readFull(Project project, File file, IMutationsRecorder recorder) throws ParserConfigurationException, IOException, SAXException, InvalidMutatedFileException {
        record Bad(String file, String reportPath) {
        }
        Set<Bad> badFiles = new HashSet<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.parse(file);

        NodeList nodeList = document.getElementsByTagName("mutation");
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element node = (Element) nodeList.item(i);
            int lineNumber = Integer.parseInt(node.getElementsByTagName("lineNumber").item(0).getTextContent());
            String description = node.getElementsByTagName("description").item(0).getTextContent();
            MutationImpact impact = MutationImpact.valueOf(node.getAttribute("status"));
            String sourceFile = node.getElementsByTagName("sourceFile").item(0).getTextContent();
            String filePath = node.getElementsByTagName("mutatedClass").item(0).getTextContent();
            String method = node.getElementsByTagName("mutatedMethod").item(0).getTextContent();

            int ix = filePath.lastIndexOf('.');
            final String pkg;
            if (ix < 0) {
                pkg = "";
                filePath = sourceFile;
            } else {
                pkg = filePath.substring(0, ix);
                filePath = pkg.replace('.', '/') + '/' + sourceFile;
            }
            VirtualFile virtualFile = findFromPath(project, filePath);
            if (virtualFile == null) {
                badFiles.add(new Bad(filePath, file.getParent()));
            } else {
                recorder.record(pkg, virtualFile, method, impact, lineNumber, description);
            }
        }
        recorder.postProcess();
        if (!badFiles.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("The following PIT reports contained non-existent files that will be ignored:\n");
            for (Bad badFile : badFiles) {
                sb.append("  ");
                sb.append(badFile.reportPath);
                sb.append(" referenced ").append(badFile.file).append("\n");
            }
            sb.append("Those reports can be removed");
            LOGGER.warn(sb.toString());
            throw new InvalidMutatedFileException("Number of bad files: " + badFiles.size());
        }
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
