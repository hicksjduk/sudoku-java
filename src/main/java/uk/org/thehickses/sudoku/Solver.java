package uk.org.thehickses.sudoku;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Solver
{
    static final Solver defaultSolver = new Solver(IntStream.rangeClosed(1, 9), 0);

    static final Grid puzzle = defaultSolver.gridWith(8, 0, 0, 0, 0, 0, 0, 0, 0)
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
        defaultSolver.solve(puzzle)
                .findFirst()
                .ifPresentOrElse(System.out::println,
                        () -> System.out.println("No solution found"));
    }

    private final Structure structure;

    private static Dimensions calcBoxSize(int gridSize)
    {
        var squareRoot = Math.sqrt(gridSize);
        var cols = IntStream.rangeClosed((int) Math.ceil(squareRoot), gridSize)
                .filter(i -> gridSize % i == 0)
                .findFirst()
                .getAsInt();
        return new Dimensions(gridSize / cols, cols);
    }

    private static Box[] calcBoxes(int gridSize)
    {
        var boxSize = calcBoxSize(gridSize);
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

    public Solver()
    {
        this(IntStream.rangeClosed(1, 9), 0);
    }

    public Solver(IntStream permittedValues, int emptySquare)
    {
        structure = new Structure(permittedValues, emptySquare);
    }

    public Stream<Grid> solve(Grid grid)
    {
        return grid.emptySquares()
                .findFirst()
                .map(square -> solveAt(grid, square))
                .orElse(Stream.of(grid));
    }

    private Stream<Grid> solveAt(Grid grid, Square square)
    {
        return grid.allowedValues(square)
                .boxed()
                .flatMap(i -> solve(grid.setValueAt(square, i)));
    }

    public Grid gridWith(int... row)
    {
        return new Grid(new int[][] {}, structure).and(row);
    }

    public static class Grid
    {
        private final int[][] rows;
        private final Structure structure;
        
        private Grid(int[][] rows, Structure structure)
        {
            this.rows = rows;
            this.structure = structure;
        }

        public Grid and(int... row)
        {
            return new Grid(Stream.concat(Stream.of(rows), Stream.generate(() -> row)
                    .limit(1))
                    .toArray(int[][]::new), structure);
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
            return new Grid(newGrid, structure);
        }

        public IntStream allowedValues(Square square)
        {
            var blockedValues = Stream
                    .of(rowValues(square.row), colValues(square.col),
                            boxValues(structure.boxContaining(square)))
                    .reduce(IntStream::concat)
                    .get()
                    .boxed()
                    .collect(Collectors.toSet());
            return IntStream.of(structure.permittedValues)
                    .filter(i -> !blockedValues.contains(i));
        }

        public IntStream rowValues(int row)
        {
            return IntStream.of(rows[row])
                    .filter(n -> n != structure.emptySquare);
        }

        public IntStream colValues(int col)
        {
            return Stream.of(rows)
                    .mapToInt(r -> r[col])
                    .filter(n -> n != structure.emptySquare);
        }

        public IntStream boxValues(Box box)
        {
            return IntStream.rangeClosed(box.topLeft.row, box.bottomRight.row)
                    .flatMap(r -> IntStream.rangeClosed(box.topLeft.col, box.bottomRight.col)
                            .map(c -> value(r, c)))
                    .filter(n -> n != structure.emptySquare);
        }

        public Stream<Square> emptySquares()
        {
            return IntStream.range(0, rows.length)
                    .boxed()
                    .flatMap(row -> IntStream.range(0, row(row).length)
                            .filter(col -> value(row, col) == structure.emptySquare)
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
    }

    static record Square(int row, int col)
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

    private static record Dimensions(int rows, int cols)
    {
    }

    private static class Structure
    {
        public final int[] permittedValues;
        public final int emptySquare;
        public final int gridSize;
        public final Box[] boxes;

        private Structure(IntStream permittedValues, int emptySquare)
        {
            this.permittedValues = permittedValues.toArray();
            this.emptySquare = emptySquare;
            this.gridSize = this.permittedValues.length;
            this.boxes = calcBoxes(gridSize);
        }

        public Box boxContaining(Square square)
        {
            return Stream.of(boxes)
                    .filter(b -> b.contains(square))
                    .findFirst()
                    .get();
        }
    }
}
