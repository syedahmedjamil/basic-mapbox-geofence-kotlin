package ajcs.learn.mapboxkotlin

import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.location.LocationEngineRequest
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotation
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorBearingChangedListener
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.locationcomponent.location2
import com.mapbox.turf.TurfMeasurement

class MainActivity : AppCompatActivity(), PermissionsListener {
    val DEFAULT_INTERVAL_IN_MILLISECONDS = 100L
    val DEFAULT_MAX_WAIT_TIME = DEFAULT_INTERVAL_IN_MILLISECONDS
    private var stores = mutableListOf<Point>()
    private var permissionsManager: PermissionsManager = PermissionsManager(this)
    lateinit var locationEngine: LocationEngine
    var request = LocationEngineRequest.Builder(DEFAULT_INTERVAL_IN_MILLISECONDS)
        .setPriority(LocationEngineRequest.PRIORITY_HIGH_ACCURACY)
        .build()
    var userAnnotation: CircleAnnotation? = null
    var destinationAnnotation: CircleAnnotation? = null
    lateinit var circleAnnotationManager: CircleAnnotationManager
    var isFirst = true

    private val onIndicatorBearingChangedListener = OnIndicatorBearingChangedListener {
        //mapView.getMapboxMap().setCamera(CameraOptions.Builder().bearing(it).build())
    }
    private val onIndicatorPositionChangedListener = OnIndicatorPositionChangedListener {
        mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(it).build())
        //mapView.gestures.focalPoint = mapView.getMapboxMap().pixelForCoordinate(it)
        if (stores.size > 0) {
            if (TurfMeasurement.distance(it, stores[0]) <= 0.005) {
                Toast.makeText(this@MainActivity,
                    "You have reached your destination",
                    Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity,
                    "Keep going",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }
    private val onMoveListener = object : OnMoveListener {
        override fun onMoveBegin(detector: MoveGestureDetector) {
            onCameraTrackingDismissed()
        }

        override fun onMove(detector: MoveGestureDetector): Boolean {
            return false
        }

        override fun onMoveEnd(detector: MoveGestureDetector) {}
    }
    private lateinit var mapView: MapView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.mapView)
        circleAnnotationManager = mapView.annotations.createCircleAnnotationManager()
        if (PermissionsManager.areLocationPermissionsGranted(this@MainActivity))
            onMapReady()
        else
            permissionsManager.requestLocationPermissions(this@MainActivity)
    }

    private fun onMapReady() {
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .zoom(14.0)
                .build()
        )
        mapView.getMapboxMap().loadStyleUri("mapbox://styles/ahmedjamilcs/cl43h70sb000314p28bx1r7ae") {
            initLocationComponent()
            setupGesturesListener()
        }
    }

    private fun setupGesturesListener() {
        //mapView.gestures.addOnMoveListener(onMoveListener)
        mapView.gestures.addOnMapClickListener {
            if (destinationAnnotation == null) {
                destinationAnnotation = circleAnnotationManager
                    .create(CircleAnnotationOptions()
                        .withPoint(it)
                        .withCircleRadius(30.0)
                        .withCircleColor("#ee4e8b")
                        .withCircleOpacity(0.3)
                        .withCircleStrokeWidth(2.0)
                        .withCircleStrokeColor("#ffffff"))
                stores.add(it)
            } else {
                destinationAnnotation!!.point = it
                stores[0] = it
            }
            circleAnnotationManager.update(destinationAnnotation!!)
            true
        }
    }

    @SuppressLint("MissingPermission")
    private fun initLocationComponent() {
        locationEngine = LocationEngineProvider.getBestLocationEngine(this)
        var locationListeningCallback = LocationListeningCallback(this) { result, callback ->
            val location = result!!.lastLocation!!
            val point = Point.fromLngLat(location.longitude, location.latitude)
            if (userAnnotation == null)
                userAnnotation = circleAnnotationManager
                    .create(CircleAnnotationOptions()
                        .withPoint(point)
                        .withCircleRadius(8.0)
                        .withCircleColor("#0c54cf")
                        .withCircleStrokeWidth(2.0)
                        .withCircleStrokeColor("#ffffff"))
            mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(point).build())
            locationEngine.removeLocationUpdates(callback)
        }
        locationEngine.requestLocationUpdates(request, locationListeningCallback, mainLooper)
//        val locationComponentPlugin = mapView.location
//        locationComponentPlugin.updateSettings {
//            this.enabled = true
//            this.locationPuck = LocationPuck2D(
//                bearingImage = AppCompatResources.getDrawable(
//                    this@MainActivity,
//                    mapbox_user_icon,
//                ),
//                shadowImage = AppCompatResources.getDrawable(
//                    this@MainActivity,
//                    mapbox_user_icon_shadow
//                ),
//                scaleExpression = interpolate {
//                    linear()
//                    zoom()
//                    stop {
//                        literal(0.0)
//                        literal(0.6)
//                    }
//                    stop {
//                        literal(20.0)
//                        literal(1.0)
//                    }
//                }.toJson()
//            )
//        }
//        locationComponentPlugin.addOnIndicatorPositionChangedListener(
//            onIndicatorPositionChangedListener)
//        locationComponentPlugin.addOnIndicatorBearingChangedListener(
//            onIndicatorBearingChangedListener)
    }

    private fun onCameraTrackingDismissed() {
        Toast.makeText(this, "onCameraTrackingDismissed", Toast.LENGTH_SHORT).show()
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.location
            .removeOnIndicatorBearingChangedListener(onIndicatorBearingChangedListener)
        mapView.location
            .removeOnIndicatorPositionChangedListener(onIndicatorPositionChangedListener)
        mapView.gestures.removeOnMoveListener(onMoveListener)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @SuppressLint("MissingPermission")
    fun onReachedDestinationClick(view: View) {
        if (stores.size > 0) {
            if (isFirst) {
                var locationListeningCallback =
                    LocationListeningCallback(this) { result, callback ->
                        val location = result!!.lastLocation!!
                        val point = Point.fromLngLat(location.longitude, location.latitude)
                        if (userAnnotation == null)
                            userAnnotation = circleAnnotationManager
                                .create(CircleAnnotationOptions()
                                    .withPoint(point)
                                    .withCircleRadius(8.0)
                                    .withCircleColor("#0c54cf")
                                    .withCircleStrokeWidth(2.0)
                                    .withCircleStrokeColor("#ffffff"))
                        else
                            userAnnotation!!.point = point
                        circleAnnotationManager.update(userAnnotation!!)
                        mapView.getMapboxMap()
                            .setCamera(CameraOptions.Builder().center(point).build())


                        if (TurfMeasurement.distance(point, stores[0]) <= 0.01) {
                            Toast.makeText(this@MainActivity,
                                "You have reached your destination",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
                locationEngine.requestLocationUpdates(request,
                    locationListeningCallback,
                    mainLooper)
                isFirst = false
                Toast.makeText(this@MainActivity,
                    "Move towards destination",
                    Toast.LENGTH_SHORT).show()
            }
            else{
                Toast.makeText(this@MainActivity,
                    "Move towards destination",
                    Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this@MainActivity,
                "Tap on the map to add destination",
                Toast.LENGTH_SHORT).show()
        }

    }

    override fun onExplanationNeeded(p0: MutableList<String>?) {
        TODO("Not yet implemented")
    }

    override fun onPermissionResult(p0: Boolean) {
        if (p0)
            onMapReady()
        else
            Toast.makeText(this,
                "This app needs location permission from you to work properly.",
                Toast.LENGTH_SHORT).show()


    }
}