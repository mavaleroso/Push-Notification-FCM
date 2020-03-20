package com.marwen.pushnotificationfcm;

import android.Manifest;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;


/**
 * A simple {@link Fragment} subclass.
 */
public class ReceivedFragment extends Fragment {

    private String str;

    ArrayList<String> smsMsgList = new ArrayList<String>();
    ArrayAdapter arrayAdapter;


    public ReceivedFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_recieved, container, false);

        arrayAdapter = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_1, smsMsgList);
        ListView lvSMS = (ListView) view.findViewById(R.id.received_listview);
        lvSMS.setAdapter(arrayAdapter);
        refreshInbox();

        return view;
    }

    public  void refreshInbox(){
        ContentResolver cResolver = getActivity().getContentResolver();
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

            str = smsInboxCursor.getString(indexAddress)+" : "+
                    smsInboxCursor.getString(indexBody) + " : " + mDay;

            arrayAdapter.add(str);
        }while (smsInboxCursor.moveToNext());
    }

}
