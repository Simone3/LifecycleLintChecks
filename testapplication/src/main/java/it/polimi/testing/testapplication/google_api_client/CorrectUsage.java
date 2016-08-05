package it.polimi.testing.testapplication.google_api_client;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import it.polimi.testing.testapplication.R;


public class CorrectUsage extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener
{
    GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(this)
            .enableAutoManage(this, this)
            .build();

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    public void onStart()
    {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {

    }
}
