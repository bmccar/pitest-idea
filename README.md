# pitest-idea

<!-- Plugin description -->
Run [PIT](https://pitest.org) mutation tests from IntelliJ IDEA. Features:

* Run against any combination of files -- automatically matches sources and tests
* See mutation icons directly in IDE or jump to a browser view
* Sort/filter results, see score breakdown
* View and re-execute previous runs
* Maven and Gradle support

Currently supports Java. Kotlin is not yet supported.
<!-- Plugin description end -->

## Getting Started

Select "Run PITest for This File" from the editor menu on any Java file (left).
After a while, a popup appears when PITest has completed (middle). Choose the "Show Report" option.
The results (right) show the score in the toolwindow at the bottom of the screen, and the applied mutations
in the editor window.

<p >
  <img alt="Light" src="documentation/selectEditor.png" width="30%">
&nbsp; &nbsp; &nbsp; &nbsp;
  <img alt="Dark" src="documentation/showReport.png" width="30%">
&nbsp; &nbsp; &nbsp; &nbsp;
  <img alt="Dark" src="documentation/mutationsInEditor.png" width="30%">
</p>

<p>Alternatively, you can select any combination of files and packages from the project or package view (below left), 
again choosing to "Show Report" (below middle).
The toolwindow becomes more interesting, showing results across multiple files along with filtering and sorting
options and history (below right).

<p >
  <img alt="Light" src="documentation/multiSelect.png" width="30%">
&nbsp; &nbsp; &nbsp; &nbsp;
  <img alt="Dark" src="documentation/multiShow.png" width="30%">
&nbsp; &nbsp; &nbsp; &nbsp;
  <img alt="Dark" src="documentation/multiHistory.png" width="30%">
</p>

<p>PIT is run in the background and should not impact IDE performance while it runs. Do be aware however that PIT can 
take a while (hours even) for large test scopes with many input files, or packages with extensive progeny. 
You can always open the PIT window directly from its toolwindow icon (lower right in diagrams above) and see the status 
of any ongoing executions and also cancel them if you like. 
See the [PIT](https://pitest.org) site for more details on its execution.

## FAQ

#### 1. <i>How does the plugin identify what tests to run for a given set of inputs?</i>

<p>For package directories, it matches equivalent path between test and source. 
For files, it matches between source and test files using standard naming conventions, e.g. "FooTest.java" to "Foo.java". 
If you need an alternate mix, e.g. if your test for "Foo.java" is "MyTest.java", 
you can multi-select and run both from the project menu. 
This matching also works in the reverse direction if you select test files first. 

In all cases, the plugin removes redundant entries to simplify the collection of inputs. For example, if you select
both a file and its package, then only the package will be listed as part of the input since that file is included
implicitly.
This is both more efficient and streamlines long lists of input sets when selecting from the project menu.
The final inputs can be seen in the tooltip on entries in the history list.

#### 2. <i>Where are results stored?</i>

<p>In the output build directory (look for "pit-idea-reports" if interested). 
The practical considerations of this are that:

* If you do a 'clean' outside the IDE they will be removed just like everything else
* They are written from a PITest run but loaded back only at startup time, so there is no impact if they are deleted while the IDE is running


#### 3. <i>Where does PIT output trace go?</i>
To the console pane, which is visible by clicking the "<" icon button in the upper right of the plugin tool window.
If PIT fails, the popup dialog will also unhide the console unless you choose to "Ignore" the failure. 

The console pane can be hidden by clicking on the ">" icon button in the upper right of the scores pane where output
scores are shown. You might have to horizontally scroll rightward if it's not visible.

## Troubleshooting
First, make sure your project compiles. The plugin initiates an incremental build before execution, so PIT won't even
be attempted if the project does not compile.

Second, make sure your tests (those targeted for a given run) pass. PIT will start but will exit with an error if it can't run the tests.

If the failure persists and is from PIT, you can try turning on PIT verbose mode from the checkbox at the top of the console pane (see
FAQ above for discussion on the console pane). This option will also display output as PIT executes.

If there is nothing noticeable there, you can inspect the full PIT command which is the first line (in white) in the console window.
You can even copy/paste and run this command in a shell after suitably escaping any wildcard '*' characters. This may provide
some insight into the problem.

If none of this helps then feel free to file a bug report [here](https://github.com/bmccar/pitest-idea/issues) 
but please do include the output from verbose mode and the full PIT command as described above (if PIT is itself failing).
Also describe the language, build system, and project structure — a bug may be due to an unanticipated project structure.



## Future Enhancements

* Kotlin support
* Cross-module support
* Optional properties
* Excludes
* Run deltas
* VCS-relative partial execution

