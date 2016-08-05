package it.polimi.testing.testapplication.broadcast_receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import it.polimi.testing.testapplication.R;


public class NoUnregisterLocal extends AppCompatActivity
{
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String message = intent.getStringExtra("NOUNREGmessage");
            Log.d("NOUNREGreceiver", "NOUNREGGot message: "+message+", "+Test.A+","+Test.B);
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
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, new IntentFilter("NOUNREGmy-event"));
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    private enum Test
    {
        A, B
    }
}
