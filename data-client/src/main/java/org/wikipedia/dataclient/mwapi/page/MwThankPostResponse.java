package org.wikipedia.dataclient.mwapi.page;


import org.wikipedia.dataclient.mwapi.MwResponse;

public class MwThankPostResponse extends MwResponse {
  private Result result;

  public Result getResult() {
    return result;
  }

  public void setResult(Result result) {
    this.result = result;
  }

  public class Result {
    private Integer success;
    private String recipient;

    public Integer getSuccess() {
      return success;
    }

    public void setSuccess(Integer success) {
      this.success = success;
    }

    public String getRecipient() {
      return recipient;
    }

    public void setRecipient(String recipient) {
      this.recipient = recipient;
    }

  }
}
