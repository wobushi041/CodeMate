import {createApp} from 'vue'
import App from './App.vue'
import * as VueRouter from 'vue-router';
import routes from "./config/route";
import Vant from 'vant';
import 'vant/lib/index.css';
import '../global.css'
import './styles/mallchat.css'
import './styles/tailwind.css'
import './styles/labverse.css'
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
    const isAuthRoute = to.path === '/user/login' || to.path === '/user/register';

    let currentUser = getCurrentUserState();
    if (!currentUser) {
        currentUser = await getCurrentUser();
    }

    // 1. 已登录用户访问登录/注册页，直接重定向回首页或重定向目标页
    if (currentUser && isAuthRoute) {
        const rawRedirect = to.query?.redirect;
        if (typeof rawRedirect === 'string' && rawRedirect.startsWith('/') && !rawRedirect.startsWith('//') && !rawRedirect.startsWith('/user/login')) {
            return rawRedirect;
        }
        return '/';
    }

    // 2. 未登录用户访问受保护路由，重定向至登录页并保存目标路由
    if (!currentUser && !isAuthRoute) {
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
