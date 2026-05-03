// src/stores/user.js
import { defineStore } from 'pinia'
import { jwtDecode } from 'jwt-decode'


export const useUserStore = defineStore('user', {
  state: () => ({
    token: null,
    memberId: null,
    user: null
  }),
  actions: {
    login(token) {
      this.token = token
      localStorage.setItem('jwtToken', token)

      const decoded = jwtDecode(token)
      this.memberId = parseInt(decoded.sub) 

      this.fetchUser()
    },
    async fetchUser(){
       console.log("🔥 fetchUser triggered")
      const res = await fetch('/api/member/me',{
        headers:{
          Authorization: `Bearer ${this.token}`
        }
      })

      if(!res.ok) return

      const data = await res.json()
      const BASE_URL = 'http://localhost:8082'

      this.user = {
        ...data,
        userImage: data.userImage ? `${BASE_URL}${data.userImage}`
        :null
      }
    },

    logout() {
      this.token = null
      this.memberId = null
      this.user = null
      localStorage.removeItem('jwtToken')
      
    },
    initFromLocalStorage() {
      const token = localStorage.getItem('jwtToken')
      if (token) {
        this.login(token)
        
      }
    }
  }
})
