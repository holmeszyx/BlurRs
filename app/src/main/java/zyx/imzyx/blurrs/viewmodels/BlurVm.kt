package zyx.imzyx.blurrs.viewmodels

import android.app.Application
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.databinding.Observable
import androidx.databinding.ObservableBoolean
import androidx.databinding.ObservableInt
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.*
import zyx.imzyx.blurrs.Blurs

/**
 * Created by holmes on 19-4-16.
 */
class BlurVm(val blurs: Blurs) : ViewModel() {

    val sample = ObservableInt()
    val radius = ObservableInt()
    val ignore = ObservableBoolean(true)
    val srcSize = MutableLiveData<String>()
    val sampleSize = MutableLiveData<String>()

    val bluredDrawable = MutableLiveData<Drawable>()

    private val mainJob = Job()
    val mainScope = CoroutineScope(Dispatchers.Main + mainJob)

    var blurJob: Job? = null
    val blurScope: CoroutineScope
        get() {
            val job = Job()
            blurJob = job
            return CoroutineScope(Dispatchers.Main + job)
        }

    private var picturePath: String? = null
    private var bitmapCache: Bitmap? = null
        set(value) {
            if (field == value) {
                return
            }
            val old = field
            if (old != null && !old.isRecycled) {
                old.recycle()
            }
            field = value
        }

    private var delaySeek: Job? = null

    fun bindSeeks() {
        val blurUpdate = object : Observable.OnPropertyChangedCallback() {
            override fun onPropertyChanged(sender: Observable?, propertyId: Int) {
                delaySeek?.cancel()
                val c = mainScope.launch {
                    delay(600L)
                    if (!isActive) {
                        return@launch
                    }
                    if (!ignore.get()) {
                        blurDirect(picturePath)
                    }
                }
                delaySeek = c
            }
        }
        sample.addOnPropertyChangedCallback(blurUpdate)
        radius.addOnPropertyChangedCallback(blurUpdate)
    }

    fun blur(path: String?, w: Int, height: Int) {
        blurJob?.cancel()
        picturePath = path
        if (path.isNullOrEmpty()) {
            bluredDrawable.value?.also { drawable ->
                // bitmapDrawable recycle bitmap
            }
            bluredDrawable.value = null
            ignore.set(true)
            srcSize.value = null
            sampleSize.value = null
            bitmapCache = null
            return
        }

        blurScope.launch {
            if (w > 0 && height > 0) {
                ignore.set(true)
                val opt = blurs.dumpPicSize(path)
                val s = blurs.computeSampleSize(opt, w, height)
                sample.set(s - 1)
                srcSize.value = "${opt.outWidth}x${opt.outHeight}"
            }

            innerBlur(path)
            ignore.set(false)
        }

    }

    private fun Bitmap?.toSize(): String? {
        return this?.let { "${it.width}x${it.height}" }
    }

    fun blurDirect(path: String?) {
        blurJob?.cancel()
        if (path.isNullOrEmpty()) {
            sampleSize.value = null
            srcSize.value = null
            bitmapCache = null
            return
        }
        blurScope.launch {
            innerBlur(path)
        }
    }

    private suspend fun CoroutineScope.innerBlur(path: String) {
        blurs.setSample(sample.get() + 1)
        blurs.setRadius((radius.get() + 1).toFloat())
        val bitmap = blurs.blur(path)
        if (isActive) {
            if (bitmap != null) {
                bluredDrawable.value = BitmapDrawable(bitmap)
                sampleSize.value = bitmap.toSize()
                bitmapCache = bitmap
            }
        } else {
            if (bitmap?.isRecycled == false) {
                bitmap?.recycle()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        mainJob.cancel()
        blurJob?.cancel()
        bitmapCache = null
    }

}

class BlurVmFactory(val blurs: Blurs, application: Application) :
    ViewModelProvider.AndroidViewModelFactory(application) {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return BlurVm(blurs) as T
    }
}