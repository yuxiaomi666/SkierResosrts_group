import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

public class LiftRidesDao {
    private final DynamoDbClient dynamoDbClient;
    private final static String TABLE_NAME = "CS6650LiftEvent";


    public LiftRidesDao() {
        this.dynamoDbClient = DynamoDbClient.builder().region(Region.US_WEST_2).build();
    }

    // Make LiftRides Singleton
    private static class SingletonHelper {
        private static final LiftRidesDao INSTANCE = new LiftRidesDao();
    }

    public static LiftRidesDao getInstance() {
        return SingletonHelper.INSTANCE;
    }

    // write lift ride events into DynamoDB
    public void writeLiftRide(Map<String, AttributeValue> item) {
        PutItemRequest putItemRequest = PutItemRequest.builder()
                .tableName(TABLE_NAME)
                .item(item)
                .build();
        try {
            dynamoDbClient.putItem(putItemRequest);
        } catch (DynamoDbException e) {
            e.printStackTrace();
            System.err.println(e.getMessage());
        }
    }
}
