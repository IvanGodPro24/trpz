package db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.time.Instant;

public class MetricsRepository {
    private final MongoCollection<Document> collection;

    public MetricsRepository(MongoClient client, String dbName) {
        MongoDatabase database = client.getDatabase(dbName);
        this.collection = database.getCollection("metrics");
    }

    public void saveSnapshot(long totalRequests, double avgResponseMs, double rps) {
        try {
            String now = Instant.now().toString();
            Document doc = new Document()
                    .append("timestamp", now)
                    .append("totalRequests", totalRequests)
                    .append("avgResponseMs", avgResponseMs)
                    .append("rps", rps)
                    .append("createdAt", Instant.now().toString());
            collection.insertOne(doc);
            System.out.println("Saved metrics snapshot to MongoDB: totalRequests=" + totalRequests
                    + " avgMs=" + String.format("%.2f", avgResponseMs) + " rps=" + String.format("%.2f", rps));
        } catch (Exception e) {
            System.err.println("Failed to save metrics snapshot: " + e.getMessage());
        }
    }

    public void clearAll() {
        try {
            collection.deleteMany(new Document());
            System.out.println("Cleared metrics collection");
        } catch (Exception e) {
            System.err.println("Failed to clear metrics collection: " + e.getMessage());
        }
    }
}
