package hu.nagyi.sporttrackerthirdweekwork

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class MyMarkerClusterItem(
    lat: Double,
    lng: Double,
    title: String,
    snippet: String
) : ClusterItem {

    //region VARIABLES

    private val position: LatLng
    private val title: String
    private val snippet: String

    //endregion

    //region METHODS

    override fun getPosition(): LatLng {
        return this.position
    }

    override fun getTitle(): String? {
        return this.title
    }

    override fun getSnippet(): String? {
        return this.snippet
    }

    init {
        this.position = LatLng(lat, lng)
        this.title = title
        this.snippet = snippet
    }

    //endregion
}