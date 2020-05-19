package com.example.firebaseapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.icu.text.Transliterator;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.dynamic.IObjectWrapper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback{

    private final String TAG = "MapActivity";
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private final float DEFAULT_ZOOM = 20f;
    private boolean mLocationPermission = false;
    private FirebaseFirestore firebaseFirestore;
    private FirebaseAuth firebaseAuth;
    private String userID;
    private Marker currentLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseFirestore = FirebaseFirestore.getInstance();
        userID = firebaseAuth.getCurrentUser().getUid();

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        getLocationPermission();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_layout, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        switch(item.getItemId()){
            case R.id.myaccount:
                Intent intent = new Intent(this,FirstPage.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void getLocationPermission(){
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION};
        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                mLocationPermission = true;
                initMap();
            }else{
                ActivityCompat.requestPermissions(this,permissions,1234);
            }

        }
        else{
            ActivityCompat.requestPermissions(this,permissions,1234);
        }
    }

    private void initMap(){
        final SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);//set a callback when map is ready to be used (asta face sa se apeleze functia onMapReady.
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        mLocationPermission = false;

        switch (requestCode){
            case 1234:
                if(grantResults.length > 0){
                   for(int grantResult : grantResults){
                       if(grantResult != PackageManager.PERMISSION_GRANTED){
                           mLocationPermission = false;
                           return;
                       }
                   }
                    mLocationPermission = true;
                   //now you can initialize your map
                    initMap();
                    Toast.makeText(this,"SUPER",Toast.LENGTH_LONG);
                }
        }
    }

    public void getDeviceLocation(){
        Log.d(TAG, "getDeviceLocation: getting the devices location");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try{
            Task location = mFusedLocationProviderClient.getLastLocation();
            location.addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(task.isSuccessful()){
                        Log.d(TAG,"Location is founded!");
                        Location currentLocation = (Location) task.getResult();
                        moveCamera(new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude()),DEFAULT_ZOOM);
                    }
                    else{
                        Log.d(TAG,"Location is not founded!");
                    }
                }
            });

        }catch(SecurityException e){
            Log.e("Ex : %s",e.getMessage());
        }
    }

    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermission) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                //mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom){
        Log.d(TAG,"Camera is moving to the current position!");
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));
        MarkerOptions mypos = new MarkerOptions().position(latLng).title("Initial Position");
        currentLocation = mMap.addMarker(mypos);
        currentLocation.setVisible(false);

    }

    public void addGasStations(){
        final CollectionReference gas_stations = firebaseFirestore.collection("gas-stations");
        gas_stations.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    for(QueryDocumentSnapshot documentSnapshot : task.getResult()){
                        GeoPoint geoPoint = documentSnapshot.getGeoPoint("location");
                        LatLng latLng = new LatLng(geoPoint.getLatitude(),geoPoint.getLongitude());
                        MarkerOptions mystation = new MarkerOptions();
                        mystation.position(latLng);
                        BitmapDrawable parkingIcon = (BitmapDrawable) getResources().getDrawable(R.drawable.gas);
                        Bitmap bitmap = parkingIcon.getBitmap();
                        Bitmap smallParkingIcon = Bitmap.createScaledBitmap(bitmap, 100, 100, false);
                        mystation.icon(BitmapDescriptorFactory.fromBitmap(smallParkingIcon));
                        mMap.addMarker(mystation);
                    }
                }
            }
        });
    }

    public void addChargingStation(){
        final CollectionReference gas_stations = firebaseFirestore.collection("charging-stations");
        gas_stations.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()){
                    for(QueryDocumentSnapshot documentSnapshot : task.getResult()){
                        GeoPoint geoPoint = documentSnapshot.getGeoPoint("location");
                        LatLng latLng = new LatLng(geoPoint.getLatitude(),geoPoint.getLongitude());
                        MarkerOptions mystation = new MarkerOptions();
                        mystation.position(latLng);
                        BitmapDrawable parkingIcon = (BitmapDrawable) getResources().getDrawable(R.drawable.electric);
                        Bitmap bitmap = parkingIcon.getBitmap();
                        Bitmap smallParkingIcon = Bitmap.createScaledBitmap(bitmap, 100, 100, false);
                        mystation.icon(BitmapDescriptorFactory.fromBitmap(smallParkingIcon));
                        mMap.addMarker(mystation);
                    }
                }
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;


        addGasStations();
        addChargingStation();
        getDeviceLocation();

        updateLocationUI();
        final ArrayList<Marker> markers = new ArrayList<>();
        final ArrayList<Polyline> polylines = new ArrayList<>();

        mMap.setOnMyLocationClickListener(new GoogleMap.OnMyLocationClickListener() {
            @Override
            public void onMyLocationClick(@NonNull Location location) {
                CharSequence option[] = {"I want to park here"};
                final AlertDialog.Builder dialog = new AlertDialog.Builder(MapActivity.this);
                dialog.setTitle("Options").setItems(option, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which) {
                            case 0:
                                if (!markers.isEmpty()) {
                                    Marker marker = markers.get(0);
                                    marker.setVisible(false);
                                    markers.remove(marker);
                                    Log.d(TAG, "Acum sterg deci am ce");
                                }

                                final LatLng latLng = currentLocation.getPosition();
                                final MarkerOptions mypark = new MarkerOptions().position(latLng).title("I parked here :)");
                                BitmapDrawable parkingIcon = (BitmapDrawable) getResources().getDrawable(R.drawable.location);
                                Bitmap bitmap = parkingIcon.getBitmap();
                                Bitmap smallParkingIcon = Bitmap.createScaledBitmap(bitmap, 100, 100, false);
                                mypark.icon(BitmapDescriptorFactory.fromBitmap(smallParkingIcon));

                                userID = firebaseAuth.getCurrentUser().getUid();
                                DocumentReference documentReference = firebaseFirestore.collection("users/" + firebaseAuth.getCurrentUser().getUid() + "/locations").document(userID); //daca nu avem users in colectii se va crea automat
                                Map<String, Object> userObj = new HashMap<>();
                                userObj.put("parking-lat", latLng.latitude);
                                userObj.put("parking-long", latLng.longitude);
                                documentReference.set(userObj).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Log.d(TAG, "Location Parking is saved" + latLng.latitude + " " + latLng.longitude);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "Eroare! " + e.getMessage());
                                    }
                                });
                                break;
                        }

                    }
                });
                        dialog.create().show();


            }
        });

        final DocumentReference documentReference = firebaseFirestore.collection("users/"+firebaseAuth.getCurrentUser().getUid()+"/locations").document(userID); //daca nu avem users in colectii se va crea automat
        documentReference.addSnapshotListener(this, new EventListener<DocumentSnapshot>() { // To “listen” for changes on a document or collection, we create a snapshot listener
            @Override
            public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {
                if(documentSnapshot != null) {
                    if(documentSnapshot.getDouble("parking-lat") != null && documentSnapshot.getDouble("parking-long") != null) {
                        LatLng latLng = new LatLng(documentSnapshot.getDouble("parking-lat"), documentSnapshot.getDouble("parking-long"));
                        final MarkerOptions mypark = new MarkerOptions().position(latLng).title("I parked here :)");
                        BitmapDrawable parkingIcon = (BitmapDrawable) getResources().getDrawable(R.drawable.location);
                        Bitmap bitmap = parkingIcon.getBitmap();
                        Bitmap smallParkingIcon = Bitmap.createScaledBitmap(bitmap, 100, 100, false);
                        mypark.icon(BitmapDescriptorFactory.fromBitmap(smallParkingIcon));
                        Marker marker = mMap.addMarker(mypark);
                        markers.add(0, marker);
                    }
                }

            }
        });

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener(){
            @Override
            public void onMapLongClick(final LatLng latLng) {
                AlertDialog.Builder dialog = new AlertDialog.Builder(MapActivity.this);
                dialog.setTitle("Parking");
                dialog.setMessage("Do you want to park here?");
                final MarkerOptions mypark = new MarkerOptions().position(latLng).title("I parked here :)");

                dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                       // Log.d(TAG,"ACUM IN MARKERS AM: " + markers.get(0).getPosition().latitude + " " + markers.get(0).getPosition().longitude);

                        if(!markers.isEmpty()){
                            Marker marker = markers.get(0);
                            marker.remove();
                            markers.remove(marker);
                            Log.d(TAG,"Acum sterg deci am ce");
                        }

                        if(!polylines.isEmpty()){
                            polylines.get(0).setVisible(false);
                            polylines.remove(0);
                        }

                        BitmapDrawable parkingIcon = (BitmapDrawable)getResources().getDrawable(R.drawable.location);
                        Bitmap bitmap = parkingIcon.getBitmap();
                        Bitmap smallParkingIcon = Bitmap.createScaledBitmap(bitmap,100,100,false);
                        mypark.icon(BitmapDescriptorFactory.fromBitmap(smallParkingIcon));

                        userID = firebaseAuth.getCurrentUser().getUid();
                        DocumentReference documentReference = firebaseFirestore.collection("users/"+firebaseAuth.getCurrentUser().getUid()+"/locations").document(userID); //daca nu avem users in colectii se va crea automat
                        Map<String,Object> userObj = new HashMap<>();
                        userObj.put("parking-lat",latLng.latitude);
                        userObj.put("parking-long",latLng.longitude);
                        documentReference.set(userObj).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.d(TAG,"Location Parking is saved" + latLng.latitude + " " + latLng.longitude);
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.d(TAG,"Eroare! " + e.getMessage());
                            }
                        });
                    }
                });

                dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // close dialog
                    }
                });

                dialog.create().show();
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(final Marker marker) {
                CharSequence []menuItems = new CharSequence[]{"Delete this sign", "Let's go to this sign"};
                AlertDialog.Builder dialog = new AlertDialog.Builder(MapActivity.this);
                dialog.setTitle("Options").setItems(menuItems, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case 0:
                                userID = firebaseAuth.getCurrentUser().getUid();
                                DocumentReference documentReference = firebaseFirestore.collection("users/" + firebaseAuth.getCurrentUser().getUid() + "/locations").document(userID); //daca nu avem users in colectii se va crea automat
                                documentReference.delete().addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.d(TAG,"AM STERS");
                                        }
                                    }
                                });
                                marker.remove();
                                break;
                            case 1:
                                if(!polylines.isEmpty()){
                                    polylines.get(0).setVisible(false);
                                    Log.d(TAG,"STERG RUTA ACUM");
                                }
                                Polyline route = mMap.addPolyline(new PolylineOptions().add(marker.getPosition(),currentLocation.getPosition()));
                                polylines.add(0,route);
                                Log.d(TAG,"RUTA S-A ADAUGAT "+route.getId());


                                if(currentLocation.equals(marker)){
                                    route.setVisible(false);
                                }
                                break;
                        }
                    }
                });

                dialog.create().show();

                return false;
            }
        });

        LatLng bucuresti = new LatLng(44, 26);
        LatLng clujnapoca = new LatLng(46,23);
        LatLng iasi = new LatLng(47,27);
        mMap.addMarker(new MarkerOptions().position(bucuresti).title("Marker in Bucharest"));
        mMap.addMarker(new MarkerOptions().position(clujnapoca).title("Marker in Cluj-Napoca"));
        mMap.addMarker(new MarkerOptions().position(iasi).title("Marker in Iasi"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(bucuresti));

    }

}
