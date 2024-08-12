package com.spcrey.blog.fragment

import android.content.Intent
import android.content.res.Resources
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity.MODE_PRIVATE
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import com.google.gson.Gson
import com.spcrey.blog.MessageListActivity
import com.spcrey.blog.R
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.MessageReceiving
import com.spcrey.blog.tools.ServerApiManager
import com.spcrey.blog.tools.TimeTransform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import kotlin.math.min

class MessageUserListFragment : Fragment() {

    companion object {
        private const val TAG = "MessageUserListFragment"
        private const val SHARED_PREFERENCE_NAME = "user"
    }
    private lateinit var view: View

    private val recyclerView by lazy {
        view.findViewById<RecyclerView>(R.id.recycler_view)
    }
    private val swipeRefreshLayout by lazy {
        view.findViewById<SwipeRefreshLayout>(R.id.swipe_refresh)
    }
    private val messageUserAdapter by lazy {
        MessageUserAdapter(CachedData.multiUserMessageList.userMessageLists)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_message_user_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        EventBus.getDefault().register(this)
        this.view = view

        recyclerView.layoutManager = StaggeredGridLayoutManager(
            1, StaggeredGridLayoutManager.VERTICAL
        )
        recyclerView.adapter = messageUserAdapter

        swipeRefreshLayout.setOnRefreshListener {
            lifecycleScope.launch {
                CachedData.token?.let {
                    MessageReceiving.stop()
                    MessageReceiving.setCountCompleteAfterListener(object : MessageReceiving.CountCompleteAfterListener{
                        override suspend fun run() {
                            withContext(Dispatchers.Main) {
                                messageUserAdapter.notifyDataSetChanged()
                                swipeRefreshLayout.isRefreshing = false
                                }
                            MessageReceiving.clearCompleteAfterListener()
                        }
                    })
                    MessageReceiving.current_count = MessageReceiving.max_count
                    MessageReceiving.run()
                } ?: run {
                    swipeRefreshLayout.isRefreshing = false
                }
            }
        }

        messageUserAdapter.setItemOnClickListener(object : MessageUserAdapter.ItemOnClickListener{
            override fun onClick(withUserId: Int) {
                val intent = Intent(context, MessageListActivity::class.java)
                intent.putExtra("withUserId", withUserId)
                startActivity(intent)
            }
        })
    }

    class AdapterUpdateEvent

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAdapterUpdateEvent(event: AdapterUpdateEvent) {
        messageUserAdapter.notifyDataSetChanged()
    }

    class MessageUserAdapter(data: MutableList<ServerApiManager.UserMessageList>):
        BaseQuickAdapter<ServerApiManager.UserMessageList, BaseViewHolder>(R.layout.item_message_user, data) {

        interface ItemOnClickListener {
            fun onClick(withUserId: Int)
        }
        private var itemOnClickListener: ItemOnClickListener? = null

        fun setItemOnClickListener(listener: ItemOnClickListener) {
            itemOnClickListener = listener
        }
        override fun convert(holder: BaseViewHolder, item: ServerApiManager.UserMessageList) {
            if(holder.layoutPosition == 0) {
                val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
                layoutParams.topMargin = dpToPx(6)
                layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
                layoutParams.height = dpToPx(66)
                holder.itemView.layoutParams = layoutParams
            }
            val imgUserAvatar = holder.getView<ImageView>(R.id.img_user_avatar)
            val textUserNickname = holder.getView<TextView>(R.id.text_user_nickname)
            val textTime = holder.getView<TextView>(R.id.text_time)
            val textLastMessage = holder.getView<TextView>(R.id.text_last_message)
            val bgNewMessageNum = holder.getView<View>(R.id.bg_new_message_num)
            val textNewMessageNum = holder.getView<TextView>(R.id.text_new_message_num)
            item.messages.lastOrNull()?.let { message ->
                textTime.text = TimeTransform.transform(message.createTime)
            }

            item.newMessageCount?.let { newMessageCount ->
                bgNewMessageNum.visibility = View.VISIBLE
                textNewMessageNum.text = min(newMessageCount, 99).toString()
                textNewMessageNum.visibility = View.VISIBLE
            } ?: run {
                bgNewMessageNum.visibility = View.GONE
                textNewMessageNum.visibility = View.GONE
            }

            textUserNickname.text = item.userNickname
            val content = item.messages.lastOrNull()?.textContent?:"[图片]"
            textLastMessage.text = content

            Glide.with(context)
                .load(item.userAvatarUrl)
                .transform(CenterCrop(), RoundedCorners(dpToPx(4)))
                .into(imgUserAvatar)

            holder.itemView.setOnClickListener {
                itemOnClickListener?.onClick(item.withUserId)
            }
        }

        private fun dpToPx(dp: Int): Int {
            val density = Resources.getSystem().displayMetrics.density
            return (dp * density).toInt()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}

