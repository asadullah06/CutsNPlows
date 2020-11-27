package Com.app.cuts.plows.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import static androidx.core.app.ActivityCompat.requestPermissions;
import static androidx.core.app.ActivityCompat.startActivityForResult;


public class GoogleApiClass implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {
    private final int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 120;
    // public   int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 120;
    private GoogleApiClient mGoogleApiClient;
    private Location mLocation;
    private LocationManager mLocationManager;

    private LocationRequest mLocationRequest;
    private com.google.android.gms.location.LocationListener listener;
    private long UPDATE_INTERVAL = 2 * 1000;  /* 10 secs */
    private long FASTEST_INTERVAL = 2000; /* 2 sec */
    double latitude = 0.0;
    double longitude = 0.0;
    CurrentLocationCoordinatesListener LocationListener;
    public Context mContext;
    private LocationManager locationManager;
    private LocationCallback locationCallback;


    public GoogleApiClass(Context mContext, CurrentLocationCoordinatesListener locationListener) {

        this.mContext = mContext;
        this.LocationListener = locationListener;
        if (checkLocation()) {
            mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();

            mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            if (mGoogleApiClient != null) {
                mGoogleApiClient.connect();
            }
        }
        //  checkLocation();
    }

    public void setCurrentLocationListener(CurrentLocationCoordinatesListener currentLocationListener) {
        this.LocationListener = currentLocationListener;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            requestPermissions((Activity) mContext, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_COARSE_LOCATION);
            return;
        }


//          mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);

        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                // Logic to handle location object
                mLocation = location;
                LocationListener.CurrentLocationCoordinates(mLocation);

            } else {
                startLocationUpdates();

                // Handle null case or Request periodic location update https://developer.android.com/training/location/receive-location-updates
            }
        });
//        startLocationUpdates();

//        if (mLocation == null) {
//            startLocationUpdates();
//        }
//        if (mLocation != null) {
//
//            // mLatitudeTextView.setText(String.valueOf(mLocation.getLatitude()));
//            //mLongitudeTextView.setText(String.valueOf(mLocation.getLongitude()));
//            Toast.makeText(mContext, "Loc " + mLocation.getLatitude()
//                    + " " + mLocation.getLongitude(), Toast.LENGTH_SHORT).show();
//
//
//        } else {
//            Toast.makeText(mContext, "Location not Detected", Toast.LENGTH_SHORT).show();
//        }
    }

    @Override
    public void onConnectionSuspended(int i) {

        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i("Location ", "Connection failed. Error: " + connectionResult.getErrorCode());
    }

    @Override
    public void onLocationChanged(Location location) {
        String msg = "Updated Location: " +
                Double.toString(location.getLatitude()) + "," +
                Double.toString(location.getLongitude());
      /*  mLatitudeTextView.setText(String.valueOf(location.getLatitude()));
        mLongitudeTextView.setText(String.valueOf(location.getLongitude() ));*/
        mLocation = location;
//        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
        // You can now create a LatLng Object for use with maps


    }

    private boolean checkLocation() {
        if (!isLocationEnabled())
            showAlert();
        return isLocationEnabled();
    }

    private void showAlert() {
        final AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " +
                        "use this app")
                .setPositiveButton("Location Settings", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                        Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult((Activity) mContext, myIntent, 20, null);
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                    }
                });
        dialog.show();
    }

    private boolean isLocationEnabled() {
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    protected void startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(UPDATE_INTERVAL)
                .setFastestInterval(FASTEST_INTERVAL);
        // Request location updates
//        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // TODO: Consider calling
//            //    ActivityCompat#requestPermissions
//            // here to request the missing permissions, and then overriding
//            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//            //                                          int[] grantResults)
//            // to handle the case where the user grants the permission. See the documentation
//            // for ActivityCompat#requestPermissions for more details.
//            return;
//        }
        //  LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
//                        Toast.makeText(mContext, location.getLatitude()+", "+location.getLongitude(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
        };
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(mContext);
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        fusedLocationClient.requestLocationUpdates(mLocationRequest, locationCallback, null);
        Log.d("reque", "--->>>>");
    }

    public double getLatitude() {
        if (mLocation != null) {
            latitude = mLocation.getLatitude();
        } else {
            latitude = 0.0;
        }

        // return latitude
        return latitude;
    }

    /**
     * Function to get longitude
     */
    public double getLongitude() {
        if (mLocation != null) {
            longitude = mLocation.getLongitude();
        } else {
            longitude = 0.0;
        }

        // return longitude
        return longitude;
    }

    public void destroyApiConnection() {
        {
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
        }
    }


}
