// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.pitestidea.toolwindow;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.pitestidea.model.FileMutations;
import org.pitestidea.model.PitExecutionRecorder;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Calendar;
import java.util.Objects;

public final class PitToolWindowFactory implements ToolWindowFactory, DumbAware {

  private static MutationControlPanel mutationControlPanel = null;

  public static void show(Project project, PitExecutionRecorder recorder) {
    if (mutationControlPanel != null) {
      mutationControlPanel.clear();
    }
    String id = "PITest tool window";
    ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow(id);
    if (tw != null) {
      if (tw.isActive()) {
        addAll(project, recorder);
      } else {
        tw.activate(() -> addAll(project, recorder));
      }
    }
  }

  private static void addAll(Project project, PitExecutionRecorder recorder) {
    recorder.visit(new PitExecutionRecorder.FileVisitor() {
      @Override
      public void visit(VirtualFile file, FileMutations fileMutations) {
        float score = fileMutations.getMutationCoverageScore();
        String filePath = file.getPath();
        String fileName = filePath.substring(filePath.lastIndexOf("/")+1);
        mutationControlPanel.setLine(project, file, fileName, score);
      }
    });
    mutationControlPanel.refresh();
  }

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    System.out.println("Creating tool window content !!!");
    mutationControlPanel = new MutationControlPanel();

    ContentFactory contentFactory = ContentFactory.getInstance();
    Content content = contentFactory.createContent(mutationControlPanel.getPanel(), null, false);
    toolWindow.getContentManager().addContent(content);
  }
}
