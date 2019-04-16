package zyx.imzyx.blurrs

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.Observable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.guoxiaoxing.phoenix.core.PhoenixOption
import com.guoxiaoxing.phoenix.core.model.MimeType
import com.guoxiaoxing.phoenix.picker.Phoenix
import com.guoxiaoxing.phoenix.picker.rx.permission.RxPermissions
import io.reactivex.android.schedulers.AndroidSchedulers
import zyx.imzyx.blurrs.databinding.ActivityMainBinding
import zyx.imzyx.blurrs.viewmodels.BlurVm
import zyx.imzyx.blurrs.viewmodels.BlurVmFactory


class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "Blur"
        const val REQUEST_CODE = 9371
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var blurs: Blurs
    private lateinit var vm: BlurVm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        RxPermissions(this)
            .request(Manifest.permission.READ_EXTERNAL_STORAGE)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (Log.isLoggable(TAG, Log.DEBUG)) {
                    Log.d(TAG, "has permission")
                }
            }

        blurs = Blurs(this)
        vm = ViewModelProviders.of(this, BlurVmFactory(blurs, application)).get(BlurVm::class.java)

        binding.vm = vm
        binding.lifecycleOwner = this

        blurs.initRender()
        vm.bindSeeks()

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        blurs.destroy()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu ?: return false
        if (menu.findItem(R.id.select) == null) {
            menuInflater.inflate(R.menu.menu_select_pic, menu)
            return true
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        item ?: return false
        when (item.itemId) {
            R.id.select -> {
                selectPicture()
                return true
            }
            else -> {
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun selectPicture() {
        Phoenix.with()
            .theme(PhoenixOption.THEME_BLUE)// 主题
            .fileType(MimeType.ofImage())//显示的文件类型图片、视频、图片和视频
            .maxPickNumber(1)// 最大选择数量
            .minPickNumber(1)// 最小选择数量
            .spanCount(4)// 每行显示个数
            .enablePreview(true)// 是否开启预览
            .enableCamera(false)// 是否开启拍照
            .enableAnimation(true)// 选择界面图片点击效果
            .enableCompress(false)// 是否开启压缩
            .compressPictureFilterSize(1024)//多少kb以下的图片不压缩
            .compressVideoFilterSize(2018)//多少kb以下的视频不压缩
            .thumbnailHeight(160)// 选择界面图片高度
            .thumbnailWidth(160)// 选择界面图片宽度
            .enableClickSound(false)// 是否开启点击声音
            .pickedMediaList(emptyList())// 已选图片数据
            .videoFilterTime(0)//显示多少秒以内的视频
            .mediaFilterSize(10000)//显示多少kb以下的图片/视频，默认为0，表示不限制
            //如果是在Activity里使用就传Activity，如果是在Fragment里使用就传Fragment
            .start(this, PhoenixOption.TYPE_PICK_MEDIA, REQUEST_CODE)
    }

    private fun onPictureSelected(picture: String) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Select picture: ${picture}")
        }
        val view = binding.picture
        if (view.width > 0 && view.height > 0) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Preview imageView : ${view.width}x${view.height}")
            }
            vm.blur(picture, view.width, view.height)
        } else {
            view.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
                override fun onLayoutChange(
                    v: View?,
                    left: Int,
                    top: Int,
                    right: Int,
                    bottom: Int,
                    oldLeft: Int,
                    oldTop: Int,
                    oldRight: Int,
                    oldBottom: Int
                ) {
                    view.removeOnLayoutChangeListener(this)
                    view.post {
                        onPictureSelected(picture)
                    }
                }

            })
            view.post {
                view.visibility = View.INVISIBLE
                view.requestLayout()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            //返回的数据
            val result = Phoenix.result(data)
            if (result.isNotEmpty()) {
                val picture = result[0].finalPath
                onPictureSelected(picture)
            }
        }
    }


}
