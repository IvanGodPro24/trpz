package db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.time.Instant;

public class RequestsRepository {
    private final MongoCollection<Document> collection;

    public RequestsRepository(MongoClient client, String dbName) {
        MongoDatabase database = client.getDatabase(dbName);
        this.collection = database.getCollection("requests");
    }

    public void saveContact(String name, String email, String message, String receivedAt, String clientIp) {
        Document doc = new Document()
                .append("name", name == null ? "" : name)
                .append("email", email == null ? "" : email)
                .append("message", message == null ? "" : message)
                .append("receivedAt", receivedAt == null ? Instant.now().toString() : receivedAt)
                .append("createdAt", Instant.now().toString());

        if (clientIp != null) doc.append("clientIp", clientIp);

        try {
            collection.insertOne(doc);
            System.out.println("Saved contact request to MongoDB: " + doc.get("_id"));
        } catch (Exception e) {
            System.err.println("Failed to save contact request: " + e.getMessage());
        }
    }

    public void clearAll() {
        collection.deleteMany(new Document());
    }

    public long count() {
        return collection.countDocuments();
    }
}

