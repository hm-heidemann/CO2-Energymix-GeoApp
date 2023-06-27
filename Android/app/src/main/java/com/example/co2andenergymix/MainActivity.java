package com.example.co2andenergymix;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;

import org.osmdroid.api.IMapController;
import org.osmdroid.bonuspack.kml.ColorStyle;
import org.osmdroid.bonuspack.kml.KmlDocument;
import org.osmdroid.bonuspack.kml.LineStyle;
import org.osmdroid.bonuspack.kml.Style;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.FolderOverlay;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;

import java.lang.reflect.Type;

public class MainActivity extends AppCompatActivity {
  private MapView map;

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

    FetchDataAsyncTask fetchDataTask = new FetchDataAsyncTask();
    fetchDataTask.execute();
  }

  private class FetchDataAsyncTask extends AsyncTask<Void, Void, KmlDocument> {

    @Override
    protected KmlDocument doInBackground(Void... voids) {
      KmlDocument kmlDocument = new KmlDocument();
      boolean success = kmlDocument.parseKMLUrl("http://10.152.57.134:8080/geoserver/heidemann/ows?service=WFS&version=1.0.0&request=GetFeature&typeName=heidemann%3Aworld_co2_emissions&maxFeatures=1000000&outputFormat=application%2Fvnd.google-earth.kml%2Bxml");
      if (!success) {
        Log.d("FetchData", "Failed to load KML");
        return null;
      }
      return kmlDocument;
    }

    @Override
    protected void onPostExecute(KmlDocument kmlDocument) {
      if (kmlDocument != null) {
        FolderOverlay kmlOverlay = (FolderOverlay) kmlDocument.mKmlRoot.buildOverlay(map, null, null, kmlDocument);

        map.getOverlays().add(kmlOverlay);
        map.invalidate();
      }
    }
  }
}
