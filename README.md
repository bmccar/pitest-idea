# pitest-idea

Run [PIT](https://pitest.org) mutation tests from Intellij IDEA. Features:

* Run against any combination of files -- automatically matches sources and tests
* See mutation icons directly in IDE or jump to a browser view
* Sort/filter results, see score breakdown
* View and re-execute previous runs

Currently, only Java and Maven projects are supported.

## Getting Started

Select "Run PITest Here" from the editor menu on any Java file (left).
After a while, a popup appears when PITest has completed (middle).
Select "Show Report" to see results (right).

<p >
  <img alt="Light" src="documentation/selectEditor.png" width="30%">
&nbsp; &nbsp; &nbsp; &nbsp;
  <img alt="Dark" src="documentation/showReport.png" width="30%">
&nbsp; &nbsp; &nbsp; &nbsp;
  <img alt="Dark" src="documentation/mutationsInEditor.png" width="30%">
</p>

<p>Alternatively, you can select any combination of files and packages from the project view (below left).
The toolwindow becomes more interesting, showing results across multiple files along with filtering and sorting
options (below right).

<p >
  <img alt="Light" src="documentation/multiSelect.png" width="45%">
&nbsp; &nbsp; &nbsp; &nbsp;
  <img alt="Dark" src="documentation/multiHistory.png" width="45%">
</p>

## FAQ

#### 1 <i>How does the plugin identify what tests to run for a given set of inputs?</i>

<p>For directories it looks for matching directories between test and source. For files, it looks for matching files that
use standard naming conventions, e.g. "FooTest.java" for "Foo.java". If you need an alternate mix, e.g. if your test
for "Foo.java" is "MyTest.java", you can multi-select both from the project menu and run the plugin there.

This also works in the reverse direction if you select test files first. In all cases, the plugin removes redundant
entries to simplify the inputs. For example, if you select both a file and its package, then only the package will be
part of the input. This is both more efficient and allows eases worry about large input sets when selecting many entries
from the project menu. The final inputs are shown as the tooltip on entries in the history list.
