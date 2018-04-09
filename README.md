# decisions-uml-diagrammer
A repository to produce UML diagrams from ODM Rules or ODM DSI artifacts

The generation of UML diagram is using [PlantUML](http://plantuml.com/).
In this first version, the code herein is only producing 
a textual description that can be used by PlantUML to produce a diagram.

## Class diagram from a Business Object Model (BOM)

Here is an example of a class diagram of the Loan Validation sample BOM.
![Loan validation class diagram](doc/loanvalidation.png)

The ClassDiagramWriter class can produce a textual representation of a BOM
 to be passed to PlantUML to produce the actual class diagram. It can be 
 used with a BOM coming from ODM Rules or ODM DSI.
It has a specific representation for:
- utility classes (U)
- event classes (E)
- entity classes (N)

# License
[Apache 2.0](LICENSE)

# Notice
© Copyright IBM Corporation 2018.