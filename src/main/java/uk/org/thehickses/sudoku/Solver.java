package uk.org.thehickses.sudoku;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Solver
{
    private static final int[] permittedValues = IntStream.rangeClosed(1, 9)
            .toArray();
    private static final int emptySquare = 0;
    private static final int gridSize = permittedValues.length;
    private static final Box[] boxes = calcBoxes();

    static final Grid puzzle = Grid.with(8, 0, 0, 0, 0, 0, 0, 0, 0)
            .and(0, 0, 3, 6, 0, 0, 0, 0, 0)
            .and(0, 7, 0, 0, 9, 0, 2, 0, 0)
            .and(0, 5, 0, 0, 0, 7, 0, 0, 0)
            .and(0, 0, 0, 0, 4, 5, 7, 0, 0)
            .and(0, 0, 0, 1, 0, 0, 0, 3, 0)
            .and(0, 0, 1, 0, 0, 0, 0, 6, 8)
            .and(0, 0, 8, 5, 0, 0, 0, 1, 0)
            .and(0, 9, 0, 0, 0, 0, 4, 0, 0);

    public static void main(String[] args)
    {
        try
        {
            new Solver().solve(puzzle)
                    .findFirst()
                    .ifPresentOrElse(System.out::println, () ->
                        {
                            throw new RuntimeException("No solution found");
                        });
        }
        catch (Throwable ex)
        {
            System.out.println(ex.getLocalizedMessage());
        }
    }

    private static Dimensions calcBoxSize()
    {
        if (gridSize == 0)
            throw new RuntimeException("Grid size is 0");
        if (hasDuplicates(permittedValues))
            throw new RuntimeException("Permitted values include duplicate(s)");
        if (IntStream.of(permittedValues)
                .anyMatch(i -> i == emptySquare))
            throw new RuntimeException("Empty square value is also a permitted value");
        var squareRoot = Math.sqrt(gridSize);
        var cols = IntStream.iterate((int) Math.ceil(squareRoot), i -> i + 1)
                .filter(i -> gridSize % i == 0)
                .findFirst()
                .getAsInt();
        if (cols == gridSize)
            throw new RuntimeException("Grid size is a prime number");
        return new Dimensions(gridSize / cols, cols);
    }

    private static boolean hasDuplicates(IntStream values)
    {
        return hasDuplicates(values.toArray());
    }

    private static boolean hasDuplicates(int[] arr)
    {
        return arr.length != IntStream.of(arr)
                .distinct()
                .count();
    }

    private static Box[] calcBoxes()
    {
        var boxSize = calcBoxSize();
        var boxTopRows = IntStream.iterate(0, i -> i < gridSize, i -> i + boxSize.rows);
        var boxLeftCols = IntStream.iterate(0, i -> i < gridSize, i -> i + boxSize.cols)
                .toArray();
        var boxTopLefts = boxTopRows.boxed()
                .flatMap(row -> IntStream.of(boxLeftCols)
                        .mapToObj(col -> new Square(row, col)));
        return boxTopLefts.map(
                sq -> new Box(sq, new Square(sq.row + boxSize.rows - 1, sq.col + boxSize.cols - 1)))
                .toArray(Box[]::new);
    }

    public Stream<Grid> solve(Grid grid)
    {
        return grid.validate()
                .emptySquares()
                .findFirst()
                .map(square -> solveAt(grid, square))
                .orElse(Stream.generate(() -> grid)
                        .limit(1));
    }

    private Stream<Grid> solveAt(Grid grid, Square square)
    {
        return grid.allowedValues(square)
                .boxed()
                .flatMap(i -> solve(grid.setValueAt(square, i)));
    }

    public static record Grid(int[][] rows)
    {
        public static Grid with(int... row)
        {
            return new Grid(new int[][] {}).and(row);
        }

        public Grid and(int... row)
        {
            return new Grid(Stream.concat(Stream.of(rows), Stream.generate(() -> row)
                    .limit(1))
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

        public IntStream allowedValues(Square square)
        {
            var blockedValues = Stream
                    .of(rowValues(square.row), colValues(square.col),
                            boxValues(square.containingBox()))
                    .reduce(IntStream::concat)
                    .get()
                    .boxed()
                    .collect(Collectors.toSet());
            return IntStream.of(permittedValues)
                    .filter(i -> !blockedValues.contains(i));
        }

        public IntStream rowValues(int row)
        {
            return IntStream.of(rows[row])
                    .filter(n -> n != emptySquare);
        }

        public IntStream colValues(int col)
        {
            return Stream.of(rows)
                    .mapToInt(r -> r[col])
                    .filter(n -> n != emptySquare);
        }

        public IntStream boxValues(Box box)
        {
            return IntStream.rangeClosed(box.topLeft.row, box.bottomRight.row)
                    .flatMap(r -> IntStream.rangeClosed(box.topLeft.col, box.bottomRight.col)
                            .map(c -> value(r, c)))
                    .filter(n -> n != emptySquare);
        }

        public Stream<Square> emptySquares()
        {
            return IntStream.range(0, rows.length)
                    .boxed()
                    .flatMap(row -> IntStream.range(0, row(row).length)
                            .filter(col -> value(row, col) == emptySquare)
                            .mapToObj(col -> new Square(row, col)));
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

        private Grid validate()
        {
            if (rows.length != gridSize)
                throw new RuntimeException("Wrong number of rows");
            if (Stream.of(rows)
                    .anyMatch(r -> r.length != gridSize))
                throw new RuntimeException("Wrong number of columns");
            var allowedValues = IntStream
                    .concat(IntStream.of(permittedValues), IntStream.of(emptySquare))
                    .boxed()
                    .toList();
            if (!Stream.of(rows)
                    .flatMapToInt(IntStream::of)
                    .allMatch(allowedValues::contains))
                throw new RuntimeException("Invalid cell value");
            if (IntStream.range(0, gridSize)
                    .mapToObj(this::rowValues)
                    .anyMatch(Solver::hasDuplicates))
                throw new RuntimeException("Row has duplicate value(s)");
            if (IntStream.range(0, gridSize)
                    .mapToObj(this::colValues)
                    .anyMatch(Solver::hasDuplicates))
                throw new RuntimeException("Column has duplicate value(s)");
            if (Stream.of(boxes)
                    .map(this::boxValues)
                    .anyMatch(Solver::hasDuplicates))
                throw new RuntimeException("Box has duplicate value(s)");
            return this;
        }
    }

    static record Square(int row, int col)
    {
        public Box containingBox()
        {
            return Stream.of(boxes)
                    .filter(b -> b.contains(this))
                    .findFirst()
                    .get();
        }
    }

    private static record Box(Square topLeft, Square bottomRight)
    {
        public boolean contains(Square square)
        {
            return topLeft.row <= square.row && bottomRight.row >= square.row
                    && topLeft.col <= square.col && bottomRight.col >= square.col;
        }
    }

    private static record Dimensions(int rows, int cols)
    {
    }
}
