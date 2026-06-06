package io.github.indraftapp.sharefile.core.model.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Package-private recipient shape shared by send-share and request-share payloads. */
record ShareRecipient(@JsonProperty("User") UserReference user) {

  static ShareRecipient fromEmail(String email) {
    return new ShareRecipient(new UserReference(email));
  }

  /** Minimal nested user reference expected by the share APIs. */
  record UserReference(@JsonProperty("Email") String email) {}
}
