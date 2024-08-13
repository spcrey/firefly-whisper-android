# Firefly Whisper（安卓开发源代码）

**功能列表**

- 闪屏页：展示用户协议，logo跳转
- 文章查看：支持下拉刷新与加载更多功能
- 用户登录：支持验证码登录、密码登录，以及新用户注册
- 用户关注：允许关注其他用户，查看关注列表
- 用户信息：查看其他用户信息，修改个人用户信息
- 点赞评论：对文章进行点赞和取消点赞，发表和查看文章评论

**优化**

- 布局优化：广泛使用约束布局，尽量减少布局嵌套
- 按键防抖：实现按键防抖机制，防止用户快速多次点击造成的意外
- 内存泄漏：在开发阶段持续使用LeakCanary工具进行测试，以检测内存泄漏
- 协程应用：尽可能使用协程进行异步操作，以提升性能并有效防止内存泄漏
