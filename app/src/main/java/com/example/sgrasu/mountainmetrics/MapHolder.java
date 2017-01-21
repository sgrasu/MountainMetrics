package com.example.sgrasu.mountainmetrics;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telecom.Call;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;


/**
 * modified from flipped-classroom code
 */

public class MapHolder implements OnMapReadyCallback {

    private ArrayList<Marker> markers;
    private GoogleMap gMap;
    private Context context;
    private LocationManager locationManager;
    private Location currLoc;
    public interface MapHolderCallback{
        public void onMapHolderReady();
    }
    private MapHolderCallback callback;
    public MapHolder(Context ctx, MapHolderCallback cback) {
        context = ctx;
        locationManager =(LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        markers = new ArrayList<Marker>();
        callback = cback;
    }

    public boolean warnIfNotReady() {
        if (gMap == null) {
            Toast.makeText(context, "No map yet.", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        gMap = googleMap;
        if (context.checkCallingOrSelfPermission( android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            gMap.setMyLocationEnabled(true);
            gMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);

            //to deal with error when trying to update camera before the map is loaded
            gMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    LatLngBounds bounds = new LatLngBounds(
                        new LatLng(20, -130.0), // SW
                        new LatLng(55, -70.0)); // NE
                    gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 50));
                }
            });
            //listens for press of mylocation button and changes the camera position
            //to mylocation
            gMap.setOnMyLocationButtonClickListener(new GoogleMap.OnMyLocationButtonClickListener() {
                @Override
                public boolean onMyLocationButtonClick() {
                    if (context.checkCallingOrSelfPermission( android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {
                        currLoc = locationManager.getLastKnownLocation(LocationManager
                                .GPS_PROVIDER);
                        if(currLoc ==null)Toast.makeText(context,"Last location unknown",Toast.LENGTH_SHORT).show();
                        else{
                            LatLng currLatLng = new LatLng(currLoc.getLatitude(),currLoc.getLongitude());
                            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currLatLng,15f));
                        }
                    }
                    return true;
                }
            });
            callback.onMapHolderReady();
        }
        else {
            Toast.makeText(context,"Location Services not enabled, please enable them",Toast.LENGTH_LONG).show();
            return;
        }
    }

    //method used in OneExcursion to get snapshot of map for sharing screenshot
    //to facebook
    public void getSnapshot(GoogleMap.SnapshotReadyCallback callback){
        if(warnIfNotReady())return;
        gMap.snapshot(callback);
    }
    public void reserMarkers(){
        if(warnIfNotReady())return;
        gMap.clear();
    }

    //called by OneExcursion when initializing the Map to create a polylines
    //to show the path
    public void addMarker(LatLng latLng){
        if(warnIfNotReady()) return;
        Marker newMarker = gMap.addMarker(new MarkerOptions().position(latLng));
        if(markers.size() <=0){
            markers.add(newMarker);
        }
        else{
          Marker oldMarker = markers.get(markers.size()-1);
            gMap.addPolyline(new PolylineOptions().add(newMarker.getPosition(),
                    oldMarker.getPosition()).width(5f).color(Color.RED));
            markers.add(newMarker);
        }
        return;
    }

}
