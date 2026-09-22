import {createApp} from 'vue'
import App from './App.vue'
import * as VueRouter from 'vue-router';
import routes from "./config/route";
import Vant from 'vant';
import 'vant/lib/index.css';
import '../global.css'
import './styles/mallchat.css'
import './styles/tailwind.css'
import './styles/tailwind.css'
import {getCurrentUser} from "./services/user";
import {getCurrentUserState} from "./states/user";

const app = createApp(App);
app.use(Vant);

const router = VueRouter.createRouter({
    // 内部提供了 history 模式的实现。为了简单起见，我们在这里使用 hash 模式。
    history: VueRouter.createWebHistory(),
    routes, // `routes: routes` 的缩写
})

router.beforeEach(async (to) => {
    // 登录、注册页不需要登录即可访问
    if (to.path === '/user/login' || to.path === '/user/register') {
        return true;
    }

    let currentUser = getCurrentUserState();
    if (!currentUser) {
        currentUser = await getCurrentUser();
    }

    if (!currentUser) {
        return {
            path: '/user/login',
            query: {
                redirect: to.fullPath,
            },
        };
    }

    return true;
});

app.use(router);
app.mount('#app')
