package lt.blackbrackets.kasvalgyt.views.activities

import android.Manifest
import android.annotation.SuppressLint
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import com.mcxiaoke.koi.ext.dpToPx
import lt.blackbrackets.kasvalgyt.api.models.EatingPlace
import java.util.*
import com.readystatesoftware.systembartint.SystemBarTintManager
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import com.google.android.gms.location.LocationRequest
import com.mcxiaoke.koi.utils.currentVersion
import com.patloew.rxlocation.RxLocation
import kotlinx.android.synthetic.main.activity_main.*
import lt.blackbrackets.kasvalgyt.R
import lt.blackbrackets.kasvalgyt.adapters.EatingPlaceAdapter
import lt.blackbrackets.kasvalgyt.api.ApiClient
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import lt.blackbrackets.kasvalgyt.utils.*
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.PermissionListener
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener


class MainActivity : AppCompatActivity() {
    var onCreateAt : Long = System.currentTimeMillis()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar!!.setIcon(R.mipmap.kas_valgyt_icon)
        supportActionBar!!.setDisplayUseLogoEnabled(true)
        supportActionBar!!.setDisplayShowHomeEnabled(true)
        supportActionBar!!.title = "   " + resources.getString(R.string.app_name)

        val tintManager = SystemBarTintManager(this)
        tintManager.setNavigationBarTintEnabled(true)
        tintManager.isStatusBarTintEnabled = true
        tintManager.setNavigationBarTintColor(ContextCompat.getColor(this, R.color.nav_bar))
        tintManager.setStatusBarTintColor(ContextCompat.getColor(this, R.color.nav_bar))

        val drawable = ColorDrawable()
        drawable.color = ContextCompat.getColor(this, R.color.colorPrimary).addAlphaToColor(230)
        supportActionBar!!.setBackgroundDrawable(drawable)

        val height = getActionBarHeight() + getStatusBarHeight()

        recyclerView.setPaddingRelative(0, height, 0, 48.dpToPx())
        recyclerView.setPadding(0, height, 0, 48.dpToPx())

        val navigationBarHeight = getNavigationBarHeight()
        val layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.MATCH_PARENT
        )
        layoutParams.bottomMargin = navigationBarHeight
        myCoordinatorLayout.layoutParams = layoutParams

        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        recyclerView.setHasFixedSize(true)

        // use a linear layout manager
        val mLayoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = mLayoutManager

        requestLocation(myCoordinatorLayout, callback = { _ ->
            getLocation(applicationContext) { location ->
                getPlaces(location)
            }
        })

        onCreateAt = System.currentTimeMillis()

        loaderGif.setImageResource(R.drawable.pin_144)
    }

    fun requestLocation(view: View, callback: (PermissionGrantedResponse) -> Unit) {
        val deniedDialog = SnackbarOnDeniedPermissionListener.Builder
                .with(view as ViewGroup, getString(R.string.denied_rational))
                .withOpenSettingsButton(getString(R.string.settings))
                .withDuration(Snackbar.LENGTH_INDEFINITE)
                .build()

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : PermissionListener {
                    override fun onPermissionRationaleShouldBeShown(permission: PermissionRequest?, token: PermissionToken?) {
                        token?.continuePermissionRequest()
                    }

                    override fun onPermissionGranted(response: PermissionGrantedResponse) {
                        callback.invoke(response)
                    }

                    override fun onPermissionDenied(response: PermissionDeniedResponse) {
                        deniedDialog.onPermissionDenied(response)
                    }
                }).check()
    }

    fun getLocation(context: Context, f:(location: Location) -> Unit) {
        val rxLocation = RxLocation(context)

        val locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(5000)
                .setExpirationDuration(30000)

        locationRequest.numUpdates = 1

        rxLocation.location().updates(locationRequest)
                .subscribe({ address ->
                    f.invoke(address)
                }, { _ ->
                    // TODO: error handling
                })
    }

    fun getPlaces(location: Location) {
        val cal = Calendar.getInstance()
        ApiClient.getPlacesObservable(cal)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe({ items ->
                    setItems(items, location)
                    showContentView()
                }, { _ ->
                    // TODO: error handling
                })
    }

    fun showContentView() {
        val base = 1000L
        var delay : Long = base + (1000 * Math.random()).toLong()
        val diff = System.currentTimeMillis() - onCreateAt
        if (diff < delay) {
            delay -= diff
        }

        contentHolder.alpha = 0f
        contentHolder.visibility = View.VISIBLE
        contentHolder.animate().alpha(1f).setStartDelay(delay).start()
        loadingHolder.animate().alpha(0f).setStartDelay(delay).start()
    }

    fun setItems(items : List<EatingPlace>, location: Location) {
        val adapter = EatingPlaceAdapter(this, location)
        adapter.placeList = items
        recyclerView.removeAllViews()
        recyclerView.adapter = adapter
    }

}