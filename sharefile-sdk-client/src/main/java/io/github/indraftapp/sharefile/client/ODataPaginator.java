package io.github.indraftapp.sharefile.client;

import io.github.indraftapp.sharefile.core.model.ODataFeed;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Lazy paginator for OData feeds backed by {@code odata.nextLink}. */
final class ODataPaginator<T> implements Iterable<T> {

  private final Supplier<ODataFeed<T>> firstPageSupplier;
  private final Function<ODataFeed<T>, ODataFeed<T>> nextPageFetcher;

  ODataPaginator(
      Supplier<ODataFeed<T>> firstPageSupplier,
      Function<ODataFeed<T>, ODataFeed<T>> nextPageFetcher) {
    this.firstPageSupplier =
        Objects.requireNonNull(firstPageSupplier, "firstPageSupplier must not be null");
    this.nextPageFetcher =
        Objects.requireNonNull(nextPageFetcher, "nextPageFetcher must not be null");
  }

  Stream<T> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  @Override
  public Iterator<T> iterator() {
    return new Iterator<>() {
      private ODataFeed<T> currentPage;
      private List<T> currentItems = List.of();
      private int currentIndex;
      private boolean exhausted;

      @Override
      public boolean hasNext() {
        if (exhausted) {
          return false;
        }
        ensurePageLoaded();
        while (currentIndex >= currentItems.size()) {
          if (currentPage == null || !currentPage.hasNextPage()) {
            exhausted = true;
            return false;
          }
          currentPage = nextPageFetcher.apply(currentPage);
          currentItems = currentPage.getItems();
          currentIndex = 0;
        }
        return true;
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        return currentItems.get(currentIndex++);
      }

      private void ensurePageLoaded() {
        if (currentPage == null) {
          currentPage = firstPageSupplier.get();
          currentItems = currentPage.getItems();
          currentIndex = 0;
        }
      }
    };
  }

  @Override
  public Spliterator<T> spliterator() {
    return Spliterators.spliteratorUnknownSize(
        iterator(), Spliterator.ORDERED | Spliterator.NONNULL);
  }
}
