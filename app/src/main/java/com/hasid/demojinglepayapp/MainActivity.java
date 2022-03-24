package com.hasid.demojinglepayapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.hasid.demojinglepayapp.viewmodel.TweetsViewModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import twitter4j.Status;


public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMarkerClickListener, GoogleMap.OnInfoWindowClickListener {
    private static final String TAG = "MainActivity";
    Location lastKnownLocation;
    private static final int APP_PERMISSIONS_REQUEST_GET_LOCATION = 1;
    String searchTerm = "";
    double radius = 5.0;
    LocationManager locationManager;
    private GoogleMap mMap;
    String locationProvider = LocationManager.GPS_PROVIDER;
    HashMap<Long,Marker> markersWeakHashMap = new HashMap<Long, Marker>();
    HashMap<Long,Status> tweetsWeakHashMap = new HashMap<Long, Status>();
    List<Status> oldTweets = new ArrayList<>();
    LiveData<List<Status>> listLiveData;
    TweetsViewModel tweetsViewModel;
    TextView radiusTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        radiusTV = (TextView) findViewById(R.id.radius_tv);
        radiusTV.setText(" "+radius+" KM");
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    APP_PERMISSIONS_REQUEST_GET_LOCATION);
            return;
        }
        lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
        tweetsViewModel = ViewModelProviders.of(this).get(TweetsViewModel.class);
        listLiveData = tweetsViewModel.startStreaming(getApplicationContext(), lastKnownLocation, searchTerm, radius);

        startObserving();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        SearchView searchView;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search)
                .getActionView();

        searchView.setSearchableInfo(searchManager
                .getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchTerm = query.trim();
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    tweetsViewModel.stopStreaming();
                    listLiveData = tweetsViewModel.startStreaming(getApplicationContext(), lastKnownLocation, searchTerm, radius);
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                return false;
            }
        });

        super.onCreateOptionsMenu(menu);
        return true;
    }

    private void startObserving() {
        listLiveData.observe(this, tweets -> {
            List<Status> newTweets = new ArrayList<>();
            for(Status tweet : tweets){
                newTweets.add(tweet);
            }

            List<Status> common = new ArrayList<Status>(oldTweets);
            common.retainAll(newTweets);
            List<Status> tweetsToAdd = new ArrayList<Status>(newTweets);
            tweetsToAdd.removeAll(common);
            List<Status> tweetsToRemove = new ArrayList<Status>(oldTweets);
            tweetsToRemove.removeAll(common);

            if(tweets.size() == 0){
                //clear map
                mMap.clear();
            }else {
                for(Status newTweet : tweetsToAdd){
                    oldTweets.add(newTweet);
                    BitmapDescriptor bitmapDescriptor
                            = BitmapDescriptorFactory.defaultMarker(
                            BitmapDescriptorFactory.HUE_AZURE);
                    LatLng tweetLocation = new LatLng(newTweet.getGeoLocation().getLatitude(), newTweet.getGeoLocation().getLongitude());
                    Marker newlyAddedMarker = mMap.addMarker(new MarkerOptions().position(tweetLocation)
                            .icon(bitmapDescriptor)
                            .title("Tweet my @"+newTweet.getUser().getScreenName()));
                    newlyAddedMarker.setTag(newTweet.getId());
                    markersWeakHashMap.put(newTweet.getId(),newlyAddedMarker);

                    tweetsWeakHashMap.put(newTweet.getId(),newTweet);
                    newlyAddedMarker.showInfoWindow();
                }
                for(Status tweetToRemove : tweetsToRemove){
                    Marker markerToRemove = markersWeakHashMap.get(tweetToRemove.getId());
                    markerToRemove.remove();
                    markersWeakHashMap.remove(tweetToRemove.getId());
                    tweetsWeakHashMap.remove(tweetToRemove.getId());
                    oldTweets.remove(tweetToRemove);
                }
            }
        });
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case APP_PERMISSIONS_REQUEST_GET_LOCATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay!
                    lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
                    tweetsViewModel = ViewModelProviders.of(this).get(TweetsViewModel.class);
                    listLiveData = tweetsViewModel.startStreaming(getApplicationContext(), lastKnownLocation, searchTerm, radius);
                    startObserving();
                    updateLocationUI();
                } else {
                    // permission denied, boo!
                    Snackbar snackbar = Snackbar
                            .make(findViewById(R.id.mainConstraintLayout), getResources().getString(R.string.message_need_location_permission), Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
                return;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        updateLocationUI();
    }

    @SuppressLint("MissingPermission")
    private void updateLocationUI() {
        if (lastKnownLocation != null) {
            LatLng userLocation = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(userLocation)      // Sets the center of the map to Mountain View
                    .zoom(10)                   // Sets the zoom
                    .bearing(90)                // Sets the orientation of the camera to east
                    .tilt(30)                   // Sets the tilt of the camera to 30 degrees
                    .build();                   // Creates a CameraPosition from the builder
            mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
            mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter());
            mMap.setOnMarkerClickListener(this);
            mMap.setOnInfoWindowClickListener(this);
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
        Intent intent = new Intent(this, TweetDetailsActivity.class);
        intent.putExtra("tweetID",(Long)marker.getTag());
        startActivity(intent);
    }

    @Override
    public boolean onMarkerClick(Marker marker) {
        marker.showInfoWindow();
        return true;
    }

    public class CustomInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View mContents;

        CustomInfoWindowAdapter() {
            mContents = getLayoutInflater().inflate(R.layout.custom_info_contents, null);
        }


        @Override
        public View getInfoWindow(Marker marker) {
            return null;
        }

        @Override
        public View getInfoContents(Marker marker) {
            Status tweet = tweetsWeakHashMap.get(marker.getTag());
            if(tweet != null){
                TextView tvUserDisplayName = ((TextView) mContents.findViewById(R.id.userDisplayName));
                tvUserDisplayName.setText("@"+tweet.getUser().getScreenName());
                TextView tvTweetGist = ((TextView)mContents.findViewById(R.id.tweetGist));
                tvTweetGist.setText(tweet.getText());
            }
            return mContents;
        }
    }

    public void changeRadius(View target){
        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.MyDialogTheme);
        builder.setTitle(getResources().getString(R.string.radius_in_km));
// I'm using fragment here so I'm using getView() to provide ViewGroup
// but you can provide here any other instance of ViewGroup from your Fragment / Activity
        View viewInflated = LayoutInflater.from(this).inflate(R.layout.text_input_redius, null, false);
// Set up the input
        final EditText input = (EditText) viewInflated.findViewById(R.id.input);
// Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        builder.setView(viewInflated);

// Set up the buttons
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    radius = Long.parseLong(input.getText().toString());
                    radiusTV.setText(" "+radius+" KM");
                    tweetsViewModel.stopStreaming();
                    listLiveData = tweetsViewModel.startStreaming(getApplicationContext(), lastKnownLocation, searchTerm, radius);
                }

            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
