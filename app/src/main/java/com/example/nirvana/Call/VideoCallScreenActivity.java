package com.example.nirvana.Call;


import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.example.nirvana.Model.Rating_Model;
import com.example.nirvana.MusicPlayer.AudioPLayer;
import com.example.nirvana.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.sinch.android.rtc.AudioController;
import com.sinch.android.rtc.PushPair;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallEndCause;
import com.sinch.android.rtc.calling.CallState;
import com.sinch.android.rtc.video.VideoCallListener;
import com.sinch.android.rtc.video.VideoController;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class VideoCallScreenActivity extends BaseActivity {

    static final String TAG = VideoCallScreenActivity.class.getSimpleName();
    static final String ADDED_LISTENER = "addedListener";
    static final String VIEWS_TOGGLED = "viewsToggled";

    private AudioPLayer mAudioPlayer;
    public Integer rating=0;
    private String mCallId,recieverImage,recieverUsername,Who,senderId,review,receiverId,patientName,key;
    private boolean mAddedListener = false;
    private boolean mLocalVideoViewAdded = false;
    private boolean mRemoteVideoViewAdded = false;
    ImageView micbutton,videobutton,endCallButton,CameraSwap,profileImage;
    Call call1;
    TextView mCallState,RecieverUserName;
    boolean mToggleVideoViewPositions = true;
    RelativeLayout remoteVideo,localVideo;

    @Override
    protected void onSaveInstanceState(@NonNull Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putBoolean(ADDED_LISTENER, mAddedListener);
        savedInstanceState.putBoolean(VIEWS_TOGGLED, mToggleVideoViewPositions);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        mAddedListener = savedInstanceState.getBoolean(ADDED_LISTENER);
        mToggleVideoViewPositions = savedInstanceState.getBoolean(VIEWS_TOGGLED);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_video_call_screen);
        getSupportActionBar().hide();
        Intent intent=getIntent();
        senderId=intent.getStringExtra("senderId");
        Who=intent.getStringExtra("Who");
        recieverImage=intent.getStringExtra("link");
        recieverUsername=intent.getStringExtra("UserName");
        review=intent.getStringExtra("review");
        receiverId=intent.getStringExtra("receiverId");
        key=intent.getStringExtra("key");
        mAudioPlayer = new AudioPLayer(this);
        endCallButton=findViewById(R.id.hangupButton);
        micbutton=findViewById(R.id.micbutton);
        videobutton=findViewById(R.id.video_off);
        CameraSwap=findViewById(R.id.camera_swap);
        profileImage=findViewById(R.id.profile_image_calling);
        RecieverUserName=findViewById(R.id.receiver_name);
        mCallState=findViewById(R.id.callState);
        endCallButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCall();
            }
        });
        micbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AudioManager audioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                audioManager.setMode(AudioManager.MODE_IN_CALL);
                if(micbutton.getDrawable().getConstantState()==getResources().getDrawable(R.drawable.baseline_mic_white_18dp).getConstantState())
                {
                    micbutton.setImageResource(R.drawable.baseline_mic_off_white_18dp);
                    audioManager.setMicrophoneMute(true);
                }
                else{
                    micbutton.setImageResource(R.drawable.baseline_mic_white_18dp);
                    audioManager.setMicrophoneMute(false);
                }
            }
        });
        videobutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(videobutton.getDrawable().getConstantState()==getResources().getDrawable(R.drawable.ic_videocam_white_24px).getConstantState())
                {
                    videobutton.setImageResource(R.drawable.ic_baseline_videocam_off_24);
                    call1.pauseVideo();

                }
                else{
                    videobutton.setImageResource(R.drawable.ic_videocam_white_24px);
                    call1.resumeVideo();
                }
            }
        });
        CameraSwap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
             VideoController vc=getSinchServiceInterface().getVideoController();
             vc.toggleCaptureDevicePosition();
            }
        });
        videobutton.setEnabled(false);
        mCallId = getIntent().getStringExtra(SinchService.CALL_ID);
        RecieverUserName.setText(recieverUsername);
        if(recieverImage.equals("None"))
        {
            Glide.with(this).load(R.drawable.green_person_logo).into(profileImage);
        }
        else
        {
            Glide.with(this).load(recieverImage).into(profileImage);
        }
    }

    @Override
    public void onServiceConnected() {
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            if (!mAddedListener) {
                call.addCallListener(new SinchCallListener());
                mAddedListener = true;
            }
        } else {
            Log.e(TAG, "Started with invalid callId, aborting.");
            finish();
        }
        updateUI();
    }

    private void updateUI() {
        if (getSinchServiceInterface() == null) {
            return; // early
        }

        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            if (call.getDetails().isVideoOffered()) {
                if (call.getState() == CallState.ESTABLISHED) {
                    videobutton.setVisibility(View.VISIBLE);
                    setVideoViewsVisibility(true, true);
                } else {
                    setVideoViewsVisibility(true, false);
                }
            }
        } else {
            setVideoViewsVisibility(false, false);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        removeVideoViews();
    }

    @Override
    public void onStart() {
        super.onStart();
        updateUI();
    }

    @Override
    public void onBackPressed() {
        // User should exit activity by ending call, not by going back.
    }

    private void endCall() {
        mAudioPlayer.stopProgressTone();
        Call call = getSinchServiceInterface().getCall(mCallId);
        if (call != null) {
            call.hangup();
        }
        finish();
    }

    private ViewGroup getVideoView(boolean localView) {
        if (mToggleVideoViewPositions) {
            localView = !localView;
        }
        return (ViewGroup) (localView ? findViewById(R.id.localVideo) : findViewById(R.id.remoteVideo));
    }

    private void addLocalView() {
        if (mLocalVideoViewAdded || getSinchServiceInterface() == null) {
            return; //early
        }
        final VideoController vc =getSinchServiceInterface().getVideoController();
        if (vc != null) {
            runOnUiThread(() -> {
                ViewGroup localView = getVideoView(true);
                localView.addView(vc.getLocalView());
                localView.setOnClickListener((View v) -> {
                    Call call = getSinchServiceInterface().getCall(mCallId);
                    if(call.getState()==CallState.ESTABLISHED) {
                        vc.toggleCaptureDevicePosition();
                    }
                });
                mLocalVideoViewAdded = true;
                vc.setLocalVideoZOrder(!mToggleVideoViewPositions);
            });
        }
    }
    private void addRemoteView() {
        if (mRemoteVideoViewAdded || getSinchServiceInterface() == null) {
            return; //early
        }
        final VideoController vc = getSinchServiceInterface().getVideoController();
        if (vc != null) {
            runOnUiThread(() -> {
                ViewGroup remoteView = getVideoView(false);
                remoteView.addView(vc.getRemoteView());
                remoteView.setOnClickListener((View v) -> {
                    Call call = getSinchServiceInterface().getCall(mCallId);
                    if(call.getState()==CallState.ESTABLISHED)
                    {
                        removeVideoViews();
                        mToggleVideoViewPositions = !mToggleVideoViewPositions;
                        addRemoteView();
                        addLocalView();
                    }
                });
                mRemoteVideoViewAdded = true;
                vc.setLocalVideoZOrder(!mToggleVideoViewPositions);
            });
        }
    }


    private void removeVideoViews() {
        if (getSinchServiceInterface() == null) {
            return; // early
        }

        VideoController vc = getSinchServiceInterface().getVideoController();
        if (vc != null) {
            runOnUiThread(() -> {
                ((ViewGroup)(vc.getRemoteView().getParent())).removeView(vc.getRemoteView());
                ((ViewGroup)(vc.getLocalView().getParent())).removeView(vc.getLocalView());
                mLocalVideoViewAdded = false;
                mRemoteVideoViewAdded = false;
            });
        }
    }


    private void setVideoViewsVisibility(final boolean localVideoVisibile, final boolean remoteVideoVisible) {
        if (getSinchServiceInterface() == null)
            return;
        if (!mRemoteVideoViewAdded) {
            addRemoteView();
        }
        if (!mLocalVideoViewAdded) {
            addLocalView();
        }
        final VideoController vc = getSinchServiceInterface().getVideoController();
        if (vc != null) {
            runOnUiThread(() -> {
                vc.getLocalView().setVisibility(localVideoVisibile ? View.VISIBLE : View.GONE);
                vc.getRemoteView().setVisibility(remoteVideoVisible ? View.VISIBLE : View.GONE);
            });
        }
    }

    private class SinchCallListener implements VideoCallListener {

        @Override
        public void onCallEnded(Call call) {
            CallEndCause cause = call.getDetails().getEndCause();
            Log.d(TAG, "Call ended. Reason: " + cause.toString());
            mAudioPlayer.stopProgressTone();
            setVolumeControlStream(AudioManager.USE_DEFAULT_STREAM_TYPE);
            String endMsg = "Call ended";
            Task<Void> databaseReference= FirebaseDatabase.getInstance().getReference("CallerName").child(mCallId).removeValue();
             if(Who.equals("Patient")&&review.equals("true"))
             {
                 Review();
             }
            if(review.equals("true"))
            {
                Complete();
            }
            endCall();
            Toast.makeText(VideoCallScreenActivity.this, endMsg, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onCallEstablished(Call call) {
            Log.d(TAG, "Call established");
            videobutton.setEnabled(true);
            profileImage.setVisibility(View.GONE);
            mCallState.setVisibility(View.GONE);
            RecieverUserName.setVisibility(View.GONE);
            mAudioPlayer.stopProgressTone();
            setVolumeControlStream(AudioManager.STREAM_VOICE_CALL);
            AudioController audioController = getSinchServiceInterface().getAudioController();
            audioController.enableSpeaker();
            if (call.getDetails().isVideoOffered()) {
                setVideoViewsVisibility(true, true);
                micbutton.setVisibility(View.VISIBLE);
                videobutton.setVisibility(View.VISIBLE);
                call1=call;
            }
            Log.d(TAG, "Call offered video: " + call.getDetails().isVideoOffered());
        }

        @Override
        public void onCallProgressing(Call call) {
            mCallState.setText("Ringing..");
            mAudioPlayer.playProgressTone();
        }

        @Override
        public void onShouldSendPushNotification(Call call, List<PushPair> pushPairs) {
            // No need to implement if you use managed push
        }

        @Override
        public void onVideoTrackAdded(Call call) {

        }

        @Override
        public void onVideoTrackPaused(Call call) {

        }

        @Override
        public void onVideoTrackResumed(Call call) {

        }
    }
    public void Review()
    {
        final AlertDialog alertDialog=new AlertDialog.Builder(VideoCallScreenActivity.this).create();
        LayoutInflater layoutInflater=VideoCallScreenActivity.this.getLayoutInflater();
        View view=layoutInflater.inflate(R.layout.review_layout,null);
        Button Save,Cancel;
        EditText Review;
        ImageView star1,star2,star3,star4,star5;
        Save=view.findViewById(R.id.save);
        Cancel=view.findViewById(R.id.cancel);
        Review=view.findViewById(R.id.editText);
        star1=view.findViewById(R.id.star1);
        star2=view.findViewById(R.id.star2);
        star3=view.findViewById(R.id.star3);
        star4=view.findViewById(R.id.star4);
        star5=view.findViewById(R.id.star5);
        Cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
            }
        });
        star1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                star1.setImageResource(R.drawable.ic_baseline_star_24);
                star2.setImageResource(R.drawable.ic_star_black_24dp);
                star3.setImageResource(R.drawable.ic_star_black_24dp);
                star4.setImageResource(R.drawable.ic_star_black_24dp);
                star5.setImageResource(R.drawable.ic_star_black_24dp);
                rating=1;
            }
        });
        star2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                star1.setImageResource(R.drawable.ic_baseline_star_24);
                star2.setImageResource(R.drawable.ic_baseline_star_24);
                star3.setImageResource(R.drawable.ic_star_black_24dp);
                star4.setImageResource(R.drawable.ic_star_black_24dp);
                star5.setImageResource(R.drawable.ic_star_black_24dp);
                rating=2;
            }
        });
        star3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                star1.setImageResource(R.drawable.ic_baseline_star_24);
                star2.setImageResource(R.drawable.ic_baseline_star_24);
                star3.setImageResource(R.drawable.ic_baseline_star_24);
                star4.setImageResource(R.drawable.ic_star_black_24dp);
                star5.setImageResource(R.drawable.ic_star_black_24dp);
                rating=3;
            }
        });
        star4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                star1.setImageResource(R.drawable.ic_baseline_star_24);
                star2.setImageResource(R.drawable.ic_baseline_star_24);
                star3.setImageResource(R.drawable.ic_baseline_star_24);
                star4.setImageResource(R.drawable.ic_baseline_star_24);
                star5.setImageResource(R.drawable.ic_star_black_24dp);
                rating=4;
            }
        });
        star5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                star1.setImageResource(R.drawable.ic_baseline_star_24);
                star2.setImageResource(R.drawable.ic_baseline_star_24);
                star3.setImageResource(R.drawable.ic_baseline_star_24);
                star4.setImageResource(R.drawable.ic_baseline_star_24);
                star5.setImageResource(R.drawable.ic_baseline_star_24);
                rating=5;
            }
        });
        Save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(rating==0)
                {
                    Toast.makeText(VideoCallScreenActivity.this,"Please select the rating",Toast.LENGTH_SHORT).show();
                }
                else if(TextUtils.isEmpty(Review.getText()))
                {
                    Review.setError("Please give your reviews");
                }
                else
                {
                    String currentTime = new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
                    SimpleDateFormat currentDate = new SimpleDateFormat("dd/MM/yyyy");
                    Date todayDate = new Date();
                    String thisDate = currentDate.format(todayDate);
                    DatabaseReference databaseReference= FirebaseDatabase.getInstance().getReference(Who).child(receiverId);
                    databaseReference.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            HashMap<String,Object> hashMap= (HashMap<String, Object>) snapshot.getValue();
                            String fname=hashMap.get("fname").toString();
                            String lname=hashMap.get("lname").toString();
                            patientName=fname+" "+lname;
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
                    Rating_Model rating_model=new Rating_Model(
                            patientName,
                            String.valueOf(rating),
                            Review.getText().toString(),
                            thisDate,
                            currentTime
                    );
                    FirebaseDatabase firebaseDatabase=FirebaseDatabase.getInstance();
                    DatabaseReference databaseReference1=firebaseDatabase.getReference().child("Doctors_Reviews").child(senderId).child(receiverId);
                    databaseReference1.setValue(rating_model).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {

                        }
                    });
                }
            }
        });
    }
    public void Complete()
    {
       if(Who.equals("Doctors"))
       {
           Task<Void> databaseReference=FirebaseDatabase.getInstance().getReference("Doctor_Meetings").child(senderId).child(receiverId).child("Complete")
                   .child(key).setValue("1");
           Task<Void> databaseReference1=FirebaseDatabase.getInstance().getReference("Patient_Meetings").child(receiverId).child(senderId).child("Complete")
                   .child(key).setValue("1");
       }
       else
       {
           Task<Void> databaseReference=FirebaseDatabase.getInstance().getReference("Doctor_Meetings").child(receiverId).child(senderId).child("Complete")
                   .child(key).setValue("1");
           Task<Void> databaseReference1=FirebaseDatabase.getInstance().getReference("Patient_Meetings").child(senderId).child(receiverId).child("Complete")
                   .child(key).setValue("1");
       }
    }
}
