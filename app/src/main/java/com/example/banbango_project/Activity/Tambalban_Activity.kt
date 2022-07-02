package com.example.banbango_project.Activity

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.banbango_project.Adapter.MainAdapter
import com.example.banbango_project.Model.MainViewModel
import com.example.banbango_project.Model.MainViewModels
import com.example.banbango_project.R
import com.example.banbango_project.data.model.nearby.ModelResults
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import im.delight.android.location.SimpleLocation
import java.io.IOException
import java.util.*
import java.util.jar.Manifest
import kotlin.collections.ArrayList

class Tambalban_Activity : AppCompatActivity(), OnMapReadyCallback {
    var permissionArrays = arrayOf(Manifest.permision.ACCESS_FINE_LOCATION, Manifest.permision.ACCESS_COARSE_LOCATION)
    lateinit var mapsView: GoogleMap
    lateinit var simpleLocation: SimpleLocation
    lateinit var progressDialog: ProgressDialog
    lateinit var mainViewModels: MainViewModels
    lateinit var mainAdapter: MainAdapter
    lateinit var strCurrentLocation: String
    var strCurrentLatitude = 0.0
    var strCurrentLongitude = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tambalban)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
        }

        if (Build.VERSION.SDK_INT >=21){
            setWindowFlag(this,WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, false)
            window.statusBarColor = Color.TRANSPARENT
        }

        val setPermission = Build.VERSION.SDK_INT
        if (setPermission > Build.VERSION_CODES.LOLLIPOP_MR1){
            if (checkIfAlreadyhavePermission() && checkIfAlreadyhavePermission2()){

            } else{
                requestPermissions(permissionArrays,101)
            }
        }

        progressDialog = ProgressDialog(this)
        progressDialog.setTitle("Mohon tunggu...")
        progressDialog.setCancelable(false)
        progressDialog.setMessage("Sedang menampilkan lokasi tambal ban")

        simpleLocation = SimpleLocation(this)
        if (!simpleLocation.hasLocationEnabled()){
            SimpleLocation.openSettings(this)
        }

        //get location
        strCurrentLatitude = simpleLocation.latitude
        strCurrentLongitude = simpleLocation.longitude

        //set Location lat long
        strCurrentLocation = "$strCurrentLatitude, $strCurrentLongitude"
        val supportMapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        supportMapFragment.getMapAsync(this)
        mainAdapter = MainAdapter(this)
        rvListLocation.setLayoutManager(LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false))
        rvListLocation.setAdapter(mainAdapter)
        rvListLocation.setHasFixedSize(true)
    }

    private fun checkIfAlreadyhavePermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return result == PackageManager.PERMISSION_GRANTED
    }

    private fun checkIfAlreadyhavePermission2(): Boolean {
        val result = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return result == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (grantResult in grantResults){
            if (grantResult == PackageManager.PERMISSION_DENIED){
                val intent = intent
                finish()
                startActivity(intent)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mapsView = googleMap

        //set text Location
        val geocoder = Geocoder(this, Locale.getDefault())
        try{
            val addressList = geocoder.getFromLocation(strCurrentLatitude, strCurrentLongitude,1)
            if(addressList != null && addressList.size > 0){
                val strCity = addressList[0].locality
                tvCity.text = strCity
            }
        } catch (e: IOException){
            e.printStackTrace()
        }

        //view model
        getLocationViewModel()

    }

    private fun getLocationViewModel() {
        mainViewModels = ViewModelProvider(this, NewInstanceFactory()).get(MainViewModels::class.java)
        mainViewModels.setMarkerLocation(strCurrentLocation)
        progressDialog.show()
        mainViewModels.getMarkerLocation().observe(this, { modelResult: ArrayList<ModelResults> ->
            if (modelResult.size != 0 ){
                mainAdapter.setLocationAdapter(modelResult)

                //get multiple marker
                getMarker(modelResult)
                progressDialog.dismiss()
            } else {
                Toast.makeText(this, "Oops, tidak bisa mendapatkan lokasi kamu!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getMarker(modelResultsArrayList: ArrayList<ModelResults>) {
        for (i in modelResultsArrayList.indices) {
            //set LatLong from API
            val latLngMarker = LatLng(modelResultsArrayList[i].lat,
                modelResultsArrayList[i].lng)

            //get LatLong to Marker
            mapsView.addMarker(
                MarkerOptions()
                .position(latLngMarker)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                .title(modelResultsArrayList[i].name))

            //show Marker
            val latLngResult = LatLng(modelResultsArrayList[0].lat,
                modelResultsArrayList[0].lng)

            //set position marker
            mapsView.moveCamera(CameraUpdateFactory.newLatLng(latLngResult))
            mapsView.animateCamera(CameraUpdateFactory
                .newLatLngZoom(LatLng(
                    latLngResult.latitude,
                    latLngResult.longitude), 14f))
            mapsView.uiSettings.setAllGesturesEnabled(true)
            mapsView.uiSettings.isZoomGesturesEnabled = true
        }

        //click marker for change position recyclerview
        mapsView.setOnMarkerClickListener { marker ->
            val markerPosition = marker.position
            mapsView.addMarker(MarkerOptions()
                .position(markerPosition)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
            var markerSelected = -1
            for (i in modelResultsArrayList.indices) {
                if (markerPosition.latitude == modelResultsArrayList[i].lat && markerPosition.longitude == modelResultsArrayList[i].lng) {
                    markerSelected = i
                }
            }
            val cameraPosition = CameraPosition.Builder().target(markerPosition).zoom(14f).build()
            mapsView.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

            mainAdapter.notifyDataSetChanged()
            rvListLocation.smoothScrollToPosition(markerSelected)
            marker.showInfoWindow()
            false
        }
    }

    companion object {
        fun setWindowFlag(activity: Activity, bits: Int, on: Boolean) {
            val window = activity.window
            val layoutParams = window.attributes
            if (on) {
                layoutParams.flags = layoutParams.flags or bits
            } else {
                layoutParams.flags = layoutParams.flags and bits.inv()
            }
            window.attributes = layoutParams
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PERMISSION && resultCode == RESULT_OK) {
            getLocationViewModel()
        }
    }
            }
        })

    }
}