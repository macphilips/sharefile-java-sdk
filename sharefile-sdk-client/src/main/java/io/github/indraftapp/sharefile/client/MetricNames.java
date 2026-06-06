package io.github.indraftapp.sharefile.client;

/** Canonical metric names emitted by the ShareFile SDK client module. */
public final class MetricNames {

  public static final String HTTP_REQUESTS = "sharefile.http.requests";
  public static final String HTTP_REQUESTS_ACTIVE = "sharefile.http.requests.active";
  public static final String HTTP_ERRORS = "sharefile.http.errors";
  public static final String AUTH_TOKEN_REFRESH = "sharefile.auth.token.refresh";
  public static final String AUTH_TOKEN_EXPIRY = "sharefile.auth.token.expiry";
  public static final String RETRY_ATTEMPTS = "sharefile.retry.attempts";
  public static final String TRANSFER_ACTIVE = "sharefile.transfer.active";
  public static final String TRANSFER_UPLOAD_BYTES = "sharefile.transfer.upload.bytes";
  public static final String TRANSFER_UPLOAD_DURATION = "sharefile.transfer.upload.duration";
  public static final String TRANSFER_DOWNLOAD_BYTES = "sharefile.transfer.download.bytes";
  public static final String TRANSFER_DOWNLOAD_DURATION = "sharefile.transfer.download.duration";

  private MetricNames() {}
}
