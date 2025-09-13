package org.pitestidea.actions;

import com.intellij.openapi.application.PathManager;
import com.intellij.util.PathsList;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.pitestidea.model.SysDiffs;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

@DisabledOnOs(OS.WINDOWS)
class ClassPathConfiguratorTest {
    private static final String JUNIT7 = "junit-jupiter-engine-5.7.2.jar"; // No bundle for this
    private static final String JUNIT8 = "junit-jupiter-engine-5.8.2.jar";
    private static final String JUNIT10 = "junit-jupiter-engine-5.10.2.jar";
    private static final String JUNIT11 = "junit-jupiter-engine-5.11.2.jar"; // No bundle for this
    private static final String IFC_JUNIT_8 = "ifc-junit-jupiter-engine/5.8.2/x1.jar";
    private static final String IFC_JUNIT_10 = "ifc-junit-jupiter-engine/5.10.2/x2.jar";
    private static final String IFC_JUNIT_12 = "ifc-junit-jupiter-engine/5.12.2/x3.jar";
    private static final String IFN_P3 = "ifn-pitest/a/b/c/x5.jar";
    private static final String PITEST = "pitest/1.1.1/pitest-1.1.1.jar";
    private static final String R1 = "r-1.1.1.jar";

    private static final MockedStatic<PathManager> pathManagerMockedStatic = org.mockito.Mockito.mockStatic(PathManager.class);
    private final MockedStatic<Files> filesMockedStatic = org.mockito.Mockito.mockStatic(Files.class);
    private static MockedConstruction<File> mockedFileConstruction;
    private final Map<Path, Set<Path>> directoryMap = new HashMap<>();
    private static Path libDir;

    static {
        // JUnit bug? Without these, internal JUnit classloading error
        Object x = ClassPathConfigurator.class;
        x = SysDiffs.class;
    }

    @BeforeAll
    static void beforeAll() {
        mockedFileConstruction = mockConstruction(File.class, (mock, context) -> {
            String nm = (String) context.arguments().get(0);
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

    @Nested
    @DisplayName("Bundle Selection Tests")
    class BundleSelectionTests {
        @Test
        @DisplayName("Should add no bundles when none are available")
        void addNoneIfNoBundles() {
            PathsList clientPaths = paths(R1);
            List<String> added = ClassPathConfigurator.updateClassPathBundles(clientPaths);
            assertEquals(expect(R1), clientPaths.getPathList());
            assertEquals(expect(), added);
        }

        @Test
        @DisplayName("Should add IFC bundle when matching JUnit is present")
        void addIfJunitPresent() {
            bundle(IFC_JUNIT_8);
            PathsList clientPaths = paths(JUNIT8);
            List<String> added = ClassPathConfigurator.updateClassPathBundles(clientPaths);

            assertEquals(expect(JUNIT8, IFC_JUNIT_8), clientPaths.getPathList());
            assertEquals(expect(IFC_JUNIT_8), added);
        }
    }

    @Nested
    @DisplayName("Version Selection Tests")
    class VersionSelectionTests {
        @ParameterizedTest(name = "JUnit {0} should select IFC {1}")
        @CsvSource({
                JUNIT10 + "," + IFC_JUNIT_10,
                JUNIT11 + "," + IFC_JUNIT_10,  // In-between version
                JUNIT7 + "," + IFC_JUNIT_8     // Minimum version
        })
        void shouldChooseCorrectIfcVersionBasedOnJunit(String junitVersion, String expectedIfc) {
            chooseIfJunitVersion(junitVersion, expectedIfc);
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
    }

    @Nested
    @DisplayName("PITest Bundle Tests")
    class PitestBundleTests {
        @Test
        @DisplayName("Should add IFN bundle when PITest is not present")
        void addIfPitestNotPresent() {
            bundle(IFN_P3);
            PathsList clientPaths = paths(R1);
            List<String> added = ClassPathConfigurator.updateClassPathBundles(clientPaths);

            assertEquals(expect(R1, IFN_P3), clientPaths.getPathList());
            assertEquals(expect(IFN_P3), added);
        }

        @Test
        @DisplayName("Should not add IFN bundle when PITest is already present")
        void addNoneIfPitestPresent() {
            bundle(IFN_P3);
            PathsList clientPaths = paths(PITEST);
            List<String> added = ClassPathConfigurator.updateClassPathBundles(clientPaths);

            assertEquals(expect(PITEST), clientPaths.getPathList());
            assertEquals(expect(), added);
        }
    }

    @Nested
    @DisplayName("Utility Method Tests")
    class UtilityMethodTests {
        @ParameterizedTest(name = "lastSegmentNameOf({0}) should return {1}")
        @CsvSource({
                "c, c",
                "c-2.2.2, c",
                "c.jar, c",
                "c-2.2.2.jar, c",
                "/c.jar, c",
                "/c-d.jar, c-d",
                "a/b/c-1.2.3.jar, c"
        })
        void lastSegmentNameShouldExtractCorrectName(String input, String expected) {
            assertEquals(expected, ClassPathConfigurator.lastSegmentNameOf(input));
        }

        @ParameterizedTest(name = "lastSegmentVersionOf({0}) should return {1}")
        @CsvSource(value = {
                "c, null",
                "c-d, null",
                "c-d.jar, null",
                "c-3, 3",
                "c-3.4, 3.4",
                "c.jar, null",
                "c-3.jar, 3",
                "/c.jar, null",
                "/c-3.4.jar, 3.4",
                "a/b-8/c.jar, null"
        }, nullValues = "null")
        void lastSegmentVersionShouldExtractCorrectVersion(String input, String expected) {
            assertEquals(expected, ClassPathConfigurator.lastSegmentVersionOf(input));
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    class EdgeCaseTests {
        @Test
        @DisplayName("Should handle empty classpath gracefully")
        void shouldHandleEmptyClasspath() {
            PathsList emptyPaths = new PathsList();
            List<String> added = ClassPathConfigurator.updateClassPathBundles(emptyPaths);

            assertNotNull(added);
            assertTrue(emptyPaths.getPathList().isEmpty());
        }

        @Test
        @DisplayName("Should handle null or invalid paths")
        void shouldHandleInvalidPaths() {
            bundle("invalid/path/without-jar");
            PathsList clientPaths = paths(R1);

            assertDoesNotThrow(() -> {
                List<String> added = ClassPathConfigurator.updateClassPathBundles(clientPaths);
                assertNotNull(added);
            });
        }

        @Test
        @DisplayName("Should handle malformed version strings")
        void shouldHandleMalformedVersions() {
            String malformedJar = "junit-jupiter-engine-not-a-version.jar";
            bundle(IFC_JUNIT_8);
            PathsList clientPaths = paths(malformedJar);

            assertDoesNotThrow(() -> {
                List<String> added = ClassPathConfigurator.updateClassPathBundles(clientPaths);
                assertNotNull(added);
            });
        }

        @Test
        @DisplayName("Should handle very long version strings")
        void shouldHandleLongVersionStrings() {
            String longVersionJar = "junit-jupiter-engine-1.2.3.4.5.6.7.8.9.10.jar";
            PathsList clientPaths = paths(longVersionJar);

            assertDoesNotThrow(() -> {
                List<String> added = ClassPathConfigurator.updateClassPathBundles(clientPaths);
                assertNotNull(added);
            });
        }
    }
}