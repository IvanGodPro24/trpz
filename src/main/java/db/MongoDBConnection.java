package db;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.ServerApi;
import com.mongodb.ServerApiVersion;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import io.github.cdimascio.dotenv.Dotenv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MongoDBConnection {
    private static final Logger logger = LoggerFactory.getLogger(MongoDBConnection.class);

    private static MongoClient mongoClient;
    private static String databaseName;

    private MongoDBConnection() {}

    public static synchronized void initialize() {
        if (mongoClient != null) {
            logger.info("MongoDB already initialized");
            return;
        }

        Dotenv dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .load();

        String connectionString = dotenv.get("MONGODB_URI");
        databaseName = dotenv.get("MONGODB_DATABASE");

        if (connectionString == null || connectionString.isEmpty()) {
            logger.error("MONGODB_URI not found in .env file");
            return;
        }

        if (databaseName == null || databaseName.isEmpty()) {
            databaseName = "mydatabase";
            logger.warn("MONGODB_DATABASE not specified, using default: {}", databaseName);
        }

        ServerApi serverApi = ServerApi.builder()
                .version(ServerApiVersion.V1)
                .build();

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .serverApi(serverApi)
                .build();

        try {
            mongoClient = MongoClients.create(settings);
            logger.info("MongoDB initialized successfully. Database: {}", databaseName);
        } catch (MongoException e) {
            logger.error("Failed to initialize MongoDB: {}", e.getMessage(), e);
        }
    }

    public static MongoClient getClient() {
        if (mongoClient == null) {
            logger.error("MongoDB not initialized. Call initialize() first.");
            throw new IllegalStateException("MongoDB not initialized. Call initialize() first.");
        }
        return mongoClient;
    }

    public static String getDatabaseName() {
        return databaseName;
    }

    public static synchronized void close() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            logger.info("MongoDB connection closed successfully");
        }
    }
}