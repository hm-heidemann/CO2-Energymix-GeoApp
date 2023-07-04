package com.example.co2andenergymix;

import static com.example.co2andenergymix.helper.drawPolygons.handleCO2JSONResponse;
import static com.example.co2andenergymix.helper.drawPolygons.handleCO2PerCapitaJSONResponse;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONException;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import android.os.Bundle;
import android.preference.PreferenceManager;

import com.example.co2andenergymix.enums.DataType;

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
  private final Executor executor = Executors.newSingleThreadExecutor();

  private static final String URL_BASE = "http://10.152.57.134:8080/geoserver/heidemann/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=heidemann%3A";
  private static final String CO2_REQUEST = "world_co2_emissions&maxFeatures=1000000&outputFormat=application%2Fjson";
  private static final String CO2_PER_CAP_REQUEST = "world_co2_emissions_pc&maxFeatures=1000000&outputFormat=application%2Fjson";
  private static final String ENERGY_REQUEST = "world_energy_share&maxFeatures=1000000&outputFormat=application%2Fjson";
  private static final String ELECTRICITY_REQUEST = "world_electricity_share&maxFeatures=1000000&outputFormat=application%2Fjson";

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
    mapController.setZoom(3);
    GeoPoint startPoint = new GeoPoint(0, 0);
    mapController.setCenter(startPoint);

    getData(URL_BASE + CO2_REQUEST, DataType.CO2_TOTAL);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    if (id == R.id.action_co2_total) {
      getData(URL_BASE + CO2_REQUEST, DataType.CO2_TOTAL);
      return true;
    } else if (id == R.id.action_co2_per_capita) {
      getData(URL_BASE + CO2_PER_CAP_REQUEST, DataType.CO2_PER_CAPITA);
      return true;
    } else if (id == R.id.action_energy_mix) {
      getData(URL_BASE + ENERGY_REQUEST, DataType.ENERGY_MIX);
      return true;
    } else if (id == R.id.action_electricity_mix) {
      getData(URL_BASE + ELECTRICITY_REQUEST, DataType.ELECTRICITY_MIX);
      return true;
    }

    return super.onOptionsItemSelected(item);
  }

  public void getData(String url, DataType dataType) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        Log.d("SERVER:", url);
        String response = performApiRequest(url);
        if (response != null) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Log.d("SERVER:", "CONNECTED");
              try {
                switch(dataType) {
                  case CO2_TOTAL:
                    handleCO2JSONResponse("2021", response, map);
                    break;
                  case CO2_PER_CAPITA:
                    handleCO2PerCapitaJSONResponse("2021", response, map);
                    break;
                  case ENERGY_MIX:
                    // call handleEnergyMixJSONResponse function
                    break;
                  case ELECTRICITY_MIX:
                    // call handleElectricityMixJSONResponse function
                    break;
                }
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
        Log.e("SERVER:", "API Request failed with response code: " + responseCode);
      }

      connection.disconnect();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

}
