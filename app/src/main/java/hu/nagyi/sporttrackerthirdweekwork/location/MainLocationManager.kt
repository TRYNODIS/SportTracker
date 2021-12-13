package hu.nagyi.sporttrackerthirdweekwork.location

import android.content.Context
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlin.jvm.Throws

class MainLocationManager(
    context: Context,
    private val onNewLocationHandler: OnNewLocationAvailable
) {

    //region VARIABLES

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            this@MainLocationManager.onNewLocationHandler.onNewLocation(locationResult.lastLocation)
        }
    }

    //endregion

    //region INTERFACE

    interface OnNewLocationAvailable {
        fun onNewLocation(location: Location)
    }

    //endregion

    //region METHODS

    @Throws(SecurityException::class)
    fun getLastLocation(callback: (Location) -> Unit) {
        this.fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    callback(location)
                }
            }
    }

    @Throws(SecurityException::class)
    fun startLocationMonitoring() {
        val locationRequest = LocationRequest.create()
        locationRequest.interval = 1000
        locationRequest.fastestInterval = 500
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        this.fusedLocationClient.requestLocationUpdates(
            locationRequest,
            this.locationCallback, Looper.myLooper()
        )

    }

    @Throws(SecurityException::class)
    fun stopLocationMonitoring() {
        this.fusedLocationClient.removeLocationUpdates(this.locationCallback)
    }

    //endregion
}