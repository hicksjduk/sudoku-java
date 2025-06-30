package uk.org.thehickses.sudoku;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import uk.org.thehickses.sudoku.Solver.Grid;

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

    public static class Grid
    {
        public static Grid with(int... row)
        {
            return new Grid(new int[][] { row });
        }
    
        private final int[][] rows;
    
        private Grid(int[][] rows)
        {
            this.rows = rows;
        }
    
        public Grid and(int... row)
        {
            return new Grid(Stream.concat(Stream.of(rows), Stream.of(row))
                    .toArray(int[][]::new));
        }
    
        public int[] row(int rowIndex)
        {
            return rows[rowIndex];
        }
    
        public int value(int rowIndex, int colIndex)
        {
            return row(rowIndex)[colIndex];
        }
    
        public Grid setValueAt(Square square, int value)
        {
            var newRow = IntStream.of(row(square.row))
                    .toArray();
            newRow[square.col] = value;
            var newGrid = Stream.of(rows)
                    .toArray(int[][]::new);
            newGrid[square.row] = newRow;
            return new Grid(newGrid);
        }
    
        @Override
        public String toString()
        {
            return Stream.of(rows)
                    .map(row -> IntStream.of(row)
                            .mapToObj("%d"::formatted)
                            .collect(Collectors.joining(" ")))
                    .collect(Collectors.joining("\n"));
        }
    }

    public static record Square(int row, int col)
    {
    }

    public static record Dimension<T> (List<Square> emptySquares, List<List <T>> combinations)
    {
        public boolean contains(Square sq)
        {
            return emptySquares.contains(sq);
        }
    }
    
    public static record Puzzle<T> (List<List<Dimension<T>>> dimensionsByType, Grid grid)
    {
        
    }
}
