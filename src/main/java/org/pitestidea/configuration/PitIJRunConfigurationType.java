package org.pitestidea.configuration;

import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.NotNullLazyValue;

public final class PitIJRunConfigurationType extends ConfigurationTypeBase {

  static final String ID = "PitIJRunConfiguration";

  public PitIJRunConfigurationType() {
    super(ID, "PI Test", "Run PIT Mutation Test",
        NotNullLazyValue.createValue(() -> AllIcons.Nodes.Console));
    addFactory(new PitIJConfigurationFactory(this));
  }

}
