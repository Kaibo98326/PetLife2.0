// src/stores/user.js
import { defineStore } from 'pinia'
import { jwtDecode } from 'jwt-decode'
import Swal from 'sweetalert2'

export const useUserStore = defineStore('user', {
  state: () => ({
    token: null,
    memberId: null,
    user: null,
    cartCount: 0,
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
      this.cartCount = 0
      localStorage.removeItem('jwtToken')
      Swal.fire({
            icon: 'success',
            title: '登出成功',
            text: '歡迎下次再來!'
        }).then(() => {
                router.push('/')
            })
    },
    async initFromLocalStorage() {
      const token = localStorage.getItem('jwtToken')
      if (token) {
        this.login(token)
        await this.fetchUser()
        this.updateCartCount()
      } else {
        this.cartCount = 0
      }
    },
    async updateCartCount() {
      if (!this.memberId) {
        this.cartCount = 0
        return
      }
      try {
        const res = await fetch(`/api/cart/count/${this.memberId}`)
        if (res.ok) {
          this.cartCount = await res.json()
        }
      } catch (e) {
        console.error('更新購物車數量失敗', e)
        this.cartCount = 0
      }
    },
  },
})