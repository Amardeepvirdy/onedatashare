package stork.feather.util;

import java.lang.reflect.Method;
import java.net.UnknownHostException;
import java.time.LocalDateTime;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;


public class MethodLogs {
    static MongoClient mongoClient = null;
    static DB db = null;

    public static DB getDatabase(){
        try {
            if (db == null) {
                mongoClient = new MongoClient("localhost", 27017);
                db = mongoClient.getDB("didc");
            }
        }
        catch(UnknownHostException e){
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return db;
    }

    public static void logMessage (String name, String log_message){
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String reportDate = dateFormat.format(date);


        DBCollection coll = getDatabase().getCollection("logging");

        BasicDBObject doc = new BasicDBObject("Log Type", name)
                .append("Message", log_message).append("Date and Time", reportDate);
        coll.insert(doc);
    }
    }


