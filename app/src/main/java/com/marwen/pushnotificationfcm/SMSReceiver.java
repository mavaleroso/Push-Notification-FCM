package com.marwen.pushnotificationfcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.text.format.DateFormat;

import java.util.Calendar;
import java.util.Date;

public class SMSReceiver extends BroadcastReceiver {
    public static  final String SMS_BUNDLE = "pdus";

    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();

        if(intent.getAction().equalsIgnoreCase("android.provider.Telephony.SMS_RECEIVED")) {
            if (bundle != null) {
                Object[] sms = (Object[]) bundle.get(SMS_BUNDLE);
                String smsMsg = "";
                int smsid=0;

                String smsmessage = "";
                String smsnumber = "";
                String smsdate ="";

                SmsMessage smsMessage;

                for (int i = 0; i < sms.length; i++) {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                        String format = bundle.getString("format");

                        smsMessage = SmsMessage.createFromPdu((byte[]) sms[i], format);
                    }
                    else {
                        smsMessage = SmsMessage.createFromPdu((byte[]) sms[i]);
                    }

                    String msgBody = smsMessage.getMessageBody();
                    String msgAddress = smsMessage.getOriginatingAddress();
                    Long msgDate = smsMessage.getTimestampMillis();

                    int id = smsMessage.getIndexOnIcc();


                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(msgDate);
                    Date finaldate = calendar.getTime();
                    String smsDate = finaldate.toString();

                    Calendar messageTime = Calendar.getInstance();

                    final String strDateFormate = "dd/MM/yyyy h:mm:ss";

                    String mDay = DateFormat.format(strDateFormate, messageTime) + "";


                    smsMsg += msgAddress + " : " + msgBody + " : " + mDay;
                    smsid +=id;

                    smsmessage += msgBody;
                    smsnumber += msgAddress;
                    smsdate += smsDate;

                }
                MainActivity inst = MainActivity.Instance();
                inst.updateList(smsMsg, smsid, smsmessage, smsnumber, smsdate);
            }
        }
    }
}
