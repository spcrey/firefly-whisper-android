package com.spcrey.blog

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class CommentActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CommentActivity"
    }

    private val comments: MutableList<ServerApiManager.ArticleComment> = mutableListOf()

    private val commentAdapter by lazy {
        CommentAdapter(comments)
    }
    private val articleId by lazy {
        intent.getIntExtra("articleId", 0)
    }
    private val rootView by lazy {
        findViewById<ConstraintLayout>(R.id.main)
    }
    private val btnComment by lazy {
        findViewById<TextView>(R.id.btn_comment)
    }
    private val recyclerView by lazy {
        findViewById<RecyclerView>(R.id.recycler_view)
    }
    private val editTextContent by lazy {
        findViewById<EditText>(R.id.editText_comment)
    }
    private val icBack by lazy {
        findViewById<ImageView>(R.id.ic_back)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_comment)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        recyclerView.layoutManager = StaggeredGridLayoutManager(
            1, StaggeredGridLayoutManager.VERTICAL
        )
        recyclerView.adapter = commentAdapter

        icBack.setOnClickListener {
            finish()
        }

        rootView.viewTreeObserver.addOnGlobalLayoutListener {
            val r = Rect()
            rootView.getWindowVisibleDisplayFrame(r)
            val screenHeight = rootView.rootView.height
            val keypadHeight: Int = screenHeight - r.bottom
            if (keypadHeight > screenHeight * 0.15) {
                val params = btnComment.layoutParams as ConstraintLayout.LayoutParams
                params.bottomMargin = keypadHeight + dpToPx(12)
                btnComment.layoutParams = params
            } else {
                val params = btnComment.layoutParams as ConstraintLayout.LayoutParams
                params.bottomMargin = dpToPx(12)
                btnComment.layoutParams = params
            }
        }

        btnComment.setOnClickListener {
            CachedData.token?.let { token ->
                val content = editTextContent.text.toString()
                if (content == "") {
                    Toast.makeText(
                        this@CommentActivity,
                        "发布内容不能为空",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    btnComment.isEnabled = false
                    lifecycleScope.launch {
                        articleComment(token, content)
                        btnComment.isEnabled = true
                    }
                }
            } ?: run {
                Toast.makeText(
                    this@CommentActivity,
                    "请先登录",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        lifecycleScope.launch {
            articleListComments()
        }
    }

    class CommentAdapter(data: MutableList<ServerApiManager.ArticleComment>):
        BaseQuickAdapter<ServerApiManager.ArticleComment, BaseViewHolder>(R.layout.item_comment, data) {
        override fun convert(holder: BaseViewHolder, item: ServerApiManager.ArticleComment) {
            val imgUserAvatar = holder.getView<ImageView>(R.id.img_user_avatar)
            Glide.with(context)
                .load(item.userAvatarUrl)
                .transform(CircleCrop())
                .into(imgUserAvatar)
            val textUserNickname = holder.getView<TextView>(R.id.text_user_nickname)
            textUserNickname.text = item.userNickname
            val textContent = holder.getView<TextView>(R.id.text_content)
            textContent.text = item.content
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private suspend fun articleListComments() {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.articleListComments(
                    articleId).await()
                when(commonData.code) {
                    1 -> {
                        comments.clear()
                        comments.addAll(commonData.data)
                        withContext(Dispatchers.Main) {
                            commentAdapter.notifyDataSetChanged()
                        }
                    } else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@CommentActivity, "参数错误", Toast.LENGTH_SHORT).show()
                    }
                }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CommentActivity, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun articleComment(token: String, content: String) {
        editTextContent.text.clear()
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.articleComment(
                    token, ServerApiManager.ArticleCommentForm(content, articleId)
                ).await()
                when (commonData.code) {
                    1 -> {
                        withContext(Dispatchers.Main) {
                            editTextContent.text.clear()
                            articleListComments()
                        }
                    } else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@CommentActivity,
                            "参数错误",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CommentActivity, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun Context.dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }
}