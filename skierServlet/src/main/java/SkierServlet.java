import com.google.gson.Gson;
import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.rabbitmq.client.*;

@WebServlet(value = "/skiers/*")
public class SkierServlet extends HttpServlet {
  private static Connection connection;
  private static BlockingQueue<Channel> channelPool;
  private static final int POOL_SIZE = 10;

  public void init() throws ServletException {
    super.init();
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("54.245.22.9");

    try {
      connection = factory.newConnection();
      channelPool = new ArrayBlockingQueue<>(POOL_SIZE);
      for (int i = 0; i < POOL_SIZE; i++) {
        channelPool.add(connection.createChannel());
      }
    } catch (Exception e) {
      throw new ServletException("Failed to create RabbitMQ connection or channel pool", e);
    }
  }
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    res.setContentType("text/plain");
    String urlPath = req.getPathInfo();

    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write("missing paramterers");
      return;
    }

    String[] urlParts = urlPath.split("/");
    if (!isUrlValid(urlParts)) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } else {
      res.setStatus(HttpServletResponse.SC_OK);
      res.getWriter().write("It works!");
    }
  }

  protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    res.setContentType("application/json");
    String urlPath = req.getPathInfo();

    Gson gson = new Gson();
    LiftRide liftRide = gson.fromJson(req.getReader(), LiftRide.class);

    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write("missing parameters");
      return;
    }

    String[] urlParts = urlPath.split("/");
    if (urlParts.length != 8) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write("Some of resortID, seasonID, dayID, skierID is missing");
      return;
    }

    Integer resortID = Integer.parseInt(urlParts[1]);
    Integer seasonID = Integer.parseInt(urlParts[3]);
    Integer dayID = Integer.parseInt(urlParts[5]);
    Integer skierID = Integer.parseInt(urlParts[7]);

    if (!isPOSTRequestValid(resortID, seasonID, dayID, skierID, liftRide)) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write("Some of resortID, seasonID, dayID, skierID is not valid. "
        + "please try again!");
    } else {
      FullLiftRide fullLiftRide = new FullLiftRide()
              .liftID(liftRide.getLiftID())
              .time(liftRide.getTime())
              .resortID(resortID)
              .seasonID(seasonID)
              .dayID(dayID)
              .skierID(skierID);

      Channel channel = null;
      try {
        channel = channelPool.take();
        String queueName = "lift_rides";
        channel.queueDeclare(queueName, true, false, false, null);
        String message = gson.toJson(fullLiftRide);
        channel.basicPublish("", queueName, null, message.getBytes());
        res.setStatus(HttpServletResponse.SC_OK);
        res.getWriter().write("Received and processed data for POST request!");
        System.out.println("Success POST request!"); // for logging and debugging
      } catch (Exception e) {
        System.out.println(e.getMessage());
        res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        res.getWriter().write("Failed to publish message");
      } finally {
        if (channel != null) {
          try {
            channelPool.put(channel);
          } catch (InterruptedException e) {
            throw new RuntimeException(e);
          }
        }
      }
    }
  }

  public void destroy() {
    super.destroy();
    try {
      for (Channel ch : channelPool) {
        if (ch != null) {
          ch.close();
        }
      }
      connection.close();
    } catch (Exception e) {
      System.err.println("Failed to close channels or connection: " + e.getMessage());
    }
  }

  private boolean isUrlValid(String[] urlPath) {
    return true;
  }

  // Validate whether resortID, seasonID, dayID, skierID is within certain range:
  //  resortID - between 1 and 10
  //  seasonID - 2024
  //  dayID - 1
  //  skierID - between 1 and 100000
  //  liftId - between 1 and 40
  //  time - between 1 and 360
  private boolean isPOSTRequestValid(Integer resortID, Integer seasonID, Integer dayID, Integer skierID, LiftRide liftRide) {
    // POST API: /skiers/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}
    // urlPath  = "/1/seasons/2019/day/1/skier/123"
    // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
    if (resortID < 1 || resortID > 10 || seasonID != 2024 || dayID != 1 || skierID < 1 || skierID > 10000 || liftRide.getLiftID() < 1 || liftRide.getLiftID() > 40 || liftRide.getTime() < 1 || liftRide.getTime() > 360) {
      return false;
    } else {
      return true;
    }
  }
}
