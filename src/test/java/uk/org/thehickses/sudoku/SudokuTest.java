package uk.org.thehickses.sudoku;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

import uk.org.thehickses.sudoku.Sudoku.Grid;
import uk.org.thehickses.sudoku.Sudoku.Puzzle;

class SudokuTest
{
    @Test
    void testSudoku()
    {
        var input = """
                  800000000
                  003600000
                  070090200
                  050007000
                  000045700
                  000100030
                  001000068
                  008500010
                  090000400
                """;
        var puzzle = Puzzle.from(input);
        var expected = """
                812753649
                943682175
                675491283
                154237896
                369845721
                287169534
                521974368
                438526917
                796318452
                """;
        assertThat(Sudoku.solve(puzzle)).isEqualTo(Grid.from(expected));
    }

    @Test
    void testEasyKiller()
    {
        var input = """
                aabbcddef 14 8 16 10 9 13
                gghcciief 7 11 9
                jjhklmnno 10 9 10 12 11 7
                pqqklmrro 8 10 11
                psstttuuv 12 11 16 4
                wxxyz122v 10 9 17 6 7 12
                w33yz1455 9 11 8
                6788994AA 16 10 5 19 11
                67BB9CCDD 11 4 12
                                """;
        var puzzle = Puzzle.from(input);
        var expected = """
                863579124
                254183679
                197264835
                319748562
                548326791
                672915483
                436852917
                981437256
                725691348
                """;
        assertThat(Sudoku.solve(puzzle)).isEqualTo(Grid.from(expected));
    }

    @Test
    void testHardKiller()
    {
        var input = """
                aabbcddee 14 8 44 15 12
                affcccgge 29 26
                ffcchccgg 4
                fijjhjjkg 25 36 17
                iiljjjmkk 10 7
                inlooompk 25 45 35
                inooqoopk 18
                nnorqsopp 9 10
                nttrqsuup 12 4
                """;
        var puzzle = Puzzle.from(input);
        var expected = """
                481729635
                267583194
                359416728
                815234976
                926871543
                734965281
                693147852
                172358469
                548692317
                """;
        assertThat(Sudoku.solve(puzzle)).isEqualTo(Grid.from(expected));
    }
}
