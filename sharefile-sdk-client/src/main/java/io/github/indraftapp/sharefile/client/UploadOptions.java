package io.github.indraftapp.sharefile.client;

import io.github.indraftapp.sharefile.core.model.enums.UploadMethod;
import java.time.Instant;
import lombok.Getter;

/** Upload configuration for the transfer client. */
@Getter
public final class UploadOptions {

  private final UploadMethod method;
  private final int threadCount;
  private final int chunkSizeBytes;
  private final boolean overwrite;
  private final boolean notifyUsers;
  private final boolean autoResume;
  private final int maxResumeAttempts;
  private final Instant clientCreatedDate;
  private final Instant clientModifiedDate;
  private final Integer expirationDays;
  private final TransferProgressListener progressListener;
  private final UploadCallback callback;
  private final String batchId;
  private final boolean batchLast;

  private UploadOptions(Builder builder) {
    this.method = builder.method;
    this.threadCount = builder.threadCount;
    this.chunkSizeBytes = builder.chunkSizeBytes;
    this.overwrite = builder.overwrite;
    this.notifyUsers = builder.notifyUsers;
    this.autoResume = builder.autoResume;
    this.maxResumeAttempts = builder.maxResumeAttempts;
    this.clientCreatedDate = builder.clientCreatedDate;
    this.clientModifiedDate = builder.clientModifiedDate;
    this.expirationDays = builder.expirationDays;
    this.progressListener = builder.progressListener;
    this.callback = builder.callback;
    this.batchId = builder.batchId;
    this.batchLast = builder.batchLast;
  }

  public static UploadOptions defaults() {
    return builder().build();
  }

  public static Builder builder() {
    return new Builder();
  }

  /** Builder for {@link UploadOptions}. */
  public static final class Builder {
    private UploadMethod method;
    private int threadCount = 4;
    private int chunkSizeBytes = 8 * 1024 * 1024;
    private boolean overwrite;
    private boolean notifyUsers;
    private boolean autoResume = true;
    private int maxResumeAttempts = 3;
    private Instant clientCreatedDate;
    private Instant clientModifiedDate;
    private Integer expirationDays;
    private TransferProgressListener progressListener;
    private UploadCallback callback;
    private String batchId;
    private boolean batchLast;

    private Builder() {}

    public Builder method(UploadMethod method) {
      this.method = method;
      return this;
    }

    public Builder threadCount(int threadCount) {
      this.threadCount = threadCount;
      return this;
    }

    public Builder chunkSizeBytes(int chunkSizeBytes) {
      this.chunkSizeBytes = chunkSizeBytes;
      return this;
    }

    public Builder overwrite(boolean overwrite) {
      this.overwrite = overwrite;
      return this;
    }

    public Builder notifyUsers(boolean notifyUsers) {
      this.notifyUsers = notifyUsers;
      return this;
    }

    public Builder autoResume(boolean autoResume) {
      this.autoResume = autoResume;
      return this;
    }

    public Builder maxResumeAttempts(int maxResumeAttempts) {
      this.maxResumeAttempts = maxResumeAttempts;
      return this;
    }

    public Builder clientCreatedDate(Instant clientCreatedDate) {
      this.clientCreatedDate = clientCreatedDate;
      return this;
    }

    public Builder clientModifiedDate(Instant clientModifiedDate) {
      this.clientModifiedDate = clientModifiedDate;
      return this;
    }

    public Builder expirationDays(Integer expirationDays) {
      this.expirationDays = expirationDays;
      return this;
    }

    public Builder progressListener(TransferProgressListener progressListener) {
      this.progressListener = progressListener;
      return this;
    }

    public Builder callback(UploadCallback callback) {
      this.callback = callback;
      return this;
    }

    public Builder batchId(String batchId) {
      this.batchId = batchId;
      return this;
    }

    public Builder batchLast(boolean batchLast) {
      this.batchLast = batchLast;
      return this;
    }

    public UploadOptions build() {
      return new UploadOptions(this);
    }
  }
}
