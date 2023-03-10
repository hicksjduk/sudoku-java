package uk.org.thehickses.sudoku;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import uk.org.thehickses.sudoku.Solver.Grid;
import uk.org.thehickses.sudoku.Solver.Square;

class SolverTest
{
    @Test
    void testSolveWrongNumberOfRows()
    {
        assertThat("Wrong number of rows").isEqualTo(assertThrows(RuntimeException.class,
                () -> new Solver().solve(new Grid(Stream.of(Solver.puzzle.rows())
                        .limit(4)
                        .toArray(int[][]::new)))).getLocalizedMessage());
    }

    @Test
    void testSolveWrongNumberOfCols()
    {
        assertThat("Wrong number of columns").isEqualTo(assertThrows(RuntimeException.class,
                () -> new Solver().solve(new Grid(Stream.of(Solver.puzzle.rows())
                        .map(IntStream::of)
                        .map(IntStream::distinct)
                        .map(IntStream::toArray)
                        .toArray(int[][]::new)))).getLocalizedMessage());
    }

    @Test
    void testSolveInvalidCellValue()
    {
        assertThat("Invalid cell value").isEqualTo(assertThrows(RuntimeException.class,
                () -> new Solver().solve(Solver.puzzle.setValueAt(new Square(5, 4), 41)))
                        .getLocalizedMessage());
    }

    @Test
    void testSolveRowHasDuplicates()
    {
        assertThat("Row has duplicate value(s)").isEqualTo(assertThrows(RuntimeException.class,
                () -> new Solver().solve(Solver.puzzle.setValueAt(new Square(8, 0), 4)))
                        .getLocalizedMessage());
    }

    @Test
    void testSolveColumnHasDuplicates()
    {
        assertThat("Column has duplicate value(s)").isEqualTo(assertThrows(RuntimeException.class,
                () -> new Solver().solve(Solver.puzzle.setValueAt(new Square(3, 8), 8)))
                        .getLocalizedMessage());
    }

    @Test
    void testSolveBoxHasDuplicates()
    {
        assertThat("Box has duplicate value(s)").isEqualTo(assertThrows(RuntimeException.class,
                () -> new Solver().solve(Solver.puzzle.setValueAt(new Square(8, 0), 1)))
                        .getLocalizedMessage());
    }

    @Test
    void testUnsolvablePuzzle()
    {
        assertThat(new Solver().solve(Solver.puzzle.setValueAt(new Square(8, 8), 3))).isEmpty();
    }

    @Test
    void testPuzzleSolved()
    {
        var expected = Grid.with(8, 1, 2, 7, 5, 3, 6, 4, 9)
                .and(9, 4, 3, 6, 8, 2, 1, 7, 5)
                .and(6, 7, 5, 4, 9, 1, 2, 8, 3)
                .and(1, 5, 4, 2, 3, 7, 8, 9, 6)
                .and(3, 6, 9, 8, 4, 5, 7, 2, 1)
                .and(2, 8, 7, 1, 6, 9, 5, 3, 4)
                .and(5, 2, 1, 9, 7, 4, 3, 6, 8)
                .and(4, 3, 8, 5, 2, 6, 9, 1, 7)
                .and(7, 9, 6, 3, 1, 8, 4, 5, 2).toString();
        assertThat(new Solver().solve(Solver.puzzle)
                .findFirst().map(Grid::toString)).contains(expected);
    }
}
