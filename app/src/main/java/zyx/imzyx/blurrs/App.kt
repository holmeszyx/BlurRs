package zyx.imzyx.blurrs

import android.app.Application
import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade
import com.guoxiaoxing.phoenix.core.listener.ImageLoader
import com.guoxiaoxing.phoenix.picker.Phoenix

/**
 * Created by holmes on 19-4-16.
 */
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Phoenix.config()
            .imageLoader(object: ImageLoader {

                override fun loadImage(context: Context?, imageView: ImageView?, imagePath: String?, type: Int) {
                    if (imageView == null || imagePath == null) {
                        return
                    }
                    Glide.with(imageView)
                        .load(imagePath)
                        .transition(withCrossFade())
                        .into(imageView)
                }

            })
    }

}