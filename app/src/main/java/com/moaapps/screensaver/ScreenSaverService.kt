package com.moaapps.screensaver

import android.os.Environment
import android.os.Handler
import android.service.dreams.DreamService
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import org.json.JSONArray
import org.json.JSONException
import java.io.File
import java.util.*
import kotlin.jvm.Throws


class ScreenSaverService : DreamService() {
    private val TAG = "DreamSceneService"

    /**
     * URL for finding all wallpapers. By doing this instead of hardcoding a value, new wallpapers
     * does not require a new install of the app
     */
    private val JSON_URL = "https://raw.githubusercontent.com/klinker41/android-dreamscene/master/backgrounds.json"

    /**
     * Max time in milliseconds that a switch could occur
     */
    private val MAX_SWITCH_TIME = 10000 // 40 seconds


    /**
     * Min time in milliseconds that a switch could occur
     */
    private val MIN_SWITCH_TIME = 5000 // 20 seconds


    private var backgrounds: JSONArray? = null
    private var handler: Handler? = null
    private var background: ImageView? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        // add initial options
        isInteractive = false
        isFullscreen = true
        isScreenBright = true

        // show the view on the screen
        setContentView(R.layout.daydream_service)

        // set up the background image
        background = findViewById<View>(R.id.imageView) as ImageView

        // set the initial background
        handler = Handler()
        initBackgrounds()
    }

    /**
     * Start a thread that fetches a JSONArray of all wallpapers, then set the first one
     */
    private fun initBackgrounds() {
        Thread(Runnable {
            try {
//                val elements = "[\"https://raw.githubusercontent.com/klinker41/android-dreamscene/master/images/1.jpg\",\n" +
//                        "  \"https://raw.githubusercontent.com/klinker41/android-dreamscene/master/images/2.jpg\",\n" +
//                        "  \"https://raw.githubusercontent.com/klinker41/android-dreamscene/master/images/3.jpg\",\n" +
//                        "  \"https://raw.githubusercontent.com/klinker41/android-dreamscene/master/images/4.jpg\"]"
//                val elements = "[\"/storage/emulated/0/ScreenSaver/1.jpg\", \"/storage/emulated/0/ScreenSaver/4.jpg\"," +
//                        "\"/storage/emulated/0/ScreenSaver/5.jpg\", \"/storage/emulated/0/ScreenSaver/6.jpg\", " +
//                        "\"https://www.cricket.com.au/~/-/media/News/2018/04/24sachin1.ashx?w=1600\"]"
                val elements = ArrayList<String>()
                val file = File(Environment.getExternalStorageDirectory(),"Screensaver")
                if (file.exists() && file.isDirectory){
                    for (file in file.listFiles()){
                        if (file != null){
                            elements.add("\""+ file.absolutePath.toString() + "\"" )
                        }
                    }
                }
                var jsonArrayString = elements.joinToString(prefix = "[", postfix = "]", separator = ",")
                backgrounds = JSONArray(jsonArrayString)
                Log.v(TAG, "found JSONArray: $jsonArrayString")
                handler!!.post { switchBackground() }
            } catch (e: JSONException) {
                Log.wtf(TAG, "something wrong with backgrounds json :(", e)
            }
        }).start()
    }


    private fun switchBackground() {
        try {
            Glide.with(this).load(getRandomBackgroundUrl()).into(background)
        } catch (e: JSONException) {
            Log.e(TAG, "Error switching backgrounds", e)
        }

        // creates a continuous loop that goes forever, until the daydream is killed
        handler?.postDelayed(Runnable { switchBackground() }, getRandomSwitchTime().toLong())
    }

    @Throws(JSONException::class)
    private fun getRandomBackgroundUrl(): String? {
        // TODO keep a reference to the last wallpaper in memory so that we don't set it twice
        // in a row
        val r = Random()
        val num = backgrounds?.length()?.let { r.nextInt(it) }
        val background: String? = num?.let { backgrounds?.getString(it) }
        Log.v(TAG, "displaying new background: $background")
        return background
    }

    private fun getRandomSwitchTime(): Int {
        val r = Random()
        return r.nextInt(MAX_SWITCH_TIME - MIN_SWITCH_TIME) + MIN_SWITCH_TIME
    }
}