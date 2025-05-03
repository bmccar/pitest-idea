package org.pitestidea.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;
import org.pitestidea.actions.PITestRunProfile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.*;
import java.nio.file.FileSystems;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
public class ExecutionRecord implements Comparable<ExecutionRecord> {
    public static final String META_FILE_NAME = "run.xml";

    @VisibleForTesting
    static final int MAX_REPORT_NAME_LENGTH = 24;
    static final int MAX_PREFIX_LENGTH = 3;

    private final static String ROOT_ELEMENT = "execution-record";
    private final static String INPUTS = "inputs";
    private final static String INPUT = "input";
    private final static String START_TIME = "start-time";
    private final static String DURATION = "duration";

    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM d");

    final List<String> inputFiles;
    final String reportName;
    final String reportDirectoryName;
    private long startedAt;
    private long durationMillis;

    public static class InvalidFile extends RuntimeException {};

    public ExecutionRecord(long startedAt) {
        inputFiles = Collections.emptyList();
        reportName = "pit command line";
        reportDirectoryName = null;
        this.startedAt = startedAt;
    }

    public ExecutionRecord(List<String> inputFiles) {
        this.inputFiles = inputFiles;
        this.reportName = generateReportName(inputFiles);
        this.reportDirectoryName = generateReportDirectoryName(inputFiles);
        this.startedAt = System.currentTimeMillis();
    }

    public ExecutionRecord(File reportDir) {
        this.inputFiles = new ArrayList<>();
        File[] metaFiles = reportDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return META_FILE_NAME.equals(name);
            }
        });
        if (metaFiles == null || metaFiles.length == 0) {
            throw new InvalidFile();
        } else {
            try (InputStream inputStream = new FileInputStream(metaFiles[0])) {
                readFull(inputStream);
            } catch (ParserConfigurationException | IOException | SAXException e) {
                throw new RuntimeException(e);
            }
            this.reportName = generateReportName(inputFiles);
            this.reportDirectoryName = generateReportDirectoryName(inputFiles);
        }
    }

    public void markFinished() {
        this.durationMillis = System.currentTimeMillis() - startedAt;
    }

    /**
     * Can only initiate a PIT run if the input files are known.
     *
     * @return true iff this record can be run
     */
    public boolean isRunnable() {
        return !inputFiles.isEmpty();
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
     * @param reportDir fully-qualified path unique to this instance
     */
    public void writeToDirectory(File reportDir) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document doc = docBuilder.newDocument();
            appendContent(doc);
            writeOutput(doc, new File(reportDir,META_FILE_NAME));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

    private void readFull(InputStream inputStream) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.parse(inputStream);

        Element root = document.getDocumentElement(); // .normalize();
        NodeList inputsList = root.getElementsByTagName(INPUTS);
        for (int i = 0; i < inputsList.getLength(); i++) {
            Element node = (Element)inputsList.item(i);
            String input = node.getElementsByTagName(INPUT).item(0).getTextContent();
            inputFiles.add(input);
        }

        NodeList startTime = root.getElementsByTagName(START_TIME);
        this.startedAt = Long.parseLong(startTime.item(0).getTextContent());

        NodeList duration = root.getElementsByTagName(DURATION);
        this.durationMillis = Long.parseLong(duration.item(0).getTextContent());
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

    public String getHtmlListOfInputs(String msg, boolean forceMultiline) {
        if (inputFiles.isEmpty()) {
            return reportName;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(msg);
        List<String> inputFiles = getInputFiles();
        inputFiles = inputFiles.stream().map(PITestRunProfile::simpleNameOfPath).toList();
        if (inputFiles.size()==1 && !forceMultiline) {
            sb.append(' ');
            sb.append(inputFiles.get(0));
        } else {
            sb.append(":<br><ul>");
            for (String inputFile: inputFiles) {
                sb.append("<li>");
                sb.append(inputFile);
                sb.append("</li>");
            }
            sb.append("</ul>");
        }
        return sb.toString();
    }


    @Override
    public String toString() {
        return "xr(" + reportName + ")@" + hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ExecutionRecord that = (ExecutionRecord) o;
        return Objects.equals(inputFiles, that.inputFiles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(inputFiles);
    }

    @Override
    public int compareTo(@NotNull ExecutionRecord that) {
        int sz = this.inputFiles.size();
        int sizeComparison = Integer.compare(sz, that.inputFiles.size());
        if (sizeComparison != 0) {
            return sizeComparison;
        }
        for (int i = 0; i < sz; i++) {
            int elementComparison = this.inputFiles.get(i).compareTo(that.inputFiles.get(i));
            if (elementComparison != 0) {
                return elementComparison;
            }
        }
        return 0;
    }
}
