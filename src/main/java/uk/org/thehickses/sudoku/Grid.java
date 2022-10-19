package uk.org.thehickses.sudoku;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Grid<V>
{
    private final Collection<V> values;
    private final List<List<Square<V>>> squares;
    private final Map<String, Dimension<V>> dimensions;

    public static void main(String[] args)
    {
        var g = new Grid<>(IntStream.rangeClosed(1, 9));
        System.out.println(g.dimensions.size());
    }

    @SuppressWarnings("unchecked")
    public Grid(V... values)
    {
        this.values = Arrays.asList(values);
        squares = IntStream.range(0, values.length)
                .mapToObj(row -> IntStream.range(0, values.length)
                        .mapToObj(col -> new Square<>(row, col, this.values))
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
        dimensions = squares.stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(s -> s.id, s -> s));
        createTopLevelDimensions();
    }

    private void createTopLevelDimensions()
    {
        squares.stream()
                .map(Collection::stream)
                .forEach(this::dimensionWith);
        var size = squares.size();
        IntStream.of(0, size)
                .mapToObj(col -> squares.stream()
                        .map(row -> row.get(col)))
                .forEach(this::dimensionWith);
        var rowsPerBox = IntStream.iterate((int) Math.sqrt(squares.size()), i -> i > 1, i -> i - 1)
                .filter(i -> size % i == 0)
                .findFirst()
                .orElse(1);
        var rowsByBox = IntStream.range(0, size)
                .boxed()
                .collect(Collectors.groupingBy(i -> i / rowsPerBox))
                .values();
        var colsPerBox = size / rowsPerBox;
        var colsByBox = IntStream.range(0, size)
                .boxed()
                .collect(Collectors.groupingBy(i -> i / colsPerBox))
                .values();
        rowsByBox.stream()
                .forEach(br -> colsByBox.stream()
                        .forEach(bc ->
                            {
                                var boxSquares = br.stream()
                                        .flatMap(row -> bc.stream()
                                                .map(squares.get(row)::get));
                                dimensionWith(boxSquares);
                            }));
    }

    @SuppressWarnings("unchecked")
    private Dimension<V> dimensionWith(Stream<Square<V>> squares)
    {
        var sq = (Square<V>[]) squares.toArray();
        if (sq.length == 1)
            return sq[0];
        var key = MultiSquareDimension.keyFrom(sq);
        return dimensions.computeIfAbsent(key, k -> createDimension(sq));
    }

    @SuppressWarnings("unchecked")
    private MultiSquareDimension<V> createDimension(Square<V>... squares)
    {
        var answer = new MultiSquareDimension<>(values, squares);
        linkSubdimensions(answer);
        return answer;
    }

    private void linkSubdimensions(MultiSquareDimension<V> parent)
    {
        List<Square<V>> sq = parent.squares;
        IntStream.range(1, (2 ^ sq.size() - 1))
                .forEach(n ->
                    {
                        Iterator<Dimension<V>> it = IntStream.of(0, sq.size())
                                .boxed()
                                .collect(Collectors.partitioningBy(i -> (n & (2 ^ i)) == 0))
                                .values()
                                .stream()
                                .map(Collection::stream).map(s -> s.map(sq::get))
                                .map(this::dimensionWith)
                                .iterator();
                        var child1 = it.next();
                        var child2 = it.next();
                        parent.addListener(parentChangeProcessor(child1, child2));
                        child1.addListener(childChangeListener(parent, child2));
                        child2.addListener(childChangeListener(parent, child1));
                    });
    }

    private DimensionListener<V> parentChangeProcessor(Dimension<V> child1, Dimension<V> child2)
    {
        return evt ->
            {
                child1.makeDefinite(evt.madeDefinite.stream()
                        .filter(v -> child2.currentStatus(v) == Status.IMPOSSIBLE));
                child2.makeDefinite(evt.madeDefinite.stream()
                        .filter(v -> child1.currentStatus(v) == Status.IMPOSSIBLE));
                child1.makeImpossible(evt.madeImpossible.stream());
                child2.makeImpossible(evt.madeImpossible.stream());
            };
    }

    private DimensionListener<V> childChangeListener(Dimension<V> parent, Dimension<V> otherChild)
    {
        return evt ->
            {
                parent.makeDefinite(evt.madeDefinite.stream());
                otherChild.makeImpossible(evt.madeDefinite.stream());
                parent.makeImpossible(evt.madeImpossible.stream()
                        .filter(v -> otherChild.currentStatus(v) == Status.IMPOSSIBLE));
                otherChild.makeDefinite(evt.madeImpossible.stream()
                        .filter(v -> parent.currentStatus(v) == Status.DEFINITE));
            };
    }
}
