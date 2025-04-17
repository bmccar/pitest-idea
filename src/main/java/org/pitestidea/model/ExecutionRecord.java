package org.pitestidea.model;

import org.jetbrains.annotations.VisibleForTesting;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.FileOutputStream;
import java.nio.file.FileSystems;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Records information for a specific set of inputs such that can be written to
 * and read from an XML file.
 */
public class ExecutionRecord {
    @VisibleForTesting
    static final int MAX_REPORT_NAME_LENGTH = 24;
    static final int MAX_PREFIX_LENGTH = 3;

    private final static String ROOT_ELEMENT = "execution-record";
    private final static String INPUTS = "inputs";
    private final static String INPUT = "input";

    final List<String> inputFiles;
    final String reportName;
    final String reportDirectoryName;

    public ExecutionRecord(List<String> inputFiles) {
        this.inputFiles = inputFiles;
        this.reportName = generateReportName(inputFiles);
        this.reportDirectoryName = generateReportDirectoryName(inputFiles);
    }

    private static String generateReportName(List<String> virtualFiles) {
        StringBuilder sb = new StringBuilder();
        for (String file : virtualFiles) {
            int from = file.lastIndexOf(FileSystems.getDefault().getSeparator()) + 1;
            int to = file.lastIndexOf('.');
            if (to < 0) {
                to = file.length();
            }
            if (sb.length() + (to - from) > MAX_REPORT_NAME_LENGTH) {
                int charsLeft = MAX_REPORT_NAME_LENGTH - sb.length();
                int end = from + charsLeft;
                if (to > end) {
                    to = end;
                    if (!sb.isEmpty()) {
                        sb.append(',');
                    }
                }

                if (to > from) {
                    sb.append(file, from, to);
                }
                sb.append("...");
                break;
            } else {
                if (!sb.isEmpty()) {
                    sb.append(',');
                }
                sb.append(file, from, to);
            }
        }
        return sb.toString();
    }

    private static String generateReportDirectoryName(List<String> virtualFiles) {
        Object[] names = virtualFiles.toArray();
        String rn = String.valueOf(Math.abs(Objects.hash(names)));
        if (names.length > 0) {
            String pfx = names[0].toString();
            int ix = pfx.lastIndexOf(FileSystems.getDefault().getSeparator());
            if (ix > 0) {
                // Add a prefix just to ease task of looking through files if/when necessary
                pfx = pfx.substring(ix + 1);
            }
            pfx = pfx.substring(0, Math.min(pfx.length(), MAX_PREFIX_LENGTH));
            rn = pfx + rn;
        }
        return rn;
    }

    /**
     * Writes this class as an XML file so it can later be read if needed.
     *
     * @param dir fully-qualified path unique to this instance
     */
    public void writeToDirectory(String dir) {
        System.out.println("Writing report to " + dir);

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            appendContent(doc);
            writeOutput(doc, metaFileInDir(dir));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String metaFileInDir(String dir) {
        return dir + "/run.xml";
    }

    private void appendContent(Document doc) {
        Element rootElement = doc.createElement(ROOT_ELEMENT);
        doc.appendChild(rootElement);

        Element inputs = doc.createElement(INPUTS);
        rootElement.appendChild(inputs);

        for (String input : inputFiles) {
            Element inputElement = doc.createElement(INPUT);
            inputElement.appendChild(doc.createTextNode(input));
            inputs.appendChild(inputElement);
        }
    }

    private void writeOutput(Document doc, String dir) throws Exception {
        try (FileOutputStream output = new FileOutputStream(dir)) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(output);
            transformer.transform(source, result);
        }
    }

    public List<String> getInputFiles() {
        return inputFiles;
    }

    public String getReportName() {
        return reportName;
    }

    public String getReportDirectoryName() {
        return reportDirectoryName;
    }
}
