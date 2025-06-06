# pitest-idea

<!-- Plugin description -->
Run [PIT](https://pitest.org) mutation tests from IntelliJ IDEA. Features:

* Run against any combination of Java files -- automatically matches sources and tests
* See mutation icons directly in IDE or jump to a browser view
* Sort/filter results, see score breakdown
* View and re-execute previous runs
* Maven and Gradle support
<!-- Plugin description end -->

See [here](https://bmccar.github.io/pitest-idea) for usage, FAQ, and troubleshooting.

Available on [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/27437-pitest/edit).

## Implementation Notes
* Runs PITest in the background through the command line
* Tracks and saves executions
* User initiates execution through menu actions in the editor, project/package view, or own toolwindow
* Bundles a limited set of dependent jars to allow most users to run without additional configuration but allows user classpath [overrides from their build file](https://bmccar.github.io/pitest-idea/configuration.html)

The general flow, from an implementation package perspective, is:

    actions->            // Initiate execution
       reader->          // Read computed or previously stored results
         model->         // Capture and structure mutation results by file and line number
           render        // Editor gutter markings
           toolwindow    // Toolwindow split into several panes


## Possible Future Enhancements
* Kotlin support
* Cross-module support
* Optional properties
* Excludes
* Run deltas
* VCS-relative partial execution

