package org.pitestidea.configuration;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.components.BaseState;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PitIJConfigurationFactory extends ConfigurationFactory {

  public PitIJConfigurationFactory(ConfigurationType type) {
    super(type);
  }

  @Override
  public @NotNull String getId() {
    return PitIJRunConfigurationType.ID;
  }

  @NotNull
  @Override
  public RunConfiguration createTemplateConfiguration(
      @NotNull Project project) {
    return new PitIJRunConfiguration(project, this, "PI Test");
  }

  @Nullable
  @Override
  public Class<? extends BaseState> getOptionsClass() {
    return PitIJRunConfigurationOptions.class;
  }

}
