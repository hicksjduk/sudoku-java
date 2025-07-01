package uk.org.thehickses.sudoku;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
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

        public boolean isEmpty(T value)
        {
            return value == emptySquare;
        }

        public boolean notEmpty(T value)
        {
            return !isEmpty(value);
        }

        private Stream<Box> calcBoxes()
        {
            var boxSize = calcBoxSize();
            var boxTopRows = IntStream.iterate(0, i -> i < gridSize, i -> i + boxSize.rows);
            var boxLeftCols = IntStream.iterate(0, i -> i < gridSize, i -> i + boxSize.cols)
                    .toArray();
            var boxTopLefts = boxTopRows.boxed()
                    .flatMap(row -> IntStream.of(boxLeftCols)
                            .mapToObj(col -> new Square(row, col)));
            return boxTopLefts.map(sq -> new Box(sq,
                    new Square(sq.row + boxSize.rows - 1, sq.col + boxSize.cols - 1)));
        }

        private Dimensions calcBoxSize()
        {
            var squareRoot = Math.sqrt(gridSize);
            var cols = IntStream.rangeClosed((int) Math.ceil(squareRoot), gridSize)
                    .filter(i -> gridSize % i == 0)
                    .findFirst()
                    .getAsInt();
            return new Dimensions(gridSize / cols, cols);
        }
    }

    public static class Grid<T>
    {
        @SuppressWarnings("unchecked")
        public static <T> Grid<T> with(T... row)
        {
            return new Grid<>((T[][]) Stream.of(row)
                    .toArray());
        }

        private final T[][] rows;

        private Grid(T[][] rows)
        {
            this.rows = rows;
        }

        @SuppressWarnings("unchecked")
        public Grid<T> and(T... row)
        {
            return new Grid<>((T[][]) Stream.concat(Stream.of(rows), Stream.of(row))
                    .toArray());
        }

        public T[] row(int rowIndex)
        {
            return rows[rowIndex];
        }

        public T valueAt(int rowIndex, int colIndex)
        {
            return row(rowIndex)[colIndex];
        }

        public T valueAt(Square sq)
        {
            return valueAt(sq.row(), sq.col());
        }

        @SuppressWarnings("unchecked")
        public Grid<T> setValueAt(Square square, T value)
        {
            var newRow = (T[]) Stream.of(row(square.row))
                    .toArray();
            newRow[square.col] = value;
            var newGrid = (T[][]) Stream.of(rows)
                    .toArray();
            newGrid[square.row] = newRow;
            return new Grid<>(newGrid);
        }

        public Stream<Square> filterSquares(Predicate<T> predicate)
        {
            return IntStream.range(0, rows.length)
                    .boxed()
                    .flatMap(r -> IntStream.range(0, rows[r].length)
                            .filter(c -> predicate.test(rows[r][c]))
                            .mapToObj(c -> new Square(r, c)));
        }

        @Override
        public String toString()
        {
            return Stream.of(rows)
                    .map(row -> Stream.of(row)
                            .map("%s"::formatted)
                            .collect(Collectors.joining(" ")))
                    .collect(Collectors.joining("\n"));
        }
    }

    private static record Dimensions(int rows, int cols)
    {
    }

    private static record Box(Square topLeft, Square bottomRight)
    {
        public boolean contains(Square square)
        {
            return topLeft.row <= square.row && bottomRight.row >= square.row
                    && topLeft.col <= square.col && bottomRight.col >= square.col;
        }
    }

    public static record Square(int row, int col)
    {
    }

    public static record Dimension<T>(List<Square> emptySquares, List<List<T>> combinations)
    {
        public boolean contains(Square sq)
        {
            return emptySquares.contains(sq);
        }
    }

    public static record Puzzle<T>(List<List<Dimension<T>>> dimensionsByType, Grid grid)
    {
    }
    
    public static <T> List<List<Dimension<T>>> standardDimensions(Structure<T> str, Grid<T> g)
    {
        return List.of(rowDimensions(str, g), columnDimensions(str, g), boxDimensions(str, g));
    }

    public static <T> List<Dimension<T>> rowDimensions(Structure<T> str, Grid<T> g)
    {
        var squares = IntStream.range(0, str.gridSize)
                .mapToObj(r -> IntStream.range(0, str.gridSize)
                        .mapToObj(c -> new Square(r, c)));
        return squares.map(sqs -> standardDimension(sqs, str, g))
                .filter(Objects::nonNull)
                .toList();
    }

    public static <T> List<Dimension<T>> columnDimensions(Structure<T> str, Grid<T> g)
    {
        var squares = IntStream.range(0, str.gridSize)
                .mapToObj(c -> IntStream.range(0, str.gridSize)
                        .mapToObj(r -> new Square(r, c)));
        return squares.map(sqs -> standardDimension(sqs, str, g))
                .filter(Objects::nonNull)
                .toList();
    }

    public static <T> List<Dimension<T>> boxDimensions(Structure<T> str, Grid<T> g)
    {
        var squares = str.calcBoxes()
                .map(b -> IntStream.range(b.topLeft()
                        .row(),
                        b.bottomRight()
                                .row())
                        .boxed()
                        .flatMap(r -> IntStream.range(b.topLeft()
                                .col(),
                                b.bottomRight()
                                        .col())
                                .mapToObj(c -> new Square(r, c))));
        return squares.map(sqs -> standardDimension(sqs, str, g))
                .filter(Objects::nonNull)
                .toList();
    }

    public static <T> Dimension<T> standardDimension(Stream<Square> squares, Structure<T> str,
            Grid<T> g)
    {
        var squaresByEmptiness = squares
                .collect(Collectors.partitioningBy(sq -> str.isEmpty(g.valueAt(sq))));
        var emptySquares = squaresByEmptiness.get(true);
        if (emptySquares == null)
            return null;
        var existingValues = squaresByEmptiness.getOrDefault(false, List.of())
                .stream()
                .map(g::valueAt);
        var possibleValues = new ArrayList<>(str.permittedValues);
        possibleValues.removeAll(existingValues.toList());
        return new Dimension<>(emptySquares, List.of(possibleValues));
    }
}
