package com.example.voicecheck;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

public class IncomingCallsFragment extends Fragment {




    public static IncomingCallsFragment newInstance() {
        IncomingCallsFragment fragment = new IncomingCallsFragment();
        return fragment;
    }



    public IncomingCallsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);


    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {

        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_incoming_calls, container, false);
    }



}