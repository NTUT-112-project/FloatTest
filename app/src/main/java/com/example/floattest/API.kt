import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import java.io.IOException

class API{

    // Define your OkHttp client
    val client = OkHttpClient()

    val route = "http://10.0.2.2:8080"
    // Create the request body
    val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    interface LoginCallback {
        fun onSuccess(apiToken: String)
        fun onFailure(e: IOException)
    }

    fun login(callback: LoginCallback, username:String, password:String){
        val url = "$route/api/signIn?uid=$username&password=$password"
        val requestBody = FormBody.Builder()
            .build()
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val jsonString = response.body?.string()
                    print("data: $jsonString")
                    if (jsonString.isNullOrEmpty()) {
                        callback.onFailure(IOException("Empty or null response body"))
                        return
                    }

                    val gson = Gson()
                    try {
                        val loginResponse = gson.fromJson(jsonString, LoginResponse::class.java)
                        if (loginResponse.success) {
                            callback.onSuccess(loginResponse.data)
                        } else {
                            callback.onFailure(IOException("Wrong password"))
                        }
                    } catch (e: JsonSyntaxException) {
                        callback.onFailure(IOException("Invalid JSON response", e))
                    }
                } catch (e: IOException) {
                    callback.onFailure(IOException("Error reading response body", e))
                }
            }
        })
    }

    interface LLMTranslateCallback {
        fun onStarted()
        fun onChunkReceived(chunk: String)
        fun onFinish()
        fun onFailure(e: IOException)
    }

    fun getLLMTranslate(apiToken: String, srcLanguage: String, distLanguage: String, srcText: String, callback: LLMTranslateCallback) {
        var url = ""
        if(srcLanguage == ""){
            url = "$route/api/streamTranslate?api_token=$apiToken&apiKey=none"+"&distLanguage=$distLanguage"+"&srcLanguage=$srcLanguage"+"&srcText=$srcText"
        }else{
            url = "$route/api/streamTranslate?api_token=$apiToken&apiKey=none"+"&distLanguage=$distLanguage"+"&srcText=$srcText"
        }

        val requestBody = FormBody.Builder()
            .build()
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()
        callback.onStarted()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onFailure(e)
            }

            override fun onResponse(call: Call, response: Response) {
                // Ensure the response is successful
                if (!response.isSuccessful) {
                    println("Unexpected code $response")
                    return
                }

                // Get the response body as a stream
                val responseBody = response.body ?: return
                val source = responseBody.source()

                try {
                    // Read the stream in chunks
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line()
                        try {
                            val gson = Gson()
                            val responseChunk = gson.fromJson(line, MessageResponse::class.java)
                            println(responseChunk)
                            callback.onChunkReceived(responseChunk.message.content)
                            if (responseChunk.done){
                                callback.onFinish()
                            }

                        } catch (err: Exception){
                            error(err)
                        }
                    }
                } catch (e: IOException) {
                    callback.onFailure(e)
                } finally {
                    responseBody.close() // Always close the response
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

data class MessageResponse(
    val model: String,
    val created_at: String,
    val message: Message,
    val done: Boolean,
    val done_reason: String?,
    val total_duration: Long?,
    val load_duration: Long?,
    val prompt_eval_count: Long?,
    val prompt_eval_duration: Long?,
    val eval_count: Long?,
    val eval_duration: Long?,
)

data class Message(
    val role: String,
    val content: String
)