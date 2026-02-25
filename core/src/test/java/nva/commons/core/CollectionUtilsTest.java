package nva.commons.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class CollectionUtilsTest {

    @Test
    void partitionSplitsListIntoEqualSizedParts() {
        var result = CollectionUtils.partition(List.of(1, 2, 3, 4, 5, 6), 3);
        assertEquals(2, result.size());
        assertEquals(List.of(1, 2, 3), result.get(0));
        assertEquals(List.of(4, 5, 6), result.get(1));
    }

    @Test
    void partitionHandlesLastPartitionBeingSmaller() {
        var result = CollectionUtils.partition(List.of(1, 2, 3, 4, 5), 3);
        assertEquals(2, result.size());
        assertEquals(List.of(1, 2, 3), result.get(0));
        assertEquals(List.of(4, 5), result.get(1));
    }

    @Test
    void partitionReturnsSinglePartitionWhenSizeFitsExactly() {
        var result = CollectionUtils.partition(List.of(1, 2, 3), 3);
        assertEquals(1, result.size());
        assertEquals(List.of(1, 2, 3), result.get(0));
    }

    @Test
    void partitionReturnsSinglePartitionWhenSizeIsLargerThanList() {
        var result = CollectionUtils.partition(List.of(1, 2), 10);
        assertEquals(1, result.size());
        assertEquals(List.of(1, 2), result.get(0));
    }

    @Test
    void partitionReturnsEmptyListForEmptyInput() {
        var result = CollectionUtils.partition(List.of(), 5);
        assertTrue(result.isEmpty());
    }

    @Test
    void partitionWithSizeOneReturnsSingleElementPartitions() {
        var result = CollectionUtils.partition(List.of("a", "b", "c"), 1);
        assertEquals(3, result.size());
        assertEquals(List.of("a"), result.get(0));
        assertEquals(List.of("b"), result.get(1));
        assertEquals(List.of("c"), result.get(2));
    }

    @Test
    void partitionThrowsOnZeroSize() {
        assertThrows(ArithmeticException.class, () -> CollectionUtils.partition(List.of(1), 0));
    }
}
