package io.github.indraftapp.sharefile.client;

/** Download configuration for the transfer client. */
public final class DownloadOptions {

  private final boolean includeAllVersions;
  private final boolean includeDeleted;
  private final TransferProgressListener progressListener;

  private DownloadOptions(Builder builder) {
    this.includeAllVersions = builder.includeAllVersions;
    this.includeDeleted = builder.includeDeleted;
    this.progressListener = builder.progressListener;
  }

  public static DownloadOptions defaults() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public boolean isIncludeAllVersions() {
    return includeAllVersions;
  }

  public boolean isIncludeDeleted() {
    return includeDeleted;
  }

  public TransferProgressListener getProgressListener() {
    return progressListener;
  }

  /** Builder for {@link DownloadOptions}. */
  public static final class Builder {
    private boolean includeAllVersions;
    private boolean includeDeleted;
    private TransferProgressListener progressListener;

    private Builder() {}

    public Builder includeAllVersions(boolean includeAllVersions) {
      this.includeAllVersions = includeAllVersions;
      return this;
    }

    public Builder includeDeleted(boolean includeDeleted) {
      this.includeDeleted = includeDeleted;
      return this;
    }

    public Builder progressListener(TransferProgressListener progressListener) {
      this.progressListener = progressListener;
      return this;
    }

    public DownloadOptions build() {
      return new DownloadOptions(this);
    }
  }
}
