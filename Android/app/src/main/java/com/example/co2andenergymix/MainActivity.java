package com.example.co2andenergymix;

import static com.example.co2andenergymix.helper.AddLayer.addCO2TotalLayer;
import static com.example.co2andenergymix.helper.AddLayer.addCO2PerCapLayer;
import static com.example.co2andenergymix.helper.AddLayer.addElectricityLayer;
import static com.example.co2andenergymix.helper.AddLayer.addEnergyLayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.graphics.Color;
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
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

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
  private SeekBar yearSeekBar;

  private int minYear = 1750;
  private int maxYear = 2021;

  private TextView startYearTextView;
  private TextView endYearTextView;

  private DataType selectedLayer = DataType.CO2_TOTAL;
  private final Executor executor = Executors.newSingleThreadExecutor();

  private static final String URL_BASE = "http://10.152.57.134:8080/geoserver/heidemann/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=heidemann%3A";
  private static final String CO2_REQUEST = "world_co2_emissions&maxFeatures=1000000&outputFormat=application%2Fjson";
  private static final String CO2_PER_CAP_REQUEST = "world_co2_emissions_pc&maxFeatures=1000000&outputFormat=application%2Fjson";
  private static final String ENERGY_REQUEST = "world_energy_share&maxFeatures=1000000&outputFormat=application%2Fjson";
  private static final String ELECTRICITY_REQUEST = "world_electricity_share&maxFeatures=1000000&outputFormat=application%2Fjson";

  private static final int[] CO2_COLORS = {
      Color.parseColor("#fff7ec"),
      Color.parseColor("#feeacc"),
      Color.parseColor("#fdd8a7"),
      Color.parseColor("#fdc38d"),
      Color.parseColor("#fca16c"),
      Color.parseColor("#f67b51"),
      Color.parseColor("#e7533a"),
      Color.parseColor("#cf2518"),
      Color.parseColor("#ad0000"),
      Color.parseColor("#7f0000"),
      Color.GRAY
  };

  private static final int[] ENERGY_COLORS = {
      Color.BLUE,
      Color.GRAY
  };

  private static final long[] CO2_THRESHOLDS = {
      100_000_000,
      200_000_000,
      300_000_000,
      400_000_000,
      500_000_000,
      600_000_000,
      700_000_000,
      800_000_000,
      900_000_000,
      Long.MAX_VALUE
  };

  private static final long[] CO2_PER_CAPITA_THRESHOLDS = {
      2,
      4,
      6,
      8,
      10,
      12,
      14,
      16,
      18,
      Long.MAX_VALUE
  };


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

    TextView currentYearTextView = (TextView) findViewById(R.id.currentYear);
    startYearTextView = (TextView) findViewById(R.id.startYear);
    endYearTextView = (TextView) findViewById(R.id.endYear);
    yearSeekBar = (SeekBar) findViewById(R.id.yearSeekBar);

    yearSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
      int progressValue = 0;

      // Aktualisiere die Jahresanzeige wenn der Slider bewegt wird
      @Override
      public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        progressValue = progress;

        currentYearTextView.setText("Aktuelles Jahr: " + (minYear + progress));
      }

      @Override
      public void onStartTrackingTouch(SeekBar seekBar) {
      }

      // Lade den Layer neu wenn der Slider losgelassen wird
      @Override
      public void onStopTrackingTouch(SeekBar seekBar) {
        String year = String.valueOf(minYear + progressValue);
        switch (selectedLayer) {
          case CO2_TOTAL:
            getData(URL_BASE + CO2_REQUEST, DataType.CO2_TOTAL, year);
            break;
          case CO2_PER_CAPITA:
            getData(URL_BASE + CO2_PER_CAP_REQUEST, DataType.CO2_PER_CAPITA, year);
            break;
          case ENERGY_MIX:
            getData(URL_BASE + ENERGY_REQUEST, DataType.ENERGY_MIX, year);
            break;
          case ELECTRICITY_MIX:
            getData(URL_BASE + ELECTRICITY_REQUEST, DataType.ELECTRICITY_MIX, year);
            break;
          default:
            break;
        }
      }
    });

    getData(URL_BASE + CO2_REQUEST, DataType.CO2_TOTAL, "2021");
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();

    // Setze die Jahresanzeige abhängig vom Layer
    if (id == R.id.action_co2_total) {
      minYear = 1750;
      maxYear = 2021;
      selectedLayer = DataType.CO2_TOTAL;
    } else if (id == R.id.action_co2_per_capita) {
      minYear = 1750;
      maxYear = 2021;
      selectedLayer = DataType.CO2_PER_CAPITA;
    } else if (id == R.id.action_energy_mix) {
      minYear = 1965;
      maxYear = 2021;
      selectedLayer = DataType.ENERGY_MIX;
    } else if (id == R.id.action_electricity_mix) {
      minYear = 1985;
      maxYear = 2022;
      selectedLayer = DataType.ELECTRICITY_MIX;
    }

    yearSeekBar.setMax(maxYear - minYear);
    yearSeekBar.setProgress(yearSeekBar.getMax());

    if (startYearTextView != null && endYearTextView != null) {
      startYearTextView.setText(String.valueOf(minYear));
      endYearTextView.setText(String.valueOf(maxYear));
    }

    // Lade Layer abhängig von der Menüauswahl
    if (id == R.id.action_co2_total) {
      getData(URL_BASE + CO2_REQUEST, DataType.CO2_TOTAL, "2021");
    } else if (id == R.id.action_co2_per_capita) {
      getData(URL_BASE + CO2_PER_CAP_REQUEST, DataType.CO2_PER_CAPITA, "2021");
    } else if (id == R.id.action_energy_mix) {
      getData(URL_BASE + ENERGY_REQUEST, DataType.ENERGY_MIX, "2021");
    } else if (id == R.id.action_electricity_mix) {
      getData(URL_BASE + ELECTRICITY_REQUEST, DataType.ELECTRICITY_MIX, "2022");
    }

    return true;
  }

  public void updateLegend(int[] colors, long[] thresholds, DataType dataType) {
    LinearLayout legend = findViewById(R.id.legend);
    legend.removeAllViews();

    for (int i = 0; i < colors.length; i++) {
      View legendItem = getLayoutInflater().inflate(R.layout.legend_item, null);
      legendItem.findViewById(R.id.legend_color).setBackgroundColor(colors[i]);
      TextView legendLabel = legendItem.findViewById(R.id.legend_label);

      switch(dataType) {
        case CO2_TOTAL:
        case CO2_PER_CAPITA:
          if (colors[i] == Color.GRAY) {
            legendLabel.setText(" Keine Daten");
          } else if (i == 0) {
            legendLabel.setText(" <" + formatNumber(thresholds[0]));
          } else if (i == colors.length - 2) {
            legendLabel.setText(" " + formatNumber(thresholds[i - 1]) + "+");
          } else if (i == colors.length - 1) {
            continue;
          } else {
            legendLabel.setText(" " + formatNumber(thresholds[i - 1]) + " - " + formatNumber(thresholds[i]));
          }
          break;
        case ENERGY_MIX:
        case ELECTRICITY_MIX:
          if (colors[i] == Color.GRAY) {
            legendLabel.setText(" Keine Daten");
          } else {
            legendLabel.setText(" Daten vorhanden");
          }
          break;
        default:
          break;
      }
      legend.addView(legendItem);
    }
  }

  private String formatNumber(long number) {
    if (number >= 1_000_000_000) {
      return number / 1_000_000_000 + "B";
    } else if (number >= 1_000_000) {
      return number / 1_000_000 + "M";
    } else if (number >= 1_000) {
      return number / 1_000 + "K";
    } else {
      return String.valueOf(number);
    }
  }

  public void getData(String url, DataType dataType, String year) {
    executor.execute(new Runnable() {
      @Override
      public void run() {
        Log.d("SERVER:", url);
        String response = callGeoserverApi(url);
        if (response != null) {
          runOnUiThread(new Runnable() {
            @Override
            public void run() {
              Log.d("SERVER:", "CONNECTED");
              try {
                switch(dataType) {
                  case CO2_TOTAL:
                    addCO2TotalLayer(year, response, map);
                    updateLegend(CO2_COLORS, CO2_THRESHOLDS, DataType.CO2_TOTAL);
                    break;
                  case CO2_PER_CAPITA:
                    addCO2PerCapLayer(year, response, map);
                    updateLegend(CO2_COLORS, CO2_PER_CAPITA_THRESHOLDS, DataType.CO2_TOTAL);
                    break;
                  case ENERGY_MIX:
                    addEnergyLayer(year, response, map);
                    updateLegend(ENERGY_COLORS, new long[]{}, DataType.ENERGY_MIX);
                    break;
                  case ELECTRICITY_MIX:
                    addElectricityLayer(year, response, map);
                    updateLegend(ENERGY_COLORS, new long[]{}, DataType.ELECTRICITY_MIX);
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

  private String callGeoserverApi(String urlString) {
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
