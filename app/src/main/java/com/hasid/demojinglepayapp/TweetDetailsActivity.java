package com.hasid.demojinglepayapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.DefaultLogger;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterConfig;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;
import com.twitter.sdk.android.core.models.Tweet;
import com.twitter.sdk.android.tweetui.TweetUtils;
import com.twitter.sdk.android.tweetui.TweetView;

public class TweetDetailsActivity extends AppCompatActivity {

    TwitterLoginButton loginButton;
    Button logoutButton;
    TextView loginToLikeMsgTV;

    // launch the login activity when a guest user tries to favorite a Tweet
    final Callback<Tweet> actionCallback = new Callback<Tweet>() {
        @Override
        public void success(Result<Tweet> result) {
            // Intentionally blank
        }

        @Override
        public void failure(TwitterException exception) {

        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);



        TwitterConfig config = new TwitterConfig.Builder(this)
                .logger(new DefaultLogger(Log.DEBUG))
                .twitterAuthConfig(new TwitterAuthConfig(getResources().getString(R.string.twitter_api_key), getResources().getString(R.string.twitter_api_secret)))
                .debug(true)
                .build();
        Twitter.initialize(config);

        setContentView(R.layout.activity_tweet_details);
        loginButton = (TwitterLoginButton) findViewById(R.id.login_button);
        logoutButton = (Button) findViewById(R.id.logout_button);
        loginToLikeMsgTV = (TextView) findViewById(R.id.tv_login_to_like);
        updateLoginUI();


        final LinearLayout myLayout
                = (LinearLayout) findViewById(R.id.tweet_details);

        Bundle bundle = getIntent().getExtras();

        final long tweetId = bundle.getLong("tweetID");
        TweetUtils.loadTweet(tweetId, new Callback<Tweet>() {
            @Override
            public void success(Result<Tweet> result) {
                final TweetView tweetView = new TweetView(TweetDetailsActivity.this, result.data);
                tweetView.setTweetActionsEnabled(true);
                tweetView.setOnActionCallback(actionCallback);
                myLayout.addView(tweetView);
            }

            @Override
            public void failure(TwitterException exception) {
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result to the login button.
        loginButton.onActivityResult(requestCode, resultCode, data);
        updateLoginUI();
    }

    public void logoutUser(View target) {
        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.removeSessionCookie();
        TwitterCore.getInstance().getSessionManager().clearActiveSession();
        updateLoginUI();
    }

    private void updateLoginUI(){
        TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();
        if(session == null){
            loginButton.setVisibility(View.VISIBLE);
            loginToLikeMsgTV.setVisibility(View.VISIBLE);
            logoutButton.setVisibility(View.GONE);
            loginButton.setCallback(new Callback<TwitterSession>() {
                @Override
                public void success(Result<TwitterSession> result) {
                    // Do something with result, which provides a TwitterSession for making API calls
                }

                @Override
                public void failure(TwitterException exception) {
                    // Do something on failure
                }
            });
        }else {
            loginButton.setVisibility(View.GONE);
            loginToLikeMsgTV.setVisibility(View.GONE);
            logoutButton.setVisibility(View.VISIBLE);
        }
    }
}
