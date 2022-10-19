package uk.org.thehickses.sudoku;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.*;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class SquareTest
{
    @ParameterizedTest
    @MethodSource
    void testKeyFromIndices(int maxIndex, int row, int col, String expectedResult)
    {
        assertThat(Square.keyFromIndices(maxIndex, row, col)).isEqualTo(expectedResult);
    }

    static Stream<Arguments> testKeyFromIndices()
    {
        return Stream.of(arguments(9, 3, 4, "34"), arguments(10, 6, 2, "0602"),
                arguments(1000, 19, 213, "00190213"));
    }

    @Test
    void testConstructor()
    {
        var sq = new Square<>(4, 3, IntStream.range(3, 9)
                .boxed()
                .toList());
        assertThat(sq.id).isEqualTo("43");
        assertThat(sq.squareCount).isEqualTo(1);
        assertThat(sq.definiteValues()).isEmpty();
        assertThat(sq.possibleValues()).containsExactly(3, 4, 5, 6, 7, 8);
    }

    @Test
    void testDefiniteValues()
    {
        assertThat(new Square<>(1, 2, List.of(3)).definiteValues()).containsExactly(3);
        assertThat(new Square<>(1, 2, List.of(3, 4, 9)).definiteValues()).isEmpty();
    }

    @Test
    void testPossibleValues()
    {
        assertThat(new Square<>(1, 2, List.of(3)).possibleValues()).isEmpty();
        assertThat(new Square<>(1, 2, List.of(3, 4, 9)).possibleValues()).containsExactly(3, 4, 9);
    }

    @Test
    void testCurrentStatusNoDefiniteValue()
    {
        var sq = new Square<>(4, 2, List.of(3, 7, 9));
        assertThat(sq.currentStatus(2)).isEqualTo(Status.IMPOSSIBLE);
        assertThat(sq.currentStatus(7)).isEqualTo(Status.POSSIBLE);
    }

    @Test
    void testCurrentStatusWithDefiniteValue()
    {
        var sq = new Square<>(4, 2, List.of(8));
        assertThat(sq.currentStatus(2)).isEqualTo(Status.IMPOSSIBLE);
        assertThat(sq.currentStatus(8)).isEqualTo(Status.DEFINITE);
    }

    @Test
    void testSetStatusToDefinite()
    {
        var sq = new Square<>(3, 2, List.of(5, 6, 7));
        sq.setStatus(Status.DEFINITE, Stream.of(6));
        assertThat(sq.currentStatus(6)).isEqualTo(Status.DEFINITE);
        Stream.of(5, 7)
                .forEach(v -> assertThat(sq.currentStatus(v)).isEqualTo(Status.IMPOSSIBLE));
    }
    @Test
    void testSetStatusToImpossible()
    {
        var sq = new Square<>(3, 2, List.of(5, 6, 7));
        sq.setStatus(Status.IMPOSSIBLE, Stream.of(6));
        assertThat(sq.currentStatus(6)).isEqualTo(Status.IMPOSSIBLE);
        Stream.of(5, 7)
                .forEach(v -> assertThat(sq.currentStatus(v)).isEqualTo(Status.POSSIBLE));
        sq.setStatus(Status.IMPOSSIBLE, Stream.of(7));
        assertThat(sq.currentStatus(7)).isEqualTo(Status.IMPOSSIBLE);
        assertThat(sq.currentStatus(5)).isEqualTo(Status.DEFINITE);
    }
}
