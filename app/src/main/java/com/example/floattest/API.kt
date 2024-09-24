import com.google.gson.Gson
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException

class API{

    // Define your OkHttp client
    val client = OkHttpClient()

    val route = "http://192.168.0.100:8081"
    // Create the request body
    val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    interface LoginCallback {
        fun onSuccess(apiToken: String)
        fun onFailure(e: IOException)
    }

    fun login(callback: LoginCallback){
        val url = "$route/api/signIn?uid=user&password=user123"
        val requestBody = FormBody.Builder()
            .build()
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("login fucked up")
                callback.onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonString = response.body?.string()
                    val gson = Gson()
                    val loginResponse = gson.fromJson(jsonString, LoginResponse::class.java)
                    callback.onSuccess(loginResponse.data)
                }
            }
        })
    }
}

data class LoginResponse(
    val success: Boolean,
    val data: String,
    val message: String
)