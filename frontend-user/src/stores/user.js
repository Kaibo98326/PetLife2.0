// src/stores/user.js
import { defineStore } from 'pinia'
import { jwtDecode } from 'jwt-decode'


export const useUserStore = defineStore('user', {
  state: () => ({
    token: null,
    memberId: null,
    memberName: ''
  }),
  actions: {
    login(token) {
      this.token = token
      localStorage.setItem('jwtToken', token)

      const decoded = jwtDecode(token)
      console.log('decoded JWT:', decoded)
      this.memberId = parseInt(decoded.sub) 
      this.memberName = decoded.memberName
      this.email = decoded.email
      
    },
    logout() {
      this.token = null
      this.memberId = null
      this.memberName = ''
      this.email = ''
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
