package com.example.nirvana.Patients;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.nirvana.Adapter.RecyclerView_Adapter;
import com.example.nirvana.Adapter.Upcoming_Patient_Meetings_Adapter;
import com.example.nirvana.R;
import com.example.nirvana.StartMeetingActivity;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link UpcomingMeetings#newInstance} factory method to
 * create an instance of this fragment.
 */
public class UpcomingMeetings extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    public View view1;
    public RecyclerView recyclerView;
    Upcoming_Patient_Meetings_Adapter upcoming_patient_meetings_adapter;
    public ArrayList<String> LinkList,NameList,TypeList,DateList,TimeList,RecieverIdList,KeyList;
    public String Id;
    TextView textView;
    public UpcomingMeetings() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment UpcomingMeetings.
     */
    // TODO: Rename and change types and number of parameters
    public static UpcomingMeetings newInstance(String param1, String param2) {
        UpcomingMeetings fragment = new UpcomingMeetings();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        Bundle bundle=getArguments();
        Id=bundle.getString("Id");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view1= inflater.inflate(R.layout.fragment_upcoming_meetings, container, false);
        LinkList=new ArrayList<>();
        NameList=new ArrayList<>();
        TypeList=new ArrayList<>();
        DateList=new ArrayList<>();
        TimeList=new ArrayList<>();
        KeyList=new ArrayList<>();
        RecieverIdList=new ArrayList<>();
        textView=view1.findViewById(R.id.textView);
        recyclerView=view1.findViewById(R.id.upcoming_patientlists);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager=new LinearLayoutManager(getActivity());
        recyclerView.setLayoutManager(linearLayoutManager);
        retrieveData();
        return view1;
    }
    public void retrieveData()
    {
        FirebaseDatabase firebaseDatabase=FirebaseDatabase.getInstance();
        DatabaseReference databaseReference=firebaseDatabase.getReference().child("Patient_Meetings").child(Id);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists())
                {
                    HashMap<String,Object> hashMap= (HashMap<String, Object>) snapshot.getValue();
                    for(String key:hashMap.keySet())
                    {
                       HashMap<String,Object>hashMap1= (HashMap<String, Object>) hashMap.get(key);
                       for(String key1:hashMap1.keySet())
                       {
                           Object data=hashMap1.get(key1);
                           HashMap<String,Object> userData=(HashMap<String,Object>)data;
                           String complete=userData.get("complete").toString();
                           if(complete.equals("0"))
                           {
                               String Did=userData.get("Did").toString();
                               String name=userData.get("d_name").toString();
                               String link=userData.get("link").toString();
                               String date=userData.get("date").toString();
                               String time=userData.get("time").toString();
                               String type=userData.get("d_bio").toString();
                               RecieverIdList.add(Did);
                               NameList.add(name);
                               LinkList.add(link);
                               DateList.add(date);
                               TimeList.add(time);
                               TypeList.add(type);
                               KeyList.add(key1);
                               initRecyclerView();
                           }
                       }
                    }
                }
                else
                {
                    textView.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void initRecyclerView() {
        upcoming_patient_meetings_adapter =new Upcoming_Patient_Meetings_Adapter(getActivity(),NameList,TypeList,DateList,TimeList,LinkList);
        recyclerView.setAdapter(upcoming_patient_meetings_adapter);
        upcoming_patient_meetings_adapter.setOnItemClickListener(new Upcoming_Patient_Meetings_Adapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                Intent intent=new Intent(getActivity(), StartMeetingActivity.class);
                intent.putExtra("SenderId",Id);
                intent.putExtra("ReceiverId",RecieverIdList.get(position));
                intent.putExtra("UserName",NameList.get(position));
                intent.putExtra("Who","Patient");
                intent.putExtra("link",LinkList.get(position));
                intent.putExtra("key",KeyList.get(position));
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.slide_out_bottom,R.anim.no_animation);
            }
        });
    }
}