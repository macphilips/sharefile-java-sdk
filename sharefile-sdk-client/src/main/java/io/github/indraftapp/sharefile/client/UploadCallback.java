package io.github.indraftapp.sharefile.client;

import io.github.indraftapp.sharefile.core.model.response.UploadResult;

/** Callback for observing upload lifecycle events. */
public interface UploadCallback {

  default void onStarted(TransferProgress progress) {}

  default void onProgress(TransferProgress progress) {}

  default void onCompleted(UploadResult result) {}

  default void onFailed(Throwable error) {}

  default void onCancelled() {}
}
