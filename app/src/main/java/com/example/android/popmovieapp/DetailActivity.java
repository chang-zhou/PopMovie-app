package com.example.android.popmovieapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class DetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new DetailFragment())
                    .commit();
        }
    }


    /**
     * A placeholder fragment containing a simple view.
     */
    public static class DetailFragment extends Fragment {

        private static final String LOG_TAG = DetailFragment.class.getSimpleName();

        private ArrayList<String> movieDetails;

        public DetailFragment() {}

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.detail_fragment, container, false);

            // The detail Activity called via intent.  Inspect the intent for movie detail data.
            Intent intent = getActivity().getIntent();
            if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
                movieDetails = intent.getStringArrayListExtra(Intent.EXTRA_TEXT);
                String original_title = movieDetails.get(0);
                String poster_url = movieDetails.get(1);
                String overview = movieDetails.get(2);
                String user_rating = movieDetails.get(3);
                String release_date = movieDetails.get(4);

                ((TextView) rootView.findViewById(R.id.original_title))
                        .setText(original_title);
                ((TextView) rootView.findViewById(R.id.overview))
                        .setText(overview);
                ((TextView) rootView.findViewById(R.id.overview))
                        .setMovementMethod(new ScrollingMovementMethod());
                ((TextView) rootView.findViewById(R.id.user_rating))
                        .setText(user_rating);
                ((TextView) rootView.findViewById(R.id.release_date))
                        .setText(release_date);

                Picasso.with(getActivity()) //
                        .load(poster_url) //
                        .into((ImageView) rootView.findViewById(R.id.thumbnail));


            }

            return rootView;
        }

    }
}
