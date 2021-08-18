package rxhttp.wrapper.parse

import android.content.Context
import android.net.Uri
import okhttp3.Response
import okhttp3.ResponseBody
import rxhttp.wrapper.OkHttpCompat
import rxhttp.wrapper.callback.OutputStreamFactory
import rxhttp.wrapper.callback.ProgressCallback
import rxhttp.wrapper.callback.newOutputStreamFactory
import rxhttp.wrapper.entity.Progress
import rxhttp.wrapper.exception.ExceptionHelper
import rxhttp.wrapper.utils.IOUtil
import rxhttp.wrapper.utils.LogUtil
import java.io.IOException
import java.io.OutputStream

/**
 * User: ljx
 * Date: 2020/9/4
 * Time: 21:39
 */
class StreamParser<T> @JvmOverloads constructor(
    private val osFactory: OutputStreamFactory<T>,
    var progressCallback: ProgressCallback? = null
) : Parser<T> {

    companion object {

        @JvmStatic
        operator fun get(
            destPath: String
        ): StreamParser<String> = StreamParser(newOutputStreamFactory(destPath))

        @JvmStatic
        operator fun get(
            context: Context,
            uri: Uri
        ): StreamParser<Uri> = StreamParser(newOutputStreamFactory(context, uri))
    }

    override fun onParse(response: Response): T {
        val body = ExceptionHelper.throwIfFatal(response)
        val osWrapper = osFactory.getOutputStream(response)
        val result = osWrapper.result
        LogUtil.log(response, result.toString())
        progressCallback?.let {
            response.writeTo(body, osWrapper.os, it)
        } ?: IOUtil.write(body.byteStream(), osWrapper.os)
        return result
    }
}

@Throws(IOException::class)
private fun Response.writeTo(
    body: ResponseBody,
    os: OutputStream,
    callback: ProgressCallback
) {
    val offsetSize = OkHttpCompat.getDownloadOffSize(this)?.offSize ?: 0
    var contentLength = OkHttpCompat.getContentLength(this)
    if (contentLength != -1L) contentLength += offsetSize

    var lastProgress = 0
    var lastSize = 0L
    var lastRefreshTime = 0L

    IOUtil.write(body.byteStream(), os) {
        val currentSize = it + offsetSize
        lastSize = currentSize
        if (contentLength == -1L) {
            //响应头里取不到contentLength，仅回调已下载字节数
            val curTime = System.currentTimeMillis()
            if (curTime - lastRefreshTime > 500) {
                val progress = Progress(0, currentSize, contentLength)
                callback.onProgress(progress)
                lastRefreshTime = curTime
            }
        } else {
            //当前进度 = 当前已读取的字节 * 100 / 总字节
            val currentProgress = ((currentSize * 100 / contentLength)).toInt()
            if (currentProgress > lastProgress) {
                lastProgress = currentProgress
                val progress = Progress(currentProgress, currentSize, contentLength)
                callback.onProgress(progress)
            }
        }
    }
    if (contentLength == -1L) {
        //响应头里取不到contentLength时，保证下载完成事件能回调
        val progress = Progress(100, lastSize, contentLength)
        callback.onProgress(progress)
    }
}