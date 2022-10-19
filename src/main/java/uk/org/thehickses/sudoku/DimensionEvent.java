package uk.org.thehickses.sudoku;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DimensionEvent<V>
{
    public final Set<V> madeDefinite;
    public final Set<V> madeImpossible;

    private static <V> Set<V> copyToSet(Stream<V> values)
    {
        return (values == null ? Stream.<V> empty() : values)
                .collect(Collectors.toUnmodifiableSet());
    }

    public DimensionEvent(Stream<V> definite, Stream<V> impossible)
    {
        this.madeDefinite = copyToSet(definite);
        this.madeImpossible = copyToSet(impossible);
    }

    public DimensionEvent<V> merge(DimensionEvent<V> other)
    {
        if (other == null)
            return this;
        var definite = new HashSet<>(madeDefinite);
        definite.addAll(other.madeDefinite);
        var impossible = new HashSet<>(madeImpossible);
        impossible.addAll(other.madeImpossible);
        return new DimensionEvent<>(definite.stream(), impossible.stream());
    }
}
