package tof.cv.mpp.Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;

import tof.cv.mpp.R;
import tof.cv.mpp.adapter.TweetItemAdapter;
import tof.cv.mpp.bo.Connections;
import tof.cv.mpp.bo.Tweets;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.maps.GeoPoint;
import com.google.gson.Gson;

public class UtilsWeb {

	public static InputStream getJSONData(String url, Context context) {
		DefaultHttpClient httpClient = new DefaultHttpClient();

		String myVersion = "0.0";
		PackageManager manager = context.getPackageManager();

		try {
			myVersion = (manager.getPackageInfo(context.getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		httpClient.getParams().setParameter(
				"http.useragent",
				"BeTrains " + myVersion + " for Android - "
						+ System.getProperty("http.agent"));

		URI uri;
		InputStream data = null;
		try {
			uri = new URI(url);
			HttpGet method = new HttpGet(uri);
			HttpResponse response = httpClient.execute(method);
			data = response.getEntity().getContent();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return data;
	}

	public static Connections getAPIConnections(String year, String month,
			String day, String hour, String minutes, String language,
			String departure, String arrival, String departureArrival,
			String trainsOnly, final Context context) {
		String TAG = "BETRAINS";

		DbAdapterConnection mDbHelper = new DbAdapterConnection(context);
		mDbHelper.open();

		if (day.length() == 1)
			day = "0" + day;

		if (month.length() == 1)
			month = "0" + month;
		if (month.contentEquals("13"))
			month = "01";

		String url = "http://api.irail.be/connections.php?to="
				// String url = "http://dev.api.irail.be/connections.php?to="
				+ arrival + "&from=" + departure + "&date=" + day + month
				+ year + "&time=" + hour + minutes + "&timeSel="
				+ departureArrival + "&lang=" + language + "&typeOfTransport="
				+ trainsOnly + "&format=json&fast=true";
		url = url.replace(" ", "%20");
		Log.v(TAG, url);

		try {
			InputStream is = Utils.DownloadJsonFromUrlAndCacheToSd(url,
					Utils.DIRPATH, Utils.FILENAMECONN, context);

			Gson gson = new Gson();
			final Reader reader = new InputStreamReader(is);
			return gson.fromJson(reader, Connections.class);

		} catch (Exception ex) {
			ex.printStackTrace();
			Log.i("", "*******");
			mDbHelper.close();
			return null;
		}
	}

	public static InputStream retrieveStream(String url, Context context) {

		DefaultHttpClient client = new DefaultHttpClient();

		HttpGet request = new HttpGet(url);

		// TODO: stocker la version pour ne pas faire un appel à chaque fois.
		String myVersion = "0.0";
		PackageManager manager = context.getPackageManager();
		try {
			myVersion = (manager.getPackageInfo(context.getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		request.setHeader("User-Agent", "Waza_Be: BeTrains " + myVersion
				+ " for Android");

		Log.w("getClass().getSimpleName()", "URL TO CHECK " + url);

		try {
			HttpResponse response = client.execute(request);
			final int statusCode = response.getStatusLine().getStatusCode();

			if (statusCode != HttpStatus.SC_OK) {
				Log.w("getClass().getSimpleName()", "Error " + statusCode
						+ " for URL " + url);
				return null;
			}

			HttpEntity getResponseEntity = response.getEntity();
			Log.w("getClass().getSimpleName()", "Read the url:  " + url);
			return getResponseEntity.getContent();

		} catch (IOException e) {
			Log.w("getClass().getSimpleName()", " Error for URL " + url, e);
		}

		return null;

	}

	public static void loadTweets(final Activity a, final ListView l) {
		new Thread(new Runnable() {
			public void run() {
				try {
					String url = "http://search.twitter.com/search.json?q=BETRAINS";
					SharedPreferences mDefaultPrefs = PreferenceManager
							.getDefaultSharedPreferences(a);
					;
					if (mDefaultPrefs.getBoolean("mNMBS", a.getResources()
							.getBoolean(R.bool.nmbs)))
						url += "%20OR%20NMBS";

					if (mDefaultPrefs.getBoolean("mSNCB", a.getResources()
							.getBoolean(R.bool.sncb)))
						url += "%20OR%20SNCB";

					if (mDefaultPrefs.getBoolean("miRail", true))
						url += "%20OR%20irail";

					if (mDefaultPrefs.getBoolean("mNavetteurs", a
							.getResources().getBoolean(R.bool.navetteurs)))
						url += "%20OR%20navetteurs";

					url += "&rpp=50";

					InputStream is = Utils.DownloadJsonFromUrlAndCacheToSd(url,
							"/Android/data/BeTrains/Twitter", null, a);
					Gson gson = new Gson();
					final Reader reader = new InputStreamReader(is);
					final Tweets tweets = gson.fromJson(reader, Tweets.class);

					a.runOnUiThread(new Thread(new Runnable() {
						public void run() {

							l.setAdapter(new TweetItemAdapter(a,
									R.layout.row_tweet, tweets.results));
						}
					}));
				} catch (Exception e) {
					e.printStackTrace();
					a.runOnUiThread(new Thread(new Runnable() {
						public void run() {
							TextView tv = (TextView) a.findViewById(R.id.fail);
							tv.setVisibility(View.VISIBLE);

						}
					}));

				}

			}
		}).start();

	}

	public static Vehicle getAPIvehicle(String vehicle, final Context context) {
		// TODO
		String langue = context.getString(R.string.url_lang_2);
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				"prefnl", false))
			langue = "nl";

		String url = "http://api.irail.be/vehicle.php/?id=" + vehicle
				+ "&format=JSON&fast=true" + "&lang=" + langue;
		System.out.println("Affiche les infos train depuis la page: " + url);

		try {
			// Log.i(TAG, "Json Parser started..");
			Gson gson = new Gson();
			Reader r = new InputStreamReader(getJSONData(url, context));
			return gson.fromJson(r, Vehicle.class);

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}

	}

	public class Vehicle {

		private VehicleStops stops;
		private String version;
		private String vehicle;
		private String timestamp;

		public VehicleStops getVehicleStops() {
			return stops;
		}

		public String getVersion() {
			return version;
		}

		public String getId() {
			return vehicle;
		}

		public String getTimestamp() {
			return timestamp;
		}

	}

	public class VehicleStops {

		private ArrayList<VehicleStop> stop;

		public ArrayList<VehicleStop> getVehicleStop() {
			return stop;
		}
	}

	public class VehicleStop {

		private String station;
		private String time;
		private String delay;

		public String getStation() {
			return station;
		}

		public String getTime() {
			return time;
		}

		public String getDelay() {
			return delay;
		}

		public String getStatus() {
			if (delay.contentEquals("0"))
				return "";

			return "+" + Integer.valueOf(delay) / 60 + "'";
		}
	}

	public static Station getAPIstation(String station, final Context context) {
		// TODO
		String langue = context.getString(R.string.url_lang_2);
		if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				"prefnl", false))
			langue = "nl";

		String url = "http://api.irail.be/liveboard.php/?station=" + station
				+ "&format=JSON&fast=true" + "&lang=" + langue;
		System.out.println("Show station from: " + url);

		try {
			// Log.i(TAG, "Json Parser started..");
			Gson gson = new Gson();
			Reader r = new InputStreamReader(getJSONData(url, context));
			return gson.fromJson(r, Station.class);

		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}

	}

	public class Station {

		private String version;
		private String station;
		private StationStationinfo stationinfo;
		private StationDepartures departures;

		public StationStationinfo getStationStationinfo() {
			return stationinfo;
		}

		public String getVersion() {
			return version;
		}

		public String getStation() {
			return station;
		}

		public StationDepartures getStationDepartures() {
			return departures;
		}

	}

	public class StationStationinfo {

		private String id;
		private String locationX;
		private String locationY;

		public String getId() {
			return id;
		}

		public String getLocationX() {
			return locationX;
		}

		public String getLocationY() {
			return locationY;
		}
	}

	public class StationDepartures {

		private ArrayList<StationDeparture> departure;

		public ArrayList<StationDeparture> getStationDeparture() {
			return departure;
		}
	}

	public class StationDeparture {

		private String station;
		private String time;
		private String delay;
		private String platform;
		private String vehicle;

		public String getStation() {
			return station;
		}

		public String getTime() {
			return time;
		}

		public String getDelay() {
			return delay;
		}

		public String getPlatform() {
			return platform;
		}

		public String getVehicle() {
			return vehicle;
		}

		public String getStatus() {
			if (delay.contentEquals("0"))
				return "";

			return "+" + Integer.valueOf(delay) / 60 + "'";
		}
	}

	public static Document getKml(GeoPoint src, GeoPoint dest) {
		// connect to map web service
		StringBuilder urlString = new StringBuilder();
		urlString.append("http://maps.google.com/maps?f=d&hl=en");
		urlString.append("&saddr=");// from
		urlString.append(Double.toString((double) src.getLatitudeE6() / 1.0E6));
		urlString.append(",");
		urlString
				.append(Double.toString((double) src.getLongitudeE6() / 1.0E6));
		urlString.append("&daddr=");// to
		urlString
				.append(Double.toString((double) dest.getLatitudeE6() / 1.0E6));
		urlString.append(",");
		urlString
				.append(Double.toString((double) dest.getLongitudeE6() / 1.0E6));
		urlString.append("&ie=UTF8&0&om=0&output=kml");
		// Log.d("xxx","URL="+urlString.toString());
		// get the kml (XML) doc. And parse it to get the coordinates(direction
		// route).

		HttpURLConnection urlConnection = null;
		URL url = null;

		try {
			url = new URL(urlString.toString());
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestMethod("GET");
			urlConnection.setDoOutput(true);
			urlConnection.setDoInput(true);
			urlConnection.connect();

			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			return db.parse(urlConnection.getInputStream());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public static GeoPoint findVehiclePosition(String name, Context context) {
		String url = "http://railtime.be/website/apercu-du-trafic-trains?tn="
				+ name.replaceAll("\\D", "");

		InputStream source = retrieveStream(url, context);
		BufferedReader br = new BufferedReader(new InputStreamReader(source));
		String read = "";
		boolean foundLat = false;
		boolean foundLon = false;
		String lat = null;
		String lon = null;
		try {
			read = br.readLine();
			while (!foundLat || !foundLon) {
				if (read.contains("PARAMS.CenterLat")) {
					String[]array=read.split("=");
					lat=array[1].trim().replaceAll(";","");
					foundLat = true;	
				}

				if (read.contains("PARAMS.CenterLon")) {
					String[]array=read.split("=");
					lon=array[1].trim().replaceAll(";","");
					foundLon = true;
				}
				read = br.readLine();
				if (read==null)
						break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return new GeoPoint((int) (Float.valueOf(lat) * 1E6), (int) (Float.valueOf(lon)  * 1E6));

	}
}