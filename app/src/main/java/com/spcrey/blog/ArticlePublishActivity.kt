package com.spcrey.blog

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.spcrey.blog.fragment.HomePageFragment
import com.spcrey.blog.fragment.HomePageFragment.Companion
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.Base64

class ArticlePublishActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "ArticleAddActivity"
        private const val PICK_IMAGE_REQUEST = 1
    }

    private val imgLastImage by lazy {
        findViewById<ImageView>(R.id.img_last_image)
    }
    private val textImgNum by lazy {
        findViewById<TextView>(R.id.text_img_num)
    }
    private val editTextContent by lazy {
        findViewById<EditText>(R.id.editText_content)
    }
    private val btnPublish by lazy {
        findViewById<View>(R.id.btn_publish)
    }
    private val textPublish by lazy {
        findViewById<TextView>(R.id.text_publish)
    }
    private val btnAddImage by lazy {
        findViewById<Button>(R.id.btn_add_image)
    }
    private val icBack by lazy {
        findViewById<ImageView>(R.id.ic_back)
    }

    private val imageUrls: MutableList<String> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_article_publish)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnAddImage.setOnClickListener {
            btnAddImage.isEnabled = false
            galleryLauncher.launch("image/*")
        }

        icBack.setOnClickListener {
            finish()
        }

        suspend fun articleAdd(token: String, content: String) {
            withContext(Dispatchers.IO) {
                try {
                    val commonData = ServerApiManager.apiService.articleAdd(
                        token,
                        ServerApiManager.ArticleAddForm(content, imageUrls)
                    ).await()
                    when (commonData.code) {
                        1 -> {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ArticlePublishActivity, "发布成功", Toast.LENGTH_SHORT).show()
                                finish()
                            }
                        } else -> {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@ArticlePublishActivity, "参数错误", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "request failed: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ArticlePublishActivity, "请求异常", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnPublish.setOnClickListener {
            val content = editTextContent.text.toString()
            if (content == "") {
                Toast.makeText(this, "内容不能为空", Toast.LENGTH_SHORT).show()
            } else {
                CachedData.token?.let { token ->
                    lifecycleScope.launch {
                        btnPublish.isEnabled = false
                        textPublish.text = "发布中"
                        articleAdd(token, content)
                        btnPublish.isEnabled = true
                        textPublish.text = "发布"
                    }
                }
            }
        }
    }

    private val galleryLauncher = registerForActivityResult<String, Uri>(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri ->
            val localPath = getRealPath(imageUri)
            val file = localPath?.let {
                File(it)
            }
            val fileBytes = FileInputStream(file).readBytes()
            val fileBase64 = Base64.getEncoder().encodeToString(fileBytes)
            val layoutParams = imgLastImage.layoutParams
            layoutParams.width = resources.getDimensionPixelSize(R.dimen.dp48)
            imgLastImage.layoutParams = layoutParams
            imgLastImage.alpha = 0.4f
            Glide.with(this)
                .load(imageUri)
                .transform(CenterCrop(), RoundedCorners(dpToPx(12)))
                .into(imgLastImage)
            imageUrls.add(fileBase64)
            textImgNum.text = imageUrls.size.toString()
        }

        btnAddImage.isEnabled = true

    }

    private fun Context.dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun Context.getRealPath(uri: Uri): String? {
        var filePath: String? = null
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            it.moveToFirst()
            filePath = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
        }
        return filePath
    }
}