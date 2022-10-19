package uk.org.thehickses.sudoku;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

public abstract class Dimension<V>
{
    public final int squareCount;
    public final String id;
    private final Set<DimensionListener<V>> listeners = new HashSet<>();

    protected Dimension(int squareCount, String id)
    {
        this.squareCount = squareCount;
        this.id = id;
    }

    public void addListener(DimensionListener<V> listener)
    {
        listeners.add(listener);
    }

    private void fireChange(DimensionEvent<V> event)
    {
        listeners.stream()
                .forEach(l -> l.dimensionChanged(event));
    }

    public void makeDefinite(Stream<V> values)
    {
        var vals = values.toList();
        if (vals.size() > squareCount)
            throw new RuntimeException("Dimension of %d square(s) cannot have &d definite values"
                    .formatted(squareCount, vals.size()));
        changeStatus(Status.DEFINITE, vals.stream());
    }

    public void makeImpossible(Stream<V> values)
    {
        changeStatus(Status.IMPOSSIBLE, values);
    }

    private void changeStatus(Status newStatus, Stream<V> values)
    {
        var toChange = values.filter(changeChecker(newStatus))
                .toList();
        if (toChange.size() == 0)
            return;
        setStatus(newStatus, values);
        var event = new DimensionEvent<>(newStatus == Status.DEFINITE ? toChange.stream() : null,
                newStatus == Status.IMPOSSIBLE ? toChange.stream() : null)
                        .merge(maintainInvariants());
        fireChange(event);
    }

    private Predicate<V> changeChecker(Status newStatus)
    {
        return v ->
            {
                var current = currentStatus(v);
                if (current == newStatus)
                    return false;
                if (current != Status.POSSIBLE)
                    throw new RuntimeException("Cannot change value status from %s to %s"
                            .formatted(current, newStatus));
                return true;
            };
    }

    private DimensionEvent<V> maintainInvariants()
    {
        var definite = definiteValues();
        if (definite.size() > squareCount)
            throw new RuntimeException("Dimension with %d square(s) cannot have %d definite values"
                    .formatted(squareCount, definite.size()));
        var possible = possibleValues();
        var notImpossibleCount = definite.size() + possible.size();
        if (notImpossibleCount < squareCount)
            throw new RuntimeException(
                    "Dimension with %d square(s) cannot have %d non-impossible values"
                            .formatted(squareCount, notImpossibleCount));
        if (definite.size() == squareCount)
        {
            makeImpossible(possible.stream());
            return new DimensionEvent<>(null, possible.stream());
        }
        if (notImpossibleCount == squareCount)
        {
            makeDefinite(possible.stream());
            return new DimensionEvent<>(possible.stream(), null);
        }
        return null;
    }

    protected abstract Set<V> possibleValues();

    protected abstract Set<V> definiteValues();

    protected abstract Status currentStatus(V value);

    protected abstract void setStatus(Status status, Stream<V> values);
}
