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
                    callback.onFinish()
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