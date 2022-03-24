package com.hasid.demojinglepayapp.viewmodel;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;
import android.content.Context;
import android.location.Location;


import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.hasid.demojinglepayapp.R;
import com.hasid.demojinglepayapp.utils.LocationHelper;

import java.util.ArrayList;
import java.util.List;

import twitter4j.FilterQuery;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.User;
import twitter4j.conf.ConfigurationBuilder;


public class TweetsViewModel extends ViewModel {
    private MutableLiveData<List<Status>> tweets;
    private static final String TAG = "TweetsViewModel";
    public LiveData<List<Status>> startStreaming(Context context, Location lastKnownLocation, String searchTerm, double radius) {
        if (tweets == null) {
            tweets = new MutableLiveData<List<Status>>();
            ConfigurationBuilder cb = new ConfigurationBuilder();
            cb.setDebugEnabled(true);
            cb.setOAuthConsumerKey(context.getResources().getString(R.string.twitter_api_key));
            cb.setOAuthConsumerSecret(context.getResources().getString(R.string.twitter_api_secret));
            cb.setOAuthAccessToken(context.getResources().getString(R.string.twitter_access_token));
            cb.setOAuthAccessTokenSecret(context.getResources().getString(R.string.twitter_access_token_secret));

            TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
            StatusListener listener = new StatusListener() {

                @Override
                public void onException(Exception ex) {

                }

                @Override
                public void onStatus(Status status) {
                    User user = status.getUser();
                    // gets Username
                    if(status.getGeoLocation() != null){
                        List<Status> tempStatusList = tweets.getValue() == null ? new ArrayList<>() : tweets.getValue();
                        tempStatusList.add(status);
                        //If there are more than 100 tweets in list then remove old one
                        if(tempStatusList.size() > 100){
                            tempStatusList.remove(0);
                        }
                        tweets.postValue(tempStatusList);
                    }
                }

                @Override
                public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {

                }

                @Override
                public void onTrackLimitationNotice(int numberOfLimitedStatuses) {

                }

                @Override
                public void onScrubGeo(long userId, long upToStatusId) {

                }

                @Override
                public void onStallWarning(StallWarning warning) {
                }
            };

            FilterQuery tweetFilterQuery = new FilterQuery();
            LatLngBounds locationBounds = LocationHelper.toBounds(new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()),radius*1000);
            tweetFilterQuery.locations(new double[][]{new double[]{locationBounds.southwest.longitude, locationBounds.southwest.latitude},
                    {locationBounds.northeast.longitude, locationBounds.northeast.latitude}});
            String keywords[] = {searchTerm};
            tweetFilterQuery.track(keywords);
            twitterStream.addListener(listener);
            twitterStream.filter(tweetFilterQuery);
        }
        return tweets;
    }
    public LiveData<List<Status>> stopStreaming() {
        tweets.postValue(new ArrayList<>());
        return tweets;
    }
}
