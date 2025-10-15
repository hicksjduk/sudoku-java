package uk.org.thehickses.sudoku;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Sudoku
{
    public static Grid solve(Puzzle puzzle) throws RuntimeException
    {
        return puzzle.validate()
                .solve()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No solution found"));
    }

    final static List<Integer> permittedValues = IntStream.rangeClosed(1, 9)
            .boxed()
            .toList();
    final static int emptySquare = 0;
    final static int gridSize = permittedValues.size();

    static <T> List<T> intersection(Collection<T> a, Collection<T> b)
    {
        var answer = new ArrayList<>(a);
        answer.retainAll(b);
        return answer;
    }

    static record Puzzle(List<List<Dimension>> dimensions, Grid grid)
    {
        static final Pattern sudokuPattern = Pattern
                .compile("[%s%d]{%d}".formatted(permittedValues.stream()
                        .map("%d"::formatted)
                        .collect(Collectors.joining()), emptySquare, gridSize));
        static final Pattern killerPattern = Pattern
                .compile("\\S{%d}(?:\\s+\\d+)*".formatted(gridSize));

        Puzzle validate() throws RuntimeException
        {
            return this;
        }

        Stream<Grid> solve()
        {
            return emptySquareData().findFirst()
                    .map(this::solveAt)
                    .orElse(Stream.of(grid));
        }

        Stream<SquareData> emptySquareData()
        {
            return dimensions.stream()
                    .map(Collection::stream)
                    .flatMap(s -> s.limit(1))
                    .sorted()
                    .map(Dimension::emptySquares)
                    .flatMap(List::stream)
                    .map(sq -> new SquareData(sq, containingDimensionData(sq)));
        }

        List<DimensionData> containingDimensionData(Square sq)
        {
            return dimensions.stream()
                    .map(ds -> containingDimensionData(sq, ds))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .toList();
        }

        Optional<DimensionData> containingDimensionData(Square sq, List<Dimension> dimensions)
        {
            return findIndexed(dimensions, d -> d.contains(sq))
                    .map(ip -> new DimensionData(ip.value(), remover(dimensions, ip.index(), sq)));
        }

        Stream<Grid> solveAt(SquareData squareData)
        {
            var allowedValues = squareData.dimensionData()
                    .stream()
                    .map(DimensionData::dimension)
                    .map(Dimension::possibleValues)
                    .reduce(Sudoku::intersection)
                    .orElseGet(List::of)
                    .stream();
            return allowedValues.parallel()
                    .map(v -> withValueAt(squareData, v))
                    .flatMap(Puzzle::solve);
        }

        Puzzle withValueAt(SquareData sq, int value)
        {
            var newGrid = grid.withValueAt(sq.square(), value);
            var newDims = sq.dimensionData()
                    .stream()
                    .map(DimensionData::valueRemover)
                    .map(r -> r.remove(value))
                    .toList();
            return new Puzzle(newDims, newGrid);
        }

        static Puzzle sudokuPuzzle(Grid grid)
        {
            return new Puzzle(standardDimensions(grid), grid);
        }

        static Puzzle sudokuPuzzle(List<String> lines)
        {
            return sudokuPuzzle(Grid.from(lines));
        }

        static Puzzle killerPuzzle(List<String> lines)
        {
            var lineData = lines.stream()
                    .map(l -> l.split("\\s+"))
                    .toList();
            var grid = lineData.stream()
                    .map(a -> a[0])
                    .map(str -> IntStream.range(0, str.length())
                            .mapToObj(i -> str.substring(i, i + 1)))
                    .map(Stream::toList)
                    .toList();
            var regionsInOrder = grid.stream()
                    .flatMap(List::stream)
                    .distinct();
            var regionTotalsInOrder = lineData.stream()
                    .map(Stream::of)
                    .flatMap(s -> s.skip(1))
                    .map(Integer::valueOf);
            var emptyGrid = Grid.empty();
            var regionDimensions = zipWith(
                    (r, t) -> regionDimension(squaresContaining(r, grid).toList(), t),
                    regionsInOrder, regionTotalsInOrder).sorted()
                            .toList();
            var dimensions = Stream
                    .concat(standardDimensions(emptyGrid).stream(), Stream.of(regionDimensions))
                    .toList();
            return new Puzzle(dimensions, emptyGrid);
        }

        static Puzzle from(String str)
        {
            var lines = lines(str).toList();
            if (lines.size() != gridSize)
                throw new RuntimeException("Invalid number of lines");
            if (lines.stream()
                    .allMatch(sudokuPattern.asMatchPredicate()))
                return sudokuPuzzle(lines);
            if (lines.stream()
                    .allMatch(killerPattern.asMatchPredicate()))
                return killerPuzzle(lines);
            throw new RuntimeException("Invalid input");
        }
    }

    static record Grid(List<List<Integer>> rows)
    {
        public Grid withValueAt(Square sq, int value)
        {
            var newRow = replaceOrDeleteValueAt(sq.col(), Optional.of(value), rows.get(sq.row()));
            var newRows = replaceOrDeleteValueAt(sq.row(), Optional.of(newRow), rows);
            return new Grid(newRows);
        }

        static Grid from(String str)
        {
            return Grid.from(lines(str).toList());
        }

        static Grid from(List<String> lines)
        {
            var gridValues = lines.stream()
                    .map(s -> IntStream.range(0, s.length())
                            .mapToObj(i -> s.substring(i, i + 1))
                            .map(Integer::valueOf)
                            .toList())
                    .toList();
            return new Grid(gridValues);
        }

        static Grid empty()
        {
            var rows = Stream.generate(() -> emptySquare)
                    .limit(gridSize)
                    .map(r -> Stream.generate(() -> emptySquare)
                            .limit(gridSize)
                            .toList())
                    .toList();
            return new Grid(rows);
        }
    }

    static record Square(int row, int col)
    {
    }

    static record IntRange(int start, int end)
    {
        public boolean contains(int value)
        {
            return start <= value && end >= value;
        }

        public IntStream values()
        {
            return IntStream.rangeClosed(start, end);
        }
    }

    static record Box(IntRange rows, IntRange cols)
    {
        public boolean contains(Square sq)
        {
            return rows.contains(sq.row()) && cols.contains(sq.col());
        }

        public Stream<Square> squares()
        {
            return rows.values()
                    .boxed()
                    .flatMap(row -> cols.values()
                            .mapToObj(col -> new Square(row, col)));
        }
    }

    static record Dimension(List<Square> emptySquares, List<List<Integer>> combinations,
            DimensionType type) implements Comparable<Dimension>
    {
        static final Comparator<Dimension> COMP = Comparator
                .comparing(Dimension::emptySquares, Comparator.comparingInt(List::size))
                .thenComparing(Dimension::possibleValues, Comparator.comparingInt(List::size));

        @Override
        public int compareTo(Dimension o)
        {
            return COMP.compare(this, o);
        }

        public List<Integer> possibleValues()
        {
            switch (combinations.size())
            {
            case 0:
                return List.of();
            case 1:
                return combinations.get(0);
            default:
                return combinations.stream()
                        .flatMap(List::stream)
                        .distinct()
                        .toList();
            }
        }

        public boolean contains(Square sq)
        {
            return type.squareInDimension(sq, this);
        }
    }

    static interface DimensionType
    {
        boolean squareInDimension(Square sq, Dimension d);
    }

    static DimensionType rowType(int row)
    {
        return (sq, d) -> sq.row() == row;
    }

    static DimensionType colType(int col)
    {
        return (sq, d) -> sq.col() == col;
    }

    static DimensionType boxType(Box box)
    {
        return (sq, d) -> box.contains(sq);
    }

    static DimensionType regionType()
    {
        return (sq, d) -> d.emptySquares()
                .stream()
                .anyMatch(sq::equals);
    }

    static record SquareData(Square square, List<DimensionData> dimensionData)
    {
    }

    static record DimensionData(Dimension dimension, ValueRemover valueRemover)
    {
    }

    static interface ValueRemover
    {
        List<Dimension> remove(int value);
    }

    static List<Dimension> removeValue(List<Dimension> dims, int index, Square sq, int value)
    {
        var newDim = withoutValueAt(sq, value, dims.get(index));
        var newDims = replaceOrDeleteValueAt(index, newDim, dims);
        if (index == 0 || newDim.isEmpty())
            return newDims;
        var split = splitAt(newDims, index + 1);
        return Stream.concat(split.first()
                .sorted(), split.second())
                .toList();
    }

    static Optional<Dimension> withoutValueAt(Square sq, int v, Dimension d)
    {
        if (d.emptySquares()
                .size() == 1)
            return Optional.empty();
        var newSq = new ArrayList<>(d.emptySquares());
        newSq.remove(sq);
        var newCombs = d.combinations()
                .stream()
                .map(c -> deleteIfPresent(v, c))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        return Optional.of(new Dimension(newSq, newCombs, d.type()));
    }

    static <T> List<T> replaceOrDeleteValueAt(int i, Optional<T> v, List<T> values)
    {
        var split = splitAt(values, i);
        return Stream.of(split.first(), v.stream(), split.second()
                .skip(1))
                .flatMap(Function.identity())
                .toList();
    }

    static ValueRemover remover(List<Dimension> dims, int index, Square sq)
    {
        return v -> removeValue(dims, index, sq, v);
    }

    static <T> Optional<List<T>> deleteIfPresent(T value, List<T> values)
    {
        return findIndexed(values, value::equals)
                .map(p -> replaceOrDeleteValueAt(p.index(), Optional.empty(), values));
    }

    static record IndexedPair<T>(int index, T value)
    {
    }

    static <T> Optional<IndexedPair<T>> findIndexed(List<T> values, Predicate<T> predicate)
    {
        return IntStream.range(0, values.size())
                .mapToObj(i -> new IndexedPair<>(i, values.get(i)))
                .filter(p -> predicate.test(p.value()))
                .findFirst();
    }

    static record Pair<T, U>(T first, U second)
    {
    }

    static <T> Pair<Stream<T>, Stream<T>> splitAt(List<T> values, int i)
    {
        return new Pair<>(values.stream()
                .limit(i),
                values.stream()
                        .skip(i));
    }

    static List<List<Dimension>> standardDimensions(Grid grid)
    {
        var rows = IntStream.range(0, gridSize)
                .mapToObj(r -> rowDimension(r, grid));
        var cols = IntStream.range(0, gridSize)
                .mapToObj(c -> colDimension(c, grid));
        var boxes = Stream.of(calcBoxes())
                .map(b -> boxDimension(b, grid));
        return Stream.of(rows, cols, boxes)
                .map(Stream::sorted)
                .map(Stream::toList)
                .toList();
    }

    static Dimension rowDimension(int row, Grid grid)
    {
        var squares = IntStream.range(0, gridSize)
                .mapToObj(col -> new Square(row, col));
        return standardDimension(squares, grid, rowType(row));
    }

    static Dimension colDimension(int col, Grid grid)
    {
        var squares = IntStream.range(0, gridSize)
                .mapToObj(row -> new Square(row, col));
        return standardDimension(squares, grid, colType(col));
    }

    static Dimension boxDimension(Box box, Grid grid)
    {
        var squares = box.squares();
        return standardDimension(squares, grid, boxType(box));
    }

    static Dimension regionDimension(List<Square> emptySquares, int total)
    {
        return new Dimension(emptySquares, combinations(emptySquares.size(), total).toList(),
                regionType());
    }

    static Stream<List<Integer>> combinations(int count, int total)
    {
        if (count == 0)
            return Stream.empty();
        if (count == 1)
            return permittedValues.contains(total) ? Stream.of(List.of(total)) : Stream.empty();
        return permittedValues.stream()
                .flatMap(v -> combinations(count - 1, total - v).filter(c -> !c.contains(v))
                        .map(c -> Stream.concat(c.stream(), Stream.of(v))
                                .toList()));
    }

    static Dimension standardDimension(Stream<Square> squares, Grid grid, DimensionType type)
    {
        var sqByValue = squares.collect(Collectors.groupingBy(sq -> grid.rows()
                .get(sq.row())
                .get(sq.col())));
        var emptySquares = sqByValue.getOrDefault(emptySquare, List.of());
        var prohibitedValues = sqByValue.keySet()
                .stream()
                .filter(v -> v != emptySquare)
                .toList();
        var allowedValues = new ArrayList<>(permittedValues);
        allowedValues.removeAll(prohibitedValues);
        return new Dimension(emptySquares, List.of(allowedValues), type);
    }

    static record BoxSize(int rows, int cols)
    {
    }

    static BoxSize boxSize()
    {
        var cols = IntStream.rangeClosed((int) Math.ceil(Math.sqrt(gridSize)), gridSize)
                .filter(i -> gridSize % i == 0)
                .findFirst()
                .getAsInt();
        var rows = gridSize / cols;
        return new BoxSize(rows, cols);
    }

    static Box[] calcBoxes()
    {
        var boxSize = boxSize();
        var boxHeight = boxSize.rows();
        var boxWidth = boxSize.cols();
        return IntStream.iterate(0, i -> i < gridSize, i -> i + boxHeight)
                .boxed()
                .flatMap(r -> IntStream.iterate(0, i -> i < gridSize, i -> i + boxWidth)
                        .mapToObj(c -> new Box(new IntRange(r, r + boxHeight - 1),
                                new IntRange(c, c + boxWidth - 1))))
                .toArray(Box[]::new);
    }

    static Stream<String> lines(String str)
    {
        return str.lines()
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty));
    }

    static <T> Stream<Square> squaresContaining(T value, List<List<T>> grid)
    {
        return IntStream.range(0, grid.size())
                .mapToObj(r -> new IndexedPair<>(r, grid.get(r)))
                .flatMap(pair -> IntStream.range(0, pair.value()
                        .size())
                        .filter(c -> pair.value()
                                .get(c)
                                .equals(value))
                        .mapToObj(c -> new Square(pair.index(), c)));
    }

    static <T, U, V> Stream<V> zipWith(BiFunction<T, U, V> f, Stream<T> as, Stream<U> bs)
    {
        var aIt = as.iterator();
        return bs.takeWhile(x -> aIt.hasNext())
                .map(b -> f.apply(aIt.next(), b));
    }
}
