package com.example.floattest

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
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.floattest.databinding.ActivityMainBinding
import com.example.floattest.databinding.MenuBinding
import com.hjq.window.EasyWindow
import com.hjq.window.draggable.BaseDraggable
import com.hjq.window.draggable.SpringBackDraggable

const val REQUEST_CODE_OVERLAY_PERMISSION = 1000
class MainActivity : AppCompatActivity() {
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var menuBinding: MenuBinding;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        // Register the listener for clipboard changes
        clipboardManager.addPrimaryClipChangedListener {
            val clip = clipboardManager.primaryClip
            if (clip != null && clip.itemCount > 0) {
                val copiedText = clip.getItemAt(0).text.toString()
                // Handle the copied text (for example, send it to the floating widget)
                handleCopiedText(copiedText)
            }
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener {
            if(checkOverlayPermission(this)){
                print("Floating Action Button Clicked");
                if(!EasyWindow.existShowingByTag("floating_window")){
                    showFloatWidget()
                }
            }
        }
    }

    private fun handleCopiedText(text: String) {
        // For example, update the floating widget or perform other operations
        println("Copied text: $text")

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
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val screenWidth = displayMetrics.widthPixels

        val easyFloatWindow = EasyWindow.with(application)
        val springBackDraggable = SpringBackDraggable(SpringBackDraggable.ORIENTATION_HORIZONTAL)
        springBackDraggable.isAllowMoveToScreenNotch = false
        springBackDraggable.setSpringBackAnimCallback(object : SpringBackDraggable.SpringBackAnimCallback {
            override fun onSpringBackAnimationStart(easyWindow: EasyWindow<*>?, animator: Animator?) {}

            override fun onSpringBackAnimationEnd(easyWindow: EasyWindow<*>?, animator: Animator?) {
                if (easyFloatWindow != null) {
                    easyFloatWindow.decorView?.let { decorView ->
                        if (easyFloatWindow .windowParams.x < screenWidth / 2) {
                            decorView.translationX = (-20).toFloat()
                            val objectAnimator = ObjectAnimator.ofFloat(decorView, "translationX", 0f, (-20).toFloat())
                            objectAnimator.duration = 300
                            objectAnimator.start()
                        } else {
                            decorView.translationX = (20).toFloat()
                            val objectAnimator = ObjectAnimator.ofFloat(decorView, "translationX", 0f, (20).toFloat())
                            objectAnimator.duration = 300
                            objectAnimator.start()
                        }
                    }
                }
            }
        })
        springBackDraggable.setDraggingCallback(object : BaseDraggable.DraggingCallback {
            override fun onStartDragging(easyWindow: EasyWindow<*>?) {
                super.onStartDragging(easyWindow)
                if (easyFloatWindow  != null) {
                    easyFloatWindow .decorView.translationX = (0).toFloat()
                }
            }

            override fun onStopDragging(easyWindow: EasyWindow<*>?) {
                super.onStopDragging(easyWindow)
            }
        })

        easyFloatWindow  // 'this' refers to the current Activity
            .setTag("floating_window")
            .setDraggable(springBackDraggable)
            .setGravity(Gravity.END or Gravity.CENTER)
            .setContentView(R.layout.float_widget)
            .setOnClickListener(android.R.id.icon, EasyWindow.OnClickListener<ImageView?> { easyWindow, view ->
                easyWindow.cancel()
                showMenu()
            })
            .show()
    }
    fun showMenu(){
        val windowManager = windowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        menuBinding = MenuBinding.inflate(layoutInflater)
        val languages = resources.getStringArray(R.array.languages)
        val arrayAdapter = ArrayAdapter(this, R.layout.dropdown_item, languages)
        menuBinding.autoCompleteTextView1.setAdapter(arrayAdapter)
        menuBinding.autoCompleteTextView2.setAdapter(arrayAdapter)
        EasyWindow.with(application)
            .setTag("menu")
            .setGravity(Gravity.BOTTOM)
            .setContentView(menuBinding.root)
            .setOutsideTouchable(true)
            .setWidth(displayMetrics.widthPixels)
            .setOnClickListener(R.id.imageButton, EasyWindow.OnClickListener<ImageView?> { easyWindow, view ->
                easyWindow.cancel()
                showFloatWidget()
            })
            .show()
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

    override fun onDestroy() {
        super.onDestroy()
        // Remove the listener when the activity is destroyed
        clipboardManager.removePrimaryClipChangedListener {}
    }
}