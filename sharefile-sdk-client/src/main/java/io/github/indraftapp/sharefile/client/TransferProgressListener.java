package io.github.indraftapp.sharefile.client;

/** Callback for observing transfer progress updates. */
@FunctionalInterface
public interface TransferProgressListener {

  void onProgress(long bytesTransferred, long totalBytes);
}
