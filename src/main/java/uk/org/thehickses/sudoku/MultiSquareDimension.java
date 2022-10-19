package uk.org.thehickses.sudoku;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MultiSquareDimension<V> extends Dimension<V>
{
    public static String keyFrom(Square<?>... squares)
    {
        return Stream.of(squares)
                .map(s -> s.id)
                .sorted()
                .collect(Collectors.joining());
    }

    private final Set<V> possibleValues = new HashSet<>();
    private final Set<V> definiteValues = new HashSet<>();
    final List<Square<V>> squares;

    @SuppressWarnings("unchecked")
    public MultiSquareDimension(Collection<V> values, Square<V>... squares)
    {
        super(squares.length, keyFrom(squares));
        this.squares = Stream.of(squares)
                .collect(Collectors.toUnmodifiableList());
        if (values.size() == squares.length)
            definiteValues.addAll(values);
        else
            possibleValues.addAll(values);
    }

    @Override
    protected Set<V> possibleValues()
    {
        return new HashSet<>(possibleValues);
    }

    @Override
    protected Set<V> definiteValues()
    {
        return new HashSet<>(definiteValues);
    }

    @Override
    protected Status currentStatus(V value)
    {
        if (possibleValues.contains(value))
            return Status.POSSIBLE;
        if (definiteValues.contains(value))
            return Status.DEFINITE;
        return Status.IMPOSSIBLE;
    }

    @Override
    protected void setStatus(Status status, Stream<V> values)
    {
        var vals = values.toList();
        possibleValues.removeAll(vals);
        if (status == Status.DEFINITE)
            definiteValues.addAll(vals);
    }
}
