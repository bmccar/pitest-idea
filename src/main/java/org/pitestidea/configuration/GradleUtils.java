package org.pitestidea.configuration;

import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.externalSystem.model.*;
import com.intellij.openapi.externalSystem.model.project.*;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Gradle-specific utilities, needed where the Intellij utilities that work for Maven projects
 * behave differently for Gradle projects.
 */
public class GradleUtils {
    private static final ProjectSystemId SYSTEM_ID = new ProjectSystemId("GRADLE");
    private static final Key<ModuleData> MODULE_KEY = Key.create(ModuleData.class, 1);

    public static String findModulePathGradle(Project project, Module module) {
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
                Collection<DataNode<ModuleData>> moduleNodes =
                        ExternalSystemApiUtil.findAll(projectInfo.getExternalProjectStructure(), ProjectKeys.MODULE);
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
                    Collection<DataNode<LibraryDependencyData>> libraryDependencies =
                            ExternalSystemApiUtil.findAll(moduleNode, ProjectKeys.LIBRARY_DEPENDENCY);

                    @NotNull Collection<DataNode<LibraryDependencyData>> cnodes =
                            ExternalSystemApiUtil.findAllRecursively(
                                    moduleNode,
                                    ProjectKeys.LIBRARY_DEPENDENCY);
                    for (DataNode<LibraryDependencyData> libDataNode : cnodes) {
                        LibraryDependencyData libData = libDataNode.getData();
                        String path = libData.getTarget().getPaths(LibraryPathType.BINARY).iterator().next();
                        javaParameters.getClassPath().add(path);
                    }
                    return true;
                }
            }
        }
        return false;
    }
}

