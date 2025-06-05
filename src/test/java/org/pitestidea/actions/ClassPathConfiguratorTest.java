package org.pitestidea.actions;

import com.intellij.openapi.application.PathManager;
import com.intellij.util.PathsList;
import org.junit.jupiter.api.*;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class ClassPathConfiguratorTest {
    private static final String JUNIT7 = "junit-jupiter-engine-5.7.2.jar"; // No bundle for this
    private static final String JUNIT8 = "junit-jupiter-engine-5.8.2.jar";
    private static final String JUNIT10 = "junit-jupiter-engine-5.10.2.jar";
    private static final String JUNIT11 = "junit-jupiter-engine-5.11.2.jar"; // No bundle for this
    private static final String JUNIT12 = "junit-jupiter-engine-5.12.2.jar";
    private static final String IFC_JUNIT_8 = "ifc-junit-jupiter-engine/5.8.2/x1.jar";
    private static final String IFC_JUNIT_10 = "ifc-junit-jupiter-engine/5.10.2/x2.jar";
    private static final String IFC_JUNIT_12 = "ifc-junit-jupiter-engine/5.12.2/x3.jar";
    private static final String IFC_IFN = "ifc-junit-jupiter-engine/5.12.2/ifn-pitest/x4.jar";
    private static final String IFN_P3 = "ifn-pitest/a/b/c/x5.jar";
    private static final String IFN_P4 = "ifn-pitest/d/e/f/x6.jar";
    private static final String PITEST = "pitest/1.1.1/pitest-1.1.1.jar";
    private static final String R1 = "r-1.1.1.jar";

    private static final MockedStatic<PathManager> pathManagerMockedStatic = org.mockito.Mockito.mockStatic(PathManager.class);
    private final MockedStatic<Files> filesMockedStatic = org.mockito.Mockito.mockStatic(Files.class);
    private static MockedConstruction<File> mockedFileConstruction;
    private final Map<Path, Set<Path>> directoryMap = new HashMap<>();
    private static Path libDir;

    @BeforeAll
    static void beforeAll() {
         mockedFileConstruction = mockConstruction(File.class, (mock, context) -> {
             String nm = (String)context.arguments().get(0);
             when(mock.isDirectory()).thenReturn(!nm.endsWith(".jar"));
         });

        // Any value will do:
        pathManagerMockedStatic.when(PathManager::getPluginsDir).thenReturn(Path.of("/abc"));
        libDir = ClassPathConfigurator.libDir();
    }

    @AfterAll
    static void afterAll() {
        pathManagerMockedStatic.close();
        mockedFileConstruction.close();
    }

    @BeforeEach
    void setUp() {
        linkPathToDirectory(libDir);
    }

    @AfterEach
    void tearDown() {
        filesMockedStatic.close();
    }

    private void linkPathToDirectory(Path subPath) {
        Set<Path> subDir = directoryMap.computeIfAbsent(subPath, _x -> new HashSet<>());
        filesMockedStatic.when(() -> Files.list(eq(subPath))).thenAnswer(invocation -> subDir.stream());
    }

    void bundleOne(String stringPath) {
        String base = libDir.toString();
        String[] segments = stringPath.split("/");
        for (String nextSegment : segments) {
            if (!nextSegment.isEmpty()) {
                Path subPath = Path.of(base, nextSegment);
                directoryMap.get(Path.of(base)).add(subPath);
                linkPathToDirectory(subPath);
                base = subPath.toString();
            }
        }
    }

    void bundle(String... paths) {
        for (String path : paths) {
            bundleOne(path);
        }
    }

    private static PathsList paths(String... paths) {
        String base = libDir.toString();
        PathsList list = new PathsList();
        for (String path : paths) {
            list.add(base + File.separatorChar + path);
        }
        return list;
    }

    private static List<String> expect(String... paths) {
        return Arrays.stream(paths).map(p -> libDir.toString() + File.separatorChar + p).toList();
    }

    @Test
    void addNoneIfNoBundles() {
        PathsList clientPaths = paths(R1);
        List<String> added = ClassPathConfigurator.updateClassPathBundles(clientPaths);
        assertEquals(expect(R1), clientPaths.getPathList());
        assertEquals(expect(), added);
    }

    @Test
    void addIfJunitPresent() {
        bundle(IFC_JUNIT_8);
        PathsList clientPaths = paths(JUNIT8);
        List<String> added = ClassPathConfigurator.updateClassPathBundles(clientPaths);

        assertEquals(expect(JUNIT8, IFC_JUNIT_8), clientPaths.getPathList());
        assertEquals(expect(IFC_JUNIT_8), added);
    }

    /**
     * Given a junit version in the classpath, and 8/10/12 bundled, which of those three
     * is expected to be added?
     *
     * @param junitVersionInClasspath junit version
     * @param expectedIfc             expected ifc added
     */
    private void chooseIfJunitVersion(String junitVersionInClasspath, String expectedIfc) {
        bundle(IFC_JUNIT_8, IFC_JUNIT_12, IFC_JUNIT_10);
        PathsList clientPaths = paths(junitVersionInClasspath);
        List<String> added = ClassPathConfigurator.updateClassPathBundles(clientPaths);

        assertEquals(expect(junitVersionInClasspath, expectedIfc), clientPaths.getPathList());
        assertEquals(expect(expectedIfc), added);
    }

    @Test
    void chooseIfJunitPresent() {
        chooseIfJunitVersion(JUNIT10, IFC_JUNIT_10);
    }

    @Test
    void chooseInBetweenIfJunitPresent() {
        chooseIfJunitVersion(JUNIT11, IFC_JUNIT_10);
    }

    @Test
    void chooseMinimumIfJunitPresent() {
        chooseIfJunitVersion(JUNIT7, IFC_JUNIT_8);
    }

    @Test
    void addIfPitestNotPresent() {
        bundle(IFN_P3);
        PathsList clientPaths = paths(R1);
        List<String> added = ClassPathConfigurator.updateClassPathBundles(clientPaths);

        assertEquals(expect(R1, IFN_P3), clientPaths.getPathList());
        assertEquals(expect(IFN_P3), added);
    }

    @Test
    void addNoneIfPitestPresent() {
        bundle(IFN_P3);
        PathsList clientPaths = paths(PITEST);
        List<String> added = ClassPathConfigurator.updateClassPathBundles(clientPaths);

        assertEquals(expect(PITEST), clientPaths.getPathList());
        assertEquals(expect(), added);
    }

    @Test
    void addMultipleIfPitestNotPresent() {
        bundle(IFN_P3, IFN_P4);
        PathsList clientPaths = paths(R1);
        List<String> added = ClassPathConfigurator.updateClassPathBundles(clientPaths);

        assertEquals(expect(R1, IFN_P3, IFN_P4), clientPaths.getPathList());
        assertEquals(expect(IFN_P3, IFN_P4), added);
    }

    @Test
    void addIfJunitPresentAndPitestNotPresent() {
        bundle(IFC_IFN);
        PathsList clientPaths = paths(JUNIT12);
        List<String> added = ClassPathConfigurator.updateClassPathBundles(clientPaths);

        assertEquals(expect(JUNIT12, IFC_IFN), clientPaths.getPathList());
        assertEquals(expect(IFC_IFN), added);
    }

    @Test
    void noDupConflicts() {
        String IFN = "ifn-blue/foo-1.2.3.jar";
        String CP = "foo-4.5.6.jar";
        bundle(IFN);
        PathsList clientPaths = paths(CP);
        List<String> added = ClassPathConfigurator.updateClassPathBundles(clientPaths);

        assertEquals(expect(CP), clientPaths.getPathList());
        assertEquals(expect(), added);
    }

    @Test
    void lastSegmentName() {
        assertEquals("c",ClassPathConfigurator.lastSegmentNameOf("c"));
        assertEquals("c",ClassPathConfigurator.lastSegmentNameOf("c-2.2.2"));
        assertEquals("c",ClassPathConfigurator.lastSegmentNameOf("c.jar"));
        assertEquals("c",ClassPathConfigurator.lastSegmentNameOf("c-2.2.2.jar"));
        assertEquals("c",ClassPathConfigurator.lastSegmentNameOf("/c.jar"));
        assertEquals("c-d",ClassPathConfigurator.lastSegmentNameOf("/c-d.jar"));
        assertEquals("c",ClassPathConfigurator.lastSegmentNameOf("a/b/c-1.2.3.jar"));
    }

    @Test
    void lastSegmentVersion() {
        assertNull(ClassPathConfigurator.lastSegmentVersionOf("c"));
        assertNull(ClassPathConfigurator.lastSegmentVersionOf("c-d"));
        assertNull(ClassPathConfigurator.lastSegmentVersionOf("c-d.jar"));
        assertEquals("3",ClassPathConfigurator.lastSegmentVersionOf("c-3"));
        assertEquals("3.4",ClassPathConfigurator.lastSegmentVersionOf("c-3.4"));
        assertNull(ClassPathConfigurator.lastSegmentVersionOf("c.jar"));
        assertEquals("3",ClassPathConfigurator.lastSegmentVersionOf("c-3.jar"));
        assertNull(ClassPathConfigurator.lastSegmentVersionOf("/c.jar"));
        assertEquals("3.4",ClassPathConfigurator.lastSegmentVersionOf("/c-3.4.jar"));
        assertNull(ClassPathConfigurator.lastSegmentVersionOf("a/b-8/c.jar"));
    }

    private static void verifyVersion(String expected, String provided, List<String> matchAgainst) {
        assertEquals(expected, ClassPathConfigurator.chooseFromIfcVersion('-'+provided+".jar", matchAgainst));
    }

    @Test
    void versionGtAny() {
        String lowEnd = "1.2.3";
        String highEnd = "1.3.2";
        List<String> versions = Arrays.asList(lowEnd, highEnd);
        String lowerThanLowEnd = "1.2.1";
        String middle = "1.3.0";
        String higherThanHighEnd = "2.1.1";
        verifyVersion(lowEnd, lowerThanLowEnd, versions);
        verifyVersion(lowEnd, lowEnd, versions);
        verifyVersion(lowEnd, middle, versions);
        verifyVersion(highEnd, highEnd, versions);
        verifyVersion(highEnd, higherThanHighEnd, versions);
    }
}