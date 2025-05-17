package org.pitestidea.psi.fakes;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VirtualTestPkgFake extends VirtualPkgFake {

    public final @NotNull VirtualTestPkgFake p1;
    public final @NotNull VirtualTestPkgFake p2;
    public final @NotNull VirtualClassFake jtest;
    public final @NotNull VirtualClassFake testj;

    private static final RootFake root = RootFake.javaTest; // Force RootFake init

    /**
     * Creates a pkg fake, with children instantiated up to the provided depth.
     *
     * @param depth level of nesting
     */
    public VirtualTestPkgFake(int depth) {
        this(root, "F", depth);
    }

    private VirtualTestPkgFake(VirtualPkgFake parent, String name, int depth) {
        super(parent, name);
        if (depth-- > 0) {
            super.setChildren(
                    List.of(
                            p1 = new VirtualTestPkgFake(this, "p1", depth),
                            p2 = new VirtualTestPkgFake(this, "p2", depth),
                            jtest = new VirtualClassFake(this, "jTest.java"),
                            testj = new VirtualClassFake(this, "testJ.java")
                    )
            );
        } else {
            p1 = p2 = this; // Assuming these are never accessed
            jtest = testj = new VirtualClassFake.Terminal(this);
        }
    }
}
