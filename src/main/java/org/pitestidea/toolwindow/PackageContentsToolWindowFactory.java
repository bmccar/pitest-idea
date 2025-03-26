package org.pitestidea.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.treeStructure.SimpleTree;
import org.jetbrains.annotations.NotNull;
import org.pitestidea.model.PitExecutionRecorder;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

public class PackageContentsToolWindowFactory implements ToolWindowFactory {

    private static final String CONTENT_PANEL_NAME = "PackageExplorerToolWindow";

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        // Create the UI for the tool window
        PackageContentsPanel contentsPanel = new PackageContentsPanel();
        ContentFactory contentFactory = ContentFactory.getInstance();
        //Content content = contentFactory.createContent(contentsPanel.getPanel(), "THE_DISPLAY_NAME", false);
        Content content = contentFactory.createContent(contentsPanel.getPanel(), null, false);
        toolWindow.getContentManager().addContent(content);
    }

    public static void show(Project project, PitExecutionRecorder recorder) {
        System.out.println("showing");
        //String id = "PITest tool window";
        String id = "Package Contents";
        ToolWindow tw = ToolWindowManager.getInstance(project).getToolWindow(id);
        System.out.println("SHOW: " + tw);
        if (tw != null) {
            //tw.activate(()->addAll(recorder));
            tw.activate(null);
        }
    }

    public static class PackageContentsPanel {
        private final JPanel panel;
        private final SimpleTree tree;

        public PackageContentsPanel() {
            panel = new JPanel(new BorderLayout());
            tree = new SimpleTree();

            JLabel header = new JLabel("Package Contents");
            header.setHorizontalAlignment(SwingConstants.CENTER);

            // Add elements to the panel
            panel.add(header, BorderLayout.NORTH);
            panel.add(new JScrollPane(tree), BorderLayout.CENTER);
            populateTree("zoinks", new String[]{"foo","bar","baz"});
        }

        public JPanel getPanel() {
            return panel;
        }

        /*
        public SimpleTree getTree() {
            return tree;
        }
         */

        public void populateTree(String packageName, String[] contents) {
            DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode(packageName);
            DefaultTreeModel model = new DefaultTreeModel(rootNode);
            for (String content : contents) {
                rootNode.add(new DefaultMutableTreeNode(content));
            }
            tree.setModel(model);
        }
    }
}
