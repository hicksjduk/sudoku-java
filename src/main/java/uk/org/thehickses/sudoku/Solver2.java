package uk.org.thehickses.sudoku;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Solver2
{
    public static class Structure<T>
    {
        public final List<T> permittedValues;
        public final T emptySquare;
        public final int gridSize;

        public static Structure<Integer> of(IntStream permittedValues, int emptySquare)
        {
            return of(permittedValues.boxed(), Integer.valueOf(emptySquare));
        }

        public static <T> Structure<T> of(Stream<T> permittedValues, T emptySquare)
        {
            return new Structure<>(permittedValues, emptySquare);
        }

        private Structure(Stream<T> permittedValues, T emptySquare)
        {
            this.permittedValues = permittedValues.collect(Collectors.toUnmodifiableList());
            this.gridSize = this.permittedValues.size();
            if (new HashSet<>(this.permittedValues).size() != gridSize)
                throw new IllegalArgumentException("Permitted values cannot contain duplicates");
            if (this.permittedValues.contains(emptySquare))
                throw new IllegalArgumentException(
                        "Empty square value cannot be a permitted value");
            this.emptySquare = emptySquare;
        }

        public boolean notEmpty(T value)
        {
            return value != emptySquare;
        }
    }

//    Dimension<T>
}
