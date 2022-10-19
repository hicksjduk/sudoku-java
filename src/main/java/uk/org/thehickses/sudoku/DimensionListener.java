package uk.org.thehickses.sudoku;

@FunctionalInterface
public interface DimensionListener<V>
{
    void dimensionChanged(DimensionEvent<V> event);
}
