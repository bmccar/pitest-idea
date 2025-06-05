package org.pitestidea.configuration;

import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.externalSystem.model.*;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.DependencyScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.openapi.diagnostic.Logger;

import java.util.Collection;
import java.util.Set;

/**
 * Gradle-specific utilities, needed where the Intellij utilities that work for Maven projects
 * behave differently for Gradle projects.
 */
public class GradleUtils {
    private static final Logger LOGGER = Logger.getInstance(GradleUtils.class);

    private static final ProjectSystemId SYSTEM_ID = new ProjectSystemId("GRADLE");
    private static final Key<ModuleData> MODULE_KEY = Key.create(ModuleData.class, 1);

    /*
     * Gradle projects have separate modules for module X: "X.main" and "X.test". Depending on
     * context, we might need one or the other. Assuming Maven modules are never named this way,
     * this is safe to call in a Maven context.
     */
    private static Module ensureModule(Module module, String from, String to) {
        final String firstName = module.getName();
        if (firstName.endsWith(from)) {
            String subName = firstName.substring(0, firstName.length() - from.length()) + to;
            ModuleManager moduleManager = ModuleManager.getInstance(module.getProject());
            return moduleManager.findModuleByName(subName);
        }
        return module;
    }

    public static Module ensureMainModule(Module module) {
        return ensureModule(module, ".test", ".main");
    }

    public static Module ensureTestModule(Module module) {
        return ensureModule(module, ".main", ".test");
    }

    public static String findModulePathGradle(Module module) {
        Project project = module.getProject();
        ProjectDataManager projectDataManager = ProjectDataManager.getInstance();
        if (projectDataManager != null) {
            @NotNull Collection<ExternalProjectInfo> projectDataNodes =
                    projectDataManager.getExternalProjectsData(
                            project,
                            SYSTEM_ID);

            ExternalProjectInfo projectDataNode = projectDataNodes.stream().findFirst().orElse(null);
            if (projectDataNode != null) {
                @NotNull Collection<DataNode<ModuleData>> moduleDataNodes =
                        ExternalSystemApiUtil.findAllRecursively(
                                projectDataNode.getExternalProjectStructure(),
                                MODULE_KEY);
                for (DataNode<ModuleData> next : moduleDataNodes) {
                    ModuleData md = next.getData();
                    return md.getLinkedExternalProjectPath();
                }
            }
        }
        return null;
    }

    public static boolean configureFromGradleClasspath(Module module, JavaParameters javaParameters) {
        //if (1==1) return false;
        module = ensureTestModule(module);
        ProjectDataManager projectDataManager = ProjectDataManager.getInstance();
        Project project = module.getProject();
        String projectPath = project.getBasePath();
        if (projectPath != null) {
            ExternalProjectInfo projectInfo = projectDataManager.getExternalProjectData(
                    project,
                    SYSTEM_ID,
                    projectPath
            );
            if (projectInfo != null && projectInfo.getExternalProjectStructure() != null) {
                String moduleName = module.getName();
                int ix = moduleName.indexOf('.');
                if (ix > 0) {
                    moduleName = moduleName.substring(0, ix);
                }
                final String finalModuleName = moduleName;
                @Nullable DataNode<ModuleData> moduleNode =
                        ExternalSystemApiUtil.findChild(projectInfo.getExternalProjectStructure(),
                                ProjectKeys.MODULE,
                                m->m.getData().getExternalName().equals(finalModuleName));

                if (moduleNode != null) {
                    @NotNull Collection<DataNode<LibraryDependencyData>> cnodes =
                            ExternalSystemApiUtil.findAllRecursively(
                                    moduleNode,
                                    ProjectKeys.LIBRARY_DEPENDENCY);
                    for (DataNode<LibraryDependencyData> libDataNode : cnodes) {
                        LibraryDependencyData libData = libDataNode.getData();
                        @NotNull Set<String> binaries = libData.getTarget().getPaths(LibraryPathType.BINARY);
                        if (binaries.isEmpty()) {
                            LOGGER.warn("Invalid dependency: " + libData.getTarget().getExternalName());
                        } else {
                            DependencyScope scope = libData.getScope();
                            if (scope == DependencyScope.COMPILE || scope == DependencyScope.PROVIDED) {
                                String path = binaries.iterator().next();

                                javaParameters.getClassPath().add(path);
                            }
                        }
                    }
                    return true;
                }
            }
        }
        return false;
    }
}

