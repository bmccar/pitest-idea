package org.pitestidea.psi.fakes;

import org.apache.lucene.index.Term;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VirtualSrcPkgFake extends VirtualPkgFake {

    public final @NotNull VirtualSrcPkgFake p1;
    public final @NotNull VirtualSrcPkgFake p2;
    public final @NotNull VirtualClassFake j;

    private static final RootFake root = RootFake.javaSrc; // Force RootFake init

    /**
     * Creates a pkg fake, with children instantiated up to the provided depth.
     *
     * @param depth level of nesting
     */
    public VirtualSrcPkgFake(int depth) {
        this(root, "F", depth);
    }

    private VirtualSrcPkgFake(VirtualPkgFake parent, String name, int depth) {
        super(parent, name);
        if (depth-- > 0) {
            super.setChildren(
                    List.of(
                            p1 = new VirtualSrcPkgFake(this, "p1", depth),
                            p2 = new VirtualSrcPkgFake(this, "p2", depth),
                            j = new VirtualClassFake(this, "j.java")
                    )
            );
        } else {
            p1 = p2 = this; // Assuming these are never accessed
            j = new VirtualClassFake.Terminal(this);
        }
    }
}
