package org.pitestidea.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Records summary information for a specific for a run of PIT. Can be read/written to an XML file.
 */
public final class ExecutionRecord implements Comparable<ExecutionRecord> {
    public static final String META_FILE_NAME = "run.xml";

    private final static String ROOT_ELEMENT = "execution-record";
    private final static String INPUTS = "inputs";
    private final static String INPUT = "input";
    private final static String START_TIME = "start-time";
    private final static String DURATION = "duration";

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d");

    final @NotNull InputBundle inputBundle;
    final @NotNull String reportName;
    final @Nullable String reportDirectoryName;
    private long startedAt;
    private long durationMillis;

    public static class InvalidFile extends RuntimeException {
        public InvalidFile(String message) {
            super(message);
        }
    }

    /**
     * Creates an ExecutionRecord for externally generated files when the inputs are not known.
     *
     * @param startedAt last-modified-time from the external file
     */
    public ExecutionRecord(long startedAt) {
        inputBundle = new InputBundle();
        reportName = "pit command line";
        reportDirectoryName = null;
        this.startedAt = startedAt;
    }

    /**
     * Creates an ExecutionRecord for the provided input set.
     *
     * @param inputBundle that should already have been set in a consistent order
     */
    public ExecutionRecord(@NotNull InputBundle inputBundle) {
        this.inputBundle = inputBundle;
        this.reportName = inputBundle.generateReportName();
        this.reportDirectoryName = inputBundle.generateReportDirectoryName();
        this.startedAt = System.currentTimeMillis();
    }

    /**
     * Creates an ExecutionRecord from a report previously generated from this plugin, so that
     * report directory is expected to have a {@link #META_FILE_NAME} file written from the
     * methods in this file.
     *
     * @param reportDir previously generated directory
     */
    public ExecutionRecord(File reportDir) {
        this.inputBundle = new InputBundle();
        File[] metaFiles = reportDir.listFiles((dir, name) -> META_FILE_NAME.equals(name));
        if (metaFiles == null || metaFiles.length == 0) {
            throw new InvalidFile("No meta file present for " + reportDir);
        } else {
            try (InputStream inputStream = new FileInputStream(metaFiles[0])) {
                readFull(inputStream, reportDir);
            } catch (ParserConfigurationException | IOException | SAXException e) {
                throw new RuntimeException(e);
            }
            this.reportName = inputBundle.generateReportName();
            this.reportDirectoryName = inputBundle.generateReportDirectoryName();
        }
    }

    public void markFinished() {
        this.durationMillis = System.currentTimeMillis() - startedAt;
    }

    public boolean isRunnable() {
        return inputBundle.isRunnable();
    }

    public long getStartedAt() {
        return startedAt;
    }

    public String getFormattedStart() {
        if (startedAt == 0) {
            return "";
        } else {
            ZonedDateTime runStart = Instant.ofEpochMilli(startedAt).atZone(ZoneId.systemDefault());
            ZonedDateTime midnight = LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault());
            if (runStart.isBefore(midnight)) {
                return dateFormatter.format(runStart);
            } else {
                return timeFormatter.format(runStart);
            }
        }
    }

    public String getFormattedDuration() {
        if (durationMillis == 0) {
            return "";
        } else {
            long hours = TimeUnit.MILLISECONDS.toHours(durationMillis);
            long minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) - TimeUnit.HOURS.toMinutes(hours);
            long seconds = TimeUnit.MILLISECONDS.toSeconds(durationMillis) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(durationMillis));
            if (hours > 0) {
                return String.format("%dh:%dm:%ds", hours, minutes, seconds);
            } else if (minutes > 0) {
                return String.format("%dm:%ds", minutes, seconds);
            } else {
                return String.format("%dsec", seconds);
            }
        }
    }

    /**
     * Writes this class as an XML file so that it can later be read if needed.
     *
     * @param reportDir fully-qualified path unique to this instance
     */
    public void writeToDirectory(File reportDir) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            appendContent(doc);
            writeOutput(doc, new File(reportDir, META_FILE_NAME));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void appendContent(Document doc) {
        Element rootElement = doc.createElement(ROOT_ELEMENT);
        doc.appendChild(rootElement);

        Element inputs = doc.createElement(INPUTS);
        rootElement.appendChild(inputs);

        for (InputBundle.Category category : InputBundle.Category.values()) {
            Element cat = doc.createElement(category.getSerializableName());
            for (String next : inputBundle.asPath().get(c -> c == category)) {
                Element nextElement = doc.createElement(INPUT);
                nextElement.appendChild(doc.createTextNode(next));
                cat.appendChild(nextElement);
            }
            inputs.appendChild(cat);
        }

        Element startTime = doc.createElement(START_TIME);
        startTime.setTextContent(Long.toString(startedAt));
        rootElement.appendChild(startTime);

        Element duration = doc.createElement(DURATION);
        duration.setTextContent(Long.toString(durationMillis));
        rootElement.appendChild(duration);
    }

    private void writeOutput(Document doc, File reportDir) throws Exception {
        try (@NotNull OutputStream output = new FileOutputStream(reportDir)) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            DOMSource source = new DOMSource(doc);
            StreamResult result = new StreamResult(output);
            transformer.transform(source, result);
        }
    }

    private void readFull(InputStream inputStream, File dirName) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.parse(inputStream);

        Element root = document.getDocumentElement();
        NodeList allInputs = root.getElementsByTagName(INPUTS);
        if (allInputs.getLength() == 0) {
            throw new InvalidFile("PIT-idea meta-file corrupted and should be removed: " + dirName.getAbsolutePath());
        }
        for (InputBundle.Category category : InputBundle.Category.values()) {
            NodeList cat = root.getElementsByTagName(category.getSerializableName());
            for (int i = 0; i < cat.getLength(); i++) {
                Element node = (Element) cat.item(i);
                NodeList inputsList = node.getChildNodes();
                for (int j = 0; j < inputsList.getLength(); j++) {
                    Element inputNode = (Element) inputsList.item(j);
                    inputBundle.addPath(category, inputNode.getTextContent());
                }
            }
        }

        NodeList startTime = root.getElementsByTagName(START_TIME);
        this.startedAt = Long.parseLong(startTime.item(0).getTextContent());

        NodeList duration = root.getElementsByTagName(DURATION);
        this.durationMillis = Long.parseLong(duration.item(0).getTextContent());
    }

    public @NotNull InputBundle getInputBundle() {
        return inputBundle;
    }

    public @NotNull String getReportName() {
        return reportName;
    }

    public @Nullable String getReportDirectoryName() {
        return reportDirectoryName;
    }

    public String getHtmlListOfInputs() {
        StringBuilder sb = new StringBuilder();
        inputBundle.appendHtmlListOfInputs(sb);
        return sb.toString();
    }


    @Override
    public String toString() {
        return "xr(" + reportName + ")@" + hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof ExecutionRecord that) {
            // Only consider input files so that new ExecutionRecords can match and replace old ones
            // when based on the same set of input records.
            return this.inputBundle.equals(that.inputBundle);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputBundle);
    }

    @Override
    public int compareTo(@NotNull ExecutionRecord that) {
        return inputBundle.compareTo(that.getInputBundle());
    }
}
