package io.github.indraftapp.sharefile.client;

/** Current lifecycle state for an upload or download transfer. */
public enum TransferState {
  PENDING,
  IN_PROGRESS,
  COMPLETED,
  FAILED,
  CANCELLED
}
