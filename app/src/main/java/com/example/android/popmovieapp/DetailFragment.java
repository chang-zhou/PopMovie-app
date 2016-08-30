package com.example.android.popmovieapp;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

/**
 * Created by lay42 on 8/29/2016.
 */
public class DetailFragment extends Fragment {

    public final static String EXTRA_MESSAGE = "com.example.android.popmovieapp.app";

    private static final String LOG_TAG = DetailFragment.class.getSimpleName();

    private ArrayList<String> movieDetails;

    private ArrayList<String> trailer_urlData = new ArrayList<>();

    private String movie_id;

    private ArrayAdapter<String> myAdapter;


    public DetailFragment() {}


    @Override
    public void onStart(){
        super.onStart();
        updateUrlData();

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        //setHasOptionsMenu(true);

    }

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

        myAdapter = new ArrayAdapter<>
                (// The current context(this fragment's parent activity
                        getActivity(),
                        // ID of list item layout
                        R.layout.list_item_trailer,
                        // ID of the textview to populate
                        R.id.list_item_trailer,
                        // data from FetchUrlsTask
                        new ArrayList<String>());

        ListView listView = (ListView) rootView.findViewById(R.id.listView_trailers);
        listView.setAdapter(myAdapter);


        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l){
                //use intent to start a new activity
                String trailer_url = trailer_urlData.get(position);

                Uri trailer_uri = Uri.parse(trailer_url);

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(trailer_uri);

                if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                    startActivity(intent);
                } else {
                    Log.d(LOG_TAG, "Couldn't open trailer!");
                }

            }
        });


        return rootView;
    }


    private void updateUrlData(){
        Intent intent = getActivity().getIntent();
        if (intent != null && intent.hasExtra(Intent.EXTRA_TEXT)) {
            movieDetails = intent.getStringArrayListExtra(Intent.EXTRA_TEXT);
            movie_id = movieDetails.get(5);
        }
        FetchTrailerUrlsTask urlsTask = new FetchTrailerUrlsTask();
        urlsTask.execute(movie_id);

    }


    public class FetchTrailerUrlsTask extends AsyncTask<String, Void, ArrayList<String>> {

        private final String LOG_TAG = FetchTrailerUrlsTask.class.getSimpleName();

        private ArrayList<String> getTrailerUrlsFromJson(String TrailerJsonStr)
                throws JSONException {

            ArrayList<String> results = new ArrayList<>();

            JSONObject TrailerJSON = new JSONObject(TrailerJsonStr);

            //parse the Json and abstract the urls for the posters

            JSONArray trailers = TrailerJSON.getJSONArray("results");

            for(int i = 0; i < trailers.length(); i++){

                JSONObject trailer = trailers.getJSONObject(i);
                String trailer_key = trailer.getString("key");
                String poster_url =  "http://youtube.com/watch?v=" + trailer_key;
                results.add(poster_url);
            }

            return results;
        }

        @Override
        protected ArrayList<String> doInBackground(String...params) {
            // If there's no zip code, there's nothing to look up.  Verify size of params.
            if (params.length == 0) {
                return null;
            }

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String MovieJsonStr = null;


            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                final String BASE_URL = "http://api.themoviedb.org/3/movie";
                final String VIDEOS = "videos";
                final String APPID_PARAM = "api_key";

                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                        .appendPath(params[0])
                        .appendPath(VIDEOS)
                        .appendQueryParameter(APPID_PARAM, BuildConfig.TheMovieDB_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());

                //Log.v(LOG_TAG, "Build url: " + url);

                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                MovieJsonStr = buffer.toString();
                //Log.v(LOG_TAG, MovieJsonStr);

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return getTrailerUrlsFromJson(MovieJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, e.getMessage(), e);
                e.printStackTrace();
            }

            // This will only happen if there was an error getting or parsing the forecast.
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            if (result != null) {
                trailer_urlData.clear();
                myAdapter.clear();
                int count = 1;
                for(String url : result){
                    trailer_urlData.add(url);
                    myAdapter.add("Trailer " + count);
                    count++;

                }

            }
        }
    }
}
