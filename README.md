# A Java Sudoku solver

This repo contains a simple solver for Sudoku puzzles, written in Java.

To run the solver, run [the `Solver` class](src/main/java/uk/org/thehickses/sudoku/Solver.java). This solves the puzzle [which was 
claimed in 2012 to be the hardest Sudoku ever devised](https://abcnews.go.com/blogs/headlines/2012/06/can-you-solve-the-hardest-ever-sudoku).

The input puzzle is validated to ensure that:

* it is a 9x9 grid (a list of nine lists, each of which contains nine `Int`s).
* every square is either empty (represented by the value 0) or contains one
of the values 1 to 9 inclusive.
* no row, column or box contains duplicate values in its non-empty
squares.

The solver prints an error message if 
the puzzle is invalid or no solution can be found, otherwise the first solution found is returned.
For examples of the various error conditions, see [the `SolverTest` class](src/test/java/uk/org/thehickses/sudoku/SolverTest.java).

The program uses a straightforward algorithm which tries every possibility to solve the puzzle.
For any given grid:

* Find an empty square. If there is no empty square, the grid is solved.
* Determine which values can go into the empty square (because they do
not appear anywhere in the same row, column or box). 
* For each such value, replace the
0 at the relevant position in the source grid with that value, and solve the revised grid recursively.

The `solve` method returns a stream containing
all the possible solutions for the input grid; each solution is a grid where 
every square contains a non-zero value.
The `main` method takes the first solution in the
returned stream; thus evaluation terminates as soon as a solution is found.