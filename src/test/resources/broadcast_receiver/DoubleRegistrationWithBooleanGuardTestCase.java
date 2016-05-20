package it.polimi.lifecycle_lint;

public class DoubleRegistrationWithBooleanGuardTestCase extends Activity
{
    private boolean registered = false;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: "+message);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        registerReceiver(broadcastReceiver, new IntentFilter("my-event"));
        registered = true;
    }

    @Override
    public void onResume()
    {
        super.onResume();

        if(!registered)
        {
            registerReceiver(broadcastReceiver, new IntentFilter("my-event"));
            registered = true;
        }
    }

    @Override
    protected void onPause()
    {
        if(registered)
        {
            registered = false;
            unregisterReceiver(broadcastReceiver);
        }

        super.onPause();
    }
}