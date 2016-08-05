package it.polimi.testing.testapplication.broadcast_receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import it.polimi.testing.testapplication.R;


public class UnregisterTwiceMultipleWithTry extends AppCompatActivity
{
    private final BroadcastReceiver broadcastReceiver1 = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: "+message+", "+Test.A+","+Test.B);
        }
    };

    private final BroadcastReceiver broadcastReceiver2 = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: "+message+", "+Test.A+","+Test.B);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        registerReceiver(broadcastReceiver1, new IntentFilter("my-event"));
        registerReceiver(broadcastReceiver2, new IntentFilter("my-event"));
    }

    @Override
    public void onStart()
    {
        super.onStart();
    }

    @Override
    public void onStop()
    {
        super.onStop();
        unregisterReceiver(broadcastReceiver1);
        try
        {
            unregisterReceiver(broadcastReceiver2);
        }
        catch(IllegalArgumentException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause()
    {
        try
        {
            unregisterReceiver(broadcastReceiver2);
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
        super.onPause();
    }

    private enum Test
    {
        A, B
    }
}
