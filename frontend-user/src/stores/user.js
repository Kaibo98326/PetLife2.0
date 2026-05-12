// src/stores/user.js
import { defineStore } from 'pinia'
import { jwtDecode } from 'jwt-decode'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: null,
    memberId: null,
    user: null,
  }),
  actions: {
    login(token) {
      this.token = token
      localStorage.setItem('jwtToken', token)

      const decoded = jwtDecode(token)
      this.memberId = parseInt(decoded.sub)
    },
    async fetchUser() {
      console.log('🔥 fetchUser triggered')
      const res = await fetch('/api/member/me', {
        headers: {
          Authorization: `Bearer ${this.token}`,
        },
      })

      if (!res.ok) return

      const data = await res.json()
      const BASE_URL = 'http://localhost:8082'

      this.user = {
        ...data,
        userImage: data.userImage ? `${BASE_URL}${data.userImage}` : null,
      }
    },

    logout() {
      this.token = null
      this.memberId = null
      this.user = null
      localStorage.removeItem('jwtToken')
    },
    async initFromLocalStorage() {
      const token = localStorage.getItem('jwtToken')
      if (token) {
        this.login(token)
        await this.fetchUser()
        this.updateCartCount()
      }
    },
    async updateCartCount() {
      if (!this.memberId) return
      try {
        const res = await fetch(`/api/cart/count/${this.memberId}`)
        if (res.ok) {
          this.cartCount = await res.json()
        }
      } catch (e) {
        console.error('更新購物車數量失敗', e)
      }
    },
  },
})