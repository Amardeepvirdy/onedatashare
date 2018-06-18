package stork.feather.util;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.DBCursor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import org.json.*;
import java.util.*;



public class MethodLogs {

    static MongoClient mongoClient = null;
    static DB db = null;

    /**
     * If the connection to the database has not yet been established
     * This function establishes the connection and returns the
     * DB object to the function that called it.
     */

    public static DB getDatabase(){
            if (db == null) {
                mongoClient = new MongoClient("localhost", 27017);
                db = mongoClient.getDB("didc");
            }

        return db;
    }

    /**
     * This method takes the object from DumpStateThread in String
     * form and parses it to get the information needed to be
     * entered into the database
     */
    public static void storkMessages(String info) throws JSONException {

        JSONObject jsonObj = new JSONObject(info);
        JSONArray dataJsonArray = jsonObj.getJSONArray("scheduler");

        String User = "";
        int Job_ID = -1;

        DBCollection coll = getDatabase().getCollection("jobInformation");

        for(int i=0; i<dataJsonArray.length(); i++) {

            JSONObject dataObj = (JSONObject) dataJsonArray.get(i);

            User = dataObj.getString("owner");
            Job_ID = dataObj.getInt("job_id");

            BasicDBObject andQuery = new BasicDBObject();
            List<BasicDBObject> obj = new ArrayList<BasicDBObject>();
            obj.add(new BasicDBObject("User", User));
            obj.add(new BasicDBObject("Job_ID", Job_ID));
            andQuery.put("$and", obj);
            DBCursor cursor = coll.find(andQuery);

            if (cursor.hasNext()) {
                continue;
            }

            else{

                String  Source = "",Destination="",instant="",average="",status = "",scheduled ="";
                int  Average_Speed = -1,Instant_Speed = -1,attempts = 0;
                long started = 0, completed = 0;

                status = dataObj.getString("status");
                attempts = dataObj.getInt("attempts");
                Source = dataObj.getJSONObject("src").getString("uri");
                Destination = dataObj.getJSONObject("dest").getString("uri");

                Instant_Speed = dataObj.getJSONObject("bytes").getInt("inst");
                Average_Speed = dataObj.getJSONObject("bytes").getInt("avg");

                if (Instant_Speed >= 1000){
                    DecimalFormat df = new DecimalFormat("###.###");
                    instant = df.format(Instant_Speed/1000).toString()+" kB/s";
                }
                else{
                    instant = Instant_Speed+" B/s";
                }
                if (Average_Speed >= 1000){
                    DecimalFormat df = new DecimalFormat("###.###");
                    average = df.format(Average_Speed/1000).toString()+" kB/s";
                }
                else{
                    average = Average_Speed+" B/s";
                }

                started = dataObj.getJSONObject("times").getLong("started");
                completed = dataObj.getJSONObject("times").getLong("completed");
                Long duration = (completed - started)/1000;
                started = started/1000;
                completed = completed/1000;

                String time_duration = Long.toString(duration)+" seconds";
                Date start = new java.util.Date(started*1000L);
                Date finish = new java.util.Date(completed*1000L);
                SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd,yyyy KK:mm:ss a");
                String start_time = sdf.format(start);
                String finish_time = sdf.format(finish);

                JSONObject times = dataObj.getJSONObject("times");


               if (times.has("scheduled")){
                    long scheduled_time = times.getLong("scheduled");
                    scheduled_time = scheduled_time/1000;
                    Date schedule = new java.util.Date(scheduled_time*1000L);
                    scheduled = sdf.format(schedule);
                }
                else{
                    scheduled = "Instantly processed";
                }


                //Inserting extracted information into the database

                BasicDBObject doc = new BasicDBObject("User", User)
                        .append("Job_ID", Job_ID).append("Source", Source).append("Destination", Destination).append("Instant Speed",instant)
                        .append("Average Speed", average).append("Scheduled Time", scheduled).append("Started Time", start_time)
                        .append("Completed time", finish_time).append("Time Duration", time_duration).append("attempts", attempts)
                        .append("status", status);
                coll.insert(doc);
            }
        }

    }
}



