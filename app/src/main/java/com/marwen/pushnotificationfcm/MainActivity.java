package com.marwen.pushnotificationfcm;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MainActivity extends AppCompatActivity implements ConnectivityReceiver.ConnectivityReceiverListener {

    private static final int REQUEST_CODE_PERMISSION_SEND_SMS = 123;
    private final static int REQUEST_CODE_PERMISSION_READ_SMS = 456;
    private static final String TAG = "0";
    static final int READ_BLOCK_SIZE = 100;

    String Number, Message;
    String str, mes, dates;
    Context mContext;

    ArrayList<String> smsMsgList = new ArrayList<String>();
    ArrayAdapter arrayAdapter;
    ListView lvSMS;
    Boolean sQUeue = false;

    public static MainActivity instance;
    public static MainActivity Instance(){
        return  instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        instance = this;
        mContext = this;
        checkConnection();


        viewbox(this);

        //PERMISSIONS
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{
                (Manifest.permission.SEND_SMS)},REQUEST_CODE_PERMISSION_SEND_SMS);

        if(checkPermission(Manifest.permission.READ_SMS)){
            refreshInbox();
        }else{
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    (Manifest.permission.READ_SMS)},REQUEST_CODE_PERMISSION_READ_SMS);
        }

        //FIREBASE INSTANCE
        FirebaseInstanceId.getInstance().getInstanceId()
                .addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
                    @Override
                    public void onComplete(@NonNull Task<InstanceIdResult> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "getInstanceId failed", task.getException());
                            return;
                        }

                        String token = task.getResult().getToken();
                        Log.d(TAG, token);
                    }
                });

        //FRAGMENT ROUTING
        BottomNavigationView navigationView = findViewById(R.id.btm_nav);
        navigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {

                switch (menuItem.getItemId()) {
                    case R.id.dashboard:
                        DashFragment dashfragment = new DashFragment();
                        FragmentTransaction dashfragmentTransaction = getSupportFragmentManager().beginTransaction();
                        dashfragmentTransaction.replace(R.id.frame_layout, dashfragment);
                        dashfragmentTransaction.commit();
                        return true;
                    case R.id.sent:
                        SentFragment sentfragment = new SentFragment();
                        FragmentTransaction sentfragmentTransaction = getSupportFragmentManager().beginTransaction();
                        sentfragmentTransaction.replace(R.id.frame_layout, sentfragment);
                        sentfragmentTransaction.commit();
                        return true;
                    case R.id.recieved:
                        ReceivedFragment recfragment = new ReceivedFragment();
                        FragmentTransaction recfragmentTransaction = getSupportFragmentManager().beginTransaction();
                        recfragmentTransaction.replace(R.id.frame_layout, recfragment);
                        recfragmentTransaction.commit();
                        return true;
                }

                return false;
            }
        });
        deleteFile("send12.txt");
        navigationView.setSelectedItemId(R.id.dashboard);
    }

    private void checkConnection() {
        boolean isConnected = ConnectivityReceiver.isConnected();
        showSnack(isConnected);
    }
    private void showSnack(boolean isConnected) {
        if (isConnected) {
            try{
                int itemsCount = lvSMS.getCount();


                if (itemsCount == 0){
                    Toast.makeText(getApplicationContext(),"No data can send, its empty", Toast.LENGTH_SHORT).show();
                }
                else{
                    sendtodatabase();
                }
            }catch (Exception e){
                Toast.makeText(getApplicationContext(),"Errorss " + " " + e, Toast.LENGTH_SHORT).show();
            }


        } else {
            Toast.makeText(getApplicationContext()," No internet connection", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        MyApplication.getInstance().setConnectivityListener(this);
    }

    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        showSnack(isConnected);
    }


    //INTENT BROADCAST FOR RECEIVING DATA
    @Override
    public void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mMessageReceiver,
                new IntentFilter("MyData")
        );
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mMessageReceiver);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(context, intent.getExtras().getString("msg"), Toast.LENGTH_SHORT).show();
            jsonToString(context, intent.getExtras().getString("msg"));
        }
    };

    //SEND FUNCTION
    public void jsonToString(Context context, String jsonData) {
        Toast.makeText(context, "Json Process", Toast.LENGTH_SHORT).show();
        try {
            JSONArray arr = new JSONArray(jsonData);
            for(int i = 0; i < arr.length(); i++){
                writeToFile(context, arr.getJSONObject(i).getString("message") + " : " + arr.getJSONObject(i).getString("number"));
            }
            ReadFile(context);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void writeToFile(Context context, String data) {
        try {
            FileOutputStream fileinput = context.openFileOutput("send12.txt", context.MODE_APPEND);
            OutputStreamWriter outputWriter=new OutputStreamWriter(fileinput);
            outputWriter.append(data +"\n");
            outputWriter.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ReadFile(Context context) {
        Toast.makeText(context, "Read Process", Toast.LENGTH_SHORT).show();
        try {
            FileInputStream fileIn = context.openFileInput("send12.txt");
            InputStreamReader InputRead = new InputStreamReader(fileIn);
            char[] inputBuffer = new char[READ_BLOCK_SIZE];
            String s = "";
            int charRead;

            while ((charRead = InputRead.read(inputBuffer))>0) {
                String readstring = String.copyValueOf(inputBuffer,0,charRead);
                s +=readstring;
            }
            InputRead.close();
            readDataToList(context, s);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readDataToList(Context context, String data) {
        Toast.makeText(context, "List Process", Toast.LENGTH_SHORT).show();

        String[] ListItems;
        ListItems = data.split("\n");

        try {
            //String[] mobileArray = {"Android","IPhone","WindowsMobile","Blackberry", "WebOS","Ubuntu","Windows7","Max OS X"};
            ArrayAdapter adapter = new ArrayAdapter<String>(context, R.layout.sent_listview, ListItems);
            ListView listView = (ListView) findViewById(R.id.sent_listview);
            listView.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        if (!sQUeue) {
//            sendQueue();
//        }
        for (int i = 0; i < ListItems.length ; i++) {
            if (!ListItems[i].isEmpty()) {
                Toast.makeText(context, "Sending : " + ListItems[i], Toast.LENGTH_SHORT).show();
                String[] separated = ListItems[i].split(":");
                String Trig = sendSMS(context, separated[0], separated[1]);
                if (Trig == "Good") {
                    Toast.makeText(getApplicationContext(), "Going to delete", Toast.LENGTH_SHORT).show();
                    removeLineOnTextfile(separated[0] + " : " + separated[1]);
//                    ReadFile(context);
                }
            }
        }
    }

//    public void sendQueue() {
//        sQUeue = true;
//        Queue<String> qe = new LinkedList<String>();
//
//        qe.add("b");
//        qe.add("a");
//        qe.add("c");
//
//        //Traverse queue
//        Iterator it = qe.iterator();
//
//        System.out.println("Initial Size of Queue :" + qe.size());
//        int i = 0;
//        while(it.hasNext())
//        {
//            String iteratorValue = (String) it.next();
//            System.out.println("Queue Next Value :" + iteratorValue + " : " + i);
//            i++;
//        }
//        sQUeue = false;
//    }

    public String sendSMS(final Context context, final String Message, final String Number) {
//        Toast.makeText(getApplicationContext(), "Send Process", Toast.LENGTH_SHORT).show();
        this.Number = Number;
        this.Message = Message;

        String  SENT = "SMS_SENT";
        String  DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(context, 0,
                new Intent(SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0,
                new Intent(DELIVERED), 0);

        registerReceiver(new BroadcastReceiver(){
            public void onReceive(Context arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(context, "MSG SENT",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                        Toast.makeText(context, "Generic failure",
                                Toast.LENGTH_SHORT).show();
//                        removeLineOnTextfile(Message + " : " + Number);
//                        ReadFile(context);
                        break;
                    case SmsManager.RESULT_ERROR_NO_SERVICE:
                        Toast.makeText(getBaseContext(), "No service",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_NULL_PDU:
                        Toast.makeText(getBaseContext(), "Null PDU",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case SmsManager.RESULT_ERROR_RADIO_OFF:
                        Toast.makeText(getBaseContext(), "Radio off",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(SENT));

        //---when the SMS has been delivered---
        registerReceiver(new BroadcastReceiver(){
            public void onReceive(Context  arg0, Intent arg1) {
                switch (getResultCode())
                {
                    case Activity.RESULT_OK:
                        Toast.makeText(getBaseContext(), "SMS delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                    case Activity.RESULT_CANCELED:
                        Toast.makeText(getBaseContext(), "SMS not delivered",
                                Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }, new IntentFilter(DELIVERED));

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(Number, null, Message, sentPI, deliveredPI);
        return "Good";
    }

    class sentReceiver extends BroadcastReceiver {
        public void onReceive(Context context, Intent arg1) {
            switch (getResultCode()) {
                case Activity.RESULT_OK:
                    Toast.makeText(context, "SMS sent",
                            Toast.LENGTH_SHORT).show();
                    removeLineOnTextfile(Message + " : " + Number);
                    Log.d(TAG, Message + " : " + Number);
                    ReadFile(context);
                    break;
                case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                    Toast.makeText(context, "Generic failure",
                            Toast.LENGTH_SHORT).show();
                    removeLineOnTextfile(Message + " : " + Number);
                    Log.d(TAG, Message + " : " + Number);
                    ReadFile(context);
                    break;
                case SmsManager.RESULT_ERROR_NO_SERVICE:
                    Toast.makeText(context, "No service",
                            Toast.LENGTH_SHORT).show();
                    removeLineOnTextfile(Message + " : " + Number);
                    ReadFile(context);
                    break;
                case SmsManager.RESULT_ERROR_NULL_PDU:
                    Toast.makeText(context, "Null PDU",
                            Toast.LENGTH_SHORT).show();
                    removeLineOnTextfile(Message + " : " + Number);
                    ReadFile(context);
                    break;
                case SmsManager.RESULT_ERROR_RADIO_OFF:
                    Toast.makeText(context, "Radio off",
                            Toast.LENGTH_SHORT).show();
                    removeLineOnTextfile(Message + " : " + Number);
                    ReadFile(context);
                    break;
            }


        }
    }

    class deliverReceiver extends BroadcastReceiver {
//        @Override
        public void onReceive(Context context, Intent arg1) {
            switch (getResultCode())
            {
                case Activity.RESULT_OK:
                    Toast.makeText(context, "SMS delivered",
                            Toast.LENGTH_SHORT).show();
                    break;
                case Activity.RESULT_CANCELED:
                    Toast.makeText(context, "SMS not delivered",
                            Toast.LENGTH_SHORT).show();
                    break;
            }

        }
    }

    public void removeLineOnTextfile(String lineToRemove) {
        File inputFile = new File(getFilesDir()+"/send12.txt");
        File tempFile = new File(getFilesDir()+"/send12.txt");

        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

            String currentLine;

            while((currentLine = reader.readLine()) != null) {
                // trim newline when comparing with lineToRemove
                String trimmedLine = currentLine.trim();
                if (!trimmedLine.equals("")) {
                    if(trimmedLine.equals(lineToRemove)) continue;
                    writer.write(currentLine + System.getProperty("line.separator"));
                }
            }
            writer.close();
            reader.close();

//            boolean successful = tempFile.renameTo(inputFile);
//            Toast.makeText(getApplicationContext(), "Delete: " + lineToRemove, Toast.LENGTH_SHORT).show();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //END OF SEND FUNCTION


    //RECEIVING FUNCTION
    private  boolean checkPermission(String permission){
        int checkPermission = ContextCompat.checkSelfPermission(this, permission);
        return checkPermission == PackageManager.PERMISSION_GRANTED;
    }

    public void delete(){
        if (deleteSMS()) {
            refreshInbox();
        } else {
            Toast.makeText(mContext, "Sorry we can't delete messages.", Toast.LENGTH_LONG).show();
            refreshInbox();
        }
    }

    private boolean deleteSMS() {
        boolean isDeleted;
        try {
            mContext.getContentResolver().delete(Uri.parse("content://sms/"), null, null);
            isDeleted = true;
        } catch (Exception ex) {
            isDeleted = false;
        }
        return isDeleted;
    }

    public  void refreshInbox(){
        ContentResolver cResolver = getContentResolver();
        Cursor smsInboxCursor = cResolver.query(Uri.parse("content://sms/inbox"),null,null,null,null);
        int indexBody = smsInboxCursor.getColumnIndex("body");
        int indexAddress = smsInboxCursor.getColumnIndex("address");
        if (indexBody < 0 || !smsInboxCursor.moveToFirst()) return;
        arrayAdapter.clear();
        do{
            String date =  smsInboxCursor.getString(smsInboxCursor.getColumnIndex("date"));
            Long timestamp = Long.parseLong(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestamp);

            Date finaldate = calendar.getTime();

            final String strDateFormate = "dd/MM/yyyy h:mm:ss";

            String mDay = DateFormat.format(strDateFormate, finaldate) + "";

            str = smsInboxCursor.getString(indexAddress) + " : " + smsInboxCursor.getString(indexBody) + " : " + mDay;

            arrayAdapter.add(str);


        }while (smsInboxCursor.moveToNext());
    }

    public void sendtodatabase(){
        try{
            JSONObject obj = null;
            JSONArray jsonArray = new JSONArray();
            ArrayList<String> mylist = new ArrayList<String>();
            int itemsCount = 0;
            Log.d(TAG, "Itemcount: " + itemsCount);

            if (itemsCount == 0){
                String newArrays = mes;
                mylist = new ArrayList<>(getSMS());
                mylist.add(newArrays);
                for (int i = 0; i < mylist.size(); i++) {
                    String[] separated = mylist.get(i).split(" : ");
                    try {
                        obj = new JSONObject();
                        obj.put("number", separated[0]);
                        obj.put("message", separated[1]);
                        obj.put("datetime", separated[2]);
                        jsonArray.put(obj);
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                String jsonStr = jsonArray.toString();
                CharSequence array = jsonStr;
                Toast.makeText(getApplicationContext(), array, Toast.LENGTH_LONG).show();

                Log.d(TAG, "Array to db: " + array);
                Submit((String) array);
            }
            else{
                try{
                        String newArrays = mes;
                        mylist.add(newArrays);
                        ContentResolver cResolver = getContentResolver();
                        Cursor smsInboxCursor = cResolver.query(Uri.parse("content://sms/inbox"),null,null,null,null);
                        int indexBody = smsInboxCursor.getColumnIndex("body");
                        int indexAddress = smsInboxCursor.getColumnIndex("address");
                        String date =  smsInboxCursor.getString(smsInboxCursor.getColumnIndex("date"));
                        smsInboxCursor.moveToFirst();
                        Long timestamp = Long.parseLong(date);
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTimeInMillis(timestamp);
                        Date finaldate = calendar.getTime();

                        if (indexBody < 0 || !smsInboxCursor.moveToFirst())
                            arrayAdapter.clear();

                        final String strDateFormate = "dd/MM/yyyy h:mm ss";

                        String mDay = DateFormat.format(strDateFormate, finaldate) + "";
                        do{
                            str = smsInboxCursor.getString(indexAddress) + " : " + smsInboxCursor.getString(indexBody) + " : " + mDay;
                            arrayAdapter.add(str);
                            String newArray = str;
                            mylist.add(newArray);
                        }while (smsInboxCursor.moveToNext());
                        for (int i = 0; i < mylist.size(); i++) {
                            String[] separated = mylist.get(i).split(" : ");
                            try {
                                obj = new JSONObject();
                                obj.put("number", separated[0]);
                                obj.put("message", separated[1]);
                                obj.put("datetime", separated[2]);
                                jsonArray.put(obj);
                            } catch (JSONException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                    String jsonStr = jsonArray.toString();
                    CharSequence array = jsonStr;
                    Toast.makeText(getApplicationContext(), array, Toast.LENGTH_LONG).show();
                    Log.d(TAG, "Array to db 2: " + array);
                    Submit((String) array);
                }catch (Exception y) {
                    Toast.makeText(getApplicationContext(),"App crashed 1" + " " + y , Toast.LENGTH_SHORT).show();
                }
            }}
        catch (Exception z) {
            Toast.makeText(getApplicationContext(),"App crashed" + " " + z , Toast.LENGTH_SHORT).show();
        }

    }
    private void Submit(String data)
    {
        final String savedata= data;
        String URL="http://172.26.155.198/rest_api/insert_test.php";

        RequestQueue requestQueue = Volley.newRequestQueue(getApplicationContext());
        StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    JSONObject objres=new JSONObject(response);
                    Toast.makeText(getApplicationContext(),"Could not save to database" + objres.toString(), Toast.LENGTH_LONG).show();
                    delete();
                    arrayAdapter.clear();

                } catch (JSONException e) {
                    Toast.makeText(getApplicationContext(),"Save to database",Toast.LENGTH_LONG).show();
//                    delete();
//                    arrayAdapter.clear();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {


                int itemsCount = lvSMS.getCount();

                if (itemsCount == 0){
                    refreshInbox();
                    Toast.makeText(getApplicationContext(),"No internet connection1",Toast.LENGTH_LONG).show();
                }
                else{
                    refreshInbox();
                    String newArrays = mes;
                    arrayAdapter.add(newArrays);
                    Toast.makeText(getApplicationContext(),"No internet connection",Toast.LENGTH_LONG).show();
                }

            }
        }) {
            @Override
            public String getBodyContentType() {
                return "application/json; charset=utf-8";
            }

            @Override
            public byte[] getBody() throws AuthFailureError {
                try {

                    return savedata == null ? null : savedata.getBytes("utf-8");
                } catch (UnsupportedEncodingException uee) {
                    return null;
                }
            }

        };
        requestQueue.add(stringRequest);
    }

    public void updateList (final String smsMsg, int dels , String smsmessage, String smsnumber, String smsdate)
    {

        viewbox(this);
        mes = smsMsg;
        dates = smsdate;
        arrayAdapter.add(smsMsg);
        arrayAdapter.notifyDataSetChanged();
        sendtodatabase();

    }

    public void viewbox(Context context) {
        arrayAdapter = new ArrayAdapter(context,android.R.layout.simple_list_item_1, smsMsgList);
        try {
            lvSMS = (ListView) findViewById(R.id.received_listview);
            lvSMS.setAdapter(arrayAdapter);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public List<String> getSMS(){
        List<String> sms = new ArrayList<String>();
        Uri uriSMSURI = Uri.parse("content://sms/inbox");
        Cursor cur = getContentResolver().query(uriSMSURI, null, null, null, null);

        while (cur != null && cur.moveToNext()) {
            String address = cur.getString(cur.getColumnIndex("address"));
            String body = cur.getString(cur.getColumnIndexOrThrow("body"));
            String date =  cur.getString(cur.getColumnIndex("date"));
            Long timestamp = Long.parseLong(date);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(timestamp);
            Date finaldate = calendar.getTime();
            final String strDateFormate = "dd/MM/yyyy h:mm ss";

            String mDay = DateFormat.format(strDateFormate, finaldate) + "";
            sms.add(address + " : " + body + " : " + mDay);
        }

        if (cur != null) {
            cur.close();
        }
        return sms;
    }



}
