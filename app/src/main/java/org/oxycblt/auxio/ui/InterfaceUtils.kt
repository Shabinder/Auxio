package org.oxycblt.auxio.ui

import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.os.Build
import android.text.Spanned
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.PluralsRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import org.oxycblt.auxio.R
import org.oxycblt.auxio.logE

// --- VIEW CONFIGURATION ---

/**
 * Disable an image button.
 */
fun ImageButton.disable() {
    if (isEnabled) {
        imageTintList = ColorStateList.valueOf(
            R.color.inactive_color.toColor(context)
        )

        isEnabled = false
    }
}

/**
 * Set a [TextView] text color, without having to resolve the resource.
 */
fun TextView.setTextColorResource(@ColorRes color: Int) {
    setTextColor(color.toColor(context))
}

/**
 * Apply accents to a [MaterialButton]
 * @param highlighted Whether the MaterialButton has an "Unimportant" style or not.
 * Required because you cant determine a style of a view before API 29
 */
fun MaterialButton.applyAccents(highlighted: Boolean) {
    val accent = accent.first.toColor(context)

    if (highlighted) {
        backgroundTintList = ColorStateList.valueOf(accent)
    } else {
        setTextColor(accent)
    }
}

// --- CONVENIENCE ---

/**
 * Convenience method for getting a plural.
 * @param pluralsRes Resource for the plural
 * @param value Int value for the plural.
 * @return The formatted string requested
 */
fun Context.getPlural(@PluralsRes pluralsRes: Int, value: Int): String {
    return resources.getQuantityString(pluralsRes, value, value)
}

/**
 * Create a [Toast] from a [String]
 * @param context [Context] required to create the toast
 */
fun String.createToast(context: Context) {
    Toast.makeText(context.applicationContext, this, Toast.LENGTH_SHORT).show()
}

/**
 * Require an [AppCompatActivity]
 */
fun Fragment.requireCompatActivity(): AppCompatActivity {
    val activity = requireActivity()

    if (activity is AppCompatActivity) {
        return activity
    } else {
        error("Required AppCompatActivity, got ${activity::class.simpleName} instead.")
    }
}

/**
 * "Render" a [Spanned] using [HtmlCompat].
 * @return A [Spanned] that actually works.
 */
fun Spanned.render(): Spanned {
    return HtmlCompat.fromHtml(
        this.toString(), HtmlCompat.FROM_HTML_OPTION_USE_CSS_COLORS
    )
}

// --- CONFIGURATION ---

/**
 * Check if edge is on. Really a glorified version check.
 * @return Whether edge is on.
 */
fun isEdgeOn(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1

/**
 * Determine if the device is currently in landscape.
 * @param resources [Resources] required
 */
fun isLandscape(resources: Resources): Boolean {
    return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
}

/**
 * Get the span count for most RecyclerViews when in landscape mode.
 * @return 3 if landscape mode is tablet, 2 if landscape mode is phone
 */
fun getLandscapeSpans(resources: Resources): Int {
    return if (resources.configuration.screenLayout == Configuration.SCREENLAYOUT_SIZE_LARGE) 3 else 2
}

/**
 * Check if we are in the "Irregular" landscape mode [e.g landscape, but nav bar is on the sides]
 * Used to disable most of edge-to-edge if that's the case, as I cant get it to work on this mode.
 * @return True if we are in the irregular landscape mode, false if not.
 */
fun Activity.isIrregularLandscape(): Boolean {
    return isLandscape(resources) &&
        !isSystemBarOnBottom(this)
}

/**
 * Check if the system bars are on the bottom.
 * @return If the system bars are on the bottom, false if no.
 */
@Suppress("DEPRECATION")
private fun isSystemBarOnBottom(activity: Activity): Boolean {
    val realPoint = Point()
    val metrics = DisplayMetrics()

    var width = 0
    var height = 0

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.display?.let { display ->
            display.getRealSize(realPoint)

            activity.windowManager.currentWindowMetrics.bounds.also {
                width = it.width()
                height = it.height()
            }
        }
    } else {
        (activity.getSystemService(Context.WINDOW_SERVICE) as WindowManager).apply {
            defaultDisplay.getRealSize(realPoint)
            defaultDisplay.getMetrics(metrics)

            width = metrics.widthPixels
            height = metrics.heightPixels
        }
    }

    val config = activity.resources.configuration
    val canMove = (width != height && config.smallestScreenWidthDp < 600)

    return (!canMove || width < height)
}

// --- HACKY NIGHTMARES ---

/**
 * Use R E F L E C T I O N to fix a memory leak where mAnimationInfo will keep a reference to
 * its focused view.
 * I can't believe I have to do this.
 */
fun Fragment.fixAnimationInfoMemoryLeak() {
    try {
        Fragment::class.java.getDeclaredMethod("setFocusedView", View::class.java).let {
            it.isAccessible = true
            it.invoke(this, null)
        }
    } catch (e: Exception) {
        logE("mAnimationInfo leak fix failed.")
    }
}
