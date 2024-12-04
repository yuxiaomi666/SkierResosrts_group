public class RequestData {
  long startTime;
  String requestType;
  long latency;
  int responseCode;

  public RequestData(long startTime, String requestType, long latency, int responseCode) {
    this.startTime = startTime;
    this.requestType = requestType;
    this.latency = latency;
    this.responseCode = responseCode;
  }
}

