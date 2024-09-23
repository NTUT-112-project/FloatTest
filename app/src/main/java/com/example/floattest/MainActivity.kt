package com.example.floattest

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
const val REQUEST_CODE_OVERLAY_PERMISSION = 1000
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var menuBinding: MenuBinding;
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    EasyWindow.with(application) // 'this' refers to the current Activity
                        .setTag("floating_window")
                        .setDraggable()
                        .setContentView(R.layout.float_widget)
                        .setOnClickListener(android.R.id.icon, EasyWindow.OnClickListener<ImageView?> { easyWindow, view ->
                            easyWindow.cancel()
                            showMenu()
                        })
                        .show()
                }
            }
        }
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
//            .setOnClickListener(R.id.)
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

}