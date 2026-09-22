import axios, {AxiosInstance} from "axios";

const isDev = process.env.NODE_ENV === 'development';

const myAxios: AxiosInstance = axios.create({
    baseURL: isDev ? 'http://localhost:8080/api' : '线上地址',
});

myAxios.defaults.withCredentials = true; // 配置为true

// Add a request interceptor
myAxios.interceptors.request.use(function (config) {
    // Do something before request is sent
    return config;
}, function (error) {
    // Do something with request error
    return Promise.reject(error);
});

// Add a response interceptor
myAxios.interceptors.response.use(function (response) {
    // 未登录则跳转到登录页（排除当前已在登录页或注册页，避免循环重定向）
    if (response?.data?.code === 40100) {
        const currentPath = window.location.pathname;
        if (currentPath !== '/user/login' && currentPath !== '/user/register') {
            const redirectPath = window.location.pathname + window.location.search;
            window.location.href = `/user/login?redirect=${encodeURIComponent(redirectPath)}`;
        }
    }
    // Do something with response data
    return response.data;
}, function (error) {
    // Do something with response error
    return Promise.reject(error);
});

export default myAxios;
