package com.example.floattest

import API
import android.animation.Animator
import android.animation.ObjectAnimator
import android.content.ClipboardManager
import android.content.Context
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.floattest.databinding.ActivityMainBinding
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

const val REQUEST_CODE_OVERLAY_PERMISSION = 1000
class MainActivity : AppCompatActivity() {
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var binding: ActivityMainBinding
    private lateinit var menuBinding: MenuBinding
    private lateinit var floatWidgetBinding: FloatWidgetBinding
    private lateinit var floatWidgetOnTextingBinding: FloatWidgetOnTextingBinding

    private lateinit var easyFloatWindow: EasyWindow<*>
    private lateinit var menuWindow: EasyWindow<*>

    private var prevX = 0
    private var prevY = 0

    private var isLoading = false

    private val api: API = API()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        easyFloatWindow = EasyWindow.with(application)
        menuWindow = EasyWindow.with(application)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        api.login()
        binding = ActivityMainBinding.inflate(layoutInflater)
        floatWidgetBinding = FloatWidgetBinding.inflate(layoutInflater)
        floatWidgetOnTextingBinding = FloatWidgetOnTextingBinding.inflate(layoutInflater)
        menuBinding = MenuBinding.inflate(layoutInflater)

        // Register the listener for clipboard changes
        clipboardManager.addPrimaryClipChangedListener {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val copiedText = clip.getItemAt(0).text.toString()
                // Handle the copied text (for example, send it to the floating widget)
                handleCopiedText(copiedText)
            }
        }

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        if(checkOverlayPermission(this)){
            print("Floating Action Button Clicked");
            if(!EasyWindow.existShowingByTag("floating_window")){
                //entrypoint
                showFloatWidget()
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
            switchToTextingMode()
        }).setDraggable(getSpringBackDraggable(easyFloatWindow,(0).toFloat()))
    }

    private fun switchToTextingMode() {
        println("Switching to texting mode")
        easyFloatWindow.setContentView(floatWidgetOnTextingBinding.root)
        easyFloatWindow.setOnClickListener(R.id.textView2, EasyWindow.OnClickListener<TextView?> { easyWindow, view ->
            // if the textView is clicked, interrupt the request and return to menu icon
            isLoading = false
            getEasyWindowPosition(easyFloatWindow)
            showFloatWidget()
        })
        startLoadingAnimation()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
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

        val languages = resources.getStringArray(R.array.languages)
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
                floatWidgetOnTextingBinding.textView2.text = "."
                delay(500L) // 500ms delay
                floatWidgetOnTextingBinding.textView2.text = ".."
                delay(500L) // 500ms delay
                floatWidgetOnTextingBinding.textView2.text = "..."
                delay(500L) // 500ms delay
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
    override fun onDestroy() {
        super.onDestroy()
        // Remove the listener when the activity is destroyed
        clipboardManager.removePrimaryClipChangedListener {}
    }
}