package com.marwen.pushnotificationfcm;

import android.content.Context;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * A simple {@link Fragment} subclass.
 */
public class SentFragment extends Fragment {
    static final int READ_BLOCK_SIZE = 100;

    public SentFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sent, container, false);

        try {
            FileInputStream fileIn = getActivity().openFileInput("send12.txt");
            InputStreamReader InputRead = new InputStreamReader(fileIn);
            char[] inputBuffer = new char[READ_BLOCK_SIZE];
            String s = "";
            int charRead;

            while ((charRead = InputRead.read(inputBuffer))>0) {
                String readstring = String.copyValueOf(inputBuffer,0,charRead);
                s +=readstring;
            }
            InputRead.close();
            String[] ListItems;
            ListItems = s.split("\n");
            ArrayAdapter adapter = new ArrayAdapter<String>(getActivity().getApplicationContext(),
                    R.layout.sent_listview, ListItems);
            ListView listView = (ListView) view.findViewById(R.id.sent_listview);
            listView.setAdapter(adapter);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return view;
    }


}