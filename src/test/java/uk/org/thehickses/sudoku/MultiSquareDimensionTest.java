package uk.org.thehickses.sudoku;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class MultiSquareDimensionTest
{

    @Test
    void testKeyFromSquares()
    {
        var values = List.of(1, 2, 3, 4, 5);
        assertThat(MultiSquareDimension.keyFrom(new Square<>(1, 2, values),
                new Square<>(4, 5, values), new Square<>(2, 4, values))).isEqualTo("122445");
    }

    @SuppressWarnings("unchecked")
    @Test
    void testConstructorSameNumberOfSquaresAndValues()
    {
        var values = List.of(1, 2, 3, 4, 5);
        var dim = new MultiSquareDimension<>(values, new Square<>(1, 1, values),
                new Square<>(1, 2, values), new Square<>(1, 3, values), new Square<>(1, 4, values),
                new Square<>(1, 5, values));
        assertThat(dim.id).isEqualTo("1112131415");
        assertThat(dim.squareCount).isEqualTo(5);
        assertThat(dim.definiteValues()).containsExactlyElementsOf(values);
        assertThat(dim.possibleValues()).isEmpty();
        assertThat(dim.currentStatus(0)).isEqualTo(Status.IMPOSSIBLE);
        assertThat(dim.currentStatus(1)).isEqualTo(Status.DEFINITE);

    }

    @SuppressWarnings("unchecked")
    @Test
    void testConstructorDifferentNumberOfSquaresAndValues()
    {
        var values = List.of(1, 2, 3, 4, 5);
        var dim = new MultiSquareDimension<>(values, 
                new Square<>(1, 2, values), new Square<>(1, 3, values), new Square<>(1, 4, values),
                new Square<>(1, 5, values));
        assertThat(dim.id).isEqualTo("12131415");
        assertThat(dim.squareCount).isEqualTo(4);
        assertThat(dim.definiteValues()).isEmpty();;
        assertThat(dim.possibleValues()).containsExactlyElementsOf(values);
        assertThat(dim.currentStatus(0)).isEqualTo(Status.IMPOSSIBLE);
        assertThat(dim.currentStatus(1)).isEqualTo(Status.POSSIBLE);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    void testSetStatus()
    {
        var values = List.of(1, 2, 3, 4, 5);
        var dim = new MultiSquareDimension<>(values, 
                new Square<>(1, 2, values), new Square<>(1, 3, values), new Square<>(1, 4, values),
                new Square<>(1, 5, values));
        dim.setStatus(Status.IMPOSSIBLE, Stream.of(3, 5));
        dim.setStatus(Status.DEFINITE, Stream.of(1, 2));
        assertThat(dim.currentStatus(3)).isEqualTo(Status.IMPOSSIBLE);
        assertThat(dim.currentStatus(5)).isEqualTo(Status.IMPOSSIBLE);
        assertThat(dim.currentStatus(1)).isEqualTo(Status.DEFINITE);
        assertThat(dim.currentStatus(2)).isEqualTo(Status.DEFINITE);
        assertThat(dim.currentStatus(4)).isEqualTo(Status.POSSIBLE);
    }
}
