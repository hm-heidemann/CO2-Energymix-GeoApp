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

  private static final int[] COLORS = {
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

  public static void runOnUiThread(Runnable action) {
    if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
      Handler mainHandler = new Handler(Looper.getMainLooper());
      mainHandler.post(action);
    } else {
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

            String type = geometry.getString("type");
            JSONArray coordinates = geometry.getJSONArray("coordinates");

            int co2Value = -1;
            if (!properties.isNull("year") && !properties.isNull("annual_co2")) {
              JSONArray yearValues = new JSONArray(properties.getString("year"));
              JSONArray co2Values = new JSONArray(properties.getString("annual_co2"));

              for (int j = 0; j < yearValues.length(); j++) {
                if (yearValues.getString(j).equals(year)) {
                  co2Value = co2Values.getInt(j);
                  break;
                }
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

              if (name_de.equals("Indien")) Log.d("PARSE", "" + co2Value);
              if (name_de.equals("China")) Log.d("PARSE", "" + co2Value);

              final int color;
              int idx;
              if (co2Value == -1) {
                idx = 10;
              } else if (co2Value > 900_000_000) {
                idx = 9;
              } else if (co2Value > 800_000_000) {
                idx = 8;
              } else if (co2Value > 700_000_000) {
                idx = 7;
              } else if (co2Value > 600_000_000) {
                idx = 6;
              } else if (co2Value > 500_000_000) {
                idx = 5;
              } else if (co2Value > 400_000_000) {
                idx = 4;
              } else if (co2Value > 300_000_000) {
                idx = 3;
              } else if (co2Value > 200_000_000) {
                idx = 2;
              } else if (co2Value > 100_000_000) {
                idx = 1;
              } else {
                idx = 0;
              }
              color = COLORS[idx];

              runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  for (List<GeoPoint> geoPoints : geoPointsList) {
                    Polygon polygon = new Polygon();
                    polygon.getFillPaint().setColor(color);
                    polygon.setPoints(geoPoints);
                    polygon.setTitle(name_de);

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

  public static void handleCO2PerCapitaJSONResponse(String year, String response, MapView map) throws JSONException {
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

            String type = geometry.getString("type");
            JSONArray coordinates = geometry.getJSONArray("coordinates");

            double co2PerCapitaValue = -1;
            if (!properties.isNull("year") && !properties.isNull("annual_co2_pc")) {
              JSONArray yearValues = new JSONArray(properties.getString("year"));
              JSONArray co2PerCapitaValues = new JSONArray(properties.getString("annual_co2_pc"));
              for (int j = 0; j < yearValues.length(); j++) {
                if (yearValues.getString(j).equals(year)) {
                  co2PerCapitaValue = co2PerCapitaValues.getDouble(j);
                  break;
                }
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

              final int color;
              int idx;
              if (co2PerCapitaValue == -1) {
                idx = 10;
              } else if (co2PerCapitaValue > 18) {
                idx = 9;
              } else if (co2PerCapitaValue > 16) {
                idx = 8;
              } else if (co2PerCapitaValue > 14) {
                idx = 7;
              } else if (co2PerCapitaValue > 12) {
                idx = 6;
              } else if (co2PerCapitaValue > 10) {
                idx = 5;
              } else if (co2PerCapitaValue > 8) {
                idx = 4;
              } else if (co2PerCapitaValue > 6) {
                idx = 3;
              } else if (co2PerCapitaValue > 4) {
                idx = 2;
              } else if (co2PerCapitaValue > 2) {
                idx = 1;
              } else {
                idx = 0;
              }
              color = COLORS[idx];

              runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  for (List<GeoPoint> geoPoints : geoPointsList) {
                    Polygon polygon = new Polygon();
                    polygon.getFillPaint().setColor(color);
                    polygon.setPoints(geoPoints);
                    polygon.setTitle(name_de);

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
