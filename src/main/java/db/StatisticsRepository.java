package db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class StatisticsRepository {
    private final MongoCollection<Document> collection;

    public StatisticsRepository(MongoClient client, String dbName) {
        MongoDatabase database = client.getDatabase(dbName);
        this.collection = database.getCollection("request_stats");
    }

    public void saveRequest(long id, String method, String url, String timestamp) {
        Document doc = new Document()
                .append("_id", id)
                .append("method", method)
                .append("url", url)
                .append("timestamp", timestamp)
                .append("createdAt", Instant.now().toString());

        try {
            collection.insertOne(doc);
            System.out.println("Saved to MongoDB: id=" + id);
        } catch (Exception e) {
            if (!e.getMessage().contains("duplicate key")) {
                System.err.println("Failed to save request: " + e.getMessage());
            }
        }
    }

    public List<Long> loadAllIds() {
        List<Long> ids = new ArrayList<>();
        try {
            collection.find().forEach(doc -> {
                Object idObj = doc.get("_id");
                if (idObj instanceof Long) {
                    ids.add((Long) idObj);
                } else if (idObj instanceof Integer) {
                    ids.add(((Integer) idObj).longValue());
                }
            });
            System.out.println("Loaded " + ids.size() + " IDs from MongoDB");
        } catch (Exception e) {
            System.err.println("Failed to load IDs: " + e.getMessage());
        }
        return ids;
    }

    public long getTotalRequests() {
        return collection.countDocuments();
    }

    public void clearAll() {
        collection.deleteMany(new Document());
        System.out.println("Cleared all statistics from MongoDB");
    }
}
