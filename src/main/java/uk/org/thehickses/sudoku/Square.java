package uk.org.thehickses.sudoku;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Square<V> extends Dimension<V>
{
    private final Set<V> possibleValues = new HashSet<>();

    static String keyFromIndices(int maxIndex, int... indices)
    {
        var maxLength = "%d".formatted(maxIndex)
                .length();
        var numberPattern = "%%0%dd".formatted(maxLength);
        return IntStream.of(indices)
                .mapToObj(numberPattern::formatted)
                .collect(Collectors.joining());
    }

    public Square(int row, int col, Collection<V> values)
    {
        super(1, keyFromIndices(values.size() - 1, row, col));
        this.possibleValues.addAll(values);
    }

    @Override
    protected Set<V> possibleValues()
    {
        if (possibleValues.size() == 1)
            return new HashSet<>();
        return new HashSet<>(possibleValues);
    }

    @Override
    protected Set<V> definiteValues()
    {
        if (possibleValues.size() > 1)
            return new HashSet<>();
        return new HashSet<>(possibleValues);
    }

    @Override
    protected Status currentStatus(V value)
    {
        if (!possibleValues.contains(value))
            return Status.IMPOSSIBLE;
        if (possibleValues.size() == 1)
            return Status.DEFINITE;
        return Status.POSSIBLE;
    }

    @Override
    protected void setStatus(Status status, Stream<V> values)
    {
        if (status == Status.IMPOSSIBLE)
            possibleValues.removeAll(values.toList());
        if (status == Status.DEFINITE)
            possibleValues.retainAll(values.toList());
    }
}
