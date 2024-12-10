package com.example.floattest

import API
import android.animation.Animator
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.Menu
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.floattest.databinding.FloatWidgetBinding
import com.example.floattest.databinding.FloatWidgetOnTextingBinding
import com.example.floattest.databinding.MenuBinding
import com.hjq.window.EasyWindow
import com.hjq.window.draggable.BaseDraggable
import com.hjq.window.draggable.SpringBackDraggable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

class menuActivity : AppCompatActivity() {

    private lateinit var clipboardManager: ClipboardManager
    private lateinit var menuBinding: MenuBinding
    private lateinit var floatWidgetBinding: FloatWidgetBinding
    private lateinit var floatWidgetOnTextingBinding: FloatWidgetOnTextingBinding

    private lateinit var easyFloatWindow: EasyWindow<*>
    private lateinit var menuWindow: EasyWindow<*>

    private var prevX = 0
    private var prevY = 0

    private var isLoading = false
    private val api: API = API()

    private lateinit var startBtn: ImageButton
    private lateinit var apiToken: String

    private var srcLang: String = ""
    private var destLang: String = ""
    private lateinit var languages: Array<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)
        startBtn = findViewById(R.id.imageButton)

        apiToken = intent.getStringExtra("API_TOKEN").toString()
        languages = resources.getStringArray(R.array.languages)
        // Register the listener for clipboard changes
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboardManager.addPrimaryClipChangedListener {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val copiedText = clip.getItemAt(0).text.toString()
                // Handle the copied text (for example, send it to the floating widget)
                handleCopiedText(copiedText)
            }
        }
        easyFloatWindow = EasyWindow.with(application)
        menuWindow = EasyWindow.with(application)

        floatWidgetBinding = FloatWidgetBinding.inflate(layoutInflater)
        floatWidgetOnTextingBinding = FloatWidgetOnTextingBinding.inflate(layoutInflater)
        menuBinding = MenuBinding.inflate(layoutInflater)

        startBtn.setOnClickListener{
            if(checkOverlayPermission(this)){
                print("Floating Action Button Clicked");
                if(!EasyWindow.existShowingByTag("floating_window")){
                    //entrypoint
                    showFloatWidget()
                }
            }
        }
    }

    private fun handleCopiedText(text: String) {
        easyFloatWindow.decorView?.let { decorView ->
            val objectAnimator = ObjectAnimator.ofFloat(decorView, "translationX", 0f, 0.toFloat())
            objectAnimator.duration = 300
            objectAnimator.start()
        }
        //if something was copied, ready to send the text
        floatWidgetBinding.menuButton.setImageResource(R.drawable.ic_send)
        easyFloatWindow.setOnClickListener(R.id.menuButton, EasyWindow.OnClickListener<ImageView?> { easyWindow, view ->
            //send request with the text to backend
            switchToTextingMode(text)
        }).setDraggable(getSpringBackDraggable(easyFloatWindow,(0).toFloat()))
    }

    private fun switchToTextingMode(text: String) {
        println("Switching to texting mode")
        //TODO: resolve Attempt to invoke virtual method 'void android.view.View.setClickable(boolean)' on a null object reference here
        easyFloatWindow.setContentView(floatWidgetOnTextingBinding.root)
        easyFloatWindow.setOnClickListener(R.id.textView2, EasyWindow.OnClickListener<TextView?> { easyWindow, view ->
            // if the textView is clicked, interrupt the request and return to menu icon
            isLoading = false
            getEasyWindowPosition(easyFloatWindow)
            showFloatWidget()
        })
        var startTime: Long = 0
        api.getLLMTranslate(
            apiToken = apiToken,
            srcLanguage = srcLang,
            distLanguage = destLang,
            srcText = text,
            callback = object: API.LLMTranslateCallback {
                override fun onFailure(e: IOException) {
                    isLoading = false
                    getEasyWindowPosition(easyFloatWindow)
                    showFloatWidget()
                }
                @SuppressLint("SetTextI18n")
                override fun onChunkReceived(chunk: String) {
                    isLoading = false
                    runOnUiThread {
                        floatWidgetOnTextingBinding.textView2.text = chunk
                    }
                }
                override fun onStarted() {
                    startTime = System.nanoTime()
                    runOnUiThread{
                        floatWidgetOnTextingBinding.textView2.text = " "
                    }
                    startLoadingAnimation()
                }
                override fun onFinish() {
                    val endTime = System.nanoTime()
                    val duration = (endTime - startTime) / 1000000 // Convert to milliseconds
                    println("Translation time: $duration ms")
                    isLoading = false
                    runOnUiThread {
                        easyFloatWindow.setOnClickListener(
                            R.id.textView2,
                            EasyWindow.OnClickListener<TextView?> { easyWindow, view ->
                                getEasyWindowPosition(easyFloatWindow)
                                showFloatWidget()
                            })
                    }
                }
            })
    }
    fun showFloatWidget(){
        floatWidgetBinding.menuButton.setImageResource(R.drawable.ic_menu)
        setEasyWindowPosition(easyFloatWindow)
        easyFloatWindow  // 'this' refers to the current Activity
            .setTag("floating_window")
            .setDraggable(getSpringBackDraggable(easyFloatWindow,(50).toFloat()))
//            .setGravity(Gravity.END or Gravity.CENTER)
            .setContentView(floatWidgetBinding.root)
            .setOnClickListener(R.id.menuButton, EasyWindow.OnClickListener<ImageView?> { easyWindow, view ->
                //open large menu, close the floating widget and get current floating widget position
                easyWindow.cancel()
                getEasyWindowPosition(easyFloatWindow)
                showMenu()
            })
        if(!EasyWindow.existShowingByTag("floating_window")){
            easyFloatWindow.show()
        }
    }

    private fun getEasyWindowPosition(targetEasyWindow: EasyWindow<*>) {
        prevX = targetEasyWindow.windowParams.x
        prevY = targetEasyWindow.windowParams.y
        println("get position:\n prevX: $prevX, prevY: $prevY")
    }
    private fun setEasyWindowPosition(targetEasyWindow: EasyWindow<*>) {
        targetEasyWindow.windowParams.x = prevX
        targetEasyWindow.windowParams.y = prevY
        println("set position:\n prevX: $prevX, prevY: $prevY")
    }

    fun showMenu(){
        val windowManager = windowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val arrayAdapter = ArrayAdapter(this, R.layout.dropdown_item, languages)
        menuBinding.autoCompleteTextView1.setAdapter(arrayAdapter)
        menuBinding.autoCompleteTextView2.setAdapter(arrayAdapter)
        menuWindow
            .setTag("menu")
            .setGravity(Gravity.BOTTOM)
            .setContentView(menuBinding.root)
            .setOutsideTouchable(true)
            .setWidth(displayMetrics.widthPixels)
            .setOnClickListener(R.id.close_spot, EasyWindow.OnClickListener<View?> { easyWindow, view ->
                //close the large menu and open the floating widget
                srcLang=menuBinding.autoCompleteTextView1.text.toString()
                destLang=menuBinding.autoCompleteTextView2.text.toString()

                print("languages src: $srcLang dest: $destLang")
                easyWindow.cancel()
                showFloatWidget()
            })
        if(!EasyWindow.existShowingByTag("menu")){
            menuWindow.show()
        }
    }
    fun checkOverlayPermission(activity: Activity): Boolean {
        if (!Settings.canDrawOverlays(activity)) {
            // If permission is not granted, show a message and navigate to settings
            Toast.makeText(activity, "Overlay permission is required", Toast.LENGTH_SHORT).show()

            // Create an intent to navigate to the overlay permission settings page
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${activity.packageName}")
            )
            activity.startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
        }
        return Settings.canDrawOverlays(activity)
    }

    private fun startLoadingAnimation() {
        // Use a Coroutine to update the text asynchronously
        isLoading = true
        CoroutineScope(Dispatchers.Main).launch {
            while (isLoading) {
                floatWidgetOnTextingBinding.textView2.text = " ."
                delay(300L) // 500ms delay
                floatWidgetOnTextingBinding.textView2.text = " .."
                delay(300L) // 500ms delay
                floatWidgetOnTextingBinding.textView2.text = " ..."
                delay(300L) // 500ms delay
            }
        }
    }
    private fun getSpringBackDraggable(targetEasyWindow: EasyWindow<*>, hideRange: Float): BaseDraggable {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        val springBackDraggable = SpringBackDraggable(SpringBackDraggable.ORIENTATION_HORIZONTAL)
        springBackDraggable.isAllowMoveToScreenNotch = false
        springBackDraggable.setSpringBackAnimCallback(object : SpringBackDraggable.SpringBackAnimCallback {
            override fun onSpringBackAnimationStart(easyWindow: EasyWindow<*>?, animator: Animator?) {}

            override fun onSpringBackAnimationEnd(easyWindow: EasyWindow<*>?, animator: Animator?) {
                targetEasyWindow.decorView?.let { decorView ->
                    if (targetEasyWindow.windowParams.x < screenWidth / 2) {
                        decorView.translationX = (-20).toFloat()
                        val objectAnimator = ObjectAnimator.ofFloat(decorView, "translationX", 0f, hideRange*-1)
                        objectAnimator.duration = 300
                        objectAnimator.start()
                    } else {
                        decorView.translationX = (20).toFloat()
                        val objectAnimator = ObjectAnimator.ofFloat(decorView, "translationX", 0f, hideRange)
                        objectAnimator.duration = 300
                        objectAnimator.start()
                    }
                }
            }
        })
        springBackDraggable.setDraggingCallback(object : BaseDraggable.DraggingCallback {
            override fun onStartDragging(easyWindow: EasyWindow<*>?) {
                super.onStartDragging(easyWindow)
                targetEasyWindow .decorView.translationX = (0).toFloat()
            }

            override fun onStopDragging(easyWindow: EasyWindow<*>?) {
                super.onStopDragging(easyWindow)
            }
        })
        return springBackDraggable
    }
}