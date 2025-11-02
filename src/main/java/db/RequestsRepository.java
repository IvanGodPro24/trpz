package db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;
import java.util.Objects;

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

    public boolean updateContact(String id, String name, String email, String message, String receivedAt) {
        if (id == null || id.isBlank()) return false;
        Document set = new Document();
        if (name != null) set.append("name", name);
        if (email != null) set.append("email", email);
        if (message != null) set.append("message", message);
        if (receivedAt != null) set.append("receivedAt", receivedAt);
        set.append("updatedAt", Instant.now().toString());

        try {
            ObjectId oid = tryParseObjectId(id);
            UpdateResult res;

            res = collection.updateOne(Filters.eq("_id", Objects.requireNonNullElse(oid, id)), new Document("$set", set));

            long matched = res.getMatchedCount();
            long modified = res.getModifiedCount();
            System.out.println("UpdateContact id=" + id + " matched=" + matched + " modified=" + modified);
            return matched > 0;
        } catch (Exception e) {
            System.err.println("Failed to update contact id=" + id + " : " + e.getMessage());
            return false;
        }
    }

    public boolean deleteById(String id) {
        if (id == null || id.isBlank()) return false;
        try {
            ObjectId oid = tryParseObjectId(id);
            DeleteResult res;

            res = collection.deleteOne(Filters.eq("_id", Objects.requireNonNullElse(oid, id)));

            long deleted = res.getDeletedCount();
            System.out.println("deleteById id=" + id + " deleted=" + deleted);
            return deleted > 0;
        } catch (Exception e) {
            System.err.println("Failed to delete contact id=" + id + " : " + e.getMessage());
            return false;
        }
    }

    public void clearAll() {
        collection.deleteMany(new Document());
    }

    public long count() {
        return collection.countDocuments();
    }

    private ObjectId tryParseObjectId(String id) {
        try {
            if (ObjectId.isValid(id)) return new ObjectId(id);
        } catch (Exception ignored) {
        }
        return null;
    }
}