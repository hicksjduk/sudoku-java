package uk.org.thehickses.sudoku;

import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Solver
{
    private static int[] permittedValues = IntStream.rangeClosed(1, 9)
            .toArray();
    private static int emptySquare = 0;
    private static int gridSize = permittedValues.length;
    private static Box[] boxes = calcBoxes();

    public static void main(String[] args)
    {
        var puzzle = Grid.with(8, 0, 0, 0, 0, 0, 0, 0, 0)
                .and(0, 0, 3, 6, 0, 0, 0, 0, 0)
                .and(0, 7, 0, 0, 9, 0, 2, 0, 0)
                .and(0, 5, 0, 0, 0, 7, 0, 0, 0)
                .and(0, 0, 0, 0, 4, 5, 7, 0, 0)
                .and(0, 0, 0, 1, 0, 0, 0, 3, 0)
                .and(0, 0, 1, 0, 0, 0, 0, 6, 8)
                .and(0, 0, 8, 5, 0, 0, 0, 1, 0)
                .and(0, 9, 0, 0, 0, 0, 4, 0, 0);
        new Solver().solve(puzzle)
                .findFirst()
                .ifPresent(System.out::println);
    }

    private static Coords calcBoxSize()
    {
        var squareRoot = Math.sqrt(gridSize);
        var cols = IntStream.iterate((int) Math.ceil(squareRoot), i -> i + 1)
                .filter(i -> gridSize % i == 0)
                .findFirst()
                .getAsInt();
        return new Coords(gridSize / cols, cols);
    }

    private static Box[] calcBoxes()
    {
        var boxSize = calcBoxSize();
        var boxTopRows = IntStream.iterate(0, i -> i < gridSize, i -> i + boxSize.row);
        var boxLeftCols = IntStream.iterate(0, i -> i < gridSize, i -> i + boxSize.col)
                .toArray();
        var boxTopLefts = boxTopRows.boxed()
                .flatMap(row -> IntStream.of(boxLeftCols)
                        .mapToObj(col -> new Coords(row, col)));
        return boxTopLefts.map(
                sq -> new Box(sq, new Coords(sq.row + boxSize.row - 1, sq.col + boxSize.col - 1)))
                .toArray(Box[]::new);
    }

    Stream<Grid> solve(Grid grid)
    {
        return grid.emptySquares()
                .findFirst()
                .map(square -> solveAt(grid, square))
                .orElse(Stream.generate(() -> grid)
                        .limit(1));
    }

    Stream<Grid> solveAt(Grid grid, Coords square)
    {
        return grid.allowedValues(square)
                .boxed()
                .flatMap(i -> solve(grid.setValueAt(square, i)));
    }

    record Grid(int[][] rows)
    {
        static Grid with(int... row)
        {
            return new Grid(new int[][] {}).and(row);
        }

        Grid and(int... row)
        {
            return new Grid(Stream.concat(Stream.of(rows), Stream.generate(() -> row)
                    .limit(1))
                    .toArray(int[][]::new));
        }

        int[] row(int rowIndex)
        {
            return rows[rowIndex];
        }

        int value(int rowIndex, int colIndex)
        {
            return row(rowIndex)[colIndex];
        }

        int value(Coords square)
        {
            return value(square.row, square.col);
        }

        Grid setValueAt(Coords square, int value)
        {
            var newRow = IntStream.of(row(square.row))
                    .toArray();
            newRow[square.col] = value;
            var newGrid = Stream.of(rows)
                    .toArray(int[][]::new);
            newGrid[square.row] = newRow;
            return new Grid(newGrid);
        }

        IntStream allowedValues(Coords square)
        {
            var blockedValues = Stream
                    .of(rowValues(square.row), colValues(square.col), boxValues(square))
                    .reduce(IntStream::concat)
                    .get()
                    .boxed()
                    .collect(Collectors.toSet());
            return IntStream.of(permittedValues)
                    .filter(i -> !blockedValues.contains(i));
        }

        IntStream rowValues(int row)
        {
            return IntStream.of(rows[row])
                    .filter(n -> n != emptySquare);
        }

        IntStream colValues(int col)
        {
            return Stream.of(rows)
                    .mapToInt(r -> r[col])
                    .filter(n -> n != emptySquare);
        }

        IntStream boxValues(Coords square)
        {
            var box = square.containingBox();
            return IntStream.rangeClosed(box.topLeft.row, box.bottomRight.row)
                    .flatMap(r -> IntStream.rangeClosed(box.topLeft.col, box.bottomRight.col)
                            .map(c -> value(r, c)))
                    .filter(n -> n != emptySquare);
        }

        Stream<Coords> emptySquares()
        {
            return IntStream.range(0, rows.length)
                    .boxed()
                    .flatMap(row -> IntStream.range(0, row(row).length)
                            .filter(col -> value(row, col) == emptySquare)
                            .mapToObj(col -> new Coords(row, col)));
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

    record Coords(int row, int col)
    {
        Box containingBox()
        {
            return Stream.of(boxes)
                    .filter(b -> b.contains(this))
                    .findFirst()
                    .get();
        }
    }

    record Box(Coords topLeft, Coords bottomRight)
    {
        boolean contains(Coords square)
        {
            return topLeft.row <= square.row && bottomRight.row >= square.row
                    && topLeft.col <= square.col && bottomRight.col >= square.col;
        }
    }
}
