package ajcs.learn.mapboxkotlin

import android.widget.Toast
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineCallback
import com.mapbox.android.core.location.LocationEngineResult
import com.mapbox.geojson.Point
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.turf.TurfMeasurement
import java.lang.ref.WeakReference

class LocationListeningCallback(
    activity: MainActivity,
    onSuccess: (result: LocationEngineResult?, callback: LocationListeningCallback) -> Unit,
) :
    LocationEngineCallback<LocationEngineResult?> {
    private val activityWeakReference: WeakReference<MainActivity>
    private val execute : (result: LocationEngineResult?, callback: LocationListeningCallback) -> Unit
    override fun onSuccess(locationEngineResult: LocationEngineResult?) {
        execute(locationEngineResult, this )
    }

    override fun onFailure(e: Exception) {
        Toast.makeText(activityWeakReference.get(), "Failed to get location", Toast.LENGTH_SHORT)
            .show()
    }

    init {
        activityWeakReference = WeakReference(activity)
        this.execute = onSuccess

    }
}