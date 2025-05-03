package org.pitestidea.toolwindow;

import javax.swing.*;

public class DualTextHoverableJTree {
    private static void say(String text) {
        System.out.println("Clicked " + text);
    }

    public static void main(String[] args) {
        DTree dTree = new DTree();
        DTree.TreeRow row = dTree.addRootRow()
                .addSegment("Top", DTree.Hover.UNDERLINE,()->say("top!"));
        DTree.TreeRow row1 = row.addChildRow()
                .addSegment("86%", DTree.Hover.UNDERLINE,()->say("86er"))
                .addSegment("blue", DTree.Hover.NONE,()->say("blue"));
        DTree.TreeRow row11 = row1.addChildRow()
                .addSegment("100%", DTree.Hover.UNDERLINE,()->say("100%"))
                .addSegment("red", DTree.Hover.NONE,()->say("red"));
        DTree.TreeRow row12 = row1.addChildRow()
                .addSegment("64%", DTree.Hover.NONE,()->say("64%"))
                .addSegment("green", DTree.Hover.NONE,()->say("green"))
                .addSegment("laster", DTree.Hover.UNDERLINE,()->say("laster"));
        DTree.TreeRow row123 = row12.addChildRow()
                .addSegment("22%", DTree.Hover.UNDERLINE,()->say("22%"))
                .addSegment("black", DTree.Hover.NONE,()->say("black"));

        dTree.expandAll();

        // Create a frame to display the tree
        JFrame frame = new JFrame("Dual Text Hoverable JTree - 1");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);
        frame.add(new JScrollPane(dTree.getComponent()));
        frame.setVisible(true);
    }
}