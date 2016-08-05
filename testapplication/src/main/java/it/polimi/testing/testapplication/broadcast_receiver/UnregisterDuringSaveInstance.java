package it.polimi.testing.testapplication.broadcast_receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import it.polimi.testing.testapplication.R;


public class UnregisterDuringSaveInstance extends AppCompatActivity
{
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
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
        registerReceiver(broadcastReceiver, new IntentFilter("my-event"));
    }

    @Override
    protected void onPause()
    {
        super.onPause();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstance)
    {
        unregisterReceiver(broadcastReceiver);

        super.onSaveInstanceState(savedInstance);
    }

    private enum Test
    {
        A, B
    }
}
