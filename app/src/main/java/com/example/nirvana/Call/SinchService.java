package com.example.nirvana.Call;

import android.Manifest;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.example.nirvana.R;
import com.sinch.android.rtc.AudioController;
import com.sinch.android.rtc.ClientRegistration;
import com.sinch.android.rtc.MissingPermissionException;
import com.sinch.android.rtc.NotificationResult;
import com.sinch.android.rtc.Sinch;
import com.sinch.android.rtc.SinchClient;
import com.sinch.android.rtc.SinchClientListener;
import com.sinch.android.rtc.SinchError;
import com.sinch.android.rtc.calling.Call;
import com.sinch.android.rtc.calling.CallClient;
import com.sinch.android.rtc.calling.CallClientListener;
import com.sinch.android.rtc.video.VideoController;
import com.sinch.android.rtc.video.VideoScalingType;

import java.util.List;
import java.util.Map;

import static com.example.nirvana.Call.IncominCallScreenActivity.ACTION_ANSWER;
import static com.example.nirvana.Call.IncominCallScreenActivity.ACTION_IGNORE;
import static com.example.nirvana.Call.IncominCallScreenActivity.EXTRA_ID;
import static com.example.nirvana.Call.IncominCallScreenActivity.MESSAGE_ID;

public class SinchService extends Service {

    static final String APP_KEY ="6a9ce4e2-e655-4a59-a2b0-b76c84132546" ;  //add your keys
    static final String APP_SECRET ="2dEpHTchh0SLCsnYyv2gPw==" ;
    static final String ENVIRONMENT ="clientapi.sinch.com" ;

    public static final int MESSAGE_PERMISSIONS_NEEDED = 1;
    public static final String REQUIRED_PERMISSION = "REQUIRED_PESMISSION";
    public static final String MESSENGER = "MESSENGER";
    private Messenger messenger;

    public static final String CALL_ID = "CALL_ID";
    static final String TAG = SinchService.class.getSimpleName();
    public Integer i=0;
    private PersistedSettings mSettings;
    private final SinchServiceInterface mSinchServiceInterface = new SinchServiceInterface();
    private SinchClient mSinchClient;
    public String Id;
    private StartFailedListener mListener;

    @Override
    public void onCreate() {
        super.onCreate();
        mSettings = new PersistedSettings(getApplicationContext());
        attemptAutoStart();
    }

    private void attemptAutoStart() {
        if (messenger != null) {
            start();
        }
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId){
        Id=intent.getStringExtra("Id");
        i=intent.getIntExtra("i",0);
        if(i==0)
        {
            createClient(Id);
            i++;
        }
        onTaskRemoved(intent);
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        Intent restartServiceIntent = new Intent(getApplicationContext(),this.getClass());
        restartServiceIntent.setPackage(getPackageName());
        restartServiceIntent.putExtra("Id",Id);
        restartServiceIntent.putExtra("i",i);
        startService(restartServiceIntent);
        super.onTaskRemoved(rootIntent);
    }

    private void createClient(String username) {
        mSinchClient =Sinch.getSinchClientBuilder().context(getApplicationContext())
                .applicationKey("6a9ce4e2-e655-4a59-a2b0-b76c84132546")
                .applicationSecret("2dEpHTchh0SLCsnYyv2gPw==")
                .userId(username)
                .environmentHost("clientapi.sinch.com").build();

        mSinchClient.setSupportCalling(true);
        mSinchClient.setSupportManagedPush(true);
        mSinchClient.startListeningOnActiveConnection();
        mSinchClient.setSupportActiveConnectionInBackground(true);
        mSinchClient.addSinchClientListener(new MySinchClientListener());
        mSinchClient.getCallClient().addCallClientListener(new SinchCallClientListener());
        mSinchClient.getVideoController().setResizeBehaviour(VideoScalingType.ASPECT_FILL);
        mSinchClient.setPushNotificationDisplayName("User " + username);
        mSinchClient.start();
    }

    @Override
    public void onDestroy() {
        if (mSinchClient != null && mSinchClient.isStarted()) {
            mSinchClient.terminateGracefully();
        }
        super.onDestroy();
    }

    private boolean hasUsername() {
        if (mSettings.getUsername().isEmpty()) {
            Log.e(TAG, "Can't start a SinchClient as no username is available!");
            return false;
        }
        return true;
    }

    private void createClientIfNecessary() {
        if (mSinchClient != null)
            return;
        if (!hasUsername()) {
            throw new IllegalStateException("Can't create a SinchClient as no username is available!");
        }
        createClient(mSettings.getUsername());
    }

    private void start() {
        boolean permissionsGranted = true;
        createClientIfNecessary();
        try {
            //mandatory checks
            mSinchClient.checkManifest();
            //auxiliary check
            if (getApplicationContext().checkCallingOrSelfPermission(Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {
                throw new MissingPermissionException(Manifest.permission.CAMERA);
            }
        } catch (MissingPermissionException e) {
            permissionsGranted = false;
            if (messenger != null) {
                Message message = Message.obtain();
                Bundle bundle = new Bundle();
                bundle.putString(REQUIRED_PERMISSION, e.getRequiredPermission());
                message.setData(bundle);
                message.what = MESSAGE_PERMISSIONS_NEEDED;
                try {
                    messenger.send(message);
                } catch (RemoteException e1) {
                    e1.printStackTrace();
                }
            }
        }
        if (permissionsGranted) {
            Log.d(TAG, "Starting SinchClient");
            try {
                mSinchClient.start();
            } catch (IllegalStateException e) {
                Log.w(TAG, "Can't start SinchClient - " + e.getMessage());
            }
        }
    }

    private void stop() {
        if (mSinchClient != null) {
            mSinchClient.terminateGracefully();
            mSinchClient = null;
        }
    }

    private boolean isStarted() {
        return (mSinchClient != null && mSinchClient.isStarted());
    }

    @Override
    public IBinder onBind(Intent intent) {
        messenger = intent.getParcelableExtra(MESSENGER);
        return mSinchServiceInterface;
    }

    public class SinchServiceInterface extends Binder {

        public Call callUser(String userId) {
            return mSinchClient.getCallClient().callUser(userId);
        }

        public Call callUserVideo(String userId) {
            return mSinchClient.getCallClient().callUserVideo(userId);
        }

        public String getUsername() { return mSettings.getUsername(); }

        public void setUsername(String username) { mSettings.setUsername(username);}

        public void retryStartAfterPermissionGranted() { SinchService.this.attemptAutoStart(); }

        public boolean isStarted() {
            return SinchService.this.isStarted();
        }

        public void startClient() { start(); }

        public void stopClient() {
            stop();
        }

        public void setStartListener(StartFailedListener listener) {
            mListener = listener;
        }

        public Call getCall(String callId) {
            return mSinchClient != null ? mSinchClient.getCallClient().getCall(callId) : null;
        }

        public VideoController getVideoController() {
            if (!isStarted()) {
                return null;
            }
            return mSinchClient.getVideoController();
        }

        public AudioController getAudioController() {
            if (!isStarted()) {
                return null;
            }
            return mSinchClient.getAudioController();
        }

        public NotificationResult relayRemotePushNotificationPayload(final Map payload) {
            if (!hasUsername()) {
                Log.e(TAG, "Unable to relay the push notification!");
                return null;
            }
            return mSinchClient.relayRemotePushNotificationPayload(payload);
        }
    }

    public interface StartFailedListener {

        void onStartFailed(SinchError error);

        void onStarted();
    }

    private class MySinchClientListener implements SinchClientListener {

        @Override
        public void onClientFailed(SinchClient client, SinchError error) {
            if (mListener != null) {
                mListener.onStartFailed(error);
            }
            mSinchClient.terminate();
            mSinchClient = null;
        }

        @Override
        public void onClientStarted(SinchClient client) {
            System.out.println("SinchClient started");
            if (mListener != null) {
                mListener.onStarted();
            }
        }

        @Override
        public void onClientStopped(SinchClient client) {
            Log.d(TAG, "SinchClient stopped");
        }

        @Override
        public void onLogMessage(int level, String area, String message) {
            switch (level) {
                case Log.DEBUG:
                    Log.d(area, message);
                    break;
                case Log.ERROR:
                    Log.e(area, message);
                    break;
                case Log.INFO:
                    Log.i(area, message);
                    break;
                case Log.VERBOSE:
                    Log.v(area, message);
                    break;
                case Log.WARN:
                    Log.w(area, message);
                    break;
            }
        }

        @Override
        public void onRegistrationCredentialsRequired(SinchClient client,
                                                      ClientRegistration clientRegistration) {
        }
    }

    private class SinchCallClientListener implements CallClientListener {

        @RequiresApi(api = Build.VERSION_CODES.O)
        @Override
        public void onIncomingCall(CallClient callClient, Call call) {
            Intent intent;
            if (call.getDetails().isVideoOffered()) {
                Toast.makeText(SinchService.this,"skdkfnoisdnif",Toast.LENGTH_SHORT).show();
                intent = new Intent(getApplicationContext(), IncomingVideoCallActivity.class);
            } else {
                Toast.makeText(SinchService.this,"zjkdnfs skdkfnoisdnif",Toast.LENGTH_SHORT).show();
                intent = new Intent(getApplicationContext(), IncominCallScreenActivity.class);
            }

            intent.putExtra(EXTRA_ID, MESSAGE_ID);
            intent.putExtra(CALL_ID, call.getCallId());

            boolean inForeground = isAppOnForeground(getApplicationContext());

            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK );

            if (!inForeground) {
                NotificationChannel notificationChannel = new NotificationChannel( FcmListenerService.CHANNEL_ID, "Music_Player", NotificationManager.IMPORTANCE_HIGH);
                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationChannel.enableLights(true);
                notificationChannel.enableVibration(true);
                notificationChannel.setVibrationPattern(new long[]{0});
                notificationManager.createNotificationChannel(notificationChannel);
                notificationManager.notify(MESSAGE_ID, createIncomingCallNotification(call.getRemoteUserId(), intent));
            } else {
                SinchService.this.startActivity(intent);
            }
        }

        private boolean isAppOnForeground(Context context) {
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            assert activityManager != null;
            List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager.getRunningAppProcesses();
            if (appProcesses == null) {
                return false;
            }
            final String packageName = context.getPackageName();
            for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND && appProcess.processName.equals(packageName)) {
                    return true;
                }
            }
            return false;
        }

        private Bitmap getBitmap(Context context, int resId) {
            int largeIconWidth = (int) context.getResources()
                    .getDimension(R.dimen.notification_large_icon_width);
            int largeIconHeight = (int) context.getResources()
                    .getDimension(R.dimen.notification_large_icon_height);
            Drawable d = context.getResources().getDrawable(resId);
            Bitmap b = Bitmap.createBitmap(largeIconWidth, largeIconHeight, Bitmap.Config.ARGB_8888);
            Canvas c = new Canvas(b);
            d.setBounds(0, 0, largeIconWidth, largeIconHeight);
            d.draw(c);
            return b;
        }

        private PendingIntent getPendingIntent(Intent intent, String action) {
            intent.setAction(action);
            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 111, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            return pendingIntent;
        }

        private Notification createIncomingCallNotification(String userId, Intent fullScreenIntent) {

            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 112, fullScreenIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            NotificationCompat.Builder builder =
                    new NotificationCompat.Builder(getApplicationContext(), FcmListenerService.CHANNEL_ID)
                            .setContentTitle("Incoming call")
                            .setContentText(userId)
                            .setLargeIcon(getBitmap(getApplicationContext(), R.mipmap.niri))
                            .setSmallIcon(R.mipmap.niri)
                            .setPriority(NotificationCompat.PRIORITY_MAX)
                            .setContentIntent(pendingIntent)
                            .setFullScreenIntent(pendingIntent, true)
                            .addAction(R.drawable.button_accept, "Answer",  getPendingIntent(fullScreenIntent, ACTION_ANSWER))
                            .addAction(R.drawable.button_decline, "Ignore", getPendingIntent(fullScreenIntent, ACTION_IGNORE))
                            .setOngoing(true);

            return builder.build();
        }
    }


    private class PersistedSettings {

        private final SharedPreferences mStore;

        private static final String PREF_KEY = "Sinch";

        public PersistedSettings(Context context) {
            mStore = context.getSharedPreferences(PREF_KEY, MODE_PRIVATE);
        }

        public String getUsername() {
            return mStore.getString("Username", "None");
        }

        public void setUsername(String username) {
            SharedPreferences.Editor editor = mStore.edit();
            editor.putString("Username", username);
            editor.apply();
        }
    }

}
