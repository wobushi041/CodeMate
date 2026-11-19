export interface TagCategory {
  text: string
  children: Array<{ text: string; id: string }>
}

export const TAG_CATEGORIES: TagCategory[] = [
  { text: '性别', children: [{ text: '男', id: '男' }, { text: '女', id: '女' }] },
  { text: '年级', children: ['大一', '大二', '大三', '大四', '大五', '研一', '研二', '研三', '博士'].map((text) => ({ text, id: text })) },
  { text: '技术栈', children: ['Vue', 'React', 'Spring Boot', 'MySQL', 'Redis', 'Docker', 'Git', 'Linux', 'LangChain', 'WebSocket'].map((text) => ({ text, id: text })) },
  { text: '编程语言', children: ['Java', 'Python', 'JavaScript', 'TypeScript', 'C', 'C++', 'Go', 'Rust', 'SQL'].map((text) => ({ text, id: text })) },
  { text: '职业方向', children: ['前端开发', '后端开发', '全栈开发', '移动端开发', 'AI应用开发', '算法工程师', '测试开发', '运维开发', '数据分析', '产品经理', 'UI设计'].map((text) => ({ text, id: text })) },
  { text: '学习方向', children: ['前端', '后端', '算法', 'AI', '大数据', '网络安全', '嵌入式', '游戏开发'].map((text) => ({ text, id: text })) },
  { text: '项目经验', children: ['无项目经验', '课程项目', '竞赛项目', '实习项目', '开源项目'].map((text) => ({ text, id: text })) },
  { text: '协作偏好', children: ['线上协作', '线下协作', '长期组队', '短期冲刺', '周末空闲'].map((text) => ({ text, id: text })) },
  { text: '能力阶段', children: ['入门', '进阶', '熟练', '可带队'].map((text) => ({ text, id: text })) },
  { text: '目标方向', children: ['找队友', '找搭子', '刷题', '做项目', '备赛', '求职准备'].map((text) => ({ text, id: text })) },
]
