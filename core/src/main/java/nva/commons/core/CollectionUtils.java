package nva.commons.core;

import java.util.List;
import java.util.stream.IntStream;

public final class CollectionUtils {

  private CollectionUtils() {}

  /**
   * Splits a list into consecutive sublists, each of the given size (the last may be smaller). The
   * returned sublists are views of the original list, so changes to the original list will be
   * reflected in the partitions.
   *
   * @param items the list to partition
   * @param partitionSize the maximum size of each partition (must be positive)
   * @return an unmodifiable list of consecutive sublists
   */
  public static <T> List<List<T>> partition(List<T> items, int partitionSize) {
    var totalSize = items.size();
    var batchCount = (totalSize + partitionSize - 1) / partitionSize;

    return IntStream.range(0, batchCount)
        .mapToObj(i -> getPartition(items, partitionSize, i))
        .toList();
  }

  private static <T> List<T> getPartition(List<T> items, int partitionSize, int index) {
    var fromIndex = index * partitionSize;
    var toIndex = Math.min(fromIndex + partitionSize, items.size());
    return items.subList(fromIndex, toIndex);
  }
}
