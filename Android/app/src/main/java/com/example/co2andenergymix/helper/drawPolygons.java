package com.example.co2andenergymix.helper;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;

import java.util.ArrayList;
import java.util.List;

public class drawPolygons {

  public static void runOnUiThread(Runnable action) {
    if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
      // Nicht im Hauptthread, führe die Aktion auf dem Hauptthread aus
      Handler mainHandler = new Handler(Looper.getMainLooper());
      mainHandler.post(action);
    } else {
      // Im Hauptthread, führe die Aktion direkt aus
      action.run();
    }
  }

  public static void handleCO2JSONResponse(String year, String response, MapView map) throws JSONException {
    new Thread(new Runnable() {
      @Override
      public void run() {
        try {
          map.getOverlayManager().clear();
          JSONObject jsonObject = new JSONObject(response);
          JSONArray features = jsonObject.getJSONArray("features");
          for (int i = 0; i < features.length(); i++) {
            JSONObject feature = features.getJSONObject(i);
            JSONObject geometry = feature.getJSONObject("geometry");
            JSONObject properties = feature.getJSONObject("properties");
            String name_de = properties.getString("name_de");
            JSONArray yearValues = new JSONArray(properties.getString("year"));
            JSONArray co2Values = new JSONArray(properties.getString("annual_co2"));
            String type = geometry.getString("type");
            JSONArray coordinates = geometry.getJSONArray("coordinates");

            // Find the corresponding co2Value for the given year
            String co2Value = "N/A";
            for (int j = 0; j < yearValues.length(); j++) {
              if (yearValues.getString(j).equals(year)) {
                co2Value = co2Values.getString(j);
                break;
              }
            }

            if (!name_de.equalsIgnoreCase("Antarktika") && type.equals("MultiPolygon")) {
              List<List<GeoPoint>> geoPointsList = new ArrayList<>();

              for (int j = 0; j < coordinates.length(); j++) {
                JSONArray polygonCoordinates = coordinates.getJSONArray(j);
                List<GeoPoint> geoPoints = new ArrayList<>();

                for (int k = 0; k < polygonCoordinates.length(); k++) {
                  JSONArray ringCoordinates = polygonCoordinates.getJSONArray(k);

                  for (int m = 0; m < ringCoordinates.length(); m++) {
                    JSONArray pointCoordinates = ringCoordinates.getJSONArray(m);
                    double longitude = pointCoordinates.getDouble(0);
                    double latitude = pointCoordinates.getDouble(1);
                    geoPoints.add(new GeoPoint(latitude, longitude));
                  }
                }

                geoPointsList.add(geoPoints);
              }

              final String co2ValueFinal = co2Value;

              runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  for (List<GeoPoint> geoPoints : geoPointsList) {
                    Polygon polygon = new Polygon();
                    polygon.getFillPaint().setColor(Color.argb(128, 255, 0, 0));
                    polygon.setPoints(geoPoints);
                    polygon.setTitle(name_de);
                    Log.d("Tag:", name_de + " wurde verarbeitet\n" + "Koordinaten: " + geoPoints.toString());

                    map.getOverlayManager().add(polygon);
                  }

                  map.invalidate();
                }
              });
            }
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }
      }
    }).start();
  }
}
