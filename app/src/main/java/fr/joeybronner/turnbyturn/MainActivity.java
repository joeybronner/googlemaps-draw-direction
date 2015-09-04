package fr.joeybronner.turnbyturn;

import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.android.gms.maps.model.LatLngBounds;

import android.view.View;
import android.widget.Button;
import android.widget.EditText;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import fr.joeybronner.turnbyturn.utilsapi.AsyncTaskDrawDirection;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {

    GoogleMap mMap;
    private int once = 1;
    private LatLngBounds latlngBounds;
    private Button bNavigation;
    private Polyline newPolyline;
    private double startLat, startLng, endLat, endLng;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MapFragment mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        bNavigation = (Button) findViewById(R.id.btGo);
        bNavigation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                class Sess extends AsyncTask<Void, Integer, Long> {
                    protected Long doInBackground(Void... arg0) {
                        try {
                            EditText etDestination = (EditText) findViewById(R.id.etDestination);
                            String address = etDestination.getText().toString();
                            JSONObject jsonObject = getLocationInfo(address);
                            endLat = ((JSONArray) jsonObject.get("results")).getJSONObject(0)
                                    .getJSONObject("geometry").getJSONObject("location")
                                    .getDouble("lat");
                            endLng = ((JSONArray) jsonObject.get("results")).getJSONObject(0)
                                    .getJSONObject("geometry").getJSONObject("location")
                                    .getDouble("lng");
                        } catch (JSONException e) {
                            // Nothing.
                        }
                        return null;
                    }

                    protected void onPostExecute(Long result) {
                        findDirections(startLat, startLng, endLat, endLng, "driving");
                    }
                }
                new Sess().execute();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;
        mMap.setMyLocationEnabled(true);
        mMap.setOnMyLocationChangeListener(myLocationChangeListener);
    }

    private GoogleMap.OnMyLocationChangeListener myLocationChangeListener = new GoogleMap.OnMyLocationChangeListener() {
        @Override
        public void onMyLocationChange(Location location) {
            startLat = location.getLatitude();
            startLng = location.getLongitude();
            mMap.setMyLocationEnabled(true);
            LatLng loc = new LatLng(startLat, startLng);
            if (mMap != null) {
                if (once == 1) {
                    mMap.clear();
                    /*mMap.addMarker(new MarkerOptions()
                            .position(loc)
                            .title("Joey")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.moto)));*/
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(loc, 16.0f));
                    once = 0;
                }
            }
        }
    };

    public void handleGetDirectionsResult(ArrayList<LatLng> directionPoints) {
        PolylineOptions rectLine = new PolylineOptions().width(5).color(Color.RED);

        for (int i = 0; i < directionPoints.size(); i++) {
            rectLine.add(directionPoints.get(i));
        }
        if (newPolyline != null) {
            newPolyline.remove();
        }
        newPolyline = mMap.addPolyline(rectLine);

        latlngBounds = createLatLngBoundsObject(new LatLng(startLat, startLng), new LatLng(endLat, endLng));
        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(latlngBounds,
                getWindowManager().getDefaultDisplay().getWidth(),
                getWindowManager().getDefaultDisplay().getHeight(), 150));
    }

    private LatLngBounds createLatLngBoundsObject(LatLng firstLocation, LatLng secondLocation) {
        if (firstLocation != null && secondLocation != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(firstLocation).include(secondLocation);

            return builder.build();
        }
        return null;
    }

    public void findDirections(double fromPositionDoubleLat, double fromPositionDoubleLong, double toPositionDoubleLat, double toPositionDoubleLong, String mode) {
        Map<String, String> map = new HashMap<String, String>();
        map.put(AsyncTaskDrawDirection.USER_CURRENT_LAT, String.valueOf(fromPositionDoubleLat));
        map.put(AsyncTaskDrawDirection.USER_CURRENT_LONG, String.valueOf(fromPositionDoubleLong));
        map.put(AsyncTaskDrawDirection.DESTINATION_LAT, String.valueOf(toPositionDoubleLat));
        map.put(AsyncTaskDrawDirection.DESTINATION_LONG, String.valueOf(toPositionDoubleLong));
        map.put(AsyncTaskDrawDirection.DIRECTIONS_MODE, mode);

        AsyncTaskDrawDirection asyncTask = new AsyncTaskDrawDirection(this);
        asyncTask.execute(map);
    }

    public static JSONObject getLocationInfo(String address) {
        StringBuilder stringBuilder = new StringBuilder();
        try {

            address = address.replaceAll(" ", "%20");

            HttpPost httppost = new HttpPost("http://maps.google.com/maps/api/geocode/json?address=" + address + "&sensor=false");
            HttpClient client = new DefaultHttpClient();
            HttpResponse response;
            stringBuilder = new StringBuilder();


            response = client.execute(httppost);
            HttpEntity entity = response.getEntity();
            InputStream stream = entity.getContent();
            int b;
            while ((b = stream.read()) != -1) {
                stringBuilder.append((char) b);
            }
        } catch (ClientProtocolException e) {
        } catch (IOException e) {
        }

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject = new JSONObject(stringBuilder.toString());
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return jsonObject;
    }

}
