import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException

class API{

    // Define your OkHttp client
    val client = OkHttpClient()

    val route = "http://192.168.0.100:8081"
    // Create the request body
    val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun login(){
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
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    println("Response: $responseBody")
                }
            }
        })
    }
}