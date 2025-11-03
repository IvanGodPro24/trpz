package db;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class PeersRepository {
    private final MongoCollection<Document> collection;

    public PeersRepository(MongoClient client, String dbName) {
        MongoDatabase database = client.getDatabase(dbName);
        this.collection = database.getCollection("peers");
    }

    public void savePeer(String address) {
        if (address == null || address.isBlank()) return;
        try {
            Document doc = new Document("address", address);

            Document exists = collection.find(Filters.eq("address", address)).first();
            if (exists == null) {
                collection.insertOne(doc);
                System.out.println("Saved peer: " + address);
            }
        } catch (Exception e) {
            System.err.println("Failed to save peer: " + e.getMessage());
        }
    }

    public void deletePeer(String address) {
        if (address == null || address.isBlank()) return;
        try {
            var res = collection.deleteOne(Filters.eq("address", address));
            long deleted = res.getDeletedCount();
            System.out.println("deletePeer " + address + " deleted=" + deleted);
        } catch (Exception e) {
            System.err.println("Failed to delete peer: " + e.getMessage());
        }
    }

    public List<String> loadAll() {
        List<String> out = new ArrayList<>();
        try {
            collection.find().forEach(doc -> {
                Object a = doc.get("address");
                if (a != null) out.add(a.toString());
            });
        } catch (Exception e) {
            System.err.println("Failed to load peers: " + e.getMessage());
        }
        return out;
    }

    public void clearAll() {
        collection.deleteMany(new Document());
    }
}
