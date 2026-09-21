import BasicLayout from '../layouts/BasicLayout.vue';
import Index from '../pages/Index.vue';
import MatchPage from '../pages/MatchPage.vue';
import Team from '../pages/TeamPage.vue';
import UserPage from '../pages/UserPage.vue';
import UserUpdatePage from '../pages/UserUpdatePage.vue';
import UserTeamJoinPage from '../pages/UserTeamJoinPage.vue';
import UserTeamCreatePage from '../pages/UserTeamCreatePage.vue';
import SearchPage from '../pages/SearchPage.vue';
import SearchResultPage from '../pages/SearchResultPage.vue';
import UserLoginPage from '../pages/UserLoginPage.vue';
import UserRegisterPage from '../pages/UserRegisterPage.vue';
import TeamAddPage from '../pages/TeamAddPage.vue';
import TeamUpdatePage from '../pages/TeamUpdatePage.vue';
import AiChatPage from '../pages/AiChatPage.vue';
import TeamChatPage from '../pages/TeamChatPage.vue';

const routes = [
  {
    path: '/',
    component: BasicLayout,
    children: [
      {path: '', component: Index, meta: {title: '主页', headerMode: 'custom'}},
      {path: 'match', component: MatchPage, meta: {title: '匹配', headerMode: 'custom'}},
      {path: 'team', component: Team, meta: {title: '找队伍', headerMode: 'custom'}},
      {path: 'team/add', component: TeamAddPage, meta: {title: '创建队伍', headerMode: 'custom'}},
      {path: 'team/update', component: TeamUpdatePage, meta: {title: '更新队伍', headerMode: 'custom'}},
      {path: 'team/chat', component: TeamChatPage, meta: {title: '队伍聊天室', headerMode: 'custom'}},
      {path: 'user', component: UserPage, meta: {title: '个人信息', headerMode: 'custom'}},
      {path: 'search', component: SearchPage, meta: {title: '搜索', headerMode: 'back'}},
      {path: 'user/list', component: SearchResultPage, meta: {title: '用户列表', headerMode: 'back'}},
      {path: 'user/update', component: UserUpdatePage, meta: {title: '更新信息', headerMode: 'back', tabbar: false}},
      {path: 'user/team/join', component: UserTeamJoinPage, meta: {title: '加入队伍', headerMode: 'back'}},
      {path: 'user/team/create', component: UserTeamCreatePage, meta: {title: '创建队伍', headerMode: 'back'}},
      {path: 'ai/chat', component: AiChatPage, meta: {title: 'AI 编程助手', headerMode: 'custom'}},
    ],
  },
  {path: '/user/login', component: UserLoginPage, meta: {title: '登录', layout: 'auth'}},
  {path: '/user/register', component: UserRegisterPage, meta: {title: '注册', layout: 'auth'}},
];

export default routes;


