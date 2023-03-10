package uk.org.thehickses.sudoku;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import uk.org.thehickses.sudoku.Solver.Grid;
import uk.org.thehickses.sudoku.Solver.Square;

class SolverTest
{
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
