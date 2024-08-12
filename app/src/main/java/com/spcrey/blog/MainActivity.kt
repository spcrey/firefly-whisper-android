package com.spcrey.blog

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.spcrey.blog.fragment.HomePageFragment
import com.spcrey.blog.fragment.MessageUserListFragment
import com.spcrey.blog.fragment.MineFragment
import com.spcrey.blog.fragment.MineFragment.Companion
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.MessageReceiving
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val SHARED_PREFERENCE_NAME = "user"
    }

    private val textTitleBar by lazy {
        findViewById<TextView>(R.id.text_title_bar)
    }
    private val bgHomePage by lazy {
        findViewById<View>(R.id.bg_home_page)
    }
    private val bgMessageList by lazy {
        findViewById<View>(R.id.bg_message_list)
    }
    private val bgMine by lazy {
        findViewById<View>(R.id.bg_mine)
    }
    private val icHomePage by lazy {
        findViewById<ImageView>(R.id.ic_home_page)
    }
    private val icMessageList by lazy {
        findViewById<ImageView>(R.id.ic_message_list)
    }
    private val icMine by lazy {
        findViewById<ImageView>(R.id.ic_mine)
    }
    private val textHomePage by lazy {
        findViewById<TextView>(R.id.text_home_page)
    }
    private val textMessageList by lazy {
        findViewById<TextView>(R.id.text_message_list)
    }
    private val textMine by lazy {
        findViewById<TextView>(R.id.text_mine)
    }
    private val icFollower by lazy {
        findViewById<ImageView>(R.id.ic_follower)
    }
    private val sharedPreferences by lazy {
        getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE)
    }
    private val gson = Gson()

    override fun onDestroy() {
        super.onDestroy()
        MessageReceiving.clearCompleteListener()
    }

    enum class Fragment {
        HOME_PAGE, MESSAGE_LIST, MINE
    }

    var fragment = Fragment.HOME_PAGE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        CachedData.token = sharedPreferences.getString("token", null)

        MessageReceiving.setCountCompleteListener(object : MessageReceiving.CountCompleteListener{
            override suspend fun run() {
                withContext(Dispatchers.Main) {
                    CachedData.token?.let { token ->
                        messageList(token)
                    }
                }
            }
        })

        CachedData.token?.let {
            MessageReceiving.run()
        }

        CachedData.token?.let { token ->
            lifecycleScope.launch {
                userInfo(token)
            }
        }
        lifecycleScope.launch {
            refreshArticleList()
        }

        supportFragmentManager.beginTransaction().setReorderingAllowed(true).add(
            R.id.fragment_content, HomePageFragment::class.java, null, TAG
        ).commit()

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                delay(100)
                EventBus.getDefault().post(HomePageFragment.ArticleAdapterUpdateEvent())
            }
        }

        icFollower.setOnClickListener {
            val intent = Intent(this, FollowedActivity::class.java)
            startActivity(intent)
        }

        bgHomePage.setOnClickListener {
            textTitleBar.text = getString(R.string.title_home)
            icHomePage.alpha = 0.8f
            icMessageList.alpha = 0.2f
            icMine.alpha = 0.2f
            textHomePage.alpha = 0.8f
            textMessageList.alpha = 0.2f
            textMine.alpha = 0.2f
            fragment = Fragment.HOME_PAGE
            icFollower.visibility = View.GONE
            supportFragmentManager.beginTransaction().setReorderingAllowed(true).replace(
                R.id.fragment_content, HomePageFragment::class.java, null, TAG
            ).commit()
        }

        bgMessageList.setOnClickListener {
            textTitleBar.text = getString(R.string.title_message_list)
            icHomePage.alpha = 0.2f
            icMessageList.alpha = 0.8f
            icMine.alpha = 0.2f
            textHomePage.alpha = 0.2f
            textMessageList.alpha = 0.8f
            textMine.alpha = 0.2f
            fragment = Fragment.MESSAGE_LIST
            icFollower.visibility = View.VISIBLE
            supportFragmentManager.beginTransaction().setReorderingAllowed(true).replace(
                R.id.fragment_content, MessageUserListFragment::class.java, null, TAG
            ).commit()
        }

        bgMine.setOnClickListener {
            textTitleBar.text = getString(R.string.title_mine)
            icHomePage.alpha = 0.2f
            icMessageList.alpha = 0.2f
            icMine.alpha = 0.8f
            textHomePage.alpha = 0.2f
            textMessageList.alpha = 0.2f
            textMine.alpha = 0.8f
            fragment = Fragment.MINE
            supportFragmentManager.beginTransaction().setReorderingAllowed(true).replace(
                R.id.fragment_content, MineFragment::class.java, null, TAG
            ).commit()
            icFollower.visibility = View.GONE
        }
    }

    private suspend fun userInfo(token: String) {
        withContext(Dispatchers.IO) {
            try {
                val commonData = ServerApiManager.apiService.userInfo(token).await()
                when (commonData.code) {
                    1 -> {
                        CachedData.user = commonData.data
                        Log.d(TAG, CachedData.user.toString())
                        EventBus.getDefault().post(MineFragment.UserLoginEvent())
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                this@MainActivity, "参数错误", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun messageList(token: String) {
        withContext(Dispatchers.IO) {
            try {
                val commonData =
                    ServerApiManager.apiService.messageList(token, CachedData.multiUserMessageList.lastMessageId?:0).await()
                when (commonData.code) {
                    1 -> {
                        CachedData.multiUserMessageList.userMessageLists.addAllByWithUserId(commonData.data.userMessageLists)
                        commonData.data.lastMessageId?.let { lastMessageId ->
                            CachedData.multiUserMessageList.lastMessageId = lastMessageId
                            withContext(Dispatchers.Main) {
                                val edit = sharedPreferences.edit()
                                edit.putString("userMessageLists", gson.toJson(CachedData.multiUserMessageList.userMessageLists))
                                edit.putInt("lastMessageId", lastMessageId)
                                edit.apply()
                                EventBus.getDefault().post(MessageUserListFragment.AdapterUpdateEvent())
                                EventBus.getDefault().post(MessageListActivity.MessageUpdateEvent())
                            }
                        }
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "参数错误", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "请求异常:message", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun refreshArticleList() {
        withContext(Dispatchers.IO) {
            try {
                val commonData =
                    ServerApiManager.apiService.articleList(CachedData.token, 1, 10).await()
                when (commonData.code) {
                    1 -> {
                        CachedData.articles.clear()
                        CachedData.articles.addAll(commonData.data.items)
                        CachedData.currentArticlePageNum = 1
                        EventBus.getDefault().post(HomePageFragment.ArticleAdapterUpdateEvent())
                    }
                    else -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "参数错误", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "request failed: ${e.message}")
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "请求异常", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

private fun MutableList<ServerApiManager.UserMessageList>.addAllByWithUserId(userMessageLists: MutableList<ServerApiManager.UserMessageList>) {
    for (addUserMessageList in userMessageLists) {
        val existingMessageList = this.find { it.withUserId  == addUserMessageList.withUserId }
        existingMessageList?.let {
            if(CachedData.currentUserId!=existingMessageList.withUserId) {
                existingMessageList.newMessageCount = existingMessageList.newMessageCount?.plus(
                    addUserMessageList.messages.size
                ) ?: addUserMessageList.messages.size
            }
            existingMessageList.messages.removeIf {
                it.id == 0
            }
            existingMessageList.messages.addAll(addUserMessageList.messages)
            existingMessageList.lastMessageId = addUserMessageList.lastMessageId
        } ?: run {
            if(CachedData.currentUserId!=addUserMessageList.withUserId) {
                addUserMessageList.newMessageCount = addUserMessageList.messages.size
            }
            this.add(addUserMessageList)
        }

        this.sortByDescending {
            it.lastMessageId
        }
    }
}