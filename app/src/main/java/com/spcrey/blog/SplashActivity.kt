package com.spcrey.blog

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.method.LinkMovementMethod
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.spcrey.blog.tools.CachedData
import com.spcrey.blog.tools.ServerApiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.system.exitProcess

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "SplashActivity"
        private const val SHARED_PREFERENCE_NAME = "user"
    }

    private val icSplash by lazy {
        findViewById<ImageView>(R.id.ic_splash)
    }
    private val textSplash by lazy {
        findViewById<TextView>(R.id.text_splash)
    }
    private val bgStatementTerm by lazy {
        findViewById<ConstraintLayout>(R.id.bg_statement_term)
    }
    private val textStatementTermContent by lazy {
        findViewById<TextView>(R.id.text_statement_term_content)
    }
    private val btnDisagreeStatementTerm by lazy {
        findViewById<TextView>(R.id.btn_disagree_statement_term)
    }
    private val btnAgreeStatementTerm by lazy {
        findViewById<TextView>(R.id.btn_agree_statement_term)
    }
    private val animSplash  by lazy {
        AnimationUtils.loadAnimation(this, R.anim.anim_splash)
    }
    private val sharedPreferences by lazy {
        getSharedPreferences(SHARED_PREFERENCE_NAME, Context.MODE_PRIVATE)
    }
    private val isAgreeStatementTerm  by lazy {
        sharedPreferences.getBoolean("isAgreeStatementTerm", false)
    }
    private val gson = Gson()

    private val textStatementTermContentString by lazy {
        getString(R.string.content_statement_term)
    }
    private val linkSpannableStringSetter by lazy {
        LinkSpannableStringSetter(
            textStatementTermContent, textStatementTermContentString, this
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        linkSpannableStringSetter.set(
            LinkSpannableText(
                39,
                45,
                "查看用户协议"
            )
        )
        linkSpannableStringSetter.set(
            LinkSpannableText(
                46,
                52,
                "查看隐私政策"
            )
        )
        textStatementTermContent.text = linkSpannableStringSetter.text

        btnAgreeStatementTerm.setOnClickListener {
            val edit = sharedPreferences.edit()
            edit.putBoolean("isAgreeStatementTerm", true)
            edit.apply()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }

        btnDisagreeStatementTerm.setOnClickListener {
            finishAffinity();
            android.os.Process.killProcess(android.os.Process.myPid());
            exitProcess(0);
        }

        icSplash.startAnimation(animSplash)

        animSplash.setAnimationListener(object : Animation.AnimationListener {
            override fun onAnimationStart(animation: Animation?) {}
            override fun onAnimationEnd(animation: Animation?) {
                textSplash.alpha = 1f;
                lifecycleScope.launch {
                    runAfterSplashAnimation()
                }
            }

            override fun onAnimationRepeat(animation: Animation?) {}
        })
    }

    suspend fun runAfterSplashAnimation() {
        withContext(Dispatchers.IO) {
            delay(500);
            withContext(Dispatchers.Main) {
                when(isAgreeStatementTerm) {
                    true -> {
                        CachedData.token = sharedPreferences.getString("token", null)

                        CachedData.multiUserMessageList.userMessageLists.clear()

                        val string = sharedPreferences.getString("userMessageLists", null)


                        val userMessageLists: MutableList<ServerApiManager.UserMessageList> = if (string.isNullOrEmpty()) {
                            mutableListOf()
                        } else {
                            val type = object : TypeToken<MutableList<ServerApiManager.UserMessageList>>() {}.type
                            gson.fromJson(string, type)
                        }

                        CachedData.multiUserMessageList.userMessageLists.clear()
                        userMessageLists.removeIf {
                            it.messages.size == 0
                        }
                        CachedData.multiUserMessageList.userMessageLists.addAll(userMessageLists)
                        CachedData.multiUserMessageList.lastMessageId = sharedPreferences.getInt("lastMessageId", 0)
                        val intent = Intent(this@SplashActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    false -> {
                        icSplash.visibility = View.GONE
                        textSplash.visibility = View.GONE
                        bgStatementTerm.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private class LinkSpannableText(val posStart: Int, val posEnd: Int, val toastString: String)

    private class LinkSpannableStringSetter(
        textView: TextView, contentString: String, val context: Context
    ) {
        private val spannableString = SpannableString(contentString)

        init {
            textView.movementMethod = LinkMovementMethod.getInstance()
        }

        val text: SpannableString
            get() = spannableString

        private fun setClickableSpan(linkSpannableString: LinkSpannableText) {
            spannableString.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        Toast.makeText(context, linkSpannableString.toastString, Toast.LENGTH_SHORT)
                            .show()
                    }
                },
                linkSpannableString.posStart,
                linkSpannableString.posEnd,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        private fun setForegroundColorSpan(linkSpannableText: LinkSpannableText) {
            spannableString.setSpan(
                ForegroundColorSpan(context.getColor(R.color.light_blue)),
                linkSpannableText.posStart,
                linkSpannableText.posEnd,
                Spannable.SPAN_COMPOSING
            )
        }

        private fun setBackgroundColorSpan(linkSpannableText: LinkSpannableText) {
            spannableString.setSpan(
                BackgroundColorSpan(context.getColor(R.color.light_gray)),
                linkSpannableText.posStart,
                linkSpannableText.posEnd,
                Spannable.SPAN_COMPOSING
            )
        }

        fun set(linkSpannableText: LinkSpannableText) {
            setClickableSpan(linkSpannableText)
            setForegroundColorSpan(linkSpannableText)
            setBackgroundColorSpan(linkSpannableText)
        }
    }
}