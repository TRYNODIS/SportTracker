package hu.nagyi.sporttrackerthirdweekwork

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.graphics.Color
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.Task
import com.google.maps.android.clustering.ClusterManager
import com.sucho.placepicker.AddressData
import com.sucho.placepicker.Constants
import com.sucho.placepicker.MapType
import com.sucho.placepicker.PlacePicker
import hu.nagyi.sporttrackerthirdweekwork.databinding.ActivityMapsBinding
import hu.nagyi.sporttrackerthirdweekwork.location.MainLocationManager
import java.util.*
import kotlin.concurrent.thread

class MapsActivity : AppCompatActivity(), OnMapReadyCallback,
    MainLocationManager.OnNewLocationAvailable {

    //region VARIABLES

    private lateinit var binding: ActivityMapsBinding
    private lateinit var googleMap: GoogleMap
    private lateinit var clusterManager: ClusterManager<MyMarkerClusterItem>

    private lateinit var mainLocationManager: MainLocationManager

    private lateinit var myRoute: PolylineOptions

    private var markerCurrentPosition: Marker? = null

    var polyLine: Polyline? = null

    var previousLocation: Location? = null
    var distance: Float = 0f
    var startAltitude: Double? = null
    var actualAltitude: Double? = null
    var difAltitude = 0.0

    //endregion

    //region METHODS

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        this.binding = ActivityMapsBinding.inflate(this.layoutInflater)
        this.setContentView(this.binding.root)

        this.mainLocationManager = MainLocationManager(this, this)

        this.binding.geocodeBtn.setOnClickListener {
            if (this.previousLocation != null) {
                this.geocodeLocation(this.previousLocation!!)
            }
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        this.handleLocationStart()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap

        this.markerCurrentPosition = this.googleMap.addMarker(
            MarkerOptions()
                .position(LatLng(47.0, 19.0))
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.arrow))
        )

        this.myRoute = PolylineOptions().add()
        val polyline = this.googleMap.addPolyline(this.myRoute)
        polyline.color = Color.RED


        this.initMap()
        this.initPlaceSelect()
        //this.initMapAndMarkerClick()
        //this.drawPolygonAndLine()
        //this.setUpClusterer()
    }

    private fun initPlaceSelect() {
        this.binding.selectPlaceBtn.setOnClickListener {
            val intent = PlacePicker.IntentBuilder()
                .setLatLong(47.0, 19.0)  // Initial Latitude and Longitude the Map will load into
                .showLatLong(true)  // Show Coordinates in the Activity
                .setMapZoom(12.0f)  // Map Zoom Level. Default: 14.0
                .setAddressRequired(true) // Set If return only Coordinates if cannot fetch Address for the coordinates. Default: True
                .hideMarkerShadow(true) // Hides the shadow under the map marker. Default: False
                //.setMarkerDrawable(R.drawable.marker) // Change the default Marker Image
                .setMarkerImageImageColor(R.color.colorPrimary)
                //.setFabColor(R.color.fabColor)
                //.setPrimaryTextColor(R.color.primaryTextColor) // Change text color of Shortened Address
                //.setSecondaryTextColor(R.color.secondaryTextColor) // Change text color of full Address
                //.setBottomViewColor(R.color.bottomViewColor) // Change Address View Background Color (Default: White)
                //.setMapRawResourceStyle(R.raw.map_style)  //Set Map Style (https://mapstyle.withgoogle.com/)
                .setMapType(MapType.NORMAL)
                //.setPlaceSearchBar(true, getString(R.string.google_maps_key)) //Activate GooglePlace Search Bar. Default is false/not activated. SearchBar is a chargeable feature by Google
                .onlyCoordinates(true)  //Get only Coordinates from Place Picker
                .hideLocationButton(true)   //Hide Location Button (Default: false)
                .disableMarkerAnimation(true)   //Disable Marker Animation (Default: false)
                .build(this)
            this.startActivityForResult(intent, Constants.PLACE_PICKER_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.PLACE_PICKER_REQUEST) {
            if (resultCode == Activity.RESULT_OK) {
                val addressData = data?.getParcelableExtra<AddressData>(Constants.ADDRESS_INTENT)
                Toast.makeText(
                    this,
                    "${addressData?.latitude}, ${addressData?.longitude}",
                    Toast.LENGTH_LONG
                ).show()

                this.googleMap.animateCamera(
                    CameraUpdateFactory.newLatLng(
                        LatLng(
                            addressData!!.latitude,
                            addressData!!.longitude
                        )
                    )
                )
            }
        } else {
            Toast.makeText(this, "OK - LOCATION $requestCode", Toast.LENGTH_LONG).show()
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun drawPolygonAndLine() {
        val polyRect: PolygonOptions = PolygonOptions().add(
            LatLng(44.0, 19.0),
            LatLng(44.0, 26.0),
            LatLng(48.0, 26.0),
            LatLng(48.0, 19.0)
        )
        val polygon: Polygon = this.googleMap.addPolygon(polyRect)
        polygon.fillColor = Color.argb(100, 0, 255, 0)


        val polyLineOpts = PolylineOptions().add(
            LatLng(34.0, 19.0),
            LatLng(34.0, 26.0),
            LatLng(38.0, 26.0)
        )


        val polyline = this.googleMap.addPolyline(polyLineOpts)
        polyline.color = Color.GREEN
    }

    private fun initMapAndMarkerClick() {
        this.googleMap.setOnMapClickListener {
            val marker = this.googleMap.addMarker(
                MarkerOptions()
                    .position(it)
                    .title("Marker demo")
                    .snippet("Marker details text")
            )
            marker!!.isDraggable = true

            val random = Random(System.currentTimeMillis())
            val cameraPostion = CameraPosition.Builder()
                .target(it)
                .zoom(5f + random.nextInt(15))
                .tilt(30f + random.nextInt(15))
                .bearing(-45f + random.nextInt(90))
                .build()

            //mMap.animateCamera(CameraUpdateFactory.newLatLng(it))
            this.googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPostion))
        }

        this.googleMap.setOnMarkerClickListener { marker ->
            Toast.makeText(
                this@MapsActivity,
                "${marker!!.position.latitude}, ${marker!!.position.longitude}",
                Toast.LENGTH_LONG
            ).show()
            false
        }
    }

    private fun initMap() {
        val mapStyleOptions = MapStyleOptions.loadRawResourceStyle(this, R.raw.mapstyle)
        this.googleMap.setMapStyle(mapStyleOptions)

        this.googleMap.isTrafficEnabled = true
        this.googleMap.uiSettings.isZoomControlsEnabled = true
        this.googleMap.uiSettings.isCompassEnabled = true
        this.googleMap.uiSettings.isMapToolbarEnabled = true
/*        val markerHungary = LatLng(47.0, 19.0)
        this.googleMap.addMarker(MarkerOptions().position(markerHungary).title("Marker in Hungary"))
        this.googleMap.moveCamera(CameraUpdateFactory.newLatLng(markerHungary))*/

        this.binding.mapTypeBtn.setOnClickListener {
            if (this.binding.mapTypeBtn.isChecked) {
                this.googleMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            } else {
                this.googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            }
        }
    }


    private fun setUpClusterer() {
        this.clusterManager = ClusterManager(this, this.googleMap)
        this.googleMap.setOnCameraIdleListener(this.clusterManager)
        this.googleMap.setOnMarkerClickListener(this.clusterManager)
        this.addMarkerClusterItems()
    }


    private fun addMarkerClusterItems() {
        // Set some lat/lng coordinates to start with.
        var lat = 47.5
        var lng = 19.5

        // Add ten cluster items in close proximity, for purposes of this example.
        for (i in 0..10) {
            val offset = i / 60.0
            lat += offset
            lng += offset
            val offsetItem =
                MyMarkerClusterItem(lat, lng, "Title $i", "Snippet $i")
            this.clusterManager.addItem(offsetItem)
        }
    }

    override fun onNewLocation(location: Location) {
        val markerPosition = LatLng(location.latitude, location.longitude)
        this.markerCurrentPosition?.setPosition(markerPosition)
        //mMap.moveCamera(CameraUpdateFactory.newLatLng(markerPosition))

        this.myRoute.add(markerPosition)
        if (this.polyLine != null) {
            this.polyLine?.remove()
        }
        val polyLine = this.googleMap.addPolyline(this.myRoute)
        polyLine?.color = Color.BLUE


        val cameraPosition = CameraPosition.Builder()
            .target(markerPosition)
            .bearing(location.bearing)
            .zoom(zoomToSpeed(location.speed))
            .tilt(tiltToSpeed(location.speed))
            .build()

        this.googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))

        this.binding.locationTV.text = this.getLocationText(location)

        if (this.previousLocation != null && this.startAltitude == null) {
            this.startAltitude = this.previousLocation!!.altitude
        }
        if (this.startAltitude != null) {
            this.actualAltitude = this.previousLocation!!.altitude
        }
        if (this.actualAltitude != null && this.startAltitude != null) {
            this.difAltitude = this.actualAltitude!! - this.startAltitude!!
        }

        if (this.previousLocation != null && location.accuracy < 20) {
            if (this.previousLocation!!.time < location.time) {
                this.distance += this.previousLocation!!.distanceTo(location)
                this.binding.distanceTV.text = "$distance m"
                this.binding.altitudeTV.text = "$difAltitude m"
            }
        }

        if (this.previousLocation == null) {
            this.googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(location.latitude, location.longitude))
                    .title("Start marker")
                    .snippet("Start marker snippet")
            )
        }

        this.previousLocation = location
    }

    private fun handleLocationStart() {
        this.checkGlobalLocationSettings()
        this.showLastKnownLocation()
        this.mainLocationManager.startLocationMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        this.mainLocationManager.stopLocationMonitoring()
    }

    private fun zoomToSpeed(speed: Float): Float {
        val maxZoom = 7.0f
        val minZoom = 10.0f
        var zoom = 0f
        zoom = if (speed > 12.5f) {
            maxZoom
        } else if (speed > 0f) {
            speed * maxZoom / 12.5f
        } else {
            0f
        }

        //Toast.makeText(this, speed+", "+(minZoom+zoom), Toast.LENGTH_LONG).show();
        return minZoom + zoom
    }

    private fun tiltToSpeed(speed: Float): Float {
        val maxTilt = 90f
        val tilt: Float
        tilt = if (speed > 12.5f) {
            maxTilt
        } else if (speed > 0f) {
            speed * maxTilt / 12.5f
        } else {
            0f
        }
        return tilt
    }

    fun checkGlobalLocationSettings() {
        val locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest!!)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())


        task.addOnSuccessListener { locationSettingsResponse ->
            Toast.makeText(
                this,
                "Location enabled: ${locationSettingsResponse.locationSettingsStates.isLocationUsable}",
                Toast.LENGTH_LONG
            ).show()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {

                Toast.makeText(this, "${exception.message}", Toast.LENGTH_LONG).show()

                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(
                        this@MapsActivity,
                        1001
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }

    }

    private fun showLastKnownLocation() {
        this.mainLocationManager.getLastLocation { location ->
            this.binding.locationTV.text = this.getLocationText(location)
        }
    }

    private fun getLocationText(location: Location): String {
        return """
            Provider: ${location.provider}
            Latitude: ${location.latitude}
            Longitude: ${location.longitude}
            Accuracy: ${location.accuracy}
            Altitude: ${location.altitude}
            Speed: ${location.speed}
            Time: ${Date(location.time).toString()}
        """.trimIndent()
    }

    private fun geocodeLocation(location: Location) {
        thread {
            try {
                val gc = Geocoder(this, Locale.getDefault())
                var addrs: List<Address> =
                    gc.getFromLocation(location.latitude, location.longitude, 3)
                val addr =
                    "${addrs[0].getAddressLine(0)}, ${addrs[0].getAddressLine(1)}, ${
                        addrs[0].getAddressLine(
                            2
                        )
                    }"

                runOnUiThread {
                    Toast.makeText(this, addr, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@MapsActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    //endregion
}