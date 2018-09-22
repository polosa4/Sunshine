package com.example.oleg.sunshine;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 */
public class ForecastFragment extends Fragment {

    private ArrayAdapter<String> adapter;

    public ForecastFragment() {
        // Required empty public constructor
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
    }

    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();
        if (id == R.id.action_refresh){
            updateWeather();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateWeather(){
        FetchWeatherTask task = new FetchWeatherTask();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        String location = prefs.getString(getString(R.string.pref_location_key), getString(R.string.pref_location_default));
        task.execute(location);
    }

    public void onStart() {
        super.onStart();
        updateWeather();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View view = inflater.inflate(R.layout.fragment_first, container, false);


        final List<String> weekForecst = new ArrayList<String>();

        adapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, weekForecst);
        ListView list = (ListView) view.findViewById(R.id.item_list);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {


                Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
                detailIntent.putExtra("data", weekForecst.get(position));
                startActivity(detailIntent);
                //int duration = Toast.LENGTH_SHORT;

                //Toast toast = Toast.makeText(getContext(), , duration);
                //toast.show();

            }
        });
        return view;
    }

    public class FetchWeatherTask extends AsyncTask<String, Void, String []>{


        private String formatHigh(double high) {
                high = (high * 1.8) + 32;
                long roundedHigh = Math.round(high);
            	return Long.toString(roundedHigh);
        }
        private String formatLow(double low) {
                low = (low * 1.8) + 32;
                long roundedLow = Math.round(low);
                return Long.toString(roundedLow);
        }

        public String [] getMaxTemperatureForDay(String weatherJsonStr){
            String [] dates  = new String[4];
            String [] tempsH  = new String[4];
            String [] tempsL  = new String[4];


            if (dates[0] == null) {
                try {
                    JSONObject weather = new JSONObject(weatherJsonStr);
                    JSONObject forecast = weather.getJSONObject("forecast");
                    JSONObject simpleforecast = forecast.getJSONObject("simpleforecast");
                    JSONArray dayForecast = simpleforecast.getJSONArray("forecastday");
                    for (int i = 0; i < dayForecast.length(); i++) {
                        JSONObject forecastDay = dayForecast.getJSONObject(i);
                        JSONObject tempHigh = forecastDay.getJSONObject("high");
                        JSONObject tempLow = forecastDay.getJSONObject("low");
                        JSONObject date = forecastDay.getJSONObject("date");
                        String condition = forecastDay.getString("conditions");
                        String tempH = tempHigh.getString("celsius");
                        String tempL = tempLow.getString("celsius");
                        String day = date.getString("day");
                        String month = date.getString("monthname_short");
                        String weekday = date.getString("weekday_short");

                        tempsH[i] = tempH;
                        tempsL[i] = tempL;
                        dates[i] = weekday + ", " + month + " " + day + " - " + condition + " - ";
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            SharedPreferences sharedPrefs =
                    PreferenceManager.getDefaultSharedPreferences(getActivity());
            String unitType = sharedPrefs.getString(
                    getString(R.string.pref_units_key),
                    getString(R.string.pref_units_metric));

            if (unitType.equals(getString(R.string.pref_units_imperial)) && (dates[0] != null)){
                for (int i = 0; i < dates.length; i++) {
                    Double max = (double) Integer.parseInt(tempsH[i]);
                    Double min = (double) Integer.parseInt(tempsL[i]);
                    String tempH = formatHigh(max);
                    String tempL = formatLow(min);
                    dates[i] += " " +tempH + "/" +tempL;
                }
            }else {
                for (int i = 0; i < dates.length; i++) {
                    dates[i] += " " + tempsH[i] + "/" + tempsL[i];
                }
            }

            return dates;
        }


        private final String LOG_TAG = FetchWeatherTask.class.getSimpleName();
        @Override
        protected String [] doInBackground(String... params) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;
            String [] newRow;
            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast
                URL url = new URL("https://api.wunderground.com/api/127fe8e376eda81b/forecast/q/" + params[0] + ".json");

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
                forecastJsonStr = buffer.toString();
                newRow = getMaxTemperatureForDay(forecastJsonStr);
                //for (String s : newRow)
                //Log.v(LOG_TAG, "Forecast JSON String: " +s);
            } catch (IOException e) {
                Log.e("PlaceholderFragment", "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }
            return newRow;
        }

        @Override
        protected void onPostExecute(String [] result) {
            if (result != null){
                adapter.clear();
                for (String dayForecastStr : result){
                    adapter.add(dayForecastStr);
                }
            }
        }

    }
}