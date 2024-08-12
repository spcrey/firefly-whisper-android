package com.spcrey.blog.fragment

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.spcrey.blog.CommentActivity
import com.spcrey.blog.ImageDisplayActivity
import com.spcrey.blog.R
import com.spcrey.blog.UserInfoActivity
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class HomePageFragment : Fragment() {

    companion object {
        private const val TAG = "HomePageFragment"
    }

    private lateinit var view: View

    private val articleAdapter by lazy {
        ArticleAdapter(CachedData.articles)
    }
    private val recyclerView by lazy {
        view.findViewById<RecyclerView>(R.id.recycler_view)
    }
    private val swipeRefreshLayout by lazy {
        view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        this.view = view
        EventBus.getDefault().register(this)

        recyclerView.layoutManager = StaggeredGridLayoutManager(
            1, StaggeredGridLayoutManager.VERTICAL
        )
        articleAdapter.loadMoreModule.isAutoLoadMore = true
        articleAdapter.loadMoreModule.isEnableLoadMoreIfNotFullPage = true
        recyclerView.adapter = articleAdapter

        swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                refreshArticleList()
            }
        }

        articleAdapter.loadMoreModule.setOnLoadMoreListener {
            lifecycleScope.launch {
                loadMoreArticleList()
            }
        }

        articleAdapter.setUserOnClickListener(object : ArticleAdapter.UserOnClickListener{
            override fun onClick(userId: Int, userNickname: String, userAvatarUrl: String) {
                val intent = Intent(context, UserInfoActivity::class.java)
                intent.putExtra("userId", userId)
                intent.putExtra("userNickname", userNickname)
                intent.putExtra("userAvatarUrl", userAvatarUrl)
                startActivity(intent)
            }
        })

        articleAdapter.setIcCommentOnClickListener(object : ArticleAdapter.IcCommentOnClickListener{
            override fun onClick(articleId: Int) {
                val intent = Intent(context, CommentActivity::class.java)
                intent.putExtra("articleId", articleId)
                startActivity(intent)
            }
        })

        articleAdapter.setDeleteOnClickListener(object : ArticleAdapter.DeleteOnClickListener{
            override fun onClick(position: Int) {
                CachedData.articles.removeAt(position)
                articleAdapter.notifyItemRemoved(position)
            }
        })

        articleAdapter.setImageOnclickListener(object : ArticleAdapter.ImageOnclickListener{
            override fun onClick(imageId: Int, imageUrls: List<String>) {
                val intent = Intent(context, ImageDisplayActivity::class.java)
                intent.putExtra("imageId", imageId)
                intent.putStringArrayListExtra("imageUrls", ArrayList(imageUrls))
                startActivity(intent)
            }
        })

        articleAdapter.setIcLikeOnClickListener(object : ArticleAdapter.IcLikeOnClickListener{
            override fun onClick(bgLike: View, articleId: Int, position: Int, status: Boolean?) {
                when (status) {
                    null -> {
                        Toast.makeText(context, "请先登录", Toast.LENGTH_SHORT).show()
                    }
                    true -> {
                        lifecycleScope.launch {
                            bgLike.isEnabled = false
                            CachedData.token?.let { token ->
                                articleUnlike(articleId, token, position)
                            }
                            bgLike.isEnabled = true
                        }
                    } else -> {
                        lifecycleScope.launch {
                            bgLike.isEnabled = false
                            CachedData.token?.let { token ->
                                articleLike(articleId, token, position)
                            }
                            bgLike.isEnabled = true
                        }
                    }
                }
            }
        })
    }

    private suspend fun refreshArticleList() {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.articleList(
                    CachedData.token, 1, 10
                ).await()
                when(commonData.code) {
                    1 -> {
                        CachedData.articles.clear()
                        CachedData.articles.addAll(commonData.data.items)
                        CachedData.articles.shuffle()
                        withContext(Dispatchers.Main) {
                            articleAdapter.notifyDataSetChanged()
                            swipeRefreshLayout.isRefreshing = false
                            articleAdapter.loadMoreModule.loadMoreComplete()
                        }
                        CachedData.currentArticlePageNum = 1
                    } else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "参数错误", Toast.LENGTH_SHORT).show()
                            swipeRefreshLayout.isRefreshing = false
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "请求异常", Toast.LENGTH_SHORT).show()
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    private suspend fun loadMoreArticleList() {
        withContext(Dispatchers.IO) {
            try {
                delay(1000)
                val commonData = ServerApiManager.apiService.articleList(
                    CachedData.token, CachedData.currentArticlePageNum + 1, 10
                ).await()
                when(commonData.code) {
                    1 -> {
                        when (commonData.data.items.isNotEmpty()) {
                            true -> {
                                CachedData.articles.addAll(commonData.data.items)
                                withContext(Dispatchers.Main) {
                                    articleAdapter.notifyDataSetChanged()
                                    articleAdapter.loadMoreModule.loadMoreComplete()
                                }
                                CachedData.currentArticlePageNum += 1
                            } false -> {
                            withContext(Dispatchers.Main) {
                                articleAdapter.loadMoreModule.loadMoreEnd()
                            }
                            }
                        }
                    } else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "参数错误", Toast.LENGTH_SHORT).show()
                            swipeRefreshLayout.isRefreshing = false
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "error: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "请求异常", Toast.LENGTH_SHORT).show()
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }
    }

    suspend fun articleUnlike(articleId: Int, token: String, position: Int) {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.articleUnlike(
                    token,
                    ServerApiManager.ArticleLikeForm(articleId)
                ).await()
                when (commonData.code) {
                    1 -> {
                        withContext(Dispatchers.Main) {
                            val data = articleAdapter.getItem(position)
                            data.likeStatus = false
                            data.likeCount -= 1
                            articleAdapter.notifyDataSetChanged()
                        }
                    } else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "参数错误", Toast.LENGTH_SHORT).show()
                    }
                }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    suspend fun articleLike(articleId: Int, token: String, position: Int) {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.articleLike(
                    token,
                    ServerApiManager.ArticleLikeForm(articleId)
                ).await()
                when (commonData.code) {
                    1 -> {
                        withContext(Dispatchers.Main) {
                            val data = articleAdapter.getItem(position)
                            data.likeStatus = true
                            data.likeCount += 1
                            articleAdapter.notifyDataSetChanged()
                        }
                    } else -> {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "参数错误", Toast.LENGTH_SHORT).show()
                    }
                }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    class ArticleAdapter(data: MutableList<ServerApiManager.Article>):
        BaseQuickAdapter<ServerApiManager.Article, BaseViewHolder>(R.layout.item_article, data),
        LoadMoreModule {

        private fun Context.dpToPx(dp: Int): Int {
            return (dp * resources.displayMetrics.density).toInt()
        }

        interface IcLikeOnClickListener {
            fun onClick(bgLike: View, articleId: Int, position: Int, status: Boolean?)
        }
        private var icLikeOnClickListener: IcLikeOnClickListener? = null

        fun setIcLikeOnClickListener(listener: IcLikeOnClickListener) {
            icLikeOnClickListener = listener
        }

        interface IcCommentOnClickListener {
            fun onClick(articleId: Int)
        }
        private var icCommentOnClickListener: IcCommentOnClickListener? = null

        fun setIcCommentOnClickListener(listener: IcCommentOnClickListener) {
            icCommentOnClickListener = listener
        }

        interface UserOnClickListener {
            fun onClick(userId: Int, userNickname: String, userAvatarUrl: String)
        }
        private var userOnClickListener: UserOnClickListener? = null

        fun setUserOnClickListener(listener: UserOnClickListener) {
            userOnClickListener = listener
        }

        interface DeleteOnClickListener {
            fun onClick(position: Int)
        }
        private var deleteOnClickListener: DeleteOnClickListener? = null
        fun setDeleteOnClickListener(listener: DeleteOnClickListener) {
            deleteOnClickListener = listener
        }

        interface ImageOnclickListener {
            fun onClick(imageId: Int, imageUrls: List<String>)
        }
        private var imageOnclickListener: ImageOnclickListener? = null
        fun setImageOnclickListener(listener: ImageOnclickListener) {
            imageOnclickListener = listener
        }

        private fun addSingleImage(imageUrl: String, frameLayout: FrameLayout) {
            val img = ImageView(context).apply {
                id = View.generateViewId()
            }
            val height = context.dpToPx(216)
            val layoutParams = ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.MATCH_PARENT,
                height
            )
            layoutParams.topMargin = context.dpToPx(9)
            img.layoutParams = layoutParams
            img.setOnClickListener {
                imageOnclickListener?.onClick(0, listOf(imageUrl))
            }
            Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .transform(CenterCrop(), RoundedCorners(context.dpToPx(4)))
                .into(img)
            frameLayout.addView(img)
        }

        private fun addMultiImages(imageUrls: List<String>, frameLayout: FrameLayout) {
            val gridLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            gridLayoutParams.topMargin = context.dpToPx(12)
            val gridLayout = GridLayout(context).apply {
                layoutParams = gridLayoutParams
                columnCount = 3
            }
            for (i in 0..minOf(8, imageUrls.lastIndex)) {
                val imageView = ImageView(context).apply {
                    id = View.generateViewId()
                }
                val length = ((context.resources.displayMetrics.widthPixels - context.dpToPx(64)) / 3).toInt()
                val layoutParams = ConstraintLayout.LayoutParams(length, length)
                if (i%3 != 0) {
                    layoutParams.marginStart = context.dpToPx(9)
                }
                if (i >= 3) {
                    layoutParams.topMargin = context.dpToPx(9)
                }
                imageView.layoutParams = layoutParams
                imageView.setOnClickListener {
                    imageOnclickListener?.onClick(i, imageUrls)
                }
                Glide.with(context)
                    .asBitmap()
                    .load(imageUrls[i])
                    .transform(CenterCrop(), RoundedCorners(context.dpToPx(4)))
                    .into(imageView)
                gridLayout.addView(imageView)
            }
            frameLayout.addView(gridLayout)
        }

        override fun convert(holder: BaseViewHolder, item: ServerApiManager.Article) {
            val imgUserAvatar = holder.getView<ImageView>(R.id.img_user_avatar)
            val textUserNickname = holder.getView<TextView>(R.id.text_user_nickname)
            val textContent = holder.getView<TextView>(R.id.text_content)
            val icLike = holder.getView<ImageView>(R.id.ic_like)
            val bgLike = holder.getView<View>(R.id.bg_like)
            val textLikeCount = holder.getView<TextView>(R.id.text_like_count)
            val bgComment = holder.getView<View>(R.id.bg_comment)
            val textCommentCount = holder.getView<TextView>(R.id.text_comment_count)
            val fragmentImages = holder.getView<FrameLayout>(R.id.fragment_images)
            val icDelete = holder.getView<ImageView>(R.id.ic_delete)

            Glide.with(context)
                .load(item.userAvatarUrl)
                .transform(CircleCrop())
                .into(imgUserAvatar)
            textUserNickname.text = item.userNickname
            textContent.text = item.content
            if (item.likeStatus != null && item.likeStatus == true) {
                icLike.setImageResource(R.drawable.ic_like)
                icLike.alpha = 1f
            } else {
                icLike.setImageResource(R.drawable.ic_unlike)
                icLike.alpha = 0.8f
            }
            if (item.likeCount > 0) {
                textLikeCount.text = item.likeCount.toString()
            } else {
                textLikeCount.text = "点赞"
            }
            if (item.commentCount > 0) {
                textCommentCount.text = item.commentCount.toString()
            } else {
                textCommentCount.text = "评论"
            }

            imgUserAvatar.setOnClickListener {
                userOnClickListener?.onClick(item.userId, item.userNickname, item.userAvatarUrl)
            }
            textUserNickname.setOnClickListener {
                userOnClickListener?.onClick(item.userId, item.userNickname, item.userAvatarUrl)
            }
            bgLike.setOnClickListener {
                icLikeOnClickListener?.onClick(bgLike, item.id, holder.layoutPosition, item.likeStatus)
            }
            bgComment.setOnClickListener {
                icCommentOnClickListener?.onClick(item.id)
            }
            icDelete.setOnClickListener {
                deleteOnClickListener?.onClick(holder.layoutPosition)
            }

            fragmentImages.removeAllViews()
            if (item.imageUrls.size == 1) {
                addSingleImage(item.imageUrls[0], fragmentImages)
            }
            else if (item.imageUrls.size in 2..9) {
                addMultiImages(item.imageUrls, fragmentImages)
            }

        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onArticleAdapterUpdateEvent(event:  ArticleAdapterUpdateEvent) {
        articleAdapter.notifyDataSetChanged()
    }

    class ArticleAdapterUpdateEvent
}