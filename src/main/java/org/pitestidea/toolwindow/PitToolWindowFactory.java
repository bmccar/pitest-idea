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
    System.out.println("showing");
    String id = "PITest tool window";
    ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow(id);
    System.out.println("SHOW: " + tw);
    if (tw != null) {
      tw.activate(()->addAll(recorder));
    }
  }

  private static void addAll(PitExecutionRecorder recorder) {
    recorder.visit(new PitExecutionRecorder.FileVisitor() {
      @Override
      public void visit(VirtualFile file, FileMutations fileMutations) {
        float score = fileMutations.getMutationCoverageScore();
        String filePath = file.getPath();
        String fileName = filePath.substring(filePath.lastIndexOf("/")+1);
        mutationControlPanel.setLine(filePath, fileName, score);
      }
    });
  }

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    System.out.println("Creating tool window content !!!");
    mutationControlPanel = new MutationControlPanel();
    //toolWindow.getComponent().add(mutationControlPanel);

    //PackageContentsToolWindowFactory.PackageContentsPanel contentsPanel = new PackageContentsToolWindowFactory.PackageContentsPanel();
    ContentFactory contentFactory = ContentFactory.getInstance();
    Content content = contentFactory.createContent(mutationControlPanel.getPanel(), null, false);
    toolWindow.getContentManager().addContent(content);
  }

  private JPanel createLine(String text, int score) {
    JPanel line = new JPanel();
    JLabel main = new JLabel(text);
    main.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        super.mouseClicked(e);
        System.out.println("Clicked");
      }
    });
    line.add(main);

    JLabel value1 = new JLabel(String.valueOf(score));
    value1.setBorder( new LineBorder(JBColor.GREEN, 1, true));
    line.add(value1);
    return line;
  }
}
