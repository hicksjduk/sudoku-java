package uk.org.thehickses.sudoku;

import java.util.Arrays;
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
        var puzzle = new int[][] { new int[] { 8, 0, 0, 0, 0, 0, 0, 0, 0 },
                new int[] { 0, 0, 3, 6, 0, 0, 0, 0, 0 }, new int[] { 0, 7, 0, 0, 9, 0, 2, 0, 0 },
                new int[] { 0, 5, 0, 0, 0, 7, 0, 0, 0 }, new int[] { 0, 0, 0, 0, 4, 5, 7, 0, 0 },
                new int[] { 0, 0, 0, 1, 0, 0, 0, 3, 0 }, new int[] { 0, 0, 1, 0, 0, 0, 0, 6, 8 },
                new int[] { 0, 0, 8, 5, 0, 0, 0, 1, 0 }, new int[] { 0, 9, 0, 0, 0, 0, 4, 0, 0 } };
        new Solver().solve(puzzle)
                .findFirst()
                .map(Solver::toString)
                .ifPresent(System.out::println);
    }

    static String toString(int[][] grid)
    {
        return Stream.of(grid)
                .map(Arrays::toString)
                .collect(Collectors.joining("\n"));
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

    Stream<int[][]> solve(int[][] grid)
    {
        return emptySquares(grid).findFirst()
                .map(square -> solveAt(grid, square))
                .orElse(Stream.generate(() -> grid)
                        .limit(1));
    }

    Stream<Coords> emptySquares(int[][] grid)
    {
        return IntStream.range(0, grid.length)
                .boxed()
                .flatMap(row -> IntStream.range(0, grid[row].length)
                        .filter(col -> grid[row][col] == emptySquare)
                        .mapToObj(col -> new Coords(row, col)));
    }

    Stream<int[][]> solveAt(int[][] grid, Coords square)
    {
        return allowedValues(grid, square).boxed()
                .flatMap(i -> solve(setValueAt(grid, square, i)));
    }

    int[][] setValueAt(int[][] grid, Coords square, int value)
    {
        var newRow = IntStream.of(grid[square.row])
                .toArray();
        newRow[square.col] = value;
        var answer = Stream.of(grid)
                .toArray(int[][]::new);
        answer[square.row] = newRow;
        return answer;
    }

    IntStream allowedValues(int[][] grid, Coords square)
    {
        var blockedValues = Stream
                .of(rowValues(grid, square.row), colValues(grid, square.col),
                        boxValues(grid, square))
                .reduce(IntStream::concat)
                .get()
                .boxed()
                .collect(Collectors.toSet());
        return IntStream.of(permittedValues)
                .filter(i -> !blockedValues.contains(i));
    }

    IntStream rowValues(int[][] grid, int row)
    {
        return IntStream.of(grid[row])
                .filter(n -> n != emptySquare);
    }

    IntStream colValues(int[][] grid, int col)
    {
        return Stream.of(grid)
                .mapToInt(r -> r[col])
                .filter(n -> n != emptySquare);
    }

    IntStream boxValues(int[][] grid, Coords square)
    {
        var box = square.containingBox();
        return IntStream.rangeClosed(box.topLeft.row, box.bottomRight.row)
                .flatMap(r -> IntStream.rangeClosed(box.topLeft.col, box.bottomRight.col)
                        .map(c -> grid[r][c]))
                .filter(n -> n != emptySquare);
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
