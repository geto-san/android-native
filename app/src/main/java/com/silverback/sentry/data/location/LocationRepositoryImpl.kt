package com.silverback.sentry.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class LocationRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationRepository {

    private val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(context) }

    // The runtime permission check happens in the UI layer before this is ever
    // called; MissingPermission is suppressed here rather than duplicating that
    // check in a repository that has no way to launch a permission request itself.
    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Result<GeoLocation> = runCatching {
        val cancellationTokenSource = CancellationTokenSource()
        val location = fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            cancellationTokenSource.token,
        ).await() ?: error("Location unavailable - check that GPS is enabled")
        GeoLocation(location.latitude, location.longitude, location.accuracy)
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): String? {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                geocodeAsync(geocoder, latitude, longitude)
            } else {
                @Suppress("DEPRECATION")
                geocoder.getFromLocation(latitude, longitude, 1)?.firstOrNull()
            }
            address?.let(::formatAddress)
        } catch (
            @Suppress("TooGenericExceptionCaught")
            e: Exception,
        ) {
            // Reverse geocoding is a nice-to-have (falls back to raw coordinates
            // in the UI) - any failure here (no network, no geocoder service on
            // the device) should never block saving the observation itself.
            Timber.w(e, "Reverse geocoding failed for %f, %f", latitude, longitude)
            null
        }
    }

    // API 33+ deprecated the synchronous getFromLocation() in favor of this
    // listener-based overload.
    private suspend fun geocodeAsync(geocoder: Geocoder, latitude: Double, longitude: Double): Address? =
        suspendCancellableCoroutine { continuation ->
            geocoder.getFromLocation(
                latitude,
                longitude,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        continuation.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        continuation.resume(null)
                    }
                },
            )
        }

    private fun formatAddress(address: Address): String {
        val parts = listOfNotNull(address.featureName, address.subAdminArea, address.adminArea)
            .filter { it.isNotBlank() }
        return parts.joinToString(", ").ifBlank {
            "%.4f, %.4f".format(Locale.US, address.latitude, address.longitude)
        }
    }
}
