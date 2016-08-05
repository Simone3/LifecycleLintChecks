package it.polimi.testing.testapplication.google_api_client;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import it.polimi.testing.testapplication.R;

public class WrongPlaces extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener
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
    public void onPause()
    {
        super.onPause();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        // This is ok
        mGoogleApiClient.connect();
    }
}
