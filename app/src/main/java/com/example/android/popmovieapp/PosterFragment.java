package com.example.android.popmovieapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;

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
 * Created by lay42 on 7/23/2016.
 */
public class PosterFragment extends Fragment {

    private ArrayList<String> urlData = new ArrayList<>();

    private ArrayList<ArrayList<String>> detailsData = new ArrayList<>();

    public PosterFragment(){}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Add this line in order for this fragment to handle menu events.
        //setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){

        // The ArrayAdapter will take data from a source and
        // use it to populate the ListView it's attached to.
        final ImageAdapter mImageAdapter = new ImageAdapter(getActivity(), urlData);

        View rootView = inflater.inflate(R.layout.poster_fragment, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        GridView gridview = (GridView) rootView.findViewById(R.id.gridview);
        gridview.setAdapter(mImageAdapter);

        gridview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {

                int index = (int)mImageAdapter.getItemId(position);

                ArrayList<String> detail = detailsData.get(index);

                Intent intent = new Intent(getActivity(), DetailActivity.class)
                        .putExtra(Intent.EXTRA_TEXT, detail);
                startActivity(intent);


            }
        });


        return rootView;
    }

    public class ImageAdapter extends BaseAdapter {

        private Context mContext;

        private ArrayList<String> urls;

        public ImageAdapter(Context c, ArrayList<String> imageUrls) {
            mContext = c;
            urls = imageUrls;
        }


        // create a new ImageView for each item referenced by the Adapter
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            if (convertView == null) {
                // if it's not recycled, initialize some attributes
                imageView = new ImageView(mContext);
                imageView.setLayoutParams(new GridView.LayoutParams(350,500));
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                //imageView.setPadding(8, 8, 8, 8);
            } else {
                imageView = (ImageView) convertView;
            }

            // Get the image URL for the current position.
                 String url = getItem(position);


            Picasso.with(mContext) //
                     .load(url) //
                     .into(imageView);

            return imageView;
        }


        @Override
        public int getCount() {
            return urls.size();
        }

        @Override
        public String getItem(int position) {
            return urls.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

    }


    private void updateUrlData(){
        FetchUrlsTask urlsTask = new FetchUrlsTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String orderBy = prefs.getString(getString(R.string.pref_orderBy_key),
                getString(R.string.pref_orderBy_default));
        urlsTask.execute(orderBy);

    }

    @Override
    public void onStart(){
        super.onStart();
        updateUrlData();
    }


    public class FetchUrlsTask extends AsyncTask<String, Void, ArrayList<String>> {

        private final String LOG_TAG = FetchUrlsTask.class.getSimpleName();

        private ArrayList<String> getMovieUrlsFromJson(String MovieJsonStr)
                throws JSONException {

            ArrayList<String> results = new ArrayList<>();

            JSONObject MovieJSON = new JSONObject(MovieJsonStr);

            //parse the Json and abstract the urls for the posters

            JSONArray posters = MovieJSON.getJSONArray("results");

            for(int i = 0; i < posters.length(); i++){

                JSONObject poster = posters.getJSONObject(i);
                String poster_path = poster.getString("poster_path");
                String poster_url =  "http://image.tmdb.org/t/p/" + "w185//" + poster_path;
                results.add(poster_url);

                ArrayList<String> detail = new ArrayList<>();
                String original_title = poster.getString("original_title");
                String overview = poster.getString("overview");
                String user_rating = poster.getDouble("vote_average") + "/10";
                String release_date = poster.getString("release_date");

                detail.add(original_title);
                detail.add(poster_url);
                detail.add(overview);
                detail.add(user_rating);
                detail.add(release_date);
                detailsData.add(detail);
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
                final String APPID_PARAM = "api_key";

                Uri builtUri = Uri.parse(BASE_URL).buildUpon()
                        .appendPath(params[0])
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
                return getMovieUrlsFromJson(MovieJsonStr);
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
                urlData.clear();
                for(String url : result){
                    urlData.add(url);
                }
                // New data is back from the server.  Hooray!
            }
        }
    }


}
