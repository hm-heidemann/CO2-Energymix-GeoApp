package com.example.co2andenergymix;

import static com.example.co2andenergymix.helper.drawPolygons.handleCO2JSONResponse;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import org.json.JSONException;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import android.os.Bundle;
import android.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
  private MapView map;
  private ProgressBar loadingIndic;
  private View darkOverlay;
  private final Executor executor = Executors.newSingleThreadExecutor();

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Context ctx = getApplicationContext();
    Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
    setContentView(R.layout.activity_main);

    map = (MapView) findViewById(R.id.mapview);
    map.setTileSource(TileSourceFactory.MAPNIK);
    map.setBuiltInZoomControls(true);
    map.setMultiTouchControls(true);

    IMapController mapController = map.getController();
    mapController.setZoom(9.5);
    GeoPoint startPoint = new GeoPoint(48.8583, 2.2944);
    mapController.setCenter(startPoint);

    getData();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here.
    int id = item.getItemId();

    if (id == R.id.action_co2_total) {
      // Laden Sie die Daten für den CO2-Gesamtlayer
      return true;
    } else if (id == R.id.action_co2_per_capita) {
      // Laden Sie die Daten für den CO2-pro-Kopf-Layer
      return true;
    } else if (id == R.id.action_energy_mix) {
      // Laden Sie die Daten für den Energiemix-Layer
      return true;
    } else if (id == R.id.action_electricity_mix) {
      // Laden Sie die Daten für den Elektrizitätsmix-Layer
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  public void getData() {
    String url = "http://10.152.57.134:8080/geoserver/heidemann/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=heidemann%3Aworld_co2_emissions&maxFeatures=1000000&outputFormat=application%2Fjson";
    executor.execute(new Runnable() {
      @Override
      public void run() {
        String response = performApiRequest(url);
        if (response != null) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Log.d("Tag:", "CONNECTED");
              try {
                handleCO2JSONResponse("2021", response, map);
              } catch (JSONException e) {
                e.printStackTrace();
              }
            }
          });
        }
      }
    });
  }

  private String performApiRequest(String urlString) {
    try {
      URL url = new URL(urlString);
      HttpURLConnection connection = (HttpURLConnection) url.openConnection();
      connection.setRequestMethod("GET");
      connection.setConnectTimeout(5000);
      connection.setReadTimeout(5000);

      int responseCode = connection.getResponseCode();
      if (responseCode == HttpURLConnection.HTTP_OK) {
        InputStream inputStream = connection.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
          stringBuilder.append(line);
        }
        reader.close();
        return stringBuilder.toString();
      } else {
        Log.e("Tag:", "API Request failed with response code: " + responseCode);
      }

      connection.disconnect();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

}
