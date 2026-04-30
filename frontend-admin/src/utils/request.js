import axios from 'axios'

// 建立 axios 實例
const service = axios.create({
  // 這裡是後端 API 的基礎路徑，以後改這裡就好
  baseURL: 'http://localhost:8082', 
  timeout: 5000 // 超時時間（5秒沒回應就報錯）
})

// [進階] 請求攔截器：自動幫你把 Token 塞進 Header
service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('employeeToken')
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`
    }
    return config
  },
  error => {
    return Promise.reject(error)
  }
)

export default service