package db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.time.Instant;

public class ResponseStatsRepository {
    private final MongoCollection<Document> collection;

    public ResponseStatsRepository(MongoClient client, String dbName) {
        MongoDatabase database = client.getDatabase(dbName);
        this.collection = database.getCollection("response_stats");
    }

    public void saveResponseStat(String method, String path, int status, double durationMs, String timestamp) {
        try {
            if (method == null) method = "";
            if (path == null) path = "";
            String ts = timestamp == null ? Instant.now().toString() : timestamp;
            Document doc = new Document()
                    .append("method", method)
                    .append("path", path)
                    .append("status", status)
                    .append("durationMs", durationMs)
                    .append("timestamp", ts)
                    .append("createdAt", Instant.now().toString());
            collection.insertOne(doc);
        } catch (Exception e) {
            System.err.println("Failed to save response stat: " + e.getMessage());
        }
    }

    public void clearAll() {
        try {
            collection.deleteMany(new Document());
            System.out.println("Cleared response_stats collection");
        } catch (Exception e) {
            System.err.println("Failed to clear response_stats collection: " + e.getMessage());
        }
    }
}
