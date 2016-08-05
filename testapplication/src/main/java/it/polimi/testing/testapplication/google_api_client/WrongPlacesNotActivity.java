package it.polimi.testing.testapplication.google_api_client;

import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

public class WrongPlacesNotActivity implements GoogleApiClient.OnConnectionFailedListener
{
    public WrongPlacesNotActivity(FragmentActivity context)
    {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .enableAutoManage(context, this)
                .build();
    }

    GoogleApiClient mGoogleApiClient;

    private void myMethod1()
    {
        mGoogleApiClient.connect();
    }

    private void myMethod2()
    {
        mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult)
    {
        // This is ok
        mGoogleApiClient.connect();
    }
}
