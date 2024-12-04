import io.swagger.client.*;
import io.swagger.client.api.SkiersApi;
import io.swagger.client.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class SkierClient {
  final static private int TOTAL_THREADS = 150;
  final static private int NUM_THREADS_PHASE_1 = 32;
  final static private int NUM_THREADS_PHASE_2 = 105;
  private static final int NUM_REQUESTS_PHASE_1 = 1000;
  private static final int NUM_REQUESTS_PHASE_2 = 1600;


  public static void main(String[] args) throws InterruptedException {
    AtomicInteger successCounter = new AtomicInteger(0);
    AtomicInteger UnSuccessCounter = new AtomicInteger(0);
    ConcurrentLinkedQueue<Long> latencyDataList = new ConcurrentLinkedQueue<>();
    ExecutorService executor = Executors.newFixedThreadPool(TOTAL_THREADS);
    long startTime = System.currentTimeMillis();

    multiThreadLiftRideEvents(NUM_REQUESTS_PHASE_1, NUM_THREADS_PHASE_1, successCounter,
        UnSuccessCounter, executor, latencyDataList);
    multiThreadLiftRideEvents(NUM_REQUESTS_PHASE_2, NUM_THREADS_PHASE_2, successCounter,
          UnSuccessCounter, executor, latencyDataList);

    executor.shutdown();
    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

    long endTime = System.currentTimeMillis();
    long timeTaken = endTime - startTime;
    double throughput =
        (double) successCounter.get() / timeTaken * 1000;

    System.out.println("Number of threads used in phase 2: " + NUM_THREADS_PHASE_2);
    System.out.println("Total number of successful requests sent: " + successCounter.get());
    System.out.println("Total number of unsuccessful requests: " + UnSuccessCounter.get());
    System.out.println("Total run time: " + timeTaken);
    System.out.println("Total throughput(request per second): " + String.format("%.1f",
        throughput));
    printStatistics(latencyDataList);
  }
  public static void multiThreadLiftRideEvents(int numberRequest, int numberOfThread,
      AtomicInteger successCounter, AtomicInteger UnSuccessCounter,
      ExecutorService executor, ConcurrentLinkedQueue<Long> latencyDataList)
      throws InterruptedException {
    CountDownLatch completed = new CountDownLatch(numberOfThread);

    for (int i = 0; i < numberOfThread; i++) {
      executor.submit(() -> singleThreadLiftRideEvents(numberRequest, completed,
          successCounter, UnSuccessCounter, latencyDataList));
    }
    completed.await();
  }

  public static void singleThreadLiftRideEvents(int numberRequest,
    CountDownLatch completed,
    AtomicInteger successCounter, AtomicInteger UnSuccessCounter, ConcurrentLinkedQueue<Long> latencyDataList){
    SkiersApi apiInstance = new SkiersApi();
    // local
//      apiInstance.getApiClient().setBasePath("http://localhost:8080/skiers_servlet");
    //ec2
    apiInstance.getApiClient().setBasePath("http://54.149.90.214:8080/skierServlet_war");
    // ALB
//    apiInstance.getApiClient().setBasePath("http://cs6650-1264764045.us-west-2.elb.amazonaws.com/skierServlet_war");

    for (int i = 0; i < numberRequest; i++){
      Integer resortID = generateRandomResortID(); // Integer | ID of the resort of interest
      String seasonID = "2024"; // String | ID of the season of interest
      String dayID = "1"; // String | ID of the day of interest
      Integer skierID = generateRandomSkierID(); // Integer | ID of the resort of interest
      Integer time = generateRandomTime(); // Integer | time of interest
      Integer liftID = generateRandomLiftID(); // Integer | ID of the skier of interest
      LiftRide body = new LiftRide()
              .time(time)
              .liftID(liftID);
      long startTimeSingleRequest = System.currentTimeMillis();
      try {
        apiInstance.writeNewLiftRide(body, resortID, seasonID, dayID, skierID);
        long endTimeSingleRequest = System.currentTimeMillis();
        long latency = endTimeSingleRequest - startTimeSingleRequest;
        latencyDataList.add(latency);
        successCounter.getAndIncrement();
      } catch (ApiException e) {
        UnSuccessCounter.getAndIncrement();
        System.err.println("Exception when calling SkiersApi#writeNewLiftRide");
        System.err.println("Status Code: " + e.getCode());
        System.err.println("Response Body: " + e.getResponseBody());
        e.printStackTrace();
      }
    }
    completed.countDown();
  }

  public static void printStatistics(ConcurrentLinkedQueue<Long> latencyDataList){
    // convert to List and sort the latency data
    List<Long> latencies = latencyDataList.stream()
        .sorted()
        .collect(Collectors.toList());
    double mean = latencies.stream().mapToDouble(d -> d).average().orElse(0.0);
    double median = latencies.size() % 2 == 0 ?
        (latencies.get(latencies.size() / 2 - 1) + latencies.get(latencies.size() / 2)) / 2.0 :
        latencies.get(latencies.size() / 2);
    double min = latencies.get(0);
    double max = latencies.get(latencies.size() - 1);
    int index = (int) Math.ceil(99.0 / 100.0 * latencies.size()) - 1;
    double p99 = latencies.get(Math.min(index, latencies.size() - 1));

    System.out.println("\n----- Statistics of requests above------");
    System.out.println("Mean response time (ms): " + String.format("%.1f", mean));
    System.out.println("Median response time (ms): " + median);
    System.out.println("Min response time (ms): " + min);
    System.out.println("Max response time (ms): " + max);
    System.out.println("P99 response time (ms): " + p99);
  }

  public static Integer generateRandomResortID() {
    Random random = new Random();
    int randomNumber = 1 + random.nextInt(10);
    return randomNumber;
  }
  public static Integer generateRandomSkierID() {
    Random random = new Random();
    int randomNumber = 1 + random.nextInt(10000);
    return randomNumber;
  }

  public static Integer generateRandomLiftID() {
    Random random = new Random();
    int randomNumber = 1 + random.nextInt(40);
    return randomNumber;
  }

  public static Integer generateRandomTime() {
    Random random = new Random();
    int randomNumber = 1 + random.nextInt(360);
    return randomNumber;
  }
}
