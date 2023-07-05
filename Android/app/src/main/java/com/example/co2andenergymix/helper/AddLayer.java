package com.example.co2andenergymix.helper;

import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;

import androidx.core.content.ContextCompat;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;
import org.osmdroid.views.overlay.infowindow.InfoWindow;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddLayer {

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

  private static Marker currentMarker;
  private static InfoWindow currentInfoWindow;

  private static DecimalFormat decimalFormat = new DecimalFormat("#0.00");

  public static void runOnUiThread(Runnable action) {
    if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
      Handler mainHandler = new Handler(Looper.getMainLooper());
      mainHandler.post(action);
    } else {
      action.run();
    }
  }

  public static void addCO2TotalLayer(String year, String response, MapView map) throws JSONException {
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

            long co2Value = -1;
            if (!properties.isNull("year") && !properties.isNull("annual_co2")) {
              JSONArray yearValues = new JSONArray(properties.getString("year"));
              JSONArray co2Values = new JSONArray(properties.getString("annual_co2"));

              for (int j = 0; j < yearValues.length(); j++) {
                if (yearValues.getString(j).equals(year)) {
                  co2Value = co2Values.getLong(j);
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

              // Setze Color abhängig vom CO2 Ausstoß
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
              color = CO2_COLORS[idx];

              long finalCo2Value = co2Value;
              runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  for (List<GeoPoint> geoPoints : geoPointsList) {
                    Polygon polygon = new Polygon();
                    polygon.getFillPaint().setColor(color);
                    polygon.setPoints(geoPoints);
                    polygon.setTitle(name_de);

                    polygon.setOnClickListener(new Polygon.OnClickListener() {
                      @Override
                      public boolean onClick(Polygon polygon, MapView mapView, GeoPoint eventPos) {
                        if (currentMarker != null && currentInfoWindow != null) {
                          currentMarker.closeInfoWindow();
                          map.getOverlayManager().remove(currentMarker);
                          currentMarker = null;
                          currentInfoWindow = null;
                        }

                        Marker marker = new Marker(mapView);
                        marker.setPosition(eventPos);
                        marker.setIcon(ContextCompat.getDrawable(mapView.getContext(), org.osmdroid.bonuspack.R.drawable.marker_default));

                        BasicInfoWindow infoWindow = new BasicInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, mapView);
                        marker.setInfoWindow(infoWindow);
                        marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                        String name = polygon.getTitle();
                        marker.setTitle(name);

                        StringBuilder markerDescription = new StringBuilder();

                        if (finalCo2Value == -1) {
                          markerDescription.append("Keine Daten für das Jahr ").append(year).append(" vorhanden.");
                        } else {
                          double roundedCO2Value = Math.round(finalCo2Value / 1_000_000.0);
                          String formattedCO2Value = decimalFormat.format(roundedCO2Value);
                          markerDescription.append("Der CO2 Ausstoß im Jahr ").append(year).append(" betrug ").append(formattedCO2Value).append(" Mio Tonnen.");
                        }

                        marker.setSubDescription(markerDescription.toString());
                        marker.showInfoWindow();
                        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                          @Override
                          public boolean onMarkerClick(Marker marker, MapView mapView) {
                            if (marker.isDisplayed()) {
                              marker.closeInfoWindow();
                            }
                            return true;
                          }
                        });

                        currentMarker = marker;
                        currentInfoWindow = infoWindow;
                        return true;
                      }
                    });

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


  public static void addCO2PerCapLayer(String year, String response, MapView map) throws JSONException {
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

              // Setze Color abhängig vom CO2 Ausstoß
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
              color = CO2_COLORS[idx];

              double finalCo2PerCapitaValue = co2PerCapitaValue;
              runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  for (List<GeoPoint> geoPoints : geoPointsList) {
                    Polygon polygon = new Polygon();
                    polygon.getFillPaint().setColor(color);
                    polygon.setPoints(geoPoints);
                    polygon.setTitle(name_de);

                    polygon.setOnClickListener(new Polygon.OnClickListener() {
                      @Override
                      public boolean onClick(Polygon polygon, MapView mapView, GeoPoint eventPos) {
                        if (currentMarker != null && currentInfoWindow != null) {
                          currentMarker.closeInfoWindow();
                          map.getOverlayManager().remove(currentMarker);
                          currentMarker = null;
                          currentInfoWindow = null;
                        }

                        Marker marker = new Marker(mapView);
                        marker.setPosition(eventPos);
                        marker.setIcon(ContextCompat.getDrawable(mapView.getContext(), org.osmdroid.bonuspack.R.drawable.marker_default));

                        BasicInfoWindow infoWindow = new BasicInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, mapView);
                        marker.setInfoWindow(infoWindow);
                        marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                        String name = polygon.getTitle();
                        marker.setTitle(name);

                        StringBuilder markerDescription = new StringBuilder();

                        if (finalCo2PerCapitaValue == -1) {
                          markerDescription.append("Keine Daten für das Jahr ").append(year).append(" vorhanden.");
                        } else {
                          double formattedCO2PerCapValue = Double.parseDouble(decimalFormat.format(finalCo2PerCapitaValue));
                          markerDescription.append("Der CO2 Ausstoß pro Kopf im Jahr ").append(year).append(" betrug ").append(formattedCO2PerCapValue).append(" Tonnen.");
                        }

                        marker.setSubDescription(markerDescription.toString());
                        marker.showInfoWindow();
                        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                          @Override
                          public boolean onMarkerClick(Marker marker, MapView mapView) {
                            if (marker.isDisplayed()) {
                              marker.closeInfoWindow();
                            }
                            return true;
                          }
                        });

                        currentMarker = marker;
                        currentInfoWindow = infoWindow;
                        return true;
                      }
                    });

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

  public static void addEnergyLayer(String year, String response, MapView map) throws JSONException {
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

            Map<String, Double> energyShares = new HashMap<>();
            String[] energyTypes = {"coal", "gas", "oil", "nuclear", "hydro", "solar", "wind"};
            for (String energyType : energyTypes) {
              if (!properties.isNull("year_" + energyType.substring(0, 1)) && !properties.isNull(energyType)) {
                JSONArray energyYears = new JSONArray(properties.getString("year_" + energyType.substring(0, 1)));
                JSONArray energyValues = new JSONArray(properties.getString(energyType));

                for (int j = 0; j < energyYears.length(); j++) {
                  if (energyYears.getString(j).equals(year)) {
                    energyShares.put(energyType, energyValues.getDouble(j));
                    break;
                  }
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
              if (energyShares.isEmpty()) {
                color = ENERGY_COLORS[1];
              } else {
                color = ENERGY_COLORS[0];
              }

              runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  for (List<GeoPoint> geoPoints : geoPointsList) {
                    Polygon polygon = new Polygon();
                    polygon.getFillPaint().setColor(color);
                    polygon.setPoints(geoPoints);
                    polygon.setTitle(name_de);

                    polygon.setOnClickListener(new Polygon.OnClickListener() {
                      @Override
                      public boolean onClick(Polygon polygon, MapView mapView, GeoPoint eventPos) {
                        if (currentMarker != null && currentInfoWindow != null) {
                          currentMarker.closeInfoWindow();
                          map.getOverlayManager().remove(currentMarker);
                          currentMarker = null;
                          currentInfoWindow = null;
                        }

                        Marker marker = new Marker(mapView);
                        marker.setPosition(eventPos);
                        marker.setIcon(ContextCompat.getDrawable(mapView.getContext(), org.osmdroid.bonuspack.R.drawable.marker_default));

                        BasicInfoWindow infoWindow = new BasicInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, mapView);
                        marker.setInfoWindow(infoWindow);
                        marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                        String name = polygon.getTitle();
                        marker.setTitle(name);

                        StringBuilder markerDescription = new StringBuilder();
                        markerDescription.append("<b>Energiemix im Jahr " + year + "</b><br/>");

                        if (energyShares.isEmpty()) {
                          markerDescription.append("Für dieses Land stehen keine Daten zur Verfügung.");
                        } else {
                          for (Map.Entry<String, Double> entry : energyShares.entrySet()) {
                            String keyString = entry.getKey();
                            char firstLetter = Character.toUpperCase(keyString.charAt(0));
                            String modifiedKey = firstLetter + keyString.substring(1);
                            double val = Double.parseDouble(decimalFormat.format(entry.getValue()));
                            markerDescription.append(modifiedKey).append(": ").append(val).append("%<br/>");
                          }
                        }

                        marker.setSubDescription(markerDescription.toString());
                        marker.showInfoWindow();
                        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                          @Override
                          public boolean onMarkerClick(Marker marker, MapView mapView) {
                            if (marker.isDisplayed()) {
                              marker.closeInfoWindow();
                            }
                            return true;
                          }
                        });

                        currentMarker = marker;
                        currentInfoWindow = infoWindow;
                        return true;
                      }
                    });

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

  public static void addElectricityLayer(String year, String response, MapView map) throws JSONException {
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

            Map<String, Double> energyShares = new HashMap<>();
            String[] energyTypes = {"coal", "gas", "nuclear", "hydro", "solar", "wind"};
            for (String energyType : energyTypes) {
              if (!properties.isNull("year_" + energyType.substring(0, 1)) && !properties.isNull(energyType)) {
                JSONArray energyYears = new JSONArray(properties.getString("year_" + energyType.substring(0, 1)));
                JSONArray energyValues = new JSONArray(properties.getString(energyType));

                for (int j = 0; j < energyYears.length(); j++) {
                  if (energyYears.getString(j).equals(year)) {
                    energyShares.put(energyType, energyValues.getDouble(j));
                    break;
                  }
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
              if (energyShares.isEmpty()) {
                color = ENERGY_COLORS[1];
              } else {
                color = ENERGY_COLORS[0];
              }

              runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  for (List<GeoPoint> geoPoints : geoPointsList) {
                    Polygon polygon = new Polygon();
                    polygon.getFillPaint().setColor(color);
                    polygon.setPoints(geoPoints);
                    polygon.setTitle(name_de);

                    polygon.setOnClickListener(new Polygon.OnClickListener() {
                      @Override
                      public boolean onClick(Polygon polygon, MapView mapView, GeoPoint eventPos) {
                        if (currentMarker != null && currentInfoWindow != null) {
                          currentMarker.closeInfoWindow();
                          map.getOverlayManager().remove(currentMarker);
                          currentMarker = null;
                          currentInfoWindow = null;
                        }

                        Marker marker = new Marker(mapView);
                        marker.setPosition(eventPos);
                        marker.setIcon(ContextCompat.getDrawable(mapView.getContext(), org.osmdroid.bonuspack.R.drawable.marker_default));

                        BasicInfoWindow infoWindow = new BasicInfoWindow(org.osmdroid.bonuspack.R.layout.bonuspack_bubble, mapView);
                        marker.setInfoWindow(infoWindow);
                        marker.setInfoWindowAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);

                        String name = polygon.getTitle();
                        marker.setTitle(name);

                        StringBuilder markerDescription = new StringBuilder();
                        markerDescription.append("<b>Elektrizitätsmix im Jahr " + year + "</b><br/>");

                        if (energyShares.isEmpty()) {
                          markerDescription.append("Für dieses Land stehen keine Daten zur Verfügung.");
                        } else {
                          for (Map.Entry<String, Double> entry : energyShares.entrySet()) {
                            String keyString = entry.getKey();
                            char firstLetter = Character.toUpperCase(keyString.charAt(0));
                            String modifiedKey = firstLetter + keyString.substring(1);
                            double val = Double.parseDouble(decimalFormat.format(entry.getValue()));
                            markerDescription.append(modifiedKey).append(": ").append(val).append("%<br/>");
                          }
                        }

                        marker.setSubDescription(markerDescription.toString());
                        marker.showInfoWindow();
                        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                          @Override
                          public boolean onMarkerClick(Marker marker, MapView mapView) {
                            if (marker.isDisplayed()) {
                              marker.closeInfoWindow();
                            }
                            return true;
                          }
                        });

                        currentMarker = marker;
                        currentInfoWindow = infoWindow;
                        return true;
                      }
                    });

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
