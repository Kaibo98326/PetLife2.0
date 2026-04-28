import axios from "axios";

const instance = axios.create({
    baseURL: '/api'
});


//請求攔截器: 自動加上 JWT Token

instance.interceptors.request.use(config => {

    const token = localStorage.getItem('jwtToken')
    if(token){
        config.headers.Authorization = `Bearer ${token}`
    }
    return config

});


export default instance;