package stork.feather.util;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.MongoClient;



import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.DecimalFormat;
import java.util.Date;
import org.json.*;
import java.util.*;


public class MethodLogs {

    static MongoClient mongoClient = null;
    static DB db = null;
    public static HashMap<String, Integer> credential = new HashMap<String, Integer>();

    /**
     * If the connection to the database has not yet been established
     * This function establishes the connection and returns the
     * DB object to the function that called it.
     */

    public static DB getDatabase() throws UnknownHostException {

        if (db == null) {
            mongoClient = new MongoClient("localhost", 27017);
            db = mongoClient.getDB("didc");
        }

        return db;
    }

    /**
     * This method takes the object from DumpStateThread in String form
     * and parses it to get user information
     */

    public static void userInformation(String info) throws JSONException, UnknownHostException {

        JSONObject jsonObj = new JSONObject(info);
        JSONObject dataJsonArray = (JSONObject) jsonObj.get("users");
        DBCollection coll = getDatabase().getCollection("userInformation");

        String username = "", salt = "", validation_token = "", email = "", hash = "";
        long register_moment = -1;
        Boolean validated = false;

        for (int j = 0; j < dataJsonArray.names().length(); j++) {

            username = dataJsonArray.names().getString(j);
            JSONObject dataObj = (JSONObject) dataJsonArray.get(username);
            email = dataObj.getString("email");

            BasicDBObject andQuery = new BasicDBObject();
            List<BasicDBObject> obj = new ArrayList<BasicDBObject>();
            obj.add(new BasicDBObject("UserName", username));
            obj.add(new BasicDBObject("E-mail", email));
            andQuery.put("$and", obj);
            DBCursor cursor = coll.find(andQuery);

            if (cursor.hasNext()) {
                continue;
            } else {


                salt = dataObj.getString("salt");

                validated = dataObj.getBoolean("validated");

                hash = dataObj.getString("hash");
                register_moment = dataObj.getLong("registerMoment");

                JSONObject credObj = (JSONObject) dataObj.get("credentials");
                System.out.println("cred  " + credObj);
                if (credObj.length() == 0) {
                    BasicDBObject doc = new BasicDBObject("UserName", username)
                            .append("salt", salt).append("hash", hash).append("Validated", validated).append("E-mail", email)
                            .append("Register Moment", register_moment);
                    coll.insert(doc);
                } else {

                    BasicDBObject doc = new BasicDBObject("UserName", username)
                            .append("salt", salt).append("hash", hash).append("Validated", validated).append("E-mail", email)
                            .append("Register Moment", register_moment);
                    coll.insert(doc);


                }

            }
        }

    }

    /**
     * This method takes the object from DumpStateThread in String
     * form and parses it to get the information needed to be
     * entered into the database
     */
    public static void credentialMessages(String info) throws JSONException, UnknownHostException {


        DBCollection coll = getDatabase().getCollection("Credential");

        JSONObject jsonObj = new JSONObject(info);
        JSONObject dataJsonArray = (JSONObject) jsonObj.get("users");

        String username = "";
        int no_of_credentials;


        for (int i = 0; i < dataJsonArray.names().length(); i++) {

            username = dataJsonArray.names().getString(i);
            JSONObject dataObj = (JSONObject) dataJsonArray.get(username);
            JSONObject credObj = (JSONObject) dataObj.get("credentials");

            if (credential.containsKey(username) && credObj.length() == 0) {
                continue;
            } else if (credObj.length() == 0) {

                BasicDBObject doc = new BasicDBObject("UserName", username)
                        .append("Credentials", "No credentials stored yet");
                coll.insert(doc);
                credential.put(username, credObj.length());
            }
            else {
                no_of_credentials = credObj.length();
                int value = 0;

                if (credential.containsKey(username)) {
                    value = credential.get(username);
                    if (no_of_credentials == value) {
                        continue;
                    }} else {
                        credential.put(username, credObj.length());

                        String[] uuid = new String[no_of_credentials];
                        String[] name = new String[no_of_credentials];
                        String[] type = new String[no_of_credentials];
                        String[] token = new String[no_of_credentials];

                        BasicDBObject doc = new BasicDBObject("UserName", username);
                        // coll.insert(doc);
                        for (int j = 0; j < no_of_credentials; j++) {
                            uuid[j] = credObj.names().getString(j);
                            name[j] = credObj.getJSONObject(uuid[j]).getString("name");
                            type[j] = credObj.getJSONObject(uuid[j]).getString("type");
                            token[j] = credObj.getJSONObject(uuid[j]).getString("token");

                            doc.append("uuid " + j, uuid[j]).append("name " + j, name[j]).append("type " + j, type[j])
                                    .append("token " + j, token[j]);
                        }
                        coll.insert(doc);
                    }

                }
            }
        }

       public static void storkMessages(String info) throws JSONException, UnknownHostException {

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
                Date start = new Date(started*1000L);
                Date finish = new Date(completed*1000L);
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy KK:mm:ss a");
                String start_time = sdf.format(start);
                String finish_time = sdf.format(finish);

                JSONObject times = dataObj.getJSONObject("times");


               if (times.has("scheduled")){
                    long scheduled_time = times.getLong("scheduled");
                    scheduled_time = scheduled_time/1000;
                    Date schedule = new Date(scheduled_time*1000L);
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

    public static void mailList(String info) throws JSONException, UnknownHostException {

        JSONObject jsonObj = new JSONObject(info);
        DBCollection coll = getDatabase().getCollection("MailList");

        String list = jsonObj.getString("mailList");
        String[] mailList = list.split(" ");
        int length = mailList.length;

        for(int i =0 ;i<length;i++ ){

            DBObject query = new BasicDBObject("id", mailList[i]);
            DBCursor cursor = coll.find(query);

            if(cursor.hasNext()){
                continue;
            }
            else {
                BasicDBObject doc = new BasicDBObject("id",mailList[i]);
                coll.insert(doc);
            }

        }

    }

    public static void administrators(String info) throws JSONException, UnknownHostException {

        JSONObject jsonObj = new JSONObject(info);
        JSONArray administrators = jsonObj.getJSONArray("administrators");
        DBCollection coll = getDatabase().getCollection("Administrators");

        for(int i = 0 ; i < administrators.length() ; i++){

            String admin =  administrators.get(i).toString();
            DBObject query = new BasicDBObject("administrator", admin);
            DBCursor cursor = coll.find(query);

            if(cursor.hasNext()){
                continue;
            }
            else{
                BasicDBObject doc = new BasicDBObject("administrator",admin);
                coll.insert(doc);
            }

        }


    }
}



