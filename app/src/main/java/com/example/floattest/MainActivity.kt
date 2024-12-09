package com.example.floattest

import API
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.EditText
import java.io.IOException

const val REQUEST_CODE_OVERLAY_PERMISSION = 1000
class MainActivity : AppCompatActivity() {
    lateinit var usernameInput: EditText
    lateinit var passwordInput: EditText
    lateinit var loginBtn: Button
    private val api: API = API()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        usernameInput = findViewById(R.id.username_input)
        passwordInput = findViewById(R.id.password_input)
        loginBtn = findViewById(R.id.login_btn)

        loginBtn.setOnClickListener{
            val username = usernameInput.text.toString()
            val password = passwordInput.text.toString()
            api.login(object : API.LoginCallback {
                override fun onFailure(e: IOException) {
                    println("failed")
                }
                override fun onSuccess(apiToken: String) {
                    println("my api token is $apiToken")
                    val intent = Intent(this@MainActivity, menuActivity::class.java)
                    intent.putExtra("API_TOKEN", apiToken)
                    startActivity(intent)
                }
            }, username, password)
        }
    }
}