# CSC2002 Assignment 2

## Water flow simulation
This is an application to simulate water flow over a terrain. The UI is an image representing the elevation of the terrain as a grayscale image, which the user can click on to add water, and buttons to manipulate the simulation.

## Running the program
The best way to run the program is: 
1. Copy input files to the io-files directory
2. From the root directory run `make run`
3. Enter the file name as directed

It can also be run by using the `java` command to run the `Flow` class, and providing the data file path as an argument.

## Make options
* `compile` Compiles java class files
* `docs` Generates javadocs
* `clean` Removes all class files from the bin directory
* `clean-docs` Removes all the docs files in the doc directory
* `run` Runs a shell script that gets input and then runs the `main()` method.

## Water conservation debugging
There is a mechanism to keep track of water units and check that water is conserved (expected behavior). All of it is commented out by default. There are also some commented out `yield()` statements to increase interleavings. To find and enable all of these pieces of code, search "Uncomment for debugging" in `FlowPanel` and `Water` classes.
