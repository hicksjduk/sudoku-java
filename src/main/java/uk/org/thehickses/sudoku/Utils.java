package uk.org.thehickses.sudoku;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Utils
{
    public static class ValuesAndTotals
    {
        public final Set<Integer> values;
        public final Set<Integer> totals;

        public ValuesAndTotals(Set<Integer> values, Set<Integer> totals)
        {
            this.values = values;
            this.totals = totals;
        }
    }

    public static ValuesAndTotals recalculate(int valueCount, Set<Integer> knownValues,
            Set<Integer> possibleValues, Set<Integer> possibleTotals)
    {
        if (valueCount < knownValues.size())
        {
            throw new RuntimeException(String.format("Too many known values (%d) for %d spaces",
                    knownValues.size(), valueCount));
        }
        int baseTotal = knownValues.stream().reduce(0, (a, b) -> a + b);
        Set<Integer> values; 
        Set<Integer> totals;
        if (valueCount == knownValues.size())
        {
            if (possibleTotals.contains(baseTotal))
            {
                totals = getSet(baseTotal);
                values = getSet(knownValues);
            }
            else
            {
                totals = getSet();
                values = getSet();
            }
        }
        else
        {
            values = getSet(knownValues);
            totals = getSet(possibleTotals.stream().filter(t -> t > baseTotal));
            possibleValues.stream().forEach(v -> processPossibleValue(v, valueCount, knownValues,
                    possibleValues, possibleTotals));
        }
        return new ValuesAndTotals(values, totals);
    }
    
    private static void processPossibleValue(int value, int valueCount, Set<Integer> knownValues,
            Set<Integer> possibleValues, Set<Integer> possibleTotals)
    {
        
    }

    private static <T> Set<T> getSet()
    {
        return getSet(Collections.emptySet());
    }

    @SafeVarargs
    private final static <T> Set<T> getSet(T... values)
    {
        return getSet(Arrays.asList(values));
    }

    private static <T> Set<T> getSet(Collection<T> values)
    {
        return new HashSet<>(values);
    }

    private static <T> Set<T> getSet(Stream<T> values)
    {
        return getSet(values.collect(Collectors.toSet()));
    }
}
